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

import { CambiarContrasena } from '../cambiar-contrasena';

// HU-5 · Figma > 🌟 Autenticación > Cambiar contraseña: contraseña actual → nueva.

const base = `${env.API_URL}/auth/cambiar-contrasena`;

const renderizar = async (onCambiada = vi.fn()) =>
  renderApp(<CambiarContrasena onCambiada={onCambiada} />, {
    user: await createUser({ roles: [ROLES.DOCENTE] }),
  });

test('con la contraseña actual correcta pasa a «Nueva contraseña» y la cambia', async () => {
  const cuerpos: Record<string, unknown> = {};
  server.use(
    http.post(`${base}/verificar`, async ({ request }) => {
      cuerpos.verificar = await request.json();
      return HttpResponse.json({ message: 'ok' });
    }),
    http.post(base, async ({ request }) => {
      cuerpos.cambiar = await request.json();
      return HttpResponse.json({ message: 'ok' });
    }),
  );
  const onCambiada = vi.fn();
  await renderizar(onCambiada);

  const continuar = screen.getByRole('button', { name: 'Continuar' });
  expect(continuar).toBeDisabled();
  await userEvent.type(
    screen.getByLabelText('Contraseña actual'),
    'Galiledu2026',
  );
  await userEvent.click(continuar);
  await screen.findByText('Nueva contraseña');
  expect(cuerpos.verificar).toEqual({ contrasenaActual: 'Galiledu2026' });

  await userEvent.type(screen.getByLabelText('Contraseña'), 'Passw1234#');
  await userEvent.type(
    screen.getByLabelText('Repetir contraseña'),
    'Passw1234#',
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Cambiar contraseña' }),
  );

  await waitFor(() => expect(onCambiada).toHaveBeenCalled());
  expect(cuerpos.cambiar).toEqual({
    contrasenaActual: 'Galiledu2026',
    contrasenaNueva: 'Passw1234#',
  });
});

test('una contraseña actual incorrecta muestra «Contraseña incorrecta.» debajo del campo', async () => {
  server.use(
    http.post(`${base}/verificar`, () =>
      HttpResponse.json({ message: 'Contraseña incorrecta.' }, { status: 400 }),
    ),
  );
  await renderizar();

  await userEvent.type(screen.getByLabelText('Contraseña actual'), 'mala');
  await userEvent.click(screen.getByRole('button', { name: 'Continuar' }));

  expect(await screen.findByText('Contraseña incorrecta.')).toBeInTheDocument();
  expect(screen.getByText('Cambiar contraseña')).toBeInTheDocument();
});

test('un error al guardar se muestra bajo el botón', async () => {
  server.use(
    http.post(`${base}/verificar`, () => HttpResponse.json({ message: 'ok' })),
    http.post(base, () =>
      HttpResponse.json(
        { message: 'La nueva contraseña debe ser diferente de la actual.' },
        { status: 400 },
      ),
    ),
  );
  const onCambiada = vi.fn();
  await renderizar(onCambiada);

  await userEvent.type(
    screen.getByLabelText('Contraseña actual'),
    'Galiledu2026#',
  );
  await userEvent.click(screen.getByRole('button', { name: 'Continuar' }));
  await screen.findByText('Nueva contraseña');
  await userEvent.type(screen.getByLabelText('Contraseña'), 'Galiledu2026#');
  await userEvent.type(
    screen.getByLabelText('Repetir contraseña'),
    'Galiledu2026#',
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Cambiar contraseña' }),
  );

  expect(
    await screen.findByText(
      'La nueva contraseña debe ser diferente de la actual.',
    ),
  ).toBeInTheDocument();
  expect(onCambiada).not.toHaveBeenCalled();
});

test('la X vuelve al inicio', async () => {
  await renderizar();
  expect(
    screen.getByRole('link', { name: 'Volver al inicio' }),
  ).toHaveAttribute('href', '/app');
});
