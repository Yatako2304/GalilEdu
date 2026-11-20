import { QueryClient } from '@tanstack/react-query';
import { HttpResponse, http } from 'msw';
import { RouterProvider, createMemoryRouter } from 'react-router';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  loginAsUser,
  rtlRender,
  screen,
  waitFor,
} from '@/testing/test-utils';

import { AppProvider } from '../provider';
import { createAppRouter } from '../router';

// HU-52 a HU-55: el Coordinador tiene el plan curricular y la escala de calificación, las mismas pantallas que el
// Personal administrativo ve como pestañas de «Competencias». Se prueba el router de la app de verdad, en memoria.

// La primera prueba carga en frío los módulos de las rutas (`lazy`), lo que tarda más que el límite de 1 s por defecto.
const CARGA_DE_RUTAS = 10000;

const API = env.API_URL;
const PLAN_CURRICULAR = '/app/coordinador/plan-curricular';
const ESCALA = '/app/coordinador/escala-calificacion';

// El router le pasa a `Request` el `AbortSignal` de jsdom, que el `Request` de Node no acepta (solo pasa con rutas que
// cargan con `lazy`, como las de la app). Estas pruebas no cancelan nada: se le quita la señal.
beforeEach(() => {
  const RequestDeNode = globalThis.Request;
  vi.stubGlobal(
    'Request',
    class extends RequestDeNode {
      constructor(entrada: RequestInfo | URL, opciones?: RequestInit) {
        super(entrada, opciones && { ...opciones, signal: undefined });
      }
    },
  );
  server.use(
    http.get(`${API}/areas/resumen`, () =>
      HttpResponse.json({
        areasRegistradas: 0,
        areasActivas: 0,
        competenciasRegistradas: 0,
        competenciasActivas: 0,
        cursosRegistrados: 0,
        cursosActivos: 0,
        anioActivo: 2026,
      }),
    ),
    http.get(`${API}/areas`, () => HttpResponse.json([])),
    http.get(`${API}/tablas-conversion`, () => HttpResponse.json([])),
  );
});
afterEach(() => vi.unstubAllGlobals());

const abrir = async (rol: ROLES, url: string) => {
  await loginAsUser(await createUser({ roles: [rol] }));
  const router = createAppRouter(new QueryClient(), (rutas) =>
    createMemoryRouter(rutas, { initialEntries: [url] }),
  );
  rtlRender(
    <AppProvider>
      <RouterProvider router={router} />
    </AppProvider>,
  );
  return router;
};

test('el Coordinador abre el plan curricular desde su menú', async () => {
  await abrir(ROLES.COORDINADOR, PLAN_CURRICULAR);

  expect(
    await screen.findByRole(
      'heading',
      { name: 'Plan curricular' },
      { timeout: CARGA_DE_RUTAS },
    ),
  ).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Plan curricular' })).toHaveAttribute(
    'href',
    PLAN_CURRICULAR,
  );
});

test('el Coordinador abre la escala de calificación desde su menú', async () => {
  await abrir(ROLES.COORDINADOR, ESCALA);

  expect(
    await screen.findByRole(
      'heading',
      { name: 'Escala de calificación' },
      { timeout: CARGA_DE_RUTAS },
    ),
  ).toBeInTheDocument();
  expect(
    await screen.findByText(
      'Aún no hay una tabla vigente. Registra la primera versión.',
    ),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('link', { name: 'Escala de calificación' }),
  ).toHaveAttribute('href', ESCALA);
});

test('un Docente que abre la ruta del Coordinador es llevado a su inicio', async () => {
  const router = await abrir(ROLES.DOCENTE, PLAN_CURRICULAR);

  await waitFor(
    () => expect(router.state.location.pathname).toBe('/app/docente'),
    { timeout: CARGA_DE_RUTAS },
  );
  expect(
    screen.queryByRole('heading', { name: 'Plan curricular' }),
  ).not.toBeInTheDocument();
});
