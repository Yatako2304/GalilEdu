import { zodResolver } from '@hookform/resolvers/zod';
import { Percent, Plus, Trash2 } from 'lucide-react';
import * as React from 'react';
import { useFieldArray, useForm } from 'react-hook-form';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Checkbox, Input, Select, Textarea } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { useNotifications } from '@/components/ui/notifications';
import { DescuentoVista, SolicitudDescuento } from '@/types/api';

import {
  useConfiguracionDescuentos,
  useCrearDescuento,
  useEditarDescuento,
} from '../api/descuentos';

import { ValoresDescuento, descuentoSchema } from './esquema-descuento';

const tramoVacio = (desde = '', hasta = '') => ({
  hermanosDesde: desde,
  hermanosHasta: hasta,
  porcentaje: '',
});

const valoresIniciales = (
  descuento?: DescuentoVista | null,
): ValoresDescuento =>
  descuento
    ? {
        nombre: descuento.nombre,
        descripcion: descuento.descripcion ?? '',
        tipo: descuento.tipo,
        aplicacion: descuento.aplicacion,
        porcentaje: descuento.porcentaje?.toString() ?? '',
        tramos: descuento.tramos.map((tramo) => ({
          hermanosDesde: tramo.hermanosDesde.toString(),
          hermanosHasta: tramo.hermanosHasta?.toString() ?? '',
          porcentaje: tramo.porcentaje.toString(),
        })),
        conceptos: descuento.conceptos,
        condicion: descuento.condicion ?? '',
      }
    : {
        nombre: '',
        descripcion: '',
        tipo: 'PORCENTAJE_UNICO',
        aplicacion: 'MANUAL',
        porcentaje: '',
        tramos: [tramoVacio('2', '2'), tramoVacio('3')],
        conceptos: ['PENSION'],
        condicion: '',
      };

const aSolicitud = (valores: ValoresDescuento): SolicitudDescuento => ({
  nombre: valores.nombre.trim(),
  descripcion: valores.descripcion.trim() || null,
  tipo: valores.tipo,
  aplicacion: valores.aplicacion,
  porcentaje:
    valores.tipo === 'PORCENTAJE_UNICO' ? Number(valores.porcentaje) : null,
  tramos:
    valores.tipo === 'POR_HERMANOS'
      ? valores.tramos.map((tramo) => ({
          hermanosDesde: Number(tramo.hermanosDesde),
          hermanosHasta: tramo.hermanosHasta
            ? Number(tramo.hermanosHasta)
            : null,
          porcentaje: Number(tramo.porcentaje),
        }))
      : [],
  conceptos: valores.conceptos,
  condicion: valores.condicion.trim() || null,
});

