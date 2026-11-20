import { QueryClient } from '@tanstack/react-query';
import { RouterProvider, createMemoryRouter } from 'react-router';

import { ROLES } from '@/config/roles';
import {
  createUser,
  loginAsUser,
  rtlRender,
  screen,
  waitFor,
} from '@/testing/test-utils';

import { AppProvider } from '../provider';
import { createAppRouter } from '../router';

// «Mis datos» (ampliación de HU-9) cuelga del rol activo (/app/<rol>/mis-datos): el menú lateral toma el rol de la
// URL y, sin rol, no se dibuja. Se prueba el router de la app de verdad, con uno en memoria.

// La primera prueba carga en frío los módulos de las rutas (`lazy`), lo que tarda más que el límite de 1 s por defecto.
const CARGA_DE_RUTAS = 10000;

// Igual que en rutas-pensiones.test.tsx: el `Request` de Node no acepta el `AbortSignal` de jsdom.
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
});
afterEach(() => vi.unstubAllGlobals());

const abrir = async (url: string, roles: ROLES[]) => {
  await loginAsUser(await createUser({ roles }));
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

test('«Mis datos» conserva el menú lateral del rol activo', async () => {
  await abrir('/app/docente/mis-datos', [ROLES.DOCENTE]);

  expect(
    await screen.findByRole(
      'button',
      { name: 'Editar datos' },
      { timeout: CARGA_DE_RUTAS },
    ),
  ).toBeInTheDocument();
  // Menú del docente y la tarjeta del usuario, que vuelve a llevar a «Mis datos» del mismo rol.
  expect(
    screen.getAllByRole('link', { name: 'Calificaciones' }).length,
  ).toBeGreaterThan(0);
  expect(
    screen.getAllByRole('link', { name: /Ver mis datos/ })[0],
  ).toHaveAttribute('href', '/app/docente/mis-datos');
});

test('no se entra a «Mis datos» bajo un rol que el usuario no tiene', async () => {
  const router = await abrir('/app/admin/mis-datos', [ROLES.DOCENTE]);

  // El RoleGuard lo devuelve al inicio de su propio rol.
  await waitFor(
    () => expect(router.state.location.pathname).toBe('/app/docente'),
    { timeout: CARGA_DE_RUTAS },
  );
});

test('el apoderado ve «Familia» en el menú y abre «Datos de la Familia»', async () => {
  await abrir('/app/apoderado/familia', [ROLES.APODERADO]);

  expect(
    await screen.findByRole(
      'heading',
      { name: 'Datos de la Familia' },
      { timeout: CARGA_DE_RUTAS },
    ),
  ).toBeInTheDocument();
  expect(screen.getAllByRole('link', { name: 'Familia' })[0]).toHaveAttribute(
    'href',
    '/app/apoderado/familia',
  );
  expect(
    screen.queryByRole('link', { name: 'Datos del Alumno' }),
  ).not.toBeInTheDocument();
});
