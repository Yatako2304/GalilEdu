import * as React from 'react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { IconBadge } from '@/components/ui/icon-badge';
import { cn } from '@/utils/cn';

type DialogoCatalogoProps = {
  open: boolean;
  onCerrar: () => void;
  icono: React.ReactNode;
  titulo: string;
  descripcion?: string;
  /** id del <form> que envía el botón principal. */
  formId: string;
  textoGuardar: string;
  guardando: boolean;
  /** Ancho del modal; por defecto 520 px. */
  className?: string;
  children: React.ReactNode;
};

// Marco común de los modales "Nueva área curricular", "Nueva competencia" y "Nuevo curso" (CI-14) y "Nueva versión de la tabla de conversión" (CI-23).
export const DialogoCatalogo = ({
  open,
  onCerrar,
  icono,
  titulo,
  descripcion,
  formId,
  textoGuardar,
  guardando,
  className,
  children,
}: DialogoCatalogoProps) => (
  <Dialog open={open} onOpenChange={(abierto) => !abierto && onCerrar()}>
    <DialogContent
      className={cn(
        'max-h-[92vh] max-w-[520px] gap-0 overflow-y-auto rounded-[12px] border-stroke-tertiary bg-white p-0',
        className,
      )}
    >
      <div className="flex flex-col gap-3 px-6 pt-6">
        <IconBadge>{icono}</IconBadge>
        <div>
          <DialogTitle className="text-h6 font-semibold text-content-primary">
            {titulo}
          </DialogTitle>
          <DialogDescription
            className={
              descripcion ? 'text-body-3 text-content-tertiary' : 'sr-only'
            }
          >
            {descripcion ?? titulo}
          </DialogDescription>
        </div>
      </div>
      <div className="space-y-4 px-6 py-5">{children}</div>
      <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
        <Button variant="primary-light" onClick={onCerrar} disabled={guardando}>
          Cancelar
        </Button>
        <Button type="submit" form={formId} isLoading={guardando}>
          {textoGuardar}
        </Button>
      </div>
    </DialogContent>
  </Dialog>
);
