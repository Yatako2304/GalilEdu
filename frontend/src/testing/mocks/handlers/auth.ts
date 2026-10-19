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
//   POST /auth/recuperar-contrasena  { email } -> { message }
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

  http.post(`${env.API_URL}/auth/recuperar-contrasena`, async () => {
    await networkDelay();

    return HttpResponse.json({
      message:
        'Si el correo está registrado, recibirá un enlace para restablecer su contraseña.',
    });
  }),
];
