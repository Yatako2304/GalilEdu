import { RefreshCw } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { useNotifications } from '@/components/ui/notifications';
import { DetalleMatricula } from '@/types/api';

import {
  useCambiarSeccion,
  useSeccionesDeMatricula,
} from '../api/matricula-detalle';

import {
  DialogoCambiarSeccion,
  MotivoElegido,
} from './dialogo-cambiar-seccion';

// Figma > Personal Administrativo > A5 · botón "Cambiar sección" y su modal. Lo usan el detalle de
// matrícula y la ficha del estudiante.
export const BotonCambiarSeccion = ({
  matricula,
}: {
  matricula: DetalleMatricula;
}) => {
  const { addNotification } = useNotifications();
  const [cambiando, setCambiando] = React.useState(false);
  const secciones = useSeccionesDeMatricula(matricula.id, cambiando);
  const cambiarSeccion = useCambiarSeccion(matricula.id);

  const confirmarCambio = async (seccionId: string, motivo?: MotivoElegido) => {
    // El modal pide el motivo; sin él no se envía nada (el backend también lo exige).
    if (!motivo) return;
    try {
      const nuevo = await cambiarSeccion.mutateAsync({ seccionId, ...motivo });
      addNotification({
        type: 'success',
        title: 'Sección cambiada',
        message: `${matricula.nombre} pasó a ${nuevo.seccion?.etiqueta}.`,
      });
      setCambiando(false);
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    }
  };

  return (
    <>
      <Button
        variant="secondary"
        disabled={!matricula.puedeCambiarSeccion}
        onClick={() => setCambiando(true)}
        icon={<RefreshCw className="size-4" aria-hidden="true" />}
      >
        Cambiar sección
      </Button>
      {cambiando && (
        <DialogoCambiarSeccion
          secciones={secciones.data}
          cargando={secciones.isLoading}
          guardando={cambiarSeccion.isPending}
          pedirMotivo
          onCerrar={() => setCambiando(false)}
          alConfirmar={confirmarCambio}
        />
      )}
    </>
  );
};
