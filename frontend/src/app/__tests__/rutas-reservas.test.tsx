import { QueryClient } from '@tanstack/react-query';
import { RouterProvider, createMemoryRouter } from 'react-router';

import { ROLES } from '@/config/roles';
import { reiniciarReservas } from '@/testing/mocks/handlers/reservas-ambiente';
import {
  createUser,
  loginAsUser,
  rtlRender,
  screen,
  waitFor,
} from '@/testing/test-utils';

import { AppProvider } from '../provider';
import { createAppRouter } from '../router';

// Ruta `coordinador/ambientes` (HU-48): la pantalla «Reserva de espacios especiales» es solo del Coordinador. Se
// prueba el router de la app de verdad, con uno en memoria (como las rutas de Pensiones).

// La primera prueba carga en frío los módulos de las rutas (`lazy`), lo que tarda más que el límite de 1 s por defecto.
const CARGA_DE_RUTAS = 10000;

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
});
afterEach(() => {
  vi.unstubAllGlobals();
  reiniciarReservas();
});

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

const RUTA = '/app/coordinador/ambientes';

test('el Coordinador ve la pantalla de reserva de espacios especiales con su historial', async () => {
  await abrir(ROLES.COORDINADOR, RUTA);

  expect(
    await screen.findByRole(
      'heading',
      { name: 'Reserva de espacios especiales' },
      { timeout: CARGA_DE_RUTAS },
    ),
  ).toBeInTheDocument();
  expect(await screen.findByText('Historial de reservas')).toBeInTheDocument();
  expect(
    await screen.findByText('Práctica de laboratorio de química'),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('link', { name: 'Reserva de espacios especiales' }),
  ).toHaveAttribute('href', RUTA);
});

test('un Docente que abre la ruta del Coordinador es llevado a su inicio y no ve la pantalla', async () => {
  const router = await abrir(ROLES.DOCENTE, RUTA);

  await waitFor(
    () => expect(router.state.location.pathname).toBe('/app/docente'),
    { timeout: CARGA_DE_RUTAS },
  );
  expect(
    screen.queryByRole('heading', { name: 'Reserva de espacios especiales' }),
  ).not.toBeInTheDocument();
  expect(screen.queryByText('Historial de reservas')).not.toBeInTheDocument();
});

test('un Apoderado tampoco entra', async () => {
  const router = await abrir(ROLES.APODERADO, RUTA);

  await waitFor(() => expect(router.state.location.pathname).not.toBe(RUTA), {
    timeout: CARGA_DE_RUTAS,
  });
  expect(screen.queryByText('Historial de reservas')).not.toBeInTheDocument();
});
