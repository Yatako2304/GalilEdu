import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent, waitFor } from '@/testing/test-utils';

import { LoginForm } from '../login-form';

// HU-1 · Figma > Autenticación > Iniciar sesión

test('muestra el error del backend debajo de la contraseña', async () => {
  server.use(
    http.post(`${env.API_URL}/auth/login`, () =>
      HttpResponse.json(
        { message: 'El usuario o contraseña son incorrectos.' },
        { status: 401 },
      ),
    ),
  );
  const onSuccess = vi.fn();
  await renderApp(<LoginForm onSuccess={onSuccess} />, { user: null });

  await userEvent.type(screen.getByLabelText(/usuario/i), 'U20260001');
  await userEvent.type(screen.getByLabelText(/contraseña/i), 'incorrecta');
  await userEvent.click(
    screen.getByRole('button', { name: /iniciar sesión/i }),
  );

  expect(
    await screen.findByText('El usuario o contraseña son incorrectos.'),
  ).toBeInTheDocument();
  expect(onSuccess).not.toHaveBeenCalled();
});

test('el botón está deshabilitado hasta llenar usuario y contraseña', async () => {
  await renderApp(<LoginForm onSuccess={vi.fn()} />, { user: null });
  const boton = screen.getByRole('button', { name: /iniciar sesión/i });

  expect(boton).toBeDisabled();
  await userEvent.type(screen.getByLabelText(/usuario/i), 'U20260001');
  await userEvent.type(screen.getByLabelText(/contraseña/i), 'x');
  await waitFor(() => expect(boton).toBeEnabled());
});
