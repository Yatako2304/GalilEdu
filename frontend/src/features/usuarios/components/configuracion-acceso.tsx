import { KeyRound } from 'lucide-react';
import * as React from 'react';

import { Alert } from '@/components/ui/alert';
import { Card, CardHeader, CardTitle } from '@/components/ui/card';
import { Switch } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { LogoGoogle } from '@/components/ui/logo-google';
import { useNotifications } from '@/components/ui/notifications';
import { Spinner } from '@/components/ui/spinner';
import { Tag } from '@/components/ui/tag';

import {
  useCambiarAccesoGoogle,
  useConfiguracionAcceso,
} from '../api/configuracion-acceso';

const Metodo = ({
  icono,
  titulo,
  descripcion,
  control,
}: {
  icono: React.ReactNode;
  titulo: string;
  descripcion: React.ReactNode;
  control: React.ReactNode;
}) => (
  <div className="flex items-center gap-4 px-5 py-4">
    <IconBadge shape="square">{icono}</IconBadge>
    <div className="min-w-0 flex-1">
      <p className="text-body-3 font-medium text-content-primary">{titulo}</p>
      <p className="text-body-4 text-content-tertiary">{descripcion}</p>
    </div>
    {control}
  </div>
);

/**
 * HU-2 · Administrador › Seguridad › Configuración de acceso. Figma no tiene esta pantalla: se arma con la tarjeta y
 * el interruptor del sistema de diseño. Usuario y contraseña siempre están disponibles.
 */
export const ConfiguracionAccesoPanel = () => {
  const configuracion = useConfiguracionAcceso();
  const { addNotification } = useNotifications();
  const cambiar = useCambiarAccesoGoogle((c) =>
    addNotification({
      type: 'success',
      title: c.googleHabilitado
        ? 'Inicio de sesión con Google activado'
        : 'Inicio de sesión con Google desactivado',
      message: c.googleHabilitado
        ? 'El botón «Iniciar sesión con Google» ya aparece en la pantalla de acceso.'
        : 'Los usuarios ingresan solo con su usuario y contraseña.',
    }),
  );

  if (configuracion.isPending) {
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    );
  }
  if (!configuracion.data) {
    return (
      <Alert
        type="error"
        title="No se pudo cargar la configuración de acceso."
      />
    );
  }

  const { googleHabilitado, googleConfigurado } = configuracion.data;
  // Sin las llaves de Google solo se permite apagarlo (si quedó encendido).
  const bloqueado =
    cambiar.isPending || (!googleConfigurado && !googleHabilitado);

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Métodos de inicio de sesión</CardTitle>
        </CardHeader>
        <div className="divide-y divide-stroke-tertiary">
          <Metodo
            icono={<KeyRound aria-hidden="true" />}
            titulo="Usuario y contraseña"
            descripcion="Los usuarios ingresan con las credenciales que reciben por correo. Siempre está disponible."
            control={<Tag color="neutral">Siempre activo</Tag>}
          />
          <Metodo
            icono={<LogoGoogle />}
            titulo="Inicio de sesión con Google"
            descripcion="Muestra el botón «Iniciar sesión con Google». Solo ingresa quien tenga registrado en GalilEdu el mismo correo de su cuenta de Google. Actívalo si el colegio usa correos de Google."
            control={
              <Switch
                checked={googleHabilitado}
                disabled={bloqueado}
                onCheckedChange={(valor) => cambiar.mutate(valor)}
                className="data-[state=checked]:bg-surface-accent-primary data-[state=unchecked]:bg-slate-300"
                aria-label="Inicio de sesión con Google"
              />
            }
          />
        </div>
      </Card>
      {!googleConfigurado && (
        <Alert type="warning" title="Falta configurar Google en el servidor">
          Para activar el inicio de sesión con Google, el servidor necesita
          GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET (ver la guía de despliegue).
        </Alert>
      )}
    </div>
  );
};