export const DialogoDescuento = ({
  abierto,
  onOpenChange,
  descuento,
}: {
  abierto: boolean;
  onOpenChange: (abierto: boolean) => void;
  descuento?: DescuentoVista | null;
}) => {
  const { addNotification } = useNotifications();
  const configuracion = useConfiguracionDescuentos();
  const form = useForm<ValoresDescuento>({
    resolver: zodResolver(descuentoSchema),
    mode: 'onChange',
    defaultValues: valoresIniciales(descuento),
  });
  const tramos = useFieldArray({ control: form.control, name: 'tramos' });
  const tipo = form.watch('tipo');

  React.useEffect(() => {
    if (abierto) form.reset(valoresIniciales(descuento));
  }, [abierto, descuento, form]);

  const cerrar = () => onOpenChange(false);
  const alGuardar = (guardado: DescuentoVista) => {
    addNotification({
      type: 'success',
      title: descuento ? 'Descuento actualizado' : 'Descuento creado',
      message: guardado.nombre,
    });
    cerrar();
  };
  const crear = useCrearDescuento(alGuardar);
  const editar = useEditarDescuento(alGuardar);

  const enviar = form.handleSubmit((valores) => {
    const solicitud = aSolicitud(valores);
    if (descuento) editar.mutate({ id: descuento.id, solicitud });
    else crear.mutate(solicitud);
  });
  const cargando = crear.isPending || editar.isPending;

  return (
    <Dialog open={abierto} onOpenChange={onOpenChange}>
      <DialogContent className="flex max-h-[92vh] max-w-[680px] flex-col gap-0 overflow-hidden rounded-xl border-stroke-tertiary bg-white p-0">
        <form onSubmit={enviar} noValidate className="flex min-h-0 flex-col">
          <div className="space-y-5 overflow-y-auto p-6">
            <div className="space-y-3">
              <IconBadge color="accent" size="lg" shape="square">
                <Percent aria-hidden="true" />
              </IconBadge>
              <div className="space-y-1">
                <DialogTitle className="text-h6 font-semibold text-content-primary">
                  {descuento ? 'Editar descuento' : 'Nuevo descuento'}
                </DialogTitle>
                <DialogDescription className="text-body-3 text-content-secondary">
                  Se agregará al catálogo y podrá aplicarse a los compromisos de
                  pago.
                </DialogDescription>
              </div>
            </div>

            <Input
              label="Nombre *"
              maxLength={80}
              registration={form.register('nombre')}
              error={form.formState.errors.nombre}
            />
            <Textarea
              label="Descripción"
              maxLength={300}
              showCount
              registration={form.register('descripcion')}
              error={form.formState.errors.descripcion}
            />

            <div className="grid gap-4 sm:grid-cols-2">
              <Select
                label="Tipo *"
                options={[
                  { label: 'Porcentaje único', value: 'PORCENTAJE_UNICO' },
                  { label: 'Por hermanos', value: 'POR_HERMANOS' },
                ]}
                registration={form.register('tipo')}
                error={form.formState.errors.tipo}
              />
              <Select
                label="Aplicación *"
                options={[
                  { label: 'Manual', value: 'MANUAL' },
                  { label: 'Automático por condición', value: 'AUTOMATICO' },
                ]}
                registration={form.register('aplicacion')}
                error={form.formState.errors.aplicacion}
              />
            </div>

            {tipo === 'PORCENTAJE_UNICO' ? (
              <Input
                type="number"
                label="Porcentaje *"
                min={1}
                max={configuracion.data?.porcentajeMaximo ?? 100}
                endAdornment="%"
                hint={
                  configuracion.data
                    ? `Límite institucional: ${configuracion.data.porcentajeMaximo} %.`
                    : undefined
                }
                registration={form.register('porcentaje')}
                error={form.formState.errors.porcentaje}
              />
            ) : (
              <div className="space-y-3">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="text-label-1 font-semibold text-content-primary">
                      Tramos por hermanos *
                    </p>
                    <p className="text-body-4 text-content-tertiary">
                      El último tramo puede quedar sin límite.
                    </p>
                  </div>
                  <Button
                    type="button"
                    size="sm"
                    variant="primary-light"
                    icon={<Plus className="size-4" />}
                    onClick={() => tramos.append(tramoVacio())}
                  >
                    Agregar tramo
                  </Button>
                </div>
                {tramos.fields.map((campo, indice) => (
                  <div
                    key={campo.id}
                    className="grid items-start gap-3 rounded-xl border border-stroke-tertiary p-3 sm:grid-cols-[1fr_1fr_1fr_auto]"
                  >
                    <Input
                      type="number"
                      label="Desde *"
                      min={2}
                      registration={form.register(
                        `tramos.${indice}.hermanosDesde`,
                        { deps: [`tramos.${indice}.hermanosHasta`] },
                      )}
                      error={
                        form.formState.errors.tramos?.[indice]?.hermanosDesde
                      }
                    />
                    <Input
                      type="number"
                      label="Hasta"
                      min={2}
                      registration={form.register(
                        `tramos.${indice}.hermanosHasta`,
                        { deps: [`tramos.${indice}.hermanosDesde`] },
                      )}
                      error={
                        form.formState.errors.tramos?.[indice]?.hermanosHasta
                      }
                    />
                    <Input
                      type="number"
                      label="Porcentaje *"
                      min={1}
                      endAdornment="%"
                      registration={form.register(
                        `tramos.${indice}.porcentaje`,
                      )}
                      error={form.formState.errors.tramos?.[indice]?.porcentaje}
                    />
                    <Button
                      type="button"
                      size="icon"
                      variant="tertiary"
                      aria-label={`Quitar tramo ${indice + 1}`}
                      disabled={tramos.fields.length <= 1}
                      onClick={() => tramos.remove(indice)}
                    >
                      <Trash2 className="size-4" />
                    </Button>
                  </div>
                ))}
                {form.formState.errors.tramos?.root?.message && (
                  <p className="text-label-3 text-content-error">
                    {form.formState.errors.tramos.root.message}
                  </p>
                )}
              </div>
            )}

            <fieldset className="space-y-3">
              <legend className="text-label-1 font-semibold text-content-primary">
                Conceptos a los que aplica *
              </legend>
              <div className="grid gap-3 sm:grid-cols-2">
                <Checkbox
                  value="PENSION"
                  label="Pensiones"
                  {...form.register('conceptos')}
                />
                <Checkbox
                  value="MATRICULA"
                  label="Matrícula"
                  {...form.register('conceptos')}
                />
              </div>
              {form.formState.errors.conceptos && (
                <p className="text-label-3 text-content-error">
                  {form.formState.errors.conceptos.message}
                </p>
              )}
            </fieldset>

            <Textarea
              label="Condición"
              maxLength={300}
              showCount
              registration={form.register('condicion')}
              error={form.formState.errors.condicion}
            />
          </div>

          <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
            <Button
              type="button"
              variant="tertiary"
              onClick={cerrar}
              disabled={cargando}
            >
              Cancelar
            </Button>
            <Button type="submit" isLoading={cargando}>
              {descuento ? 'Guardar cambios' : 'Crear descuento'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
