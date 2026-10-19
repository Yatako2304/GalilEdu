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

api.interceptors.request.use(authRequestInterceptor);
api.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    const status = error.response?.status;
    const isSessionCheck = error.config?.url === '/auth/me';

    if (!isSessionCheck) {
      const message = error.response?.data?.message || error.message;
      useNotifications.getState().addNotification({
        type: 'error',
        title: 'Error',
        message,
      });
    }

    if (
      status === 401 &&
      !isSessionCheck &&
      !window.location.pathname.startsWith('/auth')
    ) {
      tokenStorage.clear();
      window.location.href = paths.auth.login.getHref(window.location.pathname);
    }

    return Promise.reject(error);
  },
);
