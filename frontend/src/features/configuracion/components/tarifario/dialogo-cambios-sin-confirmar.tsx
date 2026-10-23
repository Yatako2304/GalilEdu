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

type DialogoCambiosSinConfirmarProps = {
  abierto: boolean;
  titulo: string;
  /** Lo que hace salir: "Salir sin confirmar" o "Cambiar de año". */
  textoSalir: string;
  /** Sigue en la pantalla con lo que tenía escrito. */
  onSeguir: () => void;
  /** Descarta lo escrito y sigue con lo que iba a hacer. */
  onSalir: () => void;
};

// No está en Figma. Avisa antes de perder lo escrito: P19 no tiene "Guardar", así que lo que no se confirma se pierde.
export const DialogoCambiosSinConfirmar = ({
  abierto,
  titulo,
  textoSalir,
  onSeguir,
  onSalir,
}: DialogoCambiosSinConfirmarProps) => (
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
          Tienes cambios sin confirmar en el tarifario. Si sigues, se perderán.
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
