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
import { FilaPeriodoMatricula } from '@/types/api';

import { useEliminarPeriodoMatricula } from '../../api/mutaciones-periodo-matricula';

type DialogoEliminarPeriodoProps = {
  periodo: FilaPeriodoMatricula;
  abierto: boolean;
  onCerrar: () => void;
};

// Confirmación de "Eliminar" del menú ⋮ de un periodo PROGRAMADO (HU-23). Si el backend lo rechaza (el año ya tiene
// matrículas o hay documentos entregados, 409), su mensaje sale como notificación y el periodo se queda como estaba.
export const DialogoEliminarPeriodo = ({
  periodo,
  abierto,
  onCerrar,
}: DialogoEliminarPeriodoProps) => {
  const { addNotification } = useNotifications();
  const eliminar = useEliminarPeriodoMatricula();

  const confirmar = async () => {
    try {
      await eliminar.mutateAsync(periodo.id);
      addNotification({
        type: 'success',
        title: 'Periodo de matrícula eliminado',
        message: periodo.nombre,
      });
    } catch {
      // el motivo ya salió como notificación
    }
    onCerrar();
  };

  return (
    <Dialog open={abierto} onOpenChange={(open) => !open && onCerrar()}>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <OctagonAlert
              className="size-6 text-icon-error-primary"
              aria-hidden="true"
            />
            ¿Eliminar {periodo.nombre}?
          </DialogTitle>
          <DialogDescription>
            Se eliminará el periodo con sus documentos y plantillas. Esta acción
            no se puede deshacer.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button
            variant="error"
            isLoading={eliminar.isPending}
            disabled={eliminar.isPending}
            onClick={confirmar}
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
