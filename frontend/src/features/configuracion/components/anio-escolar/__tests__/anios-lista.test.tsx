import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  renderApp,
  screen,
  userEvent,
  within,
} from '@/testing/test-utils';
import {
  AnioEscolarActual,
  AnioEscolarResumen,
  EstadoAnioEscolar,
} from '@/types/api';

import { AniosLista } from '../anios-lista';

// Figma > Personal Administrativo > CI-01 · Año escolar — listado de años (HU-74)

const API = env.API_URL;

const resumen = (
  anio: number,
  estado: EstadoAnioEscolar,
  regimen: AnioEscolarResumen['regimen'] = 'BIMESTRAL',
): AnioEscolarResumen => ({
  id: `anio-${anio}`,
  anio,
  fechaInicio: `${anio}-03-0${anio === 2026 ? 2 : 1}`,
  fechaFin: `${anio}-12-1${anio === 2026 ? 8 : 7}`,
  regimen,
  cantidadPeriodos: regimen === 'BIMESTRAL' ? 4 : 3,
  seccionesActivas: 23,
  estructurasHorarias: 2,
  estado,
});

const actual2026: AnioEscolarActual = {
  id: 'anio-2026',
  anio: 2026,
  fechaInicio: '2026-03-02',
  fechaFin: '2026-12-18',
  regimen: 'BIMESTRAL',
  periodoEnCurso: {
    id: 'p3',
    numero: 3,
    nombre: 'III Bimestre',
    fechaInicio: '2026-08-03',
    fechaFin: '2026-10-09',
    fechaLimiteNotas: '2026-10-16',
    diasParaLimiteNotas: 14,
  },
  periodoNotasAbiertas: null,
  proximoPeriodo: {
    id: 'p4',
    numero: 4,
    nombre: 'IV Bimestre',
    fechaInicio: '2026-10-19',
  },
  seccionesActivas: 23,
  alumnosMatriculados: 672,
  estructurasHorarias: 2,
  codigosEstructuras: ['EH-PRI-26', 'EH-SEC-26'],
  avance: 74,
};

const todos = [
  resumen(2025, 'CERRADO'),
  resumen(2027, 'PLANIFICADO', 'TRIMESTRAL'),
  resumen(2026, 'ACTIVO'),
];

const mostrar = async () =>
  renderApp(<AniosLista />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

test('con un año ACTIVO muestra la tarjeta "Año escolar en curso" con los datos de Figma', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json(todos),
    ),
    http.get(`${API}/configuracion/anios-escolares/actual`, () =>
      HttpResponse.json(actual2026),
    ),
  );
  await mostrar();

  expect(await screen.findByText('Año escolar en curso')).toBeInTheDocument();
  expect(
    screen.getByRole('heading', { name: 'Año escolar 2026' }),
  ).toBeInTheDocument();
  expect(
    screen.getByText('Del 02/03/2026 al 18/12/2026 · Régimen bimestral'),
  ).toBeInTheDocument();
  expect(screen.getByText('III Bimestre')).toBeInTheDocument();
  expect(screen.getByText('03/08/2026 – 09/10/2026')).toBeInTheDocument();
  expect(screen.getByText('Límite para registrar notas')).toBeInTheDocument();
  expect(screen.getByText('16/10/2026')).toBeInTheDocument();
  expect(screen.getByText('III Bimestre · Faltan 14 días')).toBeInTheDocument();
  expect(screen.getByText('672 alumnos matriculados')).toBeInTheDocument();
  expect(screen.getByText('EH-PRI-26 · EH-SEC-26')).toBeInTheDocument();
  expect(
    screen.getByRole('progressbar', { name: 'Avance del año escolar' }),
  ).toBeInTheDocument();
  expect(screen.getByText('74 %')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Ver detalle' })).toHaveAttribute(
    'href',
    '/app/administrativo/anio-escolar/anio-2026',
  );
});

