import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import {
  renderApp,
  screen,
  userEvent,
  waitFor,
  within,
} from '@/testing/test-utils';
import { AsistenciaDelDia, FilaAsistencia } from '@/types/api';

import { AsistenciaSeccion } from '../asistencia-seccion';

const seccion = {
  id: 'seccion-1',
  anio: 2026,
  grado: '1.° de secundaria',
  nombre: 'A',
  cargo: 'TUTOR',
};

const fila = (
  matriculaId: string,
  nombres: string,
  apellidos: string,
  estado: FilaAsistencia['estado'] = null,
  horaLlegada: string | null = null,
): FilaAsistencia => ({
  matriculaId,
  estudianteId: `e-${matriculaId}`,
  nombres,
  apellidos,
  documento: '99006101',
  estado,
  horaLlegada,
  modificado: false,
});

const dia = (
  fecha: string,
  cambios: Partial<AsistenciaDelDia> = {},
): AsistenciaDelDia => ({
  seccion: { ...seccion, cargo: 'TUTOR' },
  fecha,
  hoy: fecha,
  editable: true,
  motivoNoEscolar: null,
  registro: null,
  resumen: { presentes: 0, tardanzas: 0, faltas: 0, sinMarcar: 3, total: 3 },
  estudiantes: [
    fila('m1', 'Ana Lucía', 'García López'),
    fila('m2', 'Diego José', 'Quispe Mendoza'),
    fila('m3', 'Camila Sofía', 'Torres Vargas'),
  ],
  ...cambios,
});

const usarDia = (crear: (fecha: string) => AsistenciaDelDia) => {
  const enviados: unknown[] = [];
  server.use(
    http.get(`${env.API_URL}/asistencia/secciones`, () =>
      HttpResponse.json([seccion]),
    ),
    http.get(
      `${env.API_URL}/asistencia/secciones/:id/dias/:fecha`,
      ({ params }) => HttpResponse.json(crear(params.fecha as string)),
    ),
    http.put(
      `${env.API_URL}/asistencia/secciones/:id/dias/:fecha`,
      async ({ request, params }) => {
        const cuerpo = await request.json();
        enviados.push(cuerpo);
        return HttpResponse.json({
          ...crear(params.fecha as string),
          registro: {
            id: 'r1',
            version: 1,
            registradoEn: '2026-11-19T08:05:00-05:00',
            registradoPor: 'María Fernández',
            corregidoEn: null,
            corregidoPor: null,
          },
        });
      },
    ),
  );
  return enviados;
};

const grupo = (nombre: string) =>
  screen.getByRole('radiogroup', { name: `Asistencia de ${nombre}` });

test('AS-01 a AS-04: avisa a quién falta marcar, exige la hora de la tardanza y guarda', async () => {
  const enviados = usarDia((fecha) => dia(fecha));
  await renderApp(<AsistenciaSeccion />);

  expect(
    await screen.findByText('Aún no registras la asistencia de hoy.'),
  ).toBeInTheDocument();
  expect(screen.getByText('Pendiente')).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'Guardar asistencia' }),
  ).toBeDisabled();

  await userEvent.click(
    within(grupo('Ana Lucía García López')).getByRole('radio', {
      name: 'Presente',
    }),
  );
  await userEvent.click(
    within(grupo('Diego José Quispe Mendoza')).getByRole('radio', {
      name: 'Tardanza',
    }),
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Guardar asistencia' }),
  );
  expect(
    screen.getByText('Falta 1 estudiante por marcar.'),
  ).toBeInTheDocument();
  expect(screen.getByText(/Camila Sofía Torres Vargas\./)).toBeInTheDocument();

  await userEvent.click(
    within(grupo('Camila Sofía Torres Vargas')).getByRole('radio', {
      name: 'Falta',
    }),
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Guardar asistencia' }),
  );
  expect(
    screen.getByText('Indica la hora de llegada de cada tardanza.'),
  ).toBeInTheDocument();

  await userEvent.type(
    screen.getByLabelText('Hora de llegada de Diego José Quispe Mendoza'),
    '0810',
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Guardar asistencia' }),
  );
  const modal = await screen.findByRole('dialog');
  expect(
    within(modal).getByText('¿Guardar la asistencia de hoy?'),
  ).toBeInTheDocument();
  await userEvent.click(
    within(modal).getByRole('button', { name: 'Guardar asistencia' }),
  );

  await waitFor(() => expect(enviados).toHaveLength(1));
  expect(enviados[0]).toEqual({
    version: null,
    asistencias: [
      { matriculaId: 'm1', estado: 'PRESENTE', horaLlegada: null },
      { matriculaId: 'm2', estado: 'TARDANZA', horaLlegada: '08:10' },
      { matriculaId: 'm3', estado: 'FALTA', horaLlegada: null },
    ],
  });
  expect(await screen.findByText('Asistencia registrada')).toBeInTheDocument();
});

