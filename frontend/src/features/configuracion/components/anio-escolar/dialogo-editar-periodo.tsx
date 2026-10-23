import { zodResolver } from '@hookform/resolvers/zod';
import { CalendarClock } from 'lucide-react';
import * as React from 'react';
import { Controller, useForm } from 'react-hook-form';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { DatePicker } from '@/components/ui/date-picker';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { useNotifications } from '@/components/ui/notifications';
import { AnioEscolarDetalle, PeriodoDetalle } from '@/types/api';
import { formatDay } from '@/utils/format';

import { useEditarPeriodo } from '../../api/mutaciones-anio';

import { ValoresPeriodo, crearPeriodoSchema } from './esquema-periodo';

type DialogoEditarPeriodoProps = {
  periodo: PeriodoDetalle;
  anio: AnioEscolarDetalle;
  onCerrar: () => void;
};

// Figma > Personal Administrativo > "Modal - Editar periodo" (HU-74). Se monta solo mientras está abierto.
export const DialogoEditarPeriodo = ({
  periodo,
  anio,
  onCerrar,
}: DialogoEditarPeriodoProps) => (
  <Dialog open onOpenChange={(open) => !open && onCerrar()}>
    <DialogContent className="flex max-h-[92vh] max-w-[560px] flex-col gap-0 overflow-hidden rounded-[12px] border-stroke-tertiary bg-white p-0">
      <FormularioPeriodo periodo={periodo} anio={anio} onCerrar={onCerrar} />
    </DialogContent>
  </Dialog>
);

const FormularioPeriodo = ({
  periodo,
  anio,
  onCerrar,
}: DialogoEditarPeriodoProps) => {
  const { addNotification } = useNotifications();
  // Lo que ya pasó no se puede cambiar: el que empezó fija su inicio y el que terminó solo mueve el límite de notas.
  const yaEmpezo = periodo.estado !== 'PLANIFICADO';
  const yaTermino = periodo.estado === 'CERRADO';

  const esquema = React.useMemo(
    () =>
      crearPeriodoSchema({
        anio,
        otros: anio.periodos.filter((p) => p.id !== periodo.id),
      }),
    [anio, periodo.id],
  );
  const { control, handleSubmit, formState } = useForm<ValoresPeriodo>({
    resolver: zodResolver(esquema) as any,
    defaultValues: {
      nombre: periodo.nombre,
      fechaInicio: periodo.fechaInicio,
      fechaFin: periodo.fechaFin,
      fechaLimiteNotas: periodo.fechaLimiteNotas,
    },
  });
  const errores = formState.errors;

  const editar = useEditarPeriodo((guardado) => {
    addNotification({
      type: 'success',
      title: 'Periodo actualizado',
      message: guardado.nombre,
    });
    onCerrar();
  });

  return (
    <form
      noValidate
      onSubmit={handleSubmit((valores) =>
        editar.mutate({ id: periodo.id, data: { ...valores } }),
      )}
      className="flex min-h-0 flex-1 flex-col"
    >
      <div className="flex-1 space-y-5 overflow-y-auto p-6">
        <div className="space-y-3">
          <IconBadge color="accent" size="lg" aria-hidden="true">
            <CalendarClock />
          </IconBadge>
          <div className="space-y-2">
            <DialogTitle className="text-h6 font-semibold text-content-primary">
              Editar {periodo.nombre}
            </DialogTitle>
            <DialogDescription className="text-body-3 text-content-secondary">
              Año escolar {anio.anio} · del {formatDay(anio.fechaInicio)} al{' '}
              {formatDay(anio.fechaFin)}
            </DialogDescription>
          </div>
        </div>

        <Controller
          control={control}
          name="nombre"
          render={({ field }) => (
            <Input
              label="Nombre del periodo *"
              value={field.value}
              onChange={field.onChange}
              onBlur={field.onBlur}
              disabled={yaTermino}
              error={errores.nombre}
            />
          )}
        />

        <div className="grid gap-4 sm:grid-cols-2">
          <Controller
            control={control}
            name="fechaInicio"
            render={({ field }) => (
              <DatePicker
                label="Fecha de inicio *"
                value={field.value}
                onChange={field.onChange}
                disabled={yaEmpezo}
                hint={
                  yaEmpezo
                    ? 'El periodo ya empezó: no se puede cambiar.'
                    : undefined
                }
                error={errores.fechaInicio}
              />
            )}
          />
          <Controller
            control={control}
            name="fechaFin"
            render={({ field }) => (
              <DatePicker
                label="Fecha de fin *"
                value={field.value}
                onChange={field.onChange}
                disabled={yaTermino}
                hint={
                  yaTermino
                    ? 'El periodo ya terminó: solo se puede cambiar la fecha límite de notas.'
                    : undefined
                }
                error={errores.fechaFin}
              />
            )}
          />
        </div>

        <Controller
          control={control}
          name="fechaLimiteNotas"
          render={({ field }) => (
            <DatePicker
              label="Fecha límite para registrar notas *"
              value={field.value}
              onChange={field.onChange}
              hint="Debe ser igual o posterior al fin del periodo."
              error={errores.fechaLimiteNotas}
            />
          )}
        />

        <Alert
          type="warning"
          title="Los docentes verán las nuevas fechas en el registro de notas."
        />
      </div>

      <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
        <Button type="button" variant="tertiary" onClick={onCerrar}>
          Cancelar
        </Button>
        <Button
          type="submit"
          isLoading={editar.isPending}
          disabled={editar.isPending}
        >
          Guardar cambios
        </Button>
      </div>
    </form>
  );
};
