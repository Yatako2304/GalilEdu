import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import { createUser, renderApp, screen, within } from '@/testing/test-utils';

import AdministrativoPensionesConfiguracionRoute from '../pensiones-configuracion';

// /app/administrativo/pensiones/configuracion: "P19 · Tarifario y calendario" (HU-34)

const RUTA = '/app/administrativo/pensiones/configuracion';

test('muestra el título y el subtítulo de Figma, con las migas Pensiones › Configuración › Tarifario y calendario', async () => {
  server.use(
    http.get(`${env.API_URL}/configuracion/anios-escolares`, () =>
      HttpResponse.json([]),
    ),
  );
  renderApp(<AdministrativoPensionesConfiguracionRoute />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
    path: RUTA,
    url: RUTA,
  });

  expect(
    await screen.findByRole('heading', { name: 'Configuración de pensiones' }),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      'Define los montos, la política de morosidad y el calendario de cuotas de cada año escolar. Una vez confirmado, el tarifario se usa para generar los compromisos de pago.',
    ),
  ).toBeInTheDocument();
  const migas = within(screen.getByRole('navigation', { name: 'Ruta' }));
  expect(migas.getByRole('link', { name: 'Pensiones' })).toHaveAttribute(
    'href',
    '/app/administrativo/pensiones',
  );
  expect(migas.getByText('Configuración')).toBeInTheDocument();
  expect(migas.getByText('Tarifario y calendario')).toHaveAttribute(
    'aria-current',
    'page',
  );
});