test('"Marcar todos como presentes" respeta lo ya marcado', async () => {
  usarDia((fecha) => dia(fecha));
  await renderApp(<AsistenciaSeccion />);
  await screen.findByText('Aún no registras la asistencia de hoy.');

  await userEvent.click(
    within(grupo('Ana Lucía García López')).getByRole('radio', {
      name: 'Falta',
    }),
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Marcar todos como presentes' }),
  );

  expect(
    within(grupo('Ana Lucía García López')).getByRole('radio', {
      name: 'Falta',
    }),
  ).toBeChecked();
  expect(
    within(grupo('Camila Sofía Torres Vargas')).getByRole('radio', {
      name: 'Presente',
    }),
  ).toBeChecked();
  expect(screen.getByText('3 de 3 estudiantes marcados')).toBeInTheDocument();
});

test('AS-05 y AS-06: corrige el mismo día y confirma solo los cambios', async () => {
  const enviados = usarDia((fecha) =>
    dia(fecha, {
      registro: {
        id: 'r1',
        version: 0,
        registradoEn: '2026-11-19T08:05:00-05:00',
        registradoPor: 'María Fernández',
        corregidoEn: null,
        corregidoPor: null,
      },
      resumen: {
        presentes: 2,
        tardanzas: 0,
        faltas: 1,
        sinMarcar: 0,
        total: 3,
      },
      estudiantes: [
        fila('m1', 'Ana Lucía', 'García López', 'PRESENTE'),
        fila('m2', 'Diego José', 'Quispe Mendoza', 'PRESENTE'),
        fila('m3', 'Camila Sofía', 'Torres Vargas', 'FALTA'),
      ],
    }),
  );
  await renderApp(<AsistenciaSeccion />);

  expect(await screen.findByText('Registrada')).toBeInTheDocument();
  expect(screen.getByText(/a las 08:05 · María Fernández/)).toBeInTheDocument();
  expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();

  await userEvent.click(
    screen.getByRole('button', { name: 'Corregir asistencia' }),
  );
  expect(
    screen.getByText('Estás corrigiendo la asistencia que ya guardaste hoy.'),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'Guardar cambios' }),
  ).toBeDisabled();

  await userEvent.click(
    within(grupo('Camila Sofía Torres Vargas')).getByRole('radio', {
      name: 'Tardanza',
    }),
  );
  await userEvent.type(
    screen.getByLabelText('Hora de llegada de Camila Sofía Torres Vargas'),
    '0910',
  );
  expect(screen.getByText('1 cambio sin guardar')).toBeInTheDocument();
  expect(screen.getByText('Modificado')).toBeInTheDocument();

  await userEvent.click(
    screen.getByRole('button', { name: 'Guardar cambios' }),
  );
  const modal = await screen.findByRole('dialog');
  expect(within(modal).getByText('1 cambio')).toBeInTheDocument();
  expect(within(modal).getByText('Tardanza · 09:10')).toBeInTheDocument();
  await userEvent.click(
    within(modal).getByRole('button', { name: 'Confirmar cambios' }),
  );

  await waitFor(() => expect(enviados).toHaveLength(1));
  expect(enviados[0]).toMatchObject({ version: 0 });
  expect(await screen.findByText('Asistencia corregida')).toBeInTheDocument();
});

test('un día que no es escolar se ve sin poder marcar', async () => {
  usarDia((fecha) =>
    dia(fecha, {
      editable: false,
      motivoNoEscolar: 'El sábado no es día de clases.',
    }),
  );
  await renderApp(<AsistenciaSeccion />);

  expect(
    await screen.findByText('El sábado no es día de clases.'),
  ).toBeInTheDocument();
  expect(screen.getByText('Sin registro')).toBeInTheDocument();
  expect(screen.queryByRole('radiogroup')).not.toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'Guardar asistencia' }),
  ).not.toBeInTheDocument();
});

test('sin sección a cargo lo dice', async () => {
  server.use(
    http.get(`${env.API_URL}/asistencia/secciones`, () =>
      HttpResponse.json([]),
    ),
  );
  await renderApp(<AsistenciaSeccion />);

  expect(
    await screen.findByText('No tienes una sección a cargo.'),
  ).toBeInTheDocument();
});
