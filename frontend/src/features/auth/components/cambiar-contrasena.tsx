import { useMutation } from '@tanstack/react-query';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { paths } from '@/config/paths';
import { cambiarContrasena, verificarContrasenaActual } from '@/lib/auth';

import {
  CampoContrasena,
  NuevaContrasenaForm,
  PantallaContrasena,
  Tarjeta,
  mensajeDe,
} from './pantalla-contrasena';

type Paso = 'actual' | 'nueva';

/**
 * HU-5 · Figma > 🌟 Autenticación > Cambiar contraseña: contraseña actual → nueva contraseña. Al terminar se cierran
 * las sesiones en todos los dispositivos y `onCambiada` lleva al login con «Contraseña actualizada correctamente».
 */
export const CambiarContrasena = ({
  onCambiada,
}: {
  onCambiada: () => void;
}) => {
  const [paso, setPaso] = React.useState<Paso>('actual');
  const [actual, setActual] = React.useState('');
  const [error, setError] = React.useState<string | null>(null);

  const verificar = useMutation({ mutationFn: verificarContrasenaActual });
  const cambiar = useMutation({ mutationFn: cambiarContrasena });

  return (
    <PantallaContrasena
      cerrarHref={paths.app.root.getHref()}
      cerrarEtiqueta="Volver al inicio"
    >
      {paso === 'actual' && (
        <Tarjeta
          titulo="Cambiar contraseña"
          descripcion="Ingrese su contraseña actual para continuar con el proceso."
        >
          <form
            onSubmit={(evento) => {
              evento.preventDefault();
              verificar.mutate(
                { contrasenaActual: actual },
                {
                  onSuccess: () => {
                    setError(null);
                    setPaso('nueva');
                  },
                  onError: (e) => setError(mensajeDe(e)),
                },
              );
            }}
            className="space-y-6"
          >
            <CampoContrasena
              label="Contraseña actual"
              autoComplete="current-password"
              value={actual}
              onChange={(valor) => {
                setActual(valor);
                setError(null);
              }}
              error={error}
            />
            <Button
              type="submit"
              variant="secondary"
              className="w-full"
              isLoading={verificar.isPending}
              disabled={actual === '' || verificar.isPending}
            >
              Continuar
            </Button>
          </form>
        </Tarjeta>
      )}

      {paso === 'nueva' && (
        <Tarjeta
          titulo="Nueva contraseña"
          descripcion="Ingrese su nueva contraseña y confírmela para acceder a su cuenta"
        >
          <NuevaContrasenaForm
            textoBoton="Cambiar contraseña"
            cargando={cambiar.isPending}
            error={error}
            onCambio={() => setError(null)}
            onGuardar={(contrasenaNueva) =>
              cambiar.mutate(
                { contrasenaActual: actual, contrasenaNueva },
                {
                  onSuccess: onCambiada,
                  onError: (e) => setError(mensajeDe(e)),
                },
              )
            }
          />
        </Tarjeta>
      )}
    </PantallaContrasena>
  );
};
