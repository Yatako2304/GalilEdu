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
import { DetalleEstructuraHoraria } from '@/types/api';

import { useEliminarEstructura } from '../../api/mutaciones-estructuras';

type DialogoEliminarEstructuraProps = {
  estructura: DetalleEstructuraHoraria;
  onCerrar: () => void;
  /** Se llama al eliminarla, para que la pantalla elija otra en la lista. */
  onEliminada?: () => void;
};

// Confirmación para eliminar una estructura horaria (HU-44). No está dibujada en Figma: usa el mismo estilo que las
// confirmaciones del año. El backend responde 409 si es la principal y quedan otras del nivel ("Marca otra como
// principal primero."), si es la única del nivel y hay cursos planificados, o si el año no es modificable.
export const DialogoEliminarEstructura = ({
  estructura,
  onCerrar,
  onEliminada,
}: DialogoEliminarEstructuraProps) => {
  const { addNotification } = useNotifications();
  // Si el backend lo rechaza, el interceptor avisa con su mensaje y el diálogo sigue abierto.
  const eliminar = useEliminarEstructura(() => {
    addNotification({
      type: 'success',
      title: 'Estructura eliminada',
      message: estructura.codigo,
    });
    onEliminada?.();
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
            ¿Eliminar la estructura {estructura.codigo}?
          </DialogTitle>
        </DialogHeader>
        <DialogDescription>
          Se eliminará junto con sus recesos. Si es la principal de su nivel y
          quedan otras estructuras, primero marca otra como principal.
        </DialogDescription>
        <DialogFooter>
          <Button
            variant="destructive"
            isLoading={eliminar.isPending}
            disabled={eliminar.isPending}
            onClick={() => eliminar.mutate(estructura.id)}
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
