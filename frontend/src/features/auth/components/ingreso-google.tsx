import { AxiosError } from 'axios';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { LogoGoogle } from '@/components/ui/logo-google';
import { useLogin, useMetodosAcceso } from '@/lib/auth';
import { GoogleCancelado, cargarGoogle, pedirCodigoGoogle } from '@/lib/google';

const NO_SE_PUDO =
  'No se pudo iniciar sesión con Google. Inténtalo nuevamente.';

const mensajeDeError = (error: unknown) => {
  if (error instanceof AxiosError) {
    if (!error.response) {
      return 'No se pudo conectar con el servidor. Inténtalo nuevamente.';
    }
    return (error.response.data as { message?: string })?.message ?? NO_SE_PUDO;
  }
  return NO_SE_PUDO;
};

type IngresoGoogleProps = {
  onSuccess: () => void;
};

/**
 * Figma > 🌟 Autenticación > Iniciar sesión: «o» y el botón «Iniciar sesión con Google» (HU-3). Solo aparece si el
 * administrador activó Google (HU-2) y el servidor tiene las llaves de Google.
 */
export const IngresoGoogle = ({ onSuccess }: IngresoGoogleProps) => {
  const metodos = useMetodosAcceso();
  const login = useLogin({ onSuccess });
  const [listo, setListo] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const idCliente = metodos.data?.google
    ? (metodos.data.googleIdCliente ?? null)
    : null;

  React.useEffect(() => {
    if (!idCliente) return;
    let vigente = true;
    cargarGoogle()
      .then(() => {
        if (vigente) setListo(true);
      })
      .catch(() => {
        if (vigente)
          setError(
            'No se pudo cargar el inicio de sesión con Google. Revisa tu conexión.',
          );
      });
    return () => {
      vigente = false;
    };
  }, [idCliente]);

  if (!idCliente) return null;

  const entrar = async () => {
    setError(null);
    login.reset();
    try {
      // Se llama en el mismo clic: el navegador solo permite abrir la ventana de Google así.
      const codigo = await pedirCodigoGoogle(idCliente);
      login.mutate({ codigoGoogle: codigo });
    } catch (e) {
      if (!(e instanceof GoogleCancelado)) setError(NO_SE_PUDO);
    }
  };

  const mensaje = error ?? (login.isError ? mensajeDeError(login.error) : null);

  return (
    <div className="mt-5 space-y-5">
      <p className="text-center text-body-3 text-content-tertiary">o</p>
      <div className="space-y-2">
        <Button
          type="button"
          variant="primary-light"
          className="h-12 w-full gap-2"
          icon={<LogoGoogle className="size-4" />}
          onClick={entrar}
          disabled={!listo || login.isPending}
          isLoading={login.isPending}
        >
          Iniciar sesión con Google
        </Button>
        {mensaje && (
          <p role="alert" className="text-body-4 text-content-error">
            {mensaje}
          </p>
        )}
      </div>
    </div>
  );
};
