import { Controller, UseFormReturn } from 'react-hook-form';

import { Card } from '@/components/ui/card';
import { DatePicker } from '@/components/ui/date-picker';
import { Select } from '@/components/ui/form';

import { BarraDuracion } from './barra-duracion';
import { ValoresPeriodoMatricula } from './esquemas';
import { opcionesDeHora } from './formato';

type SeccionFechasProps = {
  form: UseFormReturn<ValoresPeriodoMatricula>;
  /** Años que aún no tienen periodo, ya ordenados. */
  anios: { label: string; value: string }[];
  /** Un año ya no se cambia al editar un periodo, y sin años disponibles no hay nada que llenar. */
  anioFijo?: boolean;
  deshabilitado?: boolean;
};

// Figma > pantalla 13 > formulario > tarjeta "Periodo de matrícula": año, fechas y horas, y la barra de duración.
export const SeccionFechas = ({
  form,
  anios,
  anioFijo = false,
  deshabilitado = false,
}: SeccionFechasProps) => {
  const { register, control, watch, formState } = form;
  const errores = formState.errors;
  const horasInicio = opcionesDeHora(watch('horaInicio'));
  const horasCierre = opcionesDeHora(watch('horaCierre'));

  return (
    <Card className="flex flex-col gap-4 p-6">
      <div className="border-b border-stroke-tertiary pb-4">
        <h2 className="text-label-1 font-semibold text-content-primary">
          Periodo de matrícula
        </h2>
        <p className="text-body-4 text-content-tertiary">
          Rango de fechas en el que los apoderados podrán matricular a sus
          hijos.
        </p>
      </div>

      <Select
        label="Año académico"
        placeholder="Año académico"
        options={anios}
        registration={register('anioEscolarId')}
        error={errores.anioEscolarId}
        disabled={deshabilitado || anioFijo}
      />

      <div className="grid gap-4 sm:grid-cols-2">
        <Controller
          control={control}
          name="fechaInicio"
          render={({ field }) => (
            <DatePicker
              label="Fecha de inicio"
              value={field.value}
              onChange={field.onChange}
              disabled={deshabilitado}
              error={errores.fechaInicio}
            />
          )}
        />
        <Select
          label="Hora de inicio"
          placeholder="Hora de inicio"
          options={horasInicio}
          registration={register('horaInicio')}
          error={errores.horaInicio}
          disabled={deshabilitado}
        />
        <Controller
          control={control}
          name="fechaCierre"
          render={({ field }) => (
            <DatePicker
              label="Fecha de cierre"
              value={field.value}
              onChange={field.onChange}
              disabled={deshabilitado}
              error={errores.fechaCierre}
            />
          )}
        />
        <Select
          label="Hora de cierre"
          placeholder="Hora de cierre"
          options={horasCierre}
          registration={register('horaCierre')}
          error={errores.horaCierre}
          disabled={deshabilitado}
        />
      </div>

      <BarraDuracion
        fechaInicio={watch('fechaInicio')}
        fechaCierre={watch('fechaCierre')}
      />
    </Card>
  );
};
