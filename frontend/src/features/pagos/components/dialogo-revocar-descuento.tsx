import { zodResolver } from '@hookform/resolvers/zod';
import { MinusCircle } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { useNotifications } from '@/components/ui/notifications';

import { useRevocarDescuento } from '../api/descuentos';

const esquema = z.object({
  motivo: z
    .string()
    .trim()
    .min(1, 'Ingresa el motivo de la revocación.')
    .max(500, 'El motivo admite hasta 500 caracteres.'),
});

type Valores = z.infer<typeof esquema>;

export const DialogoRevocarDescuento = ({
  abierto,
  onOpenChange,
  compromisoId,
  descuentoAplicadoId,
  nombre,
}: {
  abierto: boolean;
  onOpenChange: (abierto: boolean) => void;
  compromisoId: string;
  descuentoAplicadoId: string;
  nombre: string;
}) => {
  const { addNotification } = useNotifications();
  const form = useForm<Valores>({
    resolver: zodResolver(esquema),
    defaultValues: { motivo: '' },
  });
  const revocar = useRevocarDescuento(compromisoId, (resultado) => {
    addNotification({
      type: 'success',
      title: 'Descuento revocado',
      message: `Se recalcularon ${resultado.cuotasRecalculadas} cuotas.`,
    });
    form.reset();
    onOpenChange(false);
  });

  const enviar = form.handleSubmit((valores) =>
    revocar.mutate({
      descuentoAplicadoId,
      solicitud: { motivo: valores.motivo.trim() },
    }),
  );

  return (
    <Dialog open={abierto} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-[448px] gap-0 rounded-xl border-stroke-tertiary bg-white p-0">
        <form onSubmit={enviar} noValidate>
          <div className="flex flex-col items-center gap-4 px-8 pb-6 pt-8 text-center">
            <IconBadge color="error" size="lg" shape="square">
              <MinusCircle aria-hidden="true" />
            </IconBadge>
            <DialogTitle className="text-h6 font-semibold text-content-primary">
              ¿Revocar “{nombre}”?
            </DialogTitle>
            <DialogDescription className="text-body-3 text-content-tertiary">
              Las cuotas pendientes volverán a su monto base. Las cuotas ya
              pagadas no cambian.
            </DialogDescription>
            <Textarea
              label="Motivo *"
              maxLength={500}
              showCount
              className="w-full text-left"
              registration={form.register('motivo')}
              error={form.formState.errors.motivo}
            />
          </div>
          <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
            <Button
              type="button"
              variant="tertiary"
              onClick={() => onOpenChange(false)}
              disabled={revocar.isPending}
            >
              Cancelar
            </Button>
            <Button type="submit" variant="error" isLoading={revocar.isPending}>
              Revocar descuento
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
