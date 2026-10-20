import * as React from 'react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';

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
  children: React.ReactNode;
};

// Marco común de los modales "Nueva área curricular", "Nueva competencia" y "Nuevo curso" (Figma CI-14).
export const DialogoCatalogo = ({
  open,
  onCerrar,
  icono,
  titulo,
  descripcion,
  formId,
  textoGuardar,
  guardando,
  children,
}: DialogoCatalogoProps) => (
  <Dialog open={open} onOpenChange={(abierto) => !abierto && onCerrar()}>
    <DialogContent className="max-w-[520px] gap-0 rounded-[12px] border-stroke-tertiary bg-white p-0">
      <div className="flex flex-col gap-3 px-6 pt-6">
        <span className="inline-flex size-10 items-center justify-center rounded-full bg-surface-accent-light text-content-accent-primary">
          {icono}
        </span>
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
