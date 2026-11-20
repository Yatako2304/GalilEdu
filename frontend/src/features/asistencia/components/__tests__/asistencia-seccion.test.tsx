import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { api } from '@/lib/api-client';
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
  justificacion: null,
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

// HU-67 · RF-145: justificación de faltas y tardanzas (columna de AS-05 y modal de Figma).
const diaRegistrado = (fecha: string): AsistenciaDelDia =>
  dia(fecha, {
    registro: {
      id: 'r1',
      version: 0,
      registradoEn: '2026-11-19T08:05:00-05:00',
      registradoPor: 'María Fernández',
      corregidoEn: null,
      corregidoPor: null,
    },
    resumen: { presentes: 1, tardanzas: 1, faltas: 1, sinMarcar: 0, total: 3 },
    estudiantes: [
      fila('m1', 'Ana Lucía', 'García López', 'PRESENTE'),
      fila('m2', 'Diego José', 'Quispe Mendoza', 'TARDANZA', '08:10:00'),
      fila('m3', 'Camila Sofía', 'Torres Vargas', 'FALTA'),
    ],
  });

test('justifica una falta con motivo y detalle desde la columna Justificación', async () => {
  const enviados: FormData[] = [];
  usarDia(diaRegistrado);
  // Bajo jsdom MSW no lee el multipart (ver plantillas.test.ts): se espía el cliente de la API.
  vi.spyOn(api, 'post').mockImplementation(async (url, datos) => {
    expect(url).toMatch(
      /\/asistencia\/secciones\/seccion-1\/dias\/.+\/justificaciones$/,
    );
    enviados.push(datos as FormData);
    const actual = diaRegistrado(url.split('/dias/')[1].split('/')[0]);
    actual.estudiantes[2] = {
      ...actual.estudiantes[2],
      justificacion: {
        motivo: 'ENFERMEDAD',
        detalle: 'Gripe',
        archivoNombre: null,
        registradaEn: '2026-11-19T10:00:00-05:00',
        registradaPor: 'María Fernández',
      },
    };
    return actual;
  });
  await renderApp(<AsistenciaSeccion />);

  await screen.findByRole('columnheader', { name: 'Justificación' });
  expect(
    screen.queryByRole('button', {
      name: /Justificar .* de Ana Lucía García López/,
    }),
  ).not.toBeInTheDocument();
  await userEvent.click(
    screen.getByRole('button', {
      name: 'Justificar falta de Camila Sofía Torres Vargas',
    }),
  );

  const modal = await screen.findByRole('dialog');
  expect(
    within(modal).getByText('Justificar inasistencia'),
  ).toBeInTheDocument();
  expect(within(modal).getByText('Falta registrada')).toBeInTheDocument();
  await userEvent.click(
    within(modal).getByRole('button', { name: 'Guardar justificación' }),
  );
  expect(within(modal).getByText('Elige el motivo.')).toBeInTheDocument();
  expect(enviados).toHaveLength(0);

  await userEvent.selectOptions(
    within(modal).getByLabelText('Motivo'),
    'ENFERMEDAD',
  );
  await userEvent.type(within(modal).getByLabelText('Detalle'), 'Gripe');
  await userEvent.click(
    within(modal).getByRole('button', { name: 'Guardar justificación' }),
  );

  await waitFor(() => expect(enviados).toHaveLength(1));
  expect(enviados[0].get('matriculaId')).toBe('m3');
  expect(enviados[0].get('motivo')).toBe('ENFERMEDAD');
  expect(enviados[0].get('detalle')).toBe('Gripe');
  expect(enviados[0].get('archivo')).toBeNull();
  expect(
    await screen.findByText('Justificación registrada'),
  ).toBeInTheDocument();
  expect(screen.getByText('Justificada')).toBeInTheDocument();
});

test('"Otro" pide el detalle y el sustento solo acepta PDF o imagen', async () => {
  usarDia(diaRegistrado);
  await renderApp(<AsistenciaSeccion />);

  await userEvent.click(
    await screen.findByRole('button', {
      name: 'Justificar tardanza de Diego José Quispe Mendoza',
    }),
  );
  const modal = await screen.findByRole('dialog');
  expect(within(modal).getByText('Justificar tardanza')).toBeInTheDocument();
  await userEvent.selectOptions(within(modal).getByLabelText('Motivo'), 'OTRO');
  await userEvent.click(
    within(modal).getByRole('button', { name: 'Guardar justificación' }),
  );
  expect(
    within(modal).getByText('Describe el motivo en el detalle.'),
  ).toBeInTheDocument();

  const video = new File(['x'], 'video.mp4', { type: 'video/mp4' });
  await userEvent.upload(
    modal.querySelector('input[type="file"]') as HTMLInputElement,
    video,
    { applyAccept: false },
  );
  expect(
    within(modal).getByText('El sustento debe ser PDF, JPG o PNG.'),
  ).toBeInTheDocument();

  const pdf = new File(['%PDF'], 'descanso.pdf', { type: 'application/pdf' });
  await userEvent.upload(
    modal.querySelector('input[type="file"]') as HTMLInputElement,
    pdf,
  );
  expect(within(modal).getByText('descanso.pdf')).toBeInTheDocument();
});

test('el botón de fecha abre el calendario y consulta otro día', async () => {
  const pedidas: string[] = [];
  server.use(
    http.get(`${env.API_URL}/asistencia/secciones`, () =>
      HttpResponse.json([seccion]),
    ),
    http.get(
      `${env.API_URL}/asistencia/secciones/:id/dias/:fecha`,
      ({ params }) => {
        const fecha = params.fecha as string;
        pedidas.push(fecha);
        return HttpResponse.json(
          pedidas.length === 1
            ? diaRegistrado(fecha)
            : dia(fecha, { hoy: pedidas[0], editable: false }),
        );
      },
    ),
  );
  await renderApp(<AsistenciaSeccion />);

  await userEvent.click(
    await screen.findByRole('button', { name: /Elegir otro día/ }),
  );
  const dias = screen
    .getAllByRole('button')
    .filter(
      (b) => /^\d+$/.test(b.textContent ?? '') && !b.hasAttribute('disabled'),
    );
  // El primer día habilitado del mes (siempre es anterior o igual a hoy).
  await userEvent.click(dias[0]);

  await waitFor(() => expect(pedidas).toHaveLength(2));
  expect(pedidas[1] < pedidas[0] || pedidas[1] === pedidas[0]).toBe(true);
  expect(
    await screen.findByText('No se registró la asistencia de este día.'),
  ).toBeInTheDocument();
});
