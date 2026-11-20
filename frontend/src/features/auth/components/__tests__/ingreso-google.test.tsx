import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent, waitFor } from '@/testing/test-utils';

import { LoginForm } from '../login-form';

// HU-3 · Figma > Autenticación > Iniciar sesión: «o» y «Iniciar sesión con Google». Solo con Google activado (HU-2).

const ID_CLIENTE = 'cliente-prueba.apps.googleusercontent.com';

type ConfigCodigo = {
  client_id: string;
  scope: string;
  callback: (r: { code?: string; error?: string }) => void;
  error_callback?: (e: { type?: string }) => void;
};

/** Simula la ventana de Google: `responder` decide qué devuelve al pedir el código. */
const simularGoogle = (responder: (config: ConfigCodigo) => void) => {
  const initCodeClient = vi.fn((config: ConfigCodigo) => ({
    requestCode: () => responder(config),
  }));
  window.google = { accounts: { oauth2: { initCodeClient } } };
  return initCodeClient;
};

const conGoogleActivado = () =>
  server.use(
    http.get(`${env.API_URL}/auth/metodos`, () =>
      HttpResponse.json({ google: true, googleIdCliente: ID_CLIENTE }),
    ),
  );

afterEach(() => {
  delete window.google;
});

const botonGoogle = () =>
  screen.findByRole('button', { name: 'Iniciar sesión con Google' });

test('con Google desactivado no aparece el botón', async () => {
  await renderApp(<LoginForm onSuccess={vi.fn()} />, { user: null });

  expect(
    screen.getByRole('button', { name: 'Iniciar sesión' }),
  ).toBeInTheDocument();
  await waitFor(() =>
    expect(
      screen.queryByRole('button', { name: 'Iniciar sesión con Google' }),
    ).not.toBeInTheDocument(),
  );
  expect(screen.queryByText('o')).not.toBeInTheDocument();
});

test('con Google activado entra con el código que devuelve la ventana de Google', async () => {
  conGoogleActivado();
  let codigoEnviado: unknown;
  server.use(
    http.post(`${env.API_URL}/auth/google`, async ({ request }) => {
      codigoEnviado = await request.json();
      return HttpResponse.json({
        jwt: 'jwt-google',
        user: {
          id: '1',
          username: 'U20260001',
          firstName: 'Selene',
          lastName: 'Curo',
          email: 'selene.curo@galiledu.edu.pe',
          roles: ['ADMINISTRADOR'],
        },
      });
    }),
  );
  const initCodeClient = simularGoogle((config) =>
    config.callback({ code: 'codigo-de-google' }),
  );
  const onSuccess = vi.fn();
  await renderApp(<LoginForm onSuccess={onSuccess} />, { user: null });

  const boton = await botonGoogle();
  expect(screen.getByText('o')).toBeInTheDocument();
  await waitFor(() => expect(boton).toBeEnabled());
  await userEvent.click(boton);

  await waitFor(() => expect(onSuccess).toHaveBeenCalled());
  expect(codigoEnviado).toEqual({ codigo: 'codigo-de-google' });
  expect(initCodeClient).toHaveBeenCalledWith(
    expect.objectContaining({
      client_id: ID_CLIENTE,
      scope: 'openid email profile',
      ux_mode: 'popup',
    }),
  );
});

test('si el correo de Google no es de ningún usuario muestra el mensaje del backend', async () => {
  conGoogleActivado();
  server.use(
    http.post(`${env.API_URL}/auth/google`, () =>
      HttpResponse.json(
        {
          message:
            'No se encontró una cuenta de GalilEdu asociada a este correo de Google.',
        },
        { status: 404 },
      ),
    ),
  );
  simularGoogle((config) => config.callback({ code: 'codigo' }));
  const onSuccess = vi.fn();
  await renderApp(<LoginForm onSuccess={onSuccess} />, { user: null });

  const boton = await botonGoogle();
  await waitFor(() => expect(boton).toBeEnabled());
  await userEvent.click(boton);

  expect(
    await screen.findByText(
      'No se encontró una cuenta de GalilEdu asociada a este correo de Google.',
    ),
  ).toBeInTheDocument();
  expect(onSuccess).not.toHaveBeenCalled();
});

test('si el usuario cierra la ventana de Google no muestra ningún error', async () => {
  conGoogleActivado();
  let llamadas = 0;
  server.use(
    http.post(`${env.API_URL}/auth/google`, () => {
      llamadas += 1;
      return HttpResponse.json({}, { status: 500 });
    }),
  );
  simularGoogle((config) => config.error_callback?.({ type: 'popup_closed' }));
  await renderApp(<LoginForm onSuccess={vi.fn()} />, { user: null });

  const boton = await botonGoogle();
  await waitFor(() => expect(boton).toBeEnabled());
  await userEvent.click(boton);

  await waitFor(() => expect(boton).toBeEnabled());
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  expect(llamadas).toBe(0);
});

test('si la ventana de Google falla muestra un aviso para reintentar', async () => {
  conGoogleActivado();
  simularGoogle((config) =>
    config.error_callback?.({ type: 'popup_failed_to_open' }),
  );
  await renderApp(<LoginForm onSuccess={vi.fn()} />, { user: null });

  const boton = await botonGoogle();
  await waitFor(() => expect(boton).toBeEnabled());
  await userEvent.click(boton);

  expect(
    await screen.findByText(
      'No se pudo iniciar sesión con Google. Inténtalo nuevamente.',
    ),
  ).toBeInTheDocument();
});
