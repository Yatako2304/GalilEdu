import { useMutation } from '@tanstack/react-query';

import { Button } from '@/components/ui/button';
import { Form, Input } from '@/components/ui/form';
import { Link } from '@/components/ui/link';
import { paths } from '@/config/paths';
import {
  recuperarContrasenaInputSchema,
  solicitarRecuperacion,
} from '@/lib/auth';

// Figma > 🌟 Autenticación > Restablecer Contraseña (primer paso: solicitar enlace)
export const RecuperarContrasenaForm = () => {
  const recuperar = useMutation({ mutationFn: solicitarRecuperacion });

  if (recuperar.isSuccess) {
    return (
      <div className="space-y-6">
        <p
          role="status"
          className="rounded-xl bg-surface-success-light p-4 text-body-3 text-content-success"
        >
          {recuperar.data.message}
        </p>
        <Link
          to={paths.auth.login.getHref()}
          className="block text-center text-body-3 text-content-accent-secondary"
        >
          Volver a iniciar sesión
        </Link>
      </div>
    );
  }

  return (
    <Form
      onSubmit={(values) => recuperar.mutate(values)}
      schema={recuperarContrasenaInputSchema}
      className="space-y-4"
    >
      {({ register, formState }) => (
        <>
          <Input
            type="email"
            label="Correo institucional"
            autoComplete="email"
            error={formState.errors['email']}
            registration={register('email')}
          />
          <Button
            isLoading={recuperar.isPending}
            type="submit"
            variant="secondary"
            className="w-full"
          >
            Enviar enlace
          </Button>
          <Link
            to={paths.auth.login.getHref()}
            className="block text-center text-body-3 text-content-accent-secondary"
          >
            Volver a iniciar sesión
          </Link>
        </>
      )}
    </Form>
  );
};
