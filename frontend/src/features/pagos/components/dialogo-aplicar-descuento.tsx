import { zodResolver } from '@hookform/resolvers/zod';
import Axios from 'axios';
import { Percent } from 'lucide-react';
import * as React from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input, RadioCard, Textarea } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { useNotifications } from '@/components/ui/notifications';

import {
  useAplicarDescuento,
  useDescuentosAplicables,
} from '../api/descuentos';

const esquema = z
  .object({
    descuentoId: z.string().min(1, 'Elige un descuento del catálogo.'),
    motivo: z
      .string()
      .trim()
      .min(1, 'Ingresa el motivo del descuento.')
      .max(500, 'El motivo admite hasta 500 caracteres.'),
    vigenciaDesde: z.string(),
    vigenciaHasta: z.string(),
  })
  .superRefine((valores, contexto) => {
    if (
      valores.vigenciaDesde &&
      valores.vigenciaHasta &&
      valores.vigenciaHasta < valores.vigenciaDesde
    ) {
      contexto.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['vigenciaHasta'],
        message: 'La vigencia final debe ser posterior a la inicial.',
      });
    }
  });

type Valores = z.infer<typeof esquema>;

export const DialogoAplicarDescuento = ({
  abierto,
  onOpenChange,
  compromisoId,
}: {
  abierto: boolean;
  onOpenChange: (abierto: boolean) => void;
  compromisoId: string;
}) => {
  const { addNotification } = useNotifications();
  const aplicables = useDescuentosAplicables(compromisoId, abierto);
  const [requiereConfirmacion, setRequiereConfirmacion] = React.useState(false);
  const form = useForm<Valores>({
    resolver: zodResolver(esquema),
    defaultValues: {
      descuentoId: '',
      motivo: '',
      vigenciaDesde: '',
      vigenciaHasta: '',
    },
  });
  const seleccionadoId = form.watch('descuentoId');
  const seleccionado = aplicables.data?.descuentos.find(
    (descuento) => descuento.id === seleccionadoId,
  );

  React.useEffect(() => {
    if (!abierto || form.getValues('descuentoId')) return;
    const primero = aplicables.data?.descuentos.find(
      (descuento) => descuento.aplicable,
    );
    if (primero) form.setValue('descuentoId', primero.id);
  }, [abierto, aplicables.data, form]);

  const cerrar = () => {
    form.reset();
    setRequiereConfirmacion(false);
    onOpenChange(false);
  };

  const aplicar = useAplicarDescuento(compromisoId, (resultado) => {
    addNotification({
      type: 'success',
      title: 'Descuento aplicado',
      message: `Se recalcularon ${resultado.cuotasRecalculadas} cuotas.`,
    });
    cerrar();
  });

  const enviar = form.handleSubmit((valores) => {
    aplicar.mutate(
      {
        descuentoId: valores.descuentoId,
        motivo: valores.motivo.trim(),
        vigenciaDesde: valores.vigenciaDesde || null,
        vigenciaHasta: valores.vigenciaHasta || null,
        reemplazar: !!aplicables.data?.actual || requiereConfirmacion,
      },
      {
        onError: (error) => {
          if (Axios.isAxiosError(error) && error.response?.status === 409)
            setRequiereConfirmacion(true);
        },
      },
    );
  });

  return (
    <Dialog open={abierto} onOpenChange={(open) => !open && cerrar()}>
      <DialogContent className="flex max-h-[92vh] max-w-screen-sm flex-col gap-0 overflow-hidden rounded-xl border-stroke-tertiary bg-white p-0">
        <form onSubmit={enviar} noValidate className="flex min-h-0 flex-col">
          <div className="space-y-5 overflow-y-auto p-6">
            <div className="space-y-3">
              <IconBadge color="accent" size="lg" shape="square">
                <Percent aria-hidden="true" />
              </IconBadge>
              <div className="space-y-1">
                <DialogTitle className="text-h6 font-semibold text-content-primary">
                  Añadir descuento
                </DialogTitle>
                <DialogDescription className="text-body-3 text-content-secondary">
                  Elige el beneficio, su vigencia y registra el motivo de la
                  aplicación.
                </DialogDescription>
              </div>
            </div>

            {aplicables.data?.actual && (
              <Alert
                type="warning"
                title={`El estudiante ya tiene «${aplicables.data.actual.nombre}» (${aplicables.data.actual.porcentaje} %).`}
              >
                El nuevo beneficio lo reemplazará al confirmar.
              </Alert>
            )}
            {requiereConfirmacion && !aplicables.data?.actual && (
              <Alert
                type="warning"
                title="Confirma el reemplazo del descuento vigente."
              >
                La información cambió desde que abriste el diálogo. Vuelve a
                confirmar para reemplazar el beneficio actual.
              </Alert>
            )}

            <div className="space-y-3">
              <div>
                <p className="text-label-1 font-semibold text-content-primary">
                  1. Descuento del catálogo *
                </p>
                <p className="text-body-4 text-content-tertiary">
                  Hermanos matriculados:{' '}
                  {aplicables.data?.hermanosMatriculados ?? '—'}
                </p>
              </div>
              <div className="space-y-3">
                {(aplicables.data?.descuentos ?? []).map((descuento) => (
                  <RadioCard
                    key={descuento.id}
                    value={descuento.id}
                    disabled={!descuento.aplicable}
                    label={descuento.nombre}
                    helperText={
                      descuento.aplicable
                        ? `${descuento.porcentaje ?? '—'} % · Aplica a: ${descuento.conceptos.join(', ')}`
                        : (descuento.motivo ??
                          'No aplicable para este estudiante.')
                    }
                    {...form.register('descuentoId')}
                  />
                ))}
              </div>
              {form.formState.errors.descuentoId && (
                <p className="text-label-3 text-content-error">
                  {form.formState.errors.descuentoId.message}
                </p>
              )}
              {seleccionado?.porcentaje != null && (
                <p className="text-body-3 font-medium text-content-primary">
                  Porcentaje que se aplicará: {seleccionado.porcentaje} %
                </p>
              )}
            </div>

            <div className="space-y-3">
              <p className="text-label-1 font-semibold text-content-primary">
                2. Vigencia del descuento
              </p>
              <div className="grid gap-4 sm:grid-cols-2">
                <Input
                  type="date"
                  label="Desde"
                  registration={form.register('vigenciaDesde')}
                  error={form.formState.errors.vigenciaDesde}
                />
                <Input
                  type="date"
                  label="Hasta"
                  registration={form.register('vigenciaHasta')}
                  error={form.formState.errors.vigenciaHasta}
                />
              </div>
            </div>

            <Textarea
              label="Motivo o sustento *"
              maxLength={500}
              showCount
              registration={form.register('motivo')}
              error={form.formState.errors.motivo}
            />
          </div>

          <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
            <Button
              type="button"
              variant="tertiary"
              onClick={cerrar}
              disabled={aplicar.isPending}
            >
              Cancelar
            </Button>
            <Button type="submit" isLoading={aplicar.isPending}>
              Aplicar descuento
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
