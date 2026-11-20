import { zodResolver } from '@hookform/resolvers/zod';
import { SlidersHorizontal } from 'lucide-react';
import * as React from 'react';
import { useForm } from 'react-hook-form';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { useNotifications } from '@/components/ui/notifications';

import { useCambiarConfiguracionDescuentos } from '../api/descuentos';

import {
  ValoresConfiguracionDescuento,
  configuracionDescuentoSchema,
} from './esquema-descuento';

export const DialogoLimiteDescuento = ({
  abierto,
  porcentajeMaximo,
  onOpenChange,
}: {
  abierto: boolean;
  porcentajeMaximo: number;
  onOpenChange: (abierto: boolean) => void;
}) => {
  const { addNotification } = useNotifications();
  const form = useForm<ValoresConfiguracionDescuento>({
    resolver: zodResolver(configuracionDescuentoSchema),
    mode: 'onChange',
    defaultValues: { porcentajeMaximo: porcentajeMaximo.toString() },
  });

  React.useEffect(() => {
    if (abierto) {
      form.reset({ porcentajeMaximo: porcentajeMaximo.toString() });
    }
  }, [abierto, form, porcentajeMaximo]);

  const cambiar = useCambiarConfiguracionDescuentos((configuracion) => {
    addNotification({
      type: 'success',
      title: 'Límite actualizado',
      message: `El porcentaje máximo ahora es ${configuracion.porcentajeMaximo} %.`,
    });
    onOpenChange(false);
  });

  const enviar = form.handleSubmit((valores) =>
    cambiar.mutate({ porcentajeMaximo: Number(valores.porcentajeMaximo) }),
  );

  return (
    <Dialog open={abierto} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-[480px] rounded-xl border-stroke-tertiary bg-white p-0">
        <form onSubmit={enviar} noValidate>
          <div className="space-y-5 p-6">
            <div className="space-y-3">
              <IconBadge color="accent" size="lg" shape="square">
                <SlidersHorizontal aria-hidden="true" />
              </IconBadge>
              <div className="space-y-1">
                <DialogTitle className="text-h6 font-semibold text-content-primary">
                  Límite de descuento
                </DialogTitle>
                <DialogDescription className="text-body-3 text-content-secondary">
                  Define el porcentaje máximo permitido al registrar o editar un
                  descuento.
                </DialogDescription>
              </div>
            </div>

            <Input
              type="number"
              label="Porcentaje máximo *"
              min={1}
              max={100}
              endAdornment="%"
              registration={form.register('porcentajeMaximo')}
              error={form.formState.errors.porcentajeMaximo}
            />
            <p className="text-body-4 text-content-tertiary">
              El valor debe estar entre 1 % y 100 %. El nuevo límite se aplicará
              a los descuentos que se creen o editen desde ahora.
            </p>
          </div>

          <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
            <Button
              type="button"
              variant="tertiary"
              disabled={cambiar.isPending}
              onClick={() => onOpenChange(false)}
            >
              Cancelar
            </Button>
            <Button type="submit" isLoading={cambiar.isPending}>
              Guardar límite
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
