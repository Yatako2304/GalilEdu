import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import {
  act,
  fireEvent,
  renderApp,
  screen,
  userEvent,
  waitFor,
} from '@/testing/test-utils';

import { RecuperarContrasena } from '../recuperar-contrasena';

// HU-4 · Figma > 🌟 Autenticación > Restablecer contraseña: correo → código → nueva contraseña.

const base = `${env.API_URL}/auth/recuperar-contrasena`;

const escribirCodigo = async (codigo: string) => {
  await userEvent.click(screen.getByLabelText('Dígito 1 de 6'));
  await userEvent.paste(codigo);
};

const llegarAlCodigo = async () => {
  await userEvent.type(
    screen.getByLabelText('Correo institucional'),
    'selene@galiledu.edu.pe',
  );
  await userEvent.click(screen.getByRole('button', { name: 'Enviar código' }));
  await screen.findByText('Código de verificación');
};

test('el flujo completo: pide el código, lo valida y restablece la contraseña', async () => {
  const cuerpos: Record<string, unknown> = {};
  server.use(
    http.post(base, async ({ request }) => {
      cuerpos.solicitar = await request.json();
      return HttpResponse.json({ message: 'ok' });
    }),
    http.post(`${base}/verificar`, async ({ request }) => {
      cuerpos.verificar = await request.json();
      return HttpResponse.json({ message: 'ok' });
    }),
    http.post(`${base}/restablecer`, async ({ request }) => {
      cuerpos.restablecer = await request.json();
      return HttpResponse.json({ message: 'ok' });
    }),
  );
  const onRestablecida = vi.fn();
  await renderApp(<RecuperarContrasena onRestablecida={onRestablecida} />, {
    user: null,
  });

  const enviar = screen.getByRole('button', { name: 'Enviar código' });
  expect(enviar).toBeDisabled();
  await llegarAlCodigo();
  expect(cuerpos.solicitar).toEqual({ email: 'selene@galiledu.edu.pe' });

  const validar = screen.getByRole('button', { name: 'Validar código' });
  expect(validar).toBeDisabled();
  await escribirCodigo('482913');
  await waitFor(() => expect(validar).toBeEnabled());
  await userEvent.click(validar);
  await screen.findByText('Nueva contraseña');
  expect(cuerpos.verificar).toEqual({
    email: 'selene@galiledu.edu.pe',
    codigo: '482913',
  });

  await userEvent.type(screen.getByLabelText('Contraseña'), 'Passw1234#');
  await userEvent.type(
    screen.getByLabelText('Repetir contraseña'),
    'Passw1234#',
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Restablecer contraseña' }),
  );
  await waitFor(() => expect(onRestablecida).toHaveBeenCalled());
  expect(cuerpos.restablecer).toEqual({
    email: 'selene@galiledu.edu.pe',
    codigo: '482913',
    contrasena: 'Passw1234#',
  });
});

test('un correo sin cuenta muestra el mensaje de Figma debajo del campo', async () => {
  server.use(
    http.post(base, () =>
      HttpResponse.json(
        { message: 'El correo no está asociado a ninguna cuenta.' },
        { status: 404 },
      ),
    ),
  );
  await renderApp(<RecuperarContrasena onRestablecida={vi.fn()} />, {
    user: null,
  });

  await userEvent.type(
    screen.getByLabelText('Correo institucional'),
    'correo@galiledu.edu.pe',
  );
  await userEvent.click(screen.getByRole('button', { name: 'Enviar código' }));

  expect(
    await screen.findByText('El correo no está asociado a ninguna cuenta.'),
  ).toBeInTheDocument();
  expect(screen.getByText('Restablecer contraseña')).toBeInTheDocument();
});

test('un correo mal escrito no se envía', async () => {
  const solicitar = vi.fn();
  server.use(
    http.post(base, () => {
      solicitar();
      return HttpResponse.json({ message: 'ok' });
    }),
  );
  await renderApp(<RecuperarContrasena onRestablecida={vi.fn()} />, {
    user: null,
  });

  await userEvent.type(screen.getByLabelText('Correo institucional'), 'abc');
  await userEvent.click(screen.getByRole('button', { name: 'Enviar código' }));

  expect(
    await screen.findByText('Ingrese un correo válido'),
  ).toBeInTheDocument();
  expect(solicitar).not.toHaveBeenCalled();
});

