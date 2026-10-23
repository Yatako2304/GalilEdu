import Axios, { InternalAxiosRequestConfig } from 'axios';

import { useNotifications } from '@/components/ui/notifications';
import { env } from '@/config/env';
import { paths } from '@/config/paths';

// El backend puede autenticar por cookie (withCredentials) o por token Bearer.
// Si el login devuelve `jwt`, se guarda aquí y se envía en cada petición.
const TOKEN_KEY = 'galiledu_token';

export const tokenStorage = {
  get: () => {
    try {
      return window.localStorage.getItem(TOKEN_KEY);
    } catch {
      return null;
    }
  },
  set: (token: string) => {
    try {
      window.localStorage.setItem(TOKEN_KEY, token);
    } catch {
      // almacenamiento no disponible: se sigue usando la cookie
    }
  },
  clear: () => {
    try {
      window.localStorage.removeItem(TOKEN_KEY);
    } catch {
      // almacenamiento no disponible
    }
  },
};

function authRequestInterceptor(config: InternalAxiosRequestConfig) {
  if (config.headers) {
    config.headers.Accept = 'application/json';
    const token = tokenStorage.get();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }

  config.withCredentials = true;
  return config;
}

export const api = Axios.create({
  baseURL: env.API_URL,
});

// Consulta la sesión sin pasar por los interceptores de `api` (para no volver a entrar aquí).
const sesionVencida = async () => {
  try {
    await Axios.get(`${env.API_URL}/auth/me`, {
      withCredentials: true,
      headers: tokenStorage.get()
        ? { Authorization: `Bearer ${tokenStorage.get()}` }
        : undefined,
    });
    return false;
  } catch (e) {
    return Axios.isAxiosError(e) && e.response?.status === 401;
  }
};

api.interceptors.request.use(authRequestInterceptor);
api.interceptors.response.use(
  (response) => {
    return response.data;
  },
  async (error) => {
    const status = error.response?.status;
    const isSessionCheck = error.config?.url === '/auth/me';
    // El login muestra su error debajo del formulario (Figma), no como notificación.
    const isLogin = error.config?.url === '/auth/login';

    if (
      status === 401 &&
      !isSessionCheck &&
      !isLogin &&
      !window.location.pathname.startsWith('/auth')
    ) {
      // Antes de sacar al usuario se confirma que la sesión venció de verdad. Si /auth/me sigue
      // respondiendo, el 401 vino de un endpoint puntual: mandarlo al login lo regresaría a la
      // misma pantalla y entraría en bucle (pasó con Horario del apoderado).
      if (await sesionVencida()) {
        tokenStorage.clear();
        window.location.href = paths.auth.login.getHref(
          window.location.pathname,
        );
        return Promise.reject(error);
      }
    }

    if (!isSessionCheck && !isLogin) {
      const message = error.response?.data?.message || error.message;
      useNotifications.getState().addNotification({
        type: 'error',
        title: 'Error',
        message,
      });
    }

    return Promise.reject(error);
  },
);
