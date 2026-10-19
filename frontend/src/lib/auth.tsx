import { configureAuth } from 'react-query-auth';
import { Navigate, useLocation } from 'react-router';
import { z } from 'zod';

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
    await api.post('/auth/logout');
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

export const solicitarRecuperacion = (
  data: RecuperarContrasenaInput,
): Promise<{ message: string }> => {
  return api.post('/auth/recuperar-contrasena', data);
};

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
