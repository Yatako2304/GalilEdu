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
import { useNotifications } from '@/components/ui/notifications';
import { SeccionResumen } from '@/types/api';

import { useEliminarSeccion } from '../../api/mutaciones-seccion';

type DialogoEliminarSeccionProps = {
  seccion: SeccionResumen;
  /** "4.° B": el grado y la sección, como en la lista. */
  etiqueta: string;
  onCerrar: () => void;
};

// Confirmación para eliminar una sección (HU-75). No está dibujada en Figma: usa el mismo estilo que las confirmaciones
// del año (ConfirmationDialog). El backend solo la elimina si nunca tuvo matrículas ni cambios de sección (409 si no).
export const DialogoEliminarSeccion = ({
  seccion,
  etiqueta,
  onCerrar,
}: DialogoEliminarSeccionProps) => {
  const { addNotification } = useNotifications();
  // Si el backend lo rechaza, el interceptor avisa con su mensaje y el diálogo sigue abierto.
  const eliminar = useEliminarSeccion(() => {
    addNotification({
      type: 'success',
      title: 'Sección eliminada',
      message: etiqueta,
    });
    onCerrar();
  });

  return (
    <Dialog open onOpenChange={(open) => !open && onCerrar()}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader className="flex">
          <DialogTitle className="flex items-center gap-2">
            <OctagonAlert
              className="size-6 text-icon-error-primary"
              aria-hidden="true"
            />
            ¿Eliminar la sección {etiqueta}?
          </DialogTitle>
        </DialogHeader>
        <DialogDescription>
          Se eliminará junto con sus tutores. Solo se puede eliminar una sección
          que nunca tuvo matrículas; si ya las tuvo, no se elimina.
        </DialogDescription>
        <DialogFooter>
          <Button
            variant="destructive"
            isLoading={eliminar.isPending}
            disabled={eliminar.isPending}
            onClick={() => eliminar.mutate(seccion.id)}
          >
            Eliminar
          </Button>
          <Button variant="outline" onClick={onCerrar}>
            Cancelar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};
