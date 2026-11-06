import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent, within } from '@/testing/test-utils';
import { EstadoMatriculaApoderado, HijoApoderado } from '@/types/api';

import { ConsultaMatricula } from '../consulta-matricula';

// La tarjeta de documentos pide los suyos; aquí no interesan.
beforeEach(() => {
  server.use(
    http.get(
      `${env.API_URL}/matricula/apoderado/estudiantes/:id/documentos`,
      () =>
        HttpResponse.json({
          estado: 'SECCION_RESERVADA',
          puedeSubir: true,
          documentos: [],
        }),
    ),
  );
});

const hijos: HijoApoderado[] = [
  {
    id: 'mateo',
    nombre: 'Mateo Paredes',
    grado: '1.° de primaria',
    estadoMatricula: 'SECCION_RESERVADA',
  },
  {
    id: 'sofia',
    nombre: 'Sofía Paredes',
    grado: '4.° de secundaria',
    estadoMatricula: 'MATRICULADO',
  },
];

const estado: EstadoMatriculaApoderado = {
  estudiante: {
    id: 'mateo',
    nombre: 'Mateo Paredes',
    grado: '1.° de primaria',
  },
  periodo: {
    anio: 2027,
    inicio: '2026-12-01T00:00:00-05:00',
    cierre: '2027-01-31T23:59:59-05:00',
    estado: 'ACTIVO',
    abierto: true,
  },
  matricula: {
    id: 'matricula-mateo',
    estado: 'SECCION_RESERVADA',
    grado: '1.° de primaria',
    seccion: null,
    codigo: null,
    documentosRequeridos: 4,
    documentosEntregados: 0,
    confirmadoEn: null,
  },
  pasoActual: 1,
  puedeContinuar: true,
  impedimentos: [],
  deuda: { total: 0, cuotas: [] },
};

test('muestra el periodo, grado, sección y estado asignados', async () => {
  await renderApp(
    <ConsultaMatricula
      hijos={hijos}
      estudianteId="mateo"
      estado={estado}
      isLoading={false}
      onElegir={vi.fn()}
    />,
  );

  expect(screen.getByText('Periodo abierto')).toBeInTheDocument();
  expect(screen.getAllByText('1.° de primaria')).not.toHaveLength(0);
  expect(screen.getByText('Por definir')).toBeInTheDocument();
  expect(screen.getByText('Sección reservada')).toBeInTheDocument();
  expect(screen.getByText('31 ene 2027')).toBeInTheDocument();
});

test('permite elegir otro estudiante a cargo', async () => {
  const onElegir = vi.fn();
  await renderApp(
    <ConsultaMatricula
      hijos={hijos}
      estudianteId="mateo"
      estado={estado}
      isLoading={false}
      onElegir={onElegir}
    />,
  );

  await userEvent.click(
    screen.getByRole('button', { name: 'Seleccionar estudiante' }),
  );
  await userEvent.click(await screen.findByText('Sofía Paredes'));

  expect(onElegir).toHaveBeenCalledWith('sofia');
});

test('informa cuando el periodo cerró y aún no existe una sección', async () => {
  await renderApp(
    <ConsultaMatricula
      hijos={[{ ...hijos[0], estadoMatricula: null }]}
      estudianteId="mateo"
      estado={{
        ...estado,
        periodo: { ...estado.periodo, abierto: false },
        matricula: null,
        pasoActual: 0,
        impedimentos: [
          { tipo: 'PERIODO', mensaje: 'El periodo de matrícula está cerrado.' },
        ],
      }}
      isLoading={false}
      onElegir={vi.fn()}
    />,
  );

  expect(screen.getByText('Periodo cerrado')).toBeInTheDocument();
  expect(screen.getByText('Por definir')).toBeInTheDocument();
  expect(screen.getByText('Pendiente de asignación')).toBeInTheDocument();
  expect(
    screen.getByText('El periodo de matrícula está cerrado.'),
  ).toBeInTheDocument();
});

test('aclara que la deuda de otro hijo bloquea la matrícula seleccionada', async () => {
  await renderApp(
    <ConsultaMatricula
      hijos={hijos}
      estudianteId="mateo"
      estado={{
        ...estado,
        puedeContinuar: false,
        impedimentos: [
          {
            tipo: 'DEUDA',
            mensaje: 'Tienes una deuda pendiente por regularizar.',
          },
        ],
        deuda: {
          total: 600,
          cuotas: [
            {
              estudianteId: 'sofia',
              estudiante: 'Sofía Paredes',
              concepto: 'Pensión de septiembre 2026',
              fechaVencimiento: '2026-09-30',
              monto: 600,
            },
          ],
        },
      }}
      isLoading={false}
      onElegir={vi.fn()}
    />,
  );

  expect(screen.getByRole('alert')).toHaveTextContent(
    'Mateo Paredes no tiene deuda propia',
  );
  expect(screen.getByRole('alert')).toHaveTextContent(
    'deuda pendiente de Sofía Paredes por S/ 600.00',
  );
  expect(screen.getByText('Pensión de septiembre 2026')).toBeInTheDocument();
  expect(screen.getByText(/Sofía Paredes · Venció el 30/)).toBeInTheDocument();
  expect(screen.getAllByText('S/ 600.00')).toHaveLength(2);
});

