import { Save, Trash2, UserCheck, UserX } from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { cn } from '@/utils/cn';

export type TipoConfirmacion =
  'guardar' | 'desactivar' | 'reactivar' | 'eliminar';

const ICONO = {
  guardar: {
    Icono: Save,
    clase: 'bg-surface-accent-light text-content-accent-primary',
  },
  desactivar: {
    Icono: UserX,
    clase: 'bg-surface-error-light text-content-error',
  },
  reactivar: {
    Icono: UserCheck,
    clase: 'bg-surface-success-light text-content-success',
  },
  eliminar: {
    Icono: Trash2,
    clase: 'bg-surface-error-light text-content-error',
  },
};

type DialogoConfirmacionProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  tipo: TipoConfirmacion;
  titulo: string;
  descripcion: string;
  onConfirmar: () => void;
  cargando?: boolean;
  textoConfirmar?: string;
};

// Figma > Usuarios > modales "¿Guardar los cambios…?", "¿Desactivar a…?", "¿Reactivar a…?".
export const DialogoConfirmacion = ({
  open,
  onOpenChange,
  tipo,
  titulo,
  descripcion,
  onConfirmar,
  cargando,
  textoConfirmar = 'Confirmar',
}: DialogoConfirmacionProps) => {
  const { Icono, clase } = ICONO[tipo];
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-[448px] gap-0 rounded-[12px] border-stroke-tertiary bg-white p-0">
        <div className="flex flex-col items-center gap-4 px-8 pb-6 pt-8 text-center">
          <span
            className={cn(
              'inline-flex size-16 items-center justify-center rounded-full',
              clase,
            )}
          >
            <Icono className="size-7" aria-hidden="true" />
          </span>
          <DialogTitle className="text-h6 font-semibold text-content-primary">
            {titulo}
          </DialogTitle>
          <DialogDescription className="text-body-3 text-content-tertiary">
            {descripcion}
          </DialogDescription>
        </div>
        <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
          <Button
            variant="primary-light"
            onClick={() => onOpenChange(false)}
            disabled={cargando}
          >
            Cancelar
          </Button>
          <Button
            variant={tipo === 'eliminar' ? 'error' : 'primary'}
            onClick={onConfirmar}
            isLoading={cargando}
          >
            {textoConfirmar}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