test('entre periodos la tarjeta muestra el próximo periodo y el plazo de notas abierto', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json(todos),
    ),
    http.get(`${API}/configuracion/anios-escolares/actual`, () =>
      HttpResponse.json({
        ...actual2026,
        periodoEnCurso: null,
        periodoNotasAbiertas: {
          id: 'p3',
          numero: 3,
          nombre: 'III Bimestre',
          fechaFin: '2026-10-09',
          fechaLimiteNotas: '2026-10-16',
          diasParaLimiteNotas: 1,
        },
      }),
    ),
  );
  await mostrar();

  expect(await screen.findByText('Próximo periodo')).toBeInTheDocument();
  expect(screen.getByText('IV Bimestre')).toBeInTheDocument();
  expect(screen.getByText('Empieza el 19/10/2026')).toBeInTheDocument();
  expect(screen.getByText('III Bimestre · Falta 1 día')).toBeInTheDocument();
});

test('la tabla lista los años del más reciente al más antiguo, con su estado', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json(todos),
    ),
    http.get(`${API}/configuracion/anios-escolares/actual`, () =>
      HttpResponse.json(actual2026),
    ),
  );
  await mostrar();

  expect(await screen.findByText('3 años registrados')).toBeInTheDocument();
  const tabla = screen.getByRole('table');
  expect(
    within(tabla)
      .getAllByRole('columnheader')
      .map((c) => c.textContent),
  ).toEqual([
    'Año',
    'Fechas',
    'Régimen',
    'Periodos',
    'Secciones activas',
    'Estructuras horarias',
    'Estado',
    '',
  ]);

  const filas = within(tabla).getAllByRole('row').slice(1);
  expect(filas).toHaveLength(3);
  expect(within(filas[0]).getByText('2027')).toBeInTheDocument();
  expect(
    within(filas[0]).getByText('01/03/2027 – 17/12/2027'),
  ).toBeInTheDocument();
  expect(within(filas[0]).getByText('Trimestral')).toBeInTheDocument();
  expect(within(filas[0]).getByText('3 trimestres')).toBeInTheDocument();
  expect(within(filas[0]).getByText('Planificado')).toBeInTheDocument();
  expect(within(filas[1]).getByText('2026')).toBeInTheDocument();
  expect(within(filas[1]).getByText('4 bimestres')).toBeInTheDocument();
  expect(within(filas[1]).getByText('Activo')).toBeInTheDocument();
  expect(within(filas[2]).getByText('2025')).toBeInTheDocument();
  expect(within(filas[2]).getByText('Cerrado')).toBeInTheDocument();
});

test('el menú ⋮ de una fila solo ofrece "Ver detalle"', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([resumen(2027, 'PLANIFICADO')]),
    ),
  );
  await mostrar();

  await userEvent.click(
    await screen.findByRole('button', { name: 'Acciones del año 2027' }),
  );

  const opciones = await screen.findAllByRole('menuitem');
  expect(opciones.map((o) => o.textContent?.trim())).toEqual(['Ver detalle']);
});

test('sin un año ACTIVO no hay tarjeta y no se consulta el año en curso', async () => {
  // No hay handler de /actual: pedirlo haría fallar la prueba (y mostraría un error 404 como notificación).
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([
        resumen(2027, 'PLANIFICADO'),
        resumen(2025, 'CERRADO'),
      ]),
    ),
  );
  await mostrar();

  expect(await screen.findByText('2 años registrados')).toBeInTheDocument();
  expect(screen.queryByText('Año escolar en curso')).not.toBeInTheDocument();
  expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
});

test('sin años registrados muestra el estado vacío', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([]),
    ),
  );
  await mostrar();

  expect(
    await screen.findByText('Aún no hay años escolares registrados.'),
  ).toBeInTheDocument();
  expect(screen.getByText('0 años registrados')).toBeInTheDocument();
  expect(screen.queryByText('Año escolar en curso')).not.toBeInTheDocument();
});

test('si la lista falla muestra un aviso en la pantalla', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json({ message: 'Error inesperado.' }, { status: 500 }),
    ),
  );
  await mostrar();

  expect(
    await screen.findByText('No pudimos cargar los años escolares.'),
  ).toBeInTheDocument();
  expect(screen.queryByRole('table')).not.toBeInTheDocument();
});
