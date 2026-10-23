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

// Rutas de Pensiones del Personal administrativo: la lista (P2), el compromiso de un alumno (P8) y la configuración
// del tarifario (P19, HU-34). Se prueba el router de la app de verdad, con uno en memoria.

// La primera prueba carga en frío los módulos de las rutas (`lazy`), lo que tarda más que el límite de 1 s por defecto.
const CARGA_DE_RUTAS = 10000;

const API = env.API_URL;
const UUID = '8f14e45f-ceea-4f67-9b6f-1d4e5b6a7c10';

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
afterEach(() => vi.unstubAllGlobals());

const abrir = async (url: string) => {
  await loginAsUser(
    await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  );
  const router = createAppRouter(new QueryClient(), (rutas) =>
    createMemoryRouter(rutas, { initialEntries: [url] }),
  );
  return rtlRender(
    <AppProvider>
      <RouterProvider router={router} />
    </AppProvider>,
  );
};

test('/pensiones/configuracion abre el tarifario (P19), no el compromiso de un alumno llamado "configuracion"', async () => {
  const compromisosPedidos: string[] = [];
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([]),
    ),
    http.get(`${API}/pagos/estudiantes/:id/compromisos`, ({ params }) => {
      compromisosPedidos.push(String(params.id));
      return HttpResponse.json([]);
    }),
  );

  await abrir('/app/administrativo/pensiones/configuracion');

  expect(
    await screen.findByRole(
      'heading',
      { name: 'Configuración de pensiones' },
      { timeout: CARGA_DE_RUTAS },
    ),
  ).toBeInTheDocument();
  expect(
    await screen.findByText(
      'Aún no hay años escolares para configurar su tarifario.',
    ),
  ).toBeInTheDocument();
  expect(compromisosPedidos).toEqual([]);
});

test('/pensiones/<id de estudiante> sigue abriendo el compromiso de pago (P8)', async () => {
  const compromisosPedidos: string[] = [];
  server.use(
    http.get(`${API}/pagos/estudiantes/:id/compromisos`, ({ params }) => {
      compromisosPedidos.push(String(params.id));
      return HttpResponse.json([]);
    }),
  );

  await abrir(`/app/administrativo/pensiones/${UUID}`);

  expect(
    await screen.findByRole(
      'heading',
      { name: 'Compromiso de pago' },
      { timeout: CARGA_DE_RUTAS },
    ),
  ).toBeInTheDocument();
  await waitFor(() => expect(compromisosPedidos).toEqual([UUID]));
  expect(
    screen.queryByRole('heading', { name: 'Configuración de pensiones' }),
  ).not.toBeInTheDocument();
});

test('/pensiones abre el estado de cuenta (P2), con el botón "Configuración" que lleva al tarifario', async () => {
  server.use(
    http.get(`${API}/pagos/estado-cuenta`, () =>
      HttpResponse.json({
        anio: 2026,
        comprometido: 0,
        cobrado: 0,
        porcentajeCobrado: 0,
        deudaVencida: 0,
        cuotasVencidas: 0,
        alumnosConDeuda: 0,
        alumnos: [],
      }),
    ),
  );

  await abrir('/app/administrativo/pensiones');

  expect(
    await screen.findByRole(
      'heading',
      { name: 'Estado de cuenta' },
      { timeout: CARGA_DE_RUTAS },
    ),
  ).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Configuración' })).toHaveAttribute(
    'href',
    '/app/administrativo/pensiones/configuracion',
  );
});
