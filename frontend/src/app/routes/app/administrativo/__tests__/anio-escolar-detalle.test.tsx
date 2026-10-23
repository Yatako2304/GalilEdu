import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import { createUser, renderApp, screen, within } from '@/testing/test-utils';
import { AnioEscolarDetalle, EstadoAnioEscolar } from '@/types/api';

import AdministrativoAnioEscolarDetalleRoute from '../anio-escolar-detalle';

// Figma > Personal Administrativo > CI-03 / CI-06 / CI-09 / CI-12: página del año (HU-74).

const API = env.API_URL;
const RUTA = '/app/administrativo/anio-escolar';

const anio = (
  numero: number,
  estado: EstadoAnioEscolar,
): AnioEscolarDetalle => ({
  id: `anio-${numero}`,
  anio: numero,
  fechaInicio: `${numero}-03-02`,
  fechaFin: `${numero}-12-18`,
  regimen: 'BIMESTRAL',
  estado,
  cantidadSecciones: 23,
  cantidadEstructuras: 2,
  periodos: [1, 2, 3, 4].map((n) => ({
    id: `p${n}`,
    numero: n,
    nombre: `Periodo ${n}`,
    fechaInicio: `${numero}-03-02`,
    fechaFin: `${numero}-05-08`,
    fechaLimiteNotas: `${numero}-05-15`,
    duracionSemanas: 10,
    estado: 'PLANIFICADO',
    registroNotasAbierto: false,
  })),
});

const mostrar = async (id: string) =>
  renderApp(<AdministrativoAnioEscolarDetalleRoute />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
    path: `${RUTA}/:anioId`,
    url: `${RUTA}/${id}`,
  });

test('año ACTIVO: título con su estado, subtítulo, migas, "Cerrar año" y pestañas', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares/anio-2026`, () =>
      HttpResponse.json(anio(2026, 'ACTIVO')),
    ),
  );
  await mostrar('anio-2026');

  expect(
    await screen.findByRole('heading', { name: 'Año escolar 2026' }),
  ).toBeInTheDocument();
  expect(screen.getByText('Activo')).toBeInTheDocument();
  expect(
    screen.getByText(
      'Del 02/03/2026 al 18/12/2026 · Régimen bimestral (4 periodos)',
    ),
  ).toBeInTheDocument();

  const migas = screen.getByRole('navigation', { name: 'Ruta' });
  expect(within(migas).getByText('Configuración')).toBeInTheDocument();
  expect(
    within(migas).getByRole('link', { name: 'Año escolar' }),
  ).toHaveAttribute('href', RUTA);
  expect(within(migas).getByText('2026')).toHaveAttribute(
    'aria-current',
    'page',
  );

  expect(
    screen.getByRole('button', { name: 'Cerrar año' }),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'Editar' }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole('tab', { name: 'Periodos (4)' })).toBeInTheDocument();
  expect(
    screen.getByRole('tab', { name: 'Secciones (23)' }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('tab', { name: 'Estructura horaria (2)' }),
  ).toBeInTheDocument();
});

test('año PLANIFICADO: "Editar" y "Activar año" en la misma fila del título', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares/anio-2027`, () =>
      HttpResponse.json(anio(2027, 'PLANIFICADO')),
    ),
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([]),
    ),
    http.get(`${API}/configuracion/anios-escolares/anio-2027/requisitos`, () =>
      HttpResponse.json({
        puedeActivar: true,
        obligatorios: [],
        recomendados: [],
      }),
    ),
  );
  await mostrar('anio-2027');

  expect(
    await screen.findByRole('heading', { name: 'Año escolar 2027' }),
  ).toBeInTheDocument();
  // La etiqueta del año y la de cada uno de sus 4 periodos.
  expect(screen.getAllByText('Planificado')).toHaveLength(5);
  expect(
    await screen.findByRole('button', { name: 'Editar' }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'Activar año' }),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'Cerrar año' }),
  ).not.toBeInTheDocument();
});

test('año CERRADO: sin acciones', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares/anio-2025`, () =>
      HttpResponse.json(anio(2025, 'CERRADO')),
    ),
  );
  await mostrar('anio-2025');

  expect(
    await screen.findByRole('heading', { name: 'Año escolar 2025' }),
  ).toBeInTheDocument();
  expect(screen.getByText('Cerrado')).toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: /Editar|Activar|Cerrar/ }),
  ).not.toBeInTheDocument();
});

test('un año que no existe muestra el aviso y deja las migas para volver', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares/no-existe`, () =>
      HttpResponse.json(
        { message: 'El año escolar no existe.' },
        { status: 404 },
      ),
    ),
  );
  await mostrar('no-existe');

  expect(
    await screen.findByText('No encontramos ese año escolar.'),
  ).toBeInTheDocument();
  expect(
    within(screen.getByRole('navigation', { name: 'Ruta' })).getByRole('link', {
      name: 'Año escolar',
    }),
  ).toHaveAttribute('href', RUTA);
  expect(screen.queryByRole('tab')).not.toBeInTheDocument();
});