test('un código incorrecto muestra «Código de verificación incorrecto.»', async () => {
  server.use(
    http.post(base, () => HttpResponse.json({ message: 'ok' })),
    http.post(`${base}/verificar`, () =>
      HttpResponse.json(
        { message: 'Código de verificación incorrecto.' },
        { status: 400 },
      ),
    ),
  );
  await renderApp(<RecuperarContrasena onRestablecida={vi.fn()} />, {
    user: null,
  });
  await llegarAlCodigo();

  await escribirCodigo('111112');
  await userEvent.click(screen.getByRole('button', { name: 'Validar código' }));

  expect(
    await screen.findByText('Código de verificación incorrecto.'),
  ).toBeInTheDocument();
  expect(screen.getByText('Código de verificación')).toBeInTheDocument();
});

test('«Reenviar código» espera 60 segundos y luego pide un código nuevo', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true });
  try {
    let pedidos = 0;
    server.use(
      http.post(base, () => {
        pedidos++;
        return HttpResponse.json({ message: 'ok' });
      }),
    );
    await renderApp(<RecuperarContrasena onRestablecida={vi.fn()} />, {
      user: null,
    });
    await llegarAlCodigo();

    const reenviar = screen.getByRole('button', {
      name: /Reenviar código \(\d+ s\)/,
    });
    expect(reenviar).toBeDisabled();

    await act(async () => {
      vi.advanceTimersByTime(61_000);
    });
    const listo = await screen.findByRole('button', {
      name: 'Reenviar código',
    });
    expect(listo).toBeEnabled();
    fireEvent.click(listo);

    await waitFor(() => expect(pedidos).toBe(2));
    expect(
      await screen.findByRole('button', { name: /Reenviar código \(\d+ s\)/ }),
    ).toBeDisabled();
  } finally {
    vi.useRealTimers();
  }
});

test('la contraseña debe cumplir la regla de Figma y repetirse igual', async () => {
  server.use(
    http.post(base, () => HttpResponse.json({ message: 'ok' })),
    http.post(`${base}/verificar`, () => HttpResponse.json({ message: 'ok' })),
  );
  await renderApp(<RecuperarContrasena onRestablecida={vi.fn()} />, {
    user: null,
  });
  await llegarAlCodigo();
  await escribirCodigo('482913');
  await userEvent.click(screen.getByRole('button', { name: 'Validar código' }));
  await screen.findByText('Nueva contraseña');

  const boton = screen.getByRole('button', { name: 'Restablecer contraseña' });
  await userEvent.type(screen.getByLabelText('Contraseña'), 'passw123');
  // La regla pasa de ayuda a error (en rojo).
  expect(screen.getByRole('alert')).toHaveTextContent(
    /Mínimo 8 caracteres, con al menos una mayúscula/,
  );
  expect(boton).toBeDisabled();

  await userEvent.clear(screen.getByLabelText('Contraseña'));
  await userEvent.type(screen.getByLabelText('Contraseña'), 'Passw1234#');
  await userEvent.type(
    screen.getByLabelText('Repetir contraseña'),
    'Passw1234',
  );
  expect(screen.getByText('Las contraseñas no coinciden.')).toBeInTheDocument();
  expect(boton).toBeDisabled();

  await userEvent.type(screen.getByLabelText('Repetir contraseña'), '#');
  await waitFor(() => expect(boton).toBeEnabled());
});

test('la X vuelve a iniciar sesión', async () => {
  await renderApp(<RecuperarContrasena onRestablecida={vi.fn()} />, {
    user: null,
  });
  expect(
    screen.getByRole('link', { name: 'Volver a iniciar sesión' }),
  ).toHaveAttribute('href', '/auth/login');
});
