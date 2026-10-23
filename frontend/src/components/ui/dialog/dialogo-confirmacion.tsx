import { Save, Trash2, UserCheck, UserX } from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { IconBadge } from '@/components/ui/icon-badge';

export type TipoConfirmacion =
  'guardar' | 'desactivar' | 'reactivar' | 'eliminar';

const ICONO: Record<
  TipoConfirmacion,
  { Icono: typeof Save; color: 'accent' | 'success' | 'error' }
> = {
  guardar: {
    Icono: Save,
    color: 'accent',
  },
  desactivar: {
    Icono: UserX,
    color: 'error',
  },
  reactivar: {
    Icono: UserCheck,
    color: 'success',
  },
  eliminar: {
    Icono: Trash2,
    color: 'error',
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
  const { Icono, color } = ICONO[tipo];
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-[448px] gap-0 rounded-xl border-stroke-tertiary bg-white p-0">
        <div className="flex flex-col items-center gap-4 px-8 pb-6 pt-8 text-center">
          <IconBadge color={color} size="xl">
            <Icono aria-hidden="true" />
          </IconBadge>
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
