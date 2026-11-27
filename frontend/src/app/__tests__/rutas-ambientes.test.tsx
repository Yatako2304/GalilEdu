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

// HU-43: el Administrador tiene la misma pantalla de ambientes físicos que el Personal administrativo (el plan de
// pruebas pone los espacios en su rol). Se prueba el router de la app de verdad, en memoria.

// La primera prueba carga en frío los módulos de las rutas (`lazy`), lo que tarda más que el límite de 1 s por defecto.
const CARGA_DE_RUTAS = 10000;

const API = env.API_URL;
const AMBIENTES_ADMIN = '/app/admin/ambientes';

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
    http.get(`${API}/ambientes/resumen`, () =>
      HttpResponse.json({
        activos: 1,
        total: 1,
        aulasRegulares: 1,
        espaciosEspeciales: 0,
        aforoTotal: 30,
      }),
    ),
    http.get(`${API}/ambientes`, () =>
      HttpResponse.json([
        {
          id: 'a101',
          codigo: 'A-101',
          nombre: 'Aula 101',
          tipo: 'AULA_REGULAR',
          aforoMaximo: 30,
          recursos: ['Proyector'],
          estado: 'ACTIVO',
        },
      ]),
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

test('el Administrador abre los ambientes físicos desde su menú y puede crear uno', async () => {
  await abrir(ROLES.ADMINISTRADOR, AMBIENTES_ADMIN);

  expect(
    await screen.findByRole(
      'heading',
      { name: 'Ambientes físicos' },
      { timeout: CARGA_DE_RUTAS },
    ),
  ).toBeInTheDocument();
  expect(await screen.findByText('Aula 101')).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'Nuevo ambiente' }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('link', { name: 'Ambientes físicos' }),
  ).toHaveAttribute('href', AMBIENTES_ADMIN);
});

test('un Docente que abre la ruta del Administrador es llevado a su inicio', async () => {
  const router = await abrir(ROLES.DOCENTE, AMBIENTES_ADMIN);

  await waitFor(
    () => expect(router.state.location.pathname).toBe('/app/docente'),
    { timeout: CARGA_DE_RUTAS },
  );
  expect(
    screen.queryByRole('heading', { name: 'Ambientes físicos' }),
  ).not.toBeInTheDocument();
});
