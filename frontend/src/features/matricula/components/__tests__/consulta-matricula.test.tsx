import { renderApp, screen, userEvent } from '@/testing/test-utils';
import { EstadoMatriculaApoderado, HijoApoderado } from '@/types/api';

import { ConsultaMatricula } from '../consulta-matricula';

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
    seccion: 'A',
    codigo: null,
    documentosRequeridos: 4,
    documentosEntregados: 0,
  },
  pasoActual: 1,
  puedeContinuar: true,
  impedimentos: [],
  deuda: { total: 0, cuotas: [] },
};

test('muestra la sección por asignar mientras la matrícula está en proceso', async () => {
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
  expect(screen.getByText('Por asignar')).toBeInTheDocument();
  expect(screen.queryByText('A')).not.toBeInTheDocument();
  expect(screen.getByText('En proceso')).toBeInTheDocument();
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
  expect(screen.getByText('Por asignar')).toBeInTheDocument();
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
