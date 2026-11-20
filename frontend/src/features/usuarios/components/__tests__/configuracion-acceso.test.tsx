import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  renderApp,
  screen,
  userEvent,
  waitFor,
} from '@/testing/test-utils';

import { ConfiguracionAccesoPanel } from '../configuracion-acceso';

// HU-2 · Administrador › Seguridad › Configuración de acceso.

const RUTA = `${env.API_URL}/seguridad/configuracion-acceso`;

const conConfiguracion = (
  googleHabilitado: boolean,
  googleConfigurado = true,
) =>
  server.use(
    http.get(RUTA, () =>
      HttpResponse.json({ googleHabilitado, googleConfigurado }),
    ),
  );

const renderPanel = async () =>
  renderApp(<ConfiguracionAccesoPanel />, {
    user: await createUser({ roles: [ROLES.ADMINISTRADOR] }),
  });

test('usuario y contraseña siempre están activos y Google se activa con el interruptor', async () => {
  conConfiguracion(false);
  let enviado: unknown;
  server.use(
    http.put(RUTA, async ({ request }) => {
      enviado = await request.json();
      return HttpResponse.json({
        googleHabilitado: true,
        googleConfigurado: true,
      });
    }),
  );
  await renderPanel();

  expect(await screen.findByText('Usuario y contraseña')).toBeInTheDocument();
  expect(screen.getByText('Siempre activo')).toBeInTheDocument();
  const interruptor = screen.getByRole('switch', {
    name: 'Inicio de sesión con Google',
  });
  expect(interruptor).not.toBeChecked();

  await userEvent.click(interruptor);

  await waitFor(() => expect(interruptor).toBeChecked());
  expect(enviado).toEqual({ googleHabilitado: true });
  expect(
    await screen.findByText('Inicio de sesión con Google activado'),
  ).toBeInTheDocument();
});

test('se puede desactivar Google', async () => {
  conConfiguracion(true);
  server.use(
    http.put(RUTA, () =>
      HttpResponse.json({ googleHabilitado: false, googleConfigurado: true }),
    ),
  );
  await renderPanel();

  const interruptor = await screen.findByRole('switch', {
    name: 'Inicio de sesión con Google',
  });
  expect(interruptor).toBeChecked();
  await userEvent.click(interruptor);

  await waitFor(() => expect(interruptor).not.toBeChecked());
  expect(
    await screen.findByText('Inicio de sesión con Google desactivado'),
  ).toBeInTheDocument();
});

test('sin las llaves de Google en el servidor el interruptor queda bloqueado y se avisa', async () => {
  conConfiguracion(false, false);
  await renderPanel();

  const interruptor = await screen.findByRole('switch', {
    name: 'Inicio de sesión con Google',
  });
  expect(interruptor).toBeDisabled();
  expect(
    screen.getByText('Falta configurar Google en el servidor'),
  ).toBeInTheDocument();
});
