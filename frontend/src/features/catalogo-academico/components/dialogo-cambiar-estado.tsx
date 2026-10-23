import { CircleCheck, CircleOff } from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { cn } from '@/utils/cn';

import {
  AccionEstado,
  RecursoCatalogo,
  useCambiarEstado,
} from '../api/plan-curricular';

export type CambioEstado = {
  recurso: RecursoCatalogo;
  id: string;
  nombre: string;
  accion: AccionEstado;
};

const ARTICULO: Record<RecursoCatalogo, string> = {
  areas: 'el área',
  competencias: 'la competencia',
  cursos: 'el curso',
};

/** Textos de Figma CI-14 > "Modal - ¿Desactivar el área Matemática?" / "¿Desactivar el curso Geometría?". */
export const textosCambioEstado = ({
  recurso,
  nombre,
  accion,
}: CambioEstado) =>
  accion === 'desactivar'
    ? {
        titulo: `¿Desactivar ${ARTICULO[recurso]} ${nombre}?`,
        descripcion: 'Ya no podrá agregarse a nuevos planes de evaluación.',
        boton: 'Desactivar',
      }
    : {
        titulo: `¿Reactivar ${ARTICULO[recurso]} ${nombre}?`,
        descripcion: 'Podrá volver a agregarse a nuevos planes de evaluación.',
        boton: 'Reactivar',
      };

type DialogoCambiarEstadoProps = {
  cambio: CambioEstado | null;
  onCerrar: () => void;
};

export const DialogoCambiarEstado = ({
  cambio,
  onCerrar,
}: DialogoCambiarEstadoProps) => {
  const cambiar = useCambiarEstado(onCerrar);
  if (!cambio) return null;
  const { titulo, descripcion, boton } = textosCambioEstado(cambio);
  const desactivar = cambio.accion === 'desactivar';
  const Icono = desactivar ? CircleOff : CircleCheck;

  return (
    <Dialog open onOpenChange={(abierto) => !abierto && onCerrar()}>
      <DialogContent className="max-w-[448px] gap-0 rounded-xl border-stroke-tertiary bg-white p-0">
        <div className="flex flex-col gap-3 px-6 pt-6">
          <span
            className={cn(
              'inline-flex size-10 items-center justify-center rounded-full',
              desactivar
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
            disabled={cambiar.isPending}
          >
            Cancelar
          </Button>
          <Button
            variant={desactivar ? 'error' : 'primary'}
            isLoading={cambiar.isPending}
            onClick={() => cambiar.mutate(cambio)}
          >
            {boton}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
