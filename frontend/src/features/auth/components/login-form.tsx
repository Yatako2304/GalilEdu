import { AxiosError } from 'axios';
import { Eye, EyeOff } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Form, Input } from '@/components/ui/form';
import { Link } from '@/components/ui/link';
import { paths } from '@/config/paths';
import { useLogin, loginInputSchema } from '@/lib/auth';

type LoginFormProps = {
  onSuccess: () => void;
};

const mensajeDeError = (error: unknown) => {
  if (error instanceof AxiosError) {
    if (!error.response) {
      return 'No se pudo conectar con el servidor. Inténtalo nuevamente.';
    }
    return (
      (error.response.data as { message?: string })?.message ??
      'El usuario o contraseña son incorrectos.'
    );
  }
  return 'El usuario o contraseña son incorrectos.';
};

// El enlace de Figma se oculta hasta implementar HU-4 (el backend aún no tiene el endpoint de recuperación).
const RECUPERAR_CONTRASENA_DISPONIBLE = false;

// Figma > 🌟 Autenticación > Iniciar sesión (HU-1)
export const LoginForm = ({ onSuccess }: LoginFormProps) => {
  const login = useLogin({
    onSuccess,
  });
  const [showPassword, setShowPassword] = React.useState(false);

  return (
    <Form
      onSubmit={(values) => {
        login.mutate(values);
      }}
      schema={loginInputSchema}
      className="space-y-4"
    >
      {({ register, formState, watch }) => {
        const [username, password] = watch(['username', 'password']);
        const isEmpty = !username || !password;

        return (
          <>
            <Input
              label="Usuario"
              autoComplete="username"
              placeholder="U20260001"
              error={formState.errors['username']}
              registration={register('username')}
            />
            <div className="space-y-2">
              <Input
                type={showPassword ? 'text' : 'password'}
                label="Contraseña"
                autoComplete="current-password"
                error={
                  formState.errors['password'] ??
                  (login.isError
                    ? { type: 'server', message: mensajeDeError(login.error) }
                    : undefined)
                }
                registration={register('password')}
                endAdornment={
                  <button
                    type="button"
                    onClick={() => setShowPassword((value) => !value)}
                    className="text-content-tertiary hover:text-content-primary"
                  >
                    {showPassword ? (
                      <EyeOff className="size-4" aria-hidden="true" />
                    ) : (
                      <Eye className="size-4" aria-hidden="true" />
                    )}
                    <span className="sr-only">
                      {showPassword
                        ? 'Ocultar contraseña'
                        : 'Mostrar contraseña'}
                    </span>
                  </button>
                }
              />
              {RECUPERAR_CONTRASENA_DISPONIBLE && (
                <div className="flex justify-end">
                  <Link
                    to={paths.auth.recuperarContrasena.getHref()}
                    className="text-body-4 text-content-accent-secondary"
                  >
                    ¿Olvidaste tu contraseña?
                  </Link>
                </div>
              )}
            </div>
            <Button
              isLoading={login.isPending}
              disabled={isEmpty || login.isPending}
              type="submit"
              variant="secondary"
              className="w-full"
            >
              Iniciar sesión
            </Button>
          </>
        );
      }}
    </Form>
  );
};
