import { zodResolver } from '@hookform/resolvers/zod';
import { CalendarDays } from 'lucide-react';
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
import { Input, RadioCard } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { useNotifications } from '@/components/ui/notifications';
import { AnioEscolarDetalle, AnioEscolarResumen, Regimen } from '@/types/api';

import { useCrearAnio, useEditarAnio } from '../../api/mutaciones-anio';

import {
  ValoresAnio,
  anioEscrito,
  edicionAnioSchema,
  fechasSugeridas,
  nuevoAnioSchema,
  valoresIniciales,
} from './esquemas';
import { periodosAlAnio } from './formato';

const REGIMENES: { valor: Regimen; nombre: string }[] = [
  { valor: 'BIMESTRAL', nombre: 'Bimestral' },
  { valor: 'TRIMESTRAL', nombre: 'Trimestral' },
];

type DialogoAnioProps = {
  abierto: boolean;
  onCerrar: () => void;
  /**
   * `nuevo` crea un año PLANIFICADO. `editar` cambia las fechas del año recibido en `anio`: el año y el régimen
   * quedan fijos (el régimen se cambia aparte).
   */
  modo: 'nuevo' | 'editar';
  /** Obligatorio con `modo="editar"`. */
  anio?: AnioEscolarDetalle;
  /** Años ya registrados: el modal Nuevo propone el siguiente al último. */
  aniosRegistrados?: Pick<AnioEscolarResumen, 'anio'>[];
  onGuardado?: (anio: AnioEscolarDetalle) => void;
};

// Figma > Personal Administrativo > "Modal - Nuevo año escolar" (HU-74). Editar año reutiliza el mismo modal.
// "Reutilizar configuración" del Figma no está: el backend aún no copia secciones, estructuras ni plan.
export const DialogoAnio = ({
  abierto,
  onCerrar,
  modo,
  anio,
  aniosRegistrados = [],
  onGuardado,
}: DialogoAnioProps) => (
  <Dialog open={abierto} onOpenChange={(open) => !open && onCerrar()}>
    {/* Con ventanas altas el modal no recorta ni desplaza: el calendario desplegable flota sobre su borde en vez de
        agrandar el cuerpo. Por debajo de 700 px de alto vuelve a recortar y desplazar, para que los botones siempre
        se alcancen. */}
    <DialogContent className="flex max-h-[92vh] max-w-[600px] flex-col gap-0 overflow-visible rounded-xl border-stroke-tertiary bg-white p-0 [@media(max-height:700px)]:overflow-hidden">
      <FormularioAnio
        modo={modo}
        anio={anio}
        aniosRegistrados={aniosRegistrados}
        onCerrar={onCerrar}
        onGuardado={onGuardado}
      />
    </DialogContent>
  </Dialog>
);

