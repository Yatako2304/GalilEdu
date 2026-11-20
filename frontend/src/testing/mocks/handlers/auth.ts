import Cookies from 'js-cookie';
import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';

import { authenticate, requireAuth, AUTH_COOKIE, networkDelay } from '../utils';

type LoginBody = {
  username: string;
  password: string;
};

// Contrato esperado del backend para autenticación:
//   POST /auth/login                 { username, password } -> { jwt, user }
//   POST /auth/logout                -> { message }
//   GET  /auth/me                    -> { data: User | null }
//   POST /auth/recuperar-contrasena             { email } -> { message }   (HU-4: envía el código)
//   POST /auth/recuperar-contrasena/verificar   { email, codigo } -> { message }
//   POST /auth/recuperar-contrasena/restablecer { email, codigo, contrasena } -> { message }
//   POST /auth/cambiar-contrasena/verificar { contrasenaActual } -> { message }   (HU-5)
//   POST /auth/cambiar-contrasena          { contrasenaActual, contrasenaNueva } -> { message }
// En el modo simulado el código válido es 123456; la contraseña actual «incorrecta» da error.
const CODIGO_SIMULADO = '123456';
export const authHandlers = [
  http.post(`${env.API_URL}/auth/login`, async ({ request }) => {
    await networkDelay();

    try {
      const credentials = (await request.json()) as LoginBody;
      const result = authenticate(credentials);

      // todo: remove once tests in Github Actions are fixed
      Cookies.set(AUTH_COOKIE, result.jwt, { path: '/' });

      return HttpResponse.json(result, {
        headers: {
          // con un backend real la cookie debe ser Secure y HttpOnly
          'Set-Cookie': `${AUTH_COOKIE}=${result.jwt}; Path=/;`,
        },
      });
    } catch (error: any) {
      return HttpResponse.json(
        { message: error?.message || 'Error del servidor' },
        { status: 401 },
      );
    }
  }),

  http.post(`${env.API_URL}/auth/logout`, async () => {
    await networkDelay();

    // todo: remove once tests in Github Actions are fixed
    Cookies.remove(AUTH_COOKIE);

    return HttpResponse.json(
      { message: 'Sesión cerrada' },
      {
        headers: {
          'Set-Cookie': `${AUTH_COOKIE}=; Path=/;`,
        },
      },
    );
  }),

  http.get(`${env.API_URL}/auth/me`, async ({ cookies }) => {
    await networkDelay();

    const { user } = requireAuth(cookies);
    return HttpResponse.json({ data: user });
  }),

  http.post(`${env.API_URL}/auth/recuperar-contrasena`, async ({ request }) => {
    await networkDelay();
    const { email } = (await request.json()) as { email: string };
    if (email.includes('noexiste')) {
      return HttpResponse.json(
        { message: 'El correo no está asociado a ninguna cuenta.' },
        { status: 404 },
      );
    }
    return HttpResponse.json({
      message: 'Te enviamos un código de verificación a tu correo.',
    });
  }),

  http.post(
    `${env.API_URL}/auth/recuperar-contrasena/verificar`,
    async ({ request }) => {
      await networkDelay();
      const { codigo } = (await request.json()) as { codigo: string };
      if (codigo !== CODIGO_SIMULADO) {
        return HttpResponse.json(
          { message: 'Código de verificación incorrecto.' },
          { status: 400 },
        );
      }
      return HttpResponse.json({ message: 'Código verificado.' });
    },
  ),

  http.post(
    `${env.API_URL}/auth/cambiar-contrasena/verificar`,
    async ({ request }) => {
      await networkDelay();
      const { contrasenaActual } = (await request.json()) as {
        contrasenaActual: string;
      };
      if (!contrasenaActual || contrasenaActual === 'incorrecta') {
        return HttpResponse.json(
          { message: 'Contraseña incorrecta.' },
          { status: 400 },
        );
      }
      return HttpResponse.json({ message: 'Contraseña verificada.' });
    },
  ),

  http.post(`${env.API_URL}/auth/cambiar-contrasena`, async () => {
    await networkDelay();
    return HttpResponse.json({
      message: 'Su contraseña ha sido actualizada correctamente.',
    });
  }),

  http.post(
    `${env.API_URL}/auth/recuperar-contrasena/restablecer`,
    async () => {
      await networkDelay();
      return HttpResponse.json({
        message: 'Su contraseña ha sido actualizada correctamente.',
      });
    },
  ),
];
