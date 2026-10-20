import { LogOut } from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';

type DialogoRetiroProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  alumno: string;
  onConfirmar: () => void;
  cargando?: boolean;
};

// Figma > Personal Administrativo > modal "Registrar retiro del alumno" (RF-85).
export const DialogoRetiro = ({
  open,
  onOpenChange,
  alumno,
  onConfirmar,
  cargando,
}: DialogoRetiroProps) => (
  <Dialog open={open} onOpenChange={onOpenChange}>
    <DialogContent className="max-w-[448px] gap-0 rounded-[12px] border-stroke-tertiary bg-white p-0">
      <div className="flex flex-col items-center gap-4 px-8 pb-6 pt-8 text-center">
        <span className="inline-flex size-11 items-center justify-center rounded-[12px] bg-surface-warning-light text-content-primary">
          <LogOut className="size-5" aria-hidden="true" />
        </span>
        <DialogTitle className="text-h6 font-semibold text-content-primary">
          Registrar retiro del alumno
        </DialogTitle>
        <DialogDescription className="text-left text-body-3 text-content-tertiary">
          {alumno}: se anularán las cuotas pendientes con vencimiento posterior
          a la fecha de retiro. Las cuotas pagadas y las vencidas antes del
          retiro se mantienen.
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
        <Button variant="primary" onClick={onConfirmar} isLoading={cargando}>
          Confirmar
        </Button>
      </div>
    </DialogContent>
  </Dialog>
);
