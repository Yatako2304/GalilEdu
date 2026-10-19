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

// Figma > 🌟 Autenticación > Iniciar sesión
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
              error={formState.errors['username']}
              registration={register('username')}
            />
            <div className="space-y-2">
              <Input
                type={showPassword ? 'text' : 'password'}
                label="Contraseña"
                autoComplete="current-password"
                error={formState.errors['password']}
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
              <div className="flex justify-end">
                <Link
                  to={paths.auth.recuperarContrasena.getHref()}
                  className="text-body-4 text-content-accent-secondary"
                >
                  ¿Olvidaste tu contraseña?
                </Link>
              </div>
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
