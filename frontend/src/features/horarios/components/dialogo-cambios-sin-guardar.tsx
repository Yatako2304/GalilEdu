import { OctagonAlert } from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

type DialogoCambiosSinGuardarProps = {
  abierto: boolean;
  titulo: string;
  /** Lo que hace salir: «Salir sin guardar» o «Cambiar de sección». */
  textoSalir: string;
  /** Sigue en la pantalla con el borrador que tenía. */
  onSeguir: () => void;
  /** Descarta el borrador y sigue con lo que iba a hacer. */
  onSalir: () => void;
};

// No está en Figma. El horario se edita en un borrador del navegador: si se sale sin guardar, se pierde.
export const DialogoCambiosSinGuardar = ({
  abierto,
  titulo,
  textoSalir,
  onSeguir,
  onSalir,
}: DialogoCambiosSinGuardarProps) => (
  <Dialog open={abierto} onOpenChange={(open) => !open && onSeguir()}>
    <DialogContent className="sm:max-w-[425px]">
      <DialogHeader>
        <DialogTitle className="flex items-center gap-2">
          <OctagonAlert
            className="size-6 text-icon-error-primary"
            aria-hidden="true"
          />
          {titulo}
        </DialogTitle>
        <DialogDescription>
          Tienes cambios sin guardar en el horario. Si sigues, se perderán.
        </DialogDescription>
      </DialogHeader>
      <DialogFooter>
        <Button variant="error" onClick={onSalir}>
          {textoSalir}
        </Button>
        <Button variant="outline" onClick={onSeguir}>
          Seguir editando
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
);
