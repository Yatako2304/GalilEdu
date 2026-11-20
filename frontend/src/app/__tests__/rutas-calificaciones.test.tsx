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

// HU-64: el reporte institucional de notas es del Personal administrativo (además del Coordinador), no del
// Administrador. Se prueba el router de la app de verdad, en memoria.

// La primera prueba carga en frío los módulos de las rutas (`lazy`), lo que tarda más que el límite de 1 s por defecto.
const CARGA_DE_RUTAS = 10000;

const RUTA = '/app/administrativo/calificaciones';

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
    http.get(`${env.API_URL}/calificaciones/reportes/anios`, () =>
      HttpResponse.json([]),
    ),
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

test('el Personal administrativo abre Calificaciones desde su menú', async () => {
  await abrir(ROLES.PERSONAL_ADMINISTRATIVO, RUTA);

  expect(
    await screen.findByRole(
      'heading',
      { name: 'Calificaciones' },
      { timeout: CARGA_DE_RUTAS },
    ),
  ).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Calificaciones' })).toHaveAttribute(
    'href',
    RUTA,
  );
});

test('el Administrador ya no tiene Calificaciones en su menú', async () => {
  await abrir(ROLES.ADMINISTRADOR, '/app/admin');

  expect(
    await screen.findByRole(
      'link',
      { name: 'Usuarios' },
      { timeout: CARGA_DE_RUTAS },
    ),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole('link', { name: 'Calificaciones' }),
  ).not.toBeInTheDocument();
});

test('el Administrador tampoco entra a la ruta del Personal administrativo', async () => {
  const router = await abrir(ROLES.ADMINISTRADOR, RUTA);

  await waitFor(
    () => expect(router.state.location.pathname).toBe('/app/admin'),
    { timeout: CARGA_DE_RUTAS },
  );
});
