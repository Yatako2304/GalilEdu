import { configureAuth } from 'react-query-auth';
import { Navigate, useLocation } from 'react-router';
import { z } from 'zod';

import { env } from '@/config/env';
import { paths } from '@/config/paths';
import { AuthResponse, User } from '@/types/api';

import { api, tokenStorage } from './api-client';

// Llamadas de autenticación (tipos, schemas, requests).
// No viven en un feature porque las usa toda la app.

const getUser = async (): Promise<User | null> => {
  try {
    const response: { data: User | null } = await api.get('/auth/me');
    return response.data;
  } catch {
    return null;
  }
};

const logout = async (): Promise<void> => {
  try {
    // El backend real usa JWT sin sesión: cerrar sesión consiste en borrar el token local.
    // MSW sí mantiene una cookie simulada y necesita su endpoint para eliminarla.
    if (env.ENABLE_API_MOCKING) await api.post('/auth/logout');
  } finally {
    tokenStorage.clear();
  }
};

export const loginInputSchema = z.object({
  username: z.string().trim().min(1, 'Debe ingresar su usuario.'),
  password: z.string().min(1, 'Debe ingresar su contraseña.'),
});

export type LoginInput = z.infer<typeof loginInputSchema>;

const loginWithUsernameAndPassword = (
  data: LoginInput,
): Promise<AuthResponse> => {
  // Una base recreada o un cambio de JWT invalida el token anterior. No debe
  // enviarse al endpoint público de login porque Spring lo rechaza antes de
  // comprobar las nuevas credenciales.
  tokenStorage.clear();
  return api.post('/auth/login', data);
};

export const recuperarContrasenaInputSchema = z.object({
  email: z
    .string()
    .min(1, 'Ingrese su correo')
    .email('Ingrese un correo válido'),
});

export type RecuperarContrasenaInput = z.infer<
  typeof recuperarContrasenaInputSchema
>;

// HU-4. Son endpoints públicos: un token viejo guardado haría que Spring los rechace, así que se borra antes.
// Los errores se muestran debajo de cada campo (Figma), no como notificación.
const enLinea = { silenciarNotificacion: true };
export const solicitarRecuperacion = (
  data: RecuperarContrasenaInput,
): Promise<{ message: string }> => {
  tokenStorage.clear();
  return api.post('/auth/recuperar-contrasena', data, enLinea);
};

export const verificarCodigoRecuperacion = (data: {
  email: string;
  codigo: string;
}): Promise<{ message: string }> => {
  tokenStorage.clear();
  return api.post('/auth/recuperar-contrasena/verificar', data, enLinea);
};

export const restablecerContrasena = (data: {
  email: string;
  codigo: string;
  contrasena: string;
}): Promise<{ message: string }> => {
  tokenStorage.clear();
  return api.post('/auth/recuperar-contrasena/restablecer', data, enLinea);
};

// HU-5 (con sesión). Los errores se muestran debajo del campo, como en Figma.
export const verificarContrasenaActual = (data: {
  contrasenaActual: string;
}): Promise<{ message: string }> =>
  api.post('/auth/cambiar-contrasena/verificar', data, enLinea);

export const cambiarContrasena = (data: {
  contrasenaActual: string;
  contrasenaNueva: string;
}): Promise<{ message: string }> =>
  api.post('/auth/cambiar-contrasena', data, enLinea);

const authConfig = {
  userFn: getUser,
  loginFn: async (data: LoginInput) => {
    const response = await loginWithUsernameAndPassword(data);
    if (response.jwt) tokenStorage.set(response.jwt);
    return response.user;
  },
  registerFn: async (): Promise<User> => {
    // En GalilEdu las cuentas las crea el Administrador (HU-6), no hay registro público.
    throw new Error('Registro no disponible');
  },
  logoutFn: logout,
};

export const { useUser, useLogin, useLogout, AuthLoader } =
  configureAuth(authConfig);

/**
 * Solo se vuelve a pantallas internas de la app ("/app/..."). Un redirectTo con otra forma
 * (otro dominio, "//sitio", "javascript:") se ignora y se va al inicio del rol.
 */
export const destinoSeguro = (redirectTo: string | null) =>
  redirectTo &&
  redirectTo.startsWith('/app/') &&
  !redirectTo.includes('//') &&
  !redirectTo.includes('\\')
    ? redirectTo
    : null;

export const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const user = useUser();
  const location = useLocation();

  if (!user.data) {
    return (
      <Navigate to={paths.auth.login.getHref(location.pathname)} replace />
    );
  }

  return children;
};