test('indica cuando la deuda pertenece al hijo seleccionado', async () => {
  await renderApp(
    <ConsultaMatricula
      hijos={hijos}
      estudianteId="sofia"
      estado={{
        ...estado,
        estudiante: {
          ...estado.estudiante,
          id: 'sofia',
          nombre: 'Sofía Paredes',
        },
        puedeContinuar: false,
        impedimentos: [
          {
            tipo: 'DEUDA',
            mensaje: 'Tienes una deuda pendiente por regularizar.',
          },
        ],
        deuda: {
          total: 600,
          cuotas: [
            {
              estudianteId: 'sofia',
              estudiante: 'Sofía Paredes',
              concepto: 'Pensión de septiembre 2026',
              fechaVencimiento: '2026-09-30',
              monto: 600,
            },
          ],
        },
      }}
      isLoading={false}
      onElegir={vi.fn()}
    />,
  );

  expect(screen.getByRole('alert')).toHaveTextContent(
    'Sofía Paredes tiene una deuda pendiente de S/ 600.00',
  );
});

test('con la documentación validada avisa de la cuota y muestra el pago', async () => {
  await renderApp(
    <ConsultaMatricula
      hijos={hijos}
      estudianteId="mateo"
      estado={{
        ...estado,
        matricula: { ...estado.matricula!, estado: 'PENDIENTE_PAGO' },
        pasoActual: 3,
        pagoMatricula: {
          cuotaId: 'cuota-matricula',
          concepto: 'Matrícula 2027',
          monto: 850,
          fechaVencimiento: '2027-01-31',
          estado: 'PENDIENTE',
          responsable: 'Luis Paredes',
          puedePagar: true,
          motivo: null,
        },
      }}
      isLoading={false}
      onElegir={vi.fn()}
      pagoMatricula={<p>Tarjeta de pago</p>}
    />,
  );

  expect(screen.getByText('Documentación validada')).toBeInTheDocument();
  expect(
    screen.getByText(/Se generó la cuota de matrícula de S\/ 850.00/),
  ).toBeInTheDocument();
  expect(screen.getByText('Tarjeta de pago')).toBeInTheDocument();
});

// Figma > Apoderado > "HU-32 Confirmación de matrícula": ya matriculado, los 4 pasos salen completados.
test('ya matriculado, el avance muestra los 4 pasos completados', async () => {
  await renderApp(
    <ConsultaMatricula
      hijos={hijos}
      estudianteId="mateo"
      estado={{
        ...estado,
        matricula: {
          ...estado.matricula!,
          estado: 'MATRICULADO',
          codigo: 'MAT-2027-0001',
        },
        pasoActual: 4,
      }}
      isLoading={false}
      onElegir={vi.fn()}
    />,
  );

  expect(
    await screen.findByText('Matriculado', { selector: 'p' }),
  ).toBeInTheDocument();
  // Ningún paso queda "en curso" ni muestra su número: todos tienen check.
  const avance = within(
    screen.getByRole('list', { name: 'Avance de la matrícula' }),
  );
  const pasos = avance.getAllByRole('listitem');
  expect(pasos).toHaveLength(4);
  pasos.forEach((paso, i) => {
    expect(paso).not.toHaveAttribute('aria-current');
    expect(within(paso).queryByText(String(i + 1))).not.toBeInTheDocument();
  });
});

test('la constancia y el cronograma solo se ofrecen con la matrícula confirmada', async () => {
  const props = {
    hijos,
    estudianteId: 'mateo',
    isLoading: false,
    onElegir: vi.fn(),
  };
  const { unmount } = await renderApp(
    <ConsultaMatricula {...props} estado={estado} />,
  );
  expect(
    screen.queryByRole('button', { name: /Descargar constancia/ }),
  ).not.toBeInTheDocument();
  unmount();

  await renderApp(
    <ConsultaMatricula
      {...props}
      estado={{
        ...estado,
        matricula: {
          ...estado.matricula!,
          estado: 'MATRICULADO',
          seccion: 'A',
          codigo: 'MAT-2027-0001',
          confirmadoEn: '2027-01-10T15:00:00Z',
        },
      }}
    />,
  );
  expect(
    screen.getByRole('button', { name: /Descargar constancia/ }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('link', { name: 'Ver cronograma de pensiones' }),
  ).toHaveAttribute('href', '/app/apoderado/pensiones');
});
