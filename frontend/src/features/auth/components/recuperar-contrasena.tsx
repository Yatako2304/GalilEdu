import { useMutation } from '@tanstack/react-query';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { CodigoVerificacion, Input } from '@/components/ui/form';
import { useNotifications } from '@/components/ui/notifications';
import { paths } from '@/config/paths';
import {
  recuperarContrasenaInputSchema,
  restablecerContrasena,
  solicitarRecuperacion,
  verificarCodigoRecuperacion,
} from '@/lib/auth';

import {
  NuevaContrasenaForm,
  PantallaContrasena,
  Tarjeta,
  comoError,
  mensajeDe,
} from './pantalla-contrasena';

/** Segundos que hay que esperar para pedir otro código (igual que el backend). */
export const ESPERA_REENVIO = 60;
const LONGITUD_CODIGO = 6;

type Paso = 'correo' | 'codigo' | 'contrasena';

/**
 * HU-4 · Figma > 🌟 Autenticación > Restablecer contraseña: correo → código de 6 dígitos que llega al correo → contraseña
 * nueva. Al terminar, `onRestablecida` lleva al login, que muestra «Contraseña restablecida correctamente».
 */
export const RecuperarContrasena = ({
  onRestablecida,
}: {
  onRestablecida: () => void;
}) => {
  const { addNotification } = useNotifications();
  const [paso, setPaso] = React.useState<Paso>('correo');
  const [correo, setCorreo] = React.useState('');
  const [codigo, setCodigo] = React.useState('');
  const [error, setError] = React.useState<string | null>(null);

  // «Reenviar código (60 s)»: hasta cuándo hay que esperar para pedir otro código.
  const [esperarHasta, setEsperarHasta] = React.useState(0);
  const [ahora, setAhora] = React.useState(() => Date.now());
  const segundos = Math.max(0, Math.ceil((esperarHasta - ahora) / 1000));
  const iniciarEspera = () => {
    const momento = Date.now();
    setAhora(momento);
    setEsperarHasta(momento + ESPERA_REENVIO * 1000);
  };

  React.useEffect(() => {
    if (esperarHasta <= Date.now()) return;
    const reloj = window.setInterval(() => {
      const momento = Date.now();
      setAhora(momento);
      if (momento >= esperarHasta) window.clearInterval(reloj);
    }, 1000);
    return () => window.clearInterval(reloj);
  }, [esperarHasta]);

  const solicitar = useMutation({ mutationFn: solicitarRecuperacion });
  const verificar = useMutation({ mutationFn: verificarCodigoRecuperacion });
  const restablecer = useMutation({ mutationFn: restablecerContrasena });

  const irA = (siguiente: Paso) => {
    setError(null);
    setPaso(siguiente);
  };

  const enviarCorreo = (evento: React.FormEvent) => {
    evento.preventDefault();
    const revisado = recuperarContrasenaInputSchema.safeParse({
      email: correo.trim(),
    });
    if (!revisado.success) {
      setError(revisado.error.issues[0]?.message ?? 'Ingrese un correo válido');
      return;
    }
    solicitar.mutate(revisado.data, {
      onSuccess: () => {
        setCorreo(revisado.data.email);
        iniciarEspera();
        irA('codigo');
      },
      onError: (e) => setError(mensajeDe(e)),
    });
  };

  const reenviar = () => {
    setError(null);
    solicitar.mutate(
      { email: correo },
      {
        onSuccess: () => {
          setCodigo('');
          iniciarEspera();
          addNotification({
            type: 'success',
            title: 'Código reenviado',
            message: 'Te enviamos un nuevo código a tu correo.',
          });
        },
        onError: (e) => setError(mensajeDe(e)),
      },
    );
  };

  const validarCodigo = (evento: React.FormEvent) => {
    evento.preventDefault();
    verificar.mutate(
      { email: correo, codigo },
      {
        onSuccess: () => irA('contrasena'),
        onError: (e) => setError(mensajeDe(e)),
      },
    );
  };

  return (
    <PantallaContrasena
      cerrarHref={paths.auth.login.getHref()}
      cerrarEtiqueta="Volver a iniciar sesión"
    >
      {paso === 'correo' && (
        <Tarjeta
          titulo="Restablecer contraseña"
          descripcion="Ingrese su correo electrónico registrado para continuar con el proceso."
        >
          <form onSubmit={enviarCorreo} className="space-y-6" noValidate>
            <Input
              type="email"
              label="Correo institucional"
              autoComplete="email"
              value={correo}
              onChange={(e) => {
                setCorreo(e.target.value);
                setError(null);
              }}
              error={comoError(error)}
            />
            <Button
              type="submit"
              variant="secondary"
              className="w-full"
              isLoading={solicitar.isPending}
              disabled={correo.trim() === '' || solicitar.isPending}
            >
              Enviar código
            </Button>
          </form>
        </Tarjeta>
      )}

      {paso === 'codigo' && (
        <Tarjeta
          titulo="Código de verificación"
          descripcion="Ingrese el código que ha sido enviado a su correo. En caso no encontrarlo, revise su bandeja de spam"
        >
          <form onSubmit={validarCodigo} className="space-y-6">
            <CodigoVerificacion
              value={codigo}
              onChange={(valor) => {
                setCodigo(valor);
                setError(null);
              }}
              longitud={LONGITUD_CODIGO}
              error={error}
            />
            <button
              type="button"
              onClick={reenviar}
              disabled={segundos > 0 || solicitar.isPending}
              className="text-body-4 font-medium text-content-accent-secondary disabled:cursor-not-allowed disabled:opacity-60"
            >
              {segundos > 0
                ? `Reenviar código (${segundos} s)`
                : 'Reenviar código'}
            </button>
            <Button
              type="submit"
              variant="secondary"
              className="w-full"
              isLoading={verificar.isPending}
              disabled={codigo.length < LONGITUD_CODIGO || verificar.isPending}
            >
              Validar código
            </Button>
          </form>
        </Tarjeta>
      )}

      {paso === 'contrasena' && (
        <Tarjeta
          titulo="Nueva contraseña"
          descripcion="Ingrese su nueva contraseña y confírmela para acceder a su cuenta"
        >
          <NuevaContrasenaForm
            textoBoton="Restablecer contraseña"
            cargando={restablecer.isPending}
            error={error}
            onCambio={() => setError(null)}
            onGuardar={(contrasena) =>
              restablecer.mutate(
                { email: correo, codigo, contrasena },
                {
                  onSuccess: onRestablecida,
                  onError: (e) => setError(mensajeDe(e)),
                },
              )
            }
            debajo={
              <button
                type="button"
                onClick={() => {
                  setCodigo('');
                  irA('codigo');
                }}
                className="text-body-4 font-medium text-content-accent-secondary"
              >
                Volver al código de verificación
              </button>
            }
          />
        </Tarjeta>
      )}
    </PantallaContrasena>
  );
};
