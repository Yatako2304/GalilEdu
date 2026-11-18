import { CircleCheck, CircleOff } from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { AmbienteResumen } from '@/types/api';
import { cn } from '@/utils/cn';

import { useDesactivarAmbiente, useReactivarAmbiente } from '../api/ambientes';

export type CambioEstadoAmbiente = {
  ambiente: AmbienteResumen;
  accion: 'desactivar' | 'reactivar';
};

/**
 * Figma no dibuja el modal "¿Desactivar …?" de CI-26: es una confirmación genérica, con la estructura de los modales
 * de CI-14. Los textos son nuestros. Desactivar se permite aunque el ambiente esté en uso: lo único que cambia es que
 * ya no se ofrece al armar horarios.
 */
export const textosCambioEstadoAmbiente = ({
  ambiente,
  accion,
}: CambioEstadoAmbiente) =>
  accion === 'desactivar'
    ? {
        titulo: `¿Desactivar el ambiente ${ambiente.nombre}?`,
        descripcion: 'Ya no podrá elegirse al armar horarios.',
        boton: 'Desactivar',
      }
    : {
        titulo: `¿Reactivar el ambiente ${ambiente.nombre}?`,
        descripcion: 'Volverá a poder elegirse al armar horarios.',
        boton: 'Reactivar',
      };

type DialogoCambiarEstadoAmbienteProps = {
  cambio: CambioEstadoAmbiente | null;
  onCerrar: () => void;
};

export const DialogoCambiarEstadoAmbiente = ({
  cambio,
  onCerrar,
}: DialogoCambiarEstadoAmbienteProps) => {
  const desactivar = useDesactivarAmbiente(onCerrar);
  const reactivar = useReactivarAmbiente(onCerrar);
  if (!cambio) return null;
  const { titulo, descripcion, boton } = textosCambioEstadoAmbiente(cambio);
  const esDesactivar = cambio.accion === 'desactivar';
  const mutacion = esDesactivar ? desactivar : reactivar;
  const Icono = esDesactivar ? CircleOff : CircleCheck;

  return (
    <Dialog open onOpenChange={(abierto) => !abierto && onCerrar()}>
      <DialogContent className="max-w-[448px] gap-0 rounded-xl border-stroke-tertiary bg-white p-0">
        <div className="flex flex-col gap-3 px-6 pt-6">
          <span
            className={cn(
              'inline-flex size-10 items-center justify-center rounded-full',
              esDesactivar
                ? 'bg-surface-error-light text-content-error'
                : 'bg-surface-success-light text-content-success',
            )}
          >
            <Icono className="size-5" aria-hidden="true" />
          </span>
          <DialogTitle className="text-h6 font-semibold text-content-primary">
            {titulo}
          </DialogTitle>
          <DialogDescription className="pb-5 text-body-3 text-content-tertiary">
            {descripcion}
          </DialogDescription>
        </div>
        <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
          <Button
            variant="primary-light"
            onClick={onCerrar}
            disabled={mutacion.isPending}
          >
            Cancelar
          </Button>
          <Button
            variant={esDesactivar ? 'error' : 'primary'}
            isLoading={mutacion.isPending}
            onClick={() => mutacion.mutate(cambio.ambiente.id)}
          >
            {boton}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