const FormularioAnio = ({
  modo,
  anio,
  aniosRegistrados = [],
  onCerrar,
  onGuardado,
}: Omit<DialogoAnioProps, 'abierto'>) => {
  const editando = modo === 'editar' && !!anio;
  const { addNotification } = useNotifications();

  const form = useForm<ValoresAnio>({
    resolver: zodResolver(
      editando ? edicionAnioSchema : nuevoAnioSchema,
    ) as any,
    defaultValues:
      editando && anio
        ? {
            anio: String(anio.anio),
            fechaInicio: anio.fechaInicio,
            fechaFin: anio.fechaFin,
            regimen: anio.regimen,
          }
        : valoresIniciales(aniosRegistrados),
  });
  const { register, control, watch, setValue, formState } = form;
  const errores = formState.errors;

  // Las fechas siguen al año escrito mientras el usuario no las haya cambiado a mano.
  const fechasManuales = React.useRef(false);
  const anioEscritoAhora = anioEscrito(watch('anio'));
  React.useEffect(() => {
    if (editando || fechasManuales.current || anioEscritoAhora === null) return;
    const { fechaInicio, fechaFin } = fechasSugeridas(anioEscritoAhora);
    setValue('fechaInicio', fechaInicio);
    setValue('fechaFin', fechaFin);
  }, [anioEscritoAhora, editando, setValue]);

  const cerrarGuardado = (guardado: AnioEscolarDetalle, mensaje: string) => {
    addNotification({
      type: 'success',
      title: mensaje,
      message: `Año escolar ${guardado.anio}`,
    });
    onCerrar();
    onGuardado?.(guardado);
  };
  const crear = useCrearAnio((creado) =>
    cerrarGuardado(creado, 'Año escolar creado'),
  );
  const editar = useEditarAnio((editado) =>
    cerrarGuardado(editado, 'Año escolar actualizado'),
  );
  const guardando = crear.isPending || editar.isPending;

  const enviar = form.handleSubmit((valores) => {
    if (editando && anio) {
      editar.mutate({
        id: anio.id,
        data: { fechaInicio: valores.fechaInicio, fechaFin: valores.fechaFin },
      });
    } else {
      crear.mutate({
        anio: Number(valores.anio),
        fechaInicio: valores.fechaInicio,
        fechaFin: valores.fechaFin,
        regimen: valores.regimen,
      });
    }
  });

  return (
    <form onSubmit={enviar} noValidate className="flex min-h-0 flex-1 flex-col">
      <div className="flex-1 space-y-5 p-6 [@media(max-height:700px)]:overflow-y-auto">
        <div className="space-y-3">
          <IconBadge color="accent" size="lg" aria-hidden="true">
            <CalendarDays />
          </IconBadge>
          <div className="space-y-2">
            <DialogTitle className="text-h6 font-semibold text-content-primary">
              {editando
                ? `Editar año escolar ${anio?.anio}`
                : 'Nuevo año escolar'}
            </DialogTitle>
            <DialogDescription className="text-body-3 text-content-secondary">
              {editando
                ? 'Solo se pueden cambiar las fechas mientras el año está planificado.'
                : 'Se creará en estado Planificado. Los periodos se generan según el régimen elegido.'}
            </DialogDescription>
          </div>
        </div>

        <Input
          label="Año *"
          inputMode="numeric"
          maxLength={4}
          disabled={editando}
          registration={register('anio')}
          error={errores.anio}
        />

        <div className="grid gap-4 sm:grid-cols-2">
          <Controller
            control={control}
            name="fechaInicio"
            render={({ field }) => (
              <DatePicker
                size="sm"
                label="Fecha de inicio de clases *"
                value={field.value}
                onChange={(fecha) => {
                  fechasManuales.current = true;
                  field.onChange(fecha);
                }}
                error={errores.fechaInicio}
              />
            )}
          />
          <Controller
            control={control}
            name="fechaFin"
            render={({ field }) => (
              <DatePicker
                size="sm"
                label="Fecha de fin de clases *"
                value={field.value}
                onChange={(fecha) => {
                  fechasManuales.current = true;
                  field.onChange(fecha);
                }}
                error={errores.fechaFin}
              />
            )}
          />
        </div>

        {editando ? (
          <Alert
            type="warning"
            title="Los periodos se regeneran con fechas sugeridas."
          >
            Luego podrás ajustar las fechas de cada periodo.
          </Alert>
        ) : (
          <fieldset className="space-y-2">
            <legend className="text-label-2 font-medium text-content-primary">
              Régimen de periodos *
            </legend>
            <div className="grid gap-3 sm:grid-cols-2">
              {REGIMENES.map(({ valor, nombre }) => (
                <RadioCard
                  key={valor}
                  value={valor}
                  label={nombre}
                  helperText={periodosAlAnio(valor)}
                  {...register('regimen')}
                />
              ))}
            </div>
          </fieldset>
        )}
      </div>

      <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
        <Button type="button" variant="tertiary" onClick={onCerrar}>
          Cancelar
        </Button>
        <Button type="submit" isLoading={guardando} disabled={guardando}>
          {editando ? 'Guardar cambios' : 'Crear año escolar'}
        </Button>
      </div>
    </form>
  );
};
