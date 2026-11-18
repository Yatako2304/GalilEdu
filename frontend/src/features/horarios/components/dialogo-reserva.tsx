import { zodResolver } from '@hookform/resolvers/zod';
import { useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { CalendarDays } from 'lucide-react';
import * as React from 'react';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { DatePicker } from '@/components/ui/date-picker';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input, Select } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { useNotifications } from '@/components/ui/notifications';
import { formatDay } from '@/utils/format';

import { useHorarioSeccion } from '../api/horario-seccion';
import { useOpcionesReserva } from '../api/opciones-reserva';
import {
  CLAVE_RESERVAS,
  getReserva,
  useCrearReserva,
  useEditarReserva,
} from '../api/reservas';
import { useAniosParaHorarios, useGradosParaHorarios } from '../api/selectores';
import { estadoDeError, mensajeDeError } from '../lib/formato';
import {
  anioParaReservar,
  calcularHoraFin,
  duracionEnMinutos,
  erroresDeCampos,
  esConflictoDeVersionDeReserva,
  esDiaLectivo,
  formatHora12,
  gradosConSeccionesActivas,
  mensajeDeDiaNoLectivo,
  opcionesDeDuracion,
  opcionesDeHora,
  partirMensaje,
  seccionesActivas,
} from '../lib/reservas';
import { JornadaDelDia } from '../types';
import { ReservaDeAmbiente, SolicitudReserva } from '../types-reservas';

type DialogoReservaProps = {
  /** Sin reserva, el modal crea («Nueva reserva»); con ella, edita («Editar reserva»). */
  reserva?: ReservaDeAmbiente;
  onCerrar: () => void;
};

const MAXIMO_ACTIVIDAD = 120;

const esquemaBase = z.object({
  ambienteId: z.string().min(1, 'Elige el ambiente.'),
  responsableId: z.string().min(1, 'Elige el responsable.'),
  actividad: z
    .string()
    .trim()
    .min(1, 'Escribe la actividad.')
    .max(MAXIMO_ACTIVIDAD, 'La actividad admite hasta 120 caracteres.'),
  gradoCodigo: z.string().min(1, 'Elige el grado.'),
  seccionId: z.string().min(1, 'Elige la sección.'),
  fecha: z.string().min(1, 'Elige la fecha.'),
  horaInicio: z.string().min(1, 'Elige la hora.'),
  duracion: z.string().min(1, 'Elige la duración.'),
});

type Valores = z.infer<typeof esquemaBase>;

/** El día no lectivo se valida en el cliente con la jornada de la sección; el resto de las reglas, el backend. */
const construirEsquema = (jornada: JornadaDelDia | undefined) =>
  esquemaBase.superRefine((valores, contexto) => {
    if (jornada && valores.fecha && !esDiaLectivo(valores.fecha, jornada)) {
      contexto.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['fecha'],
        message: mensajeDeDiaNoLectivo(valores.fecha),
      });
    }
  });

const valoresDe = (reserva?: ReservaDeAmbiente): Valores => ({
  ambienteId: reserva?.ambiente.id ?? '',
  responsableId: reserva?.responsable.id ?? '',
  actividad: reserva?.actividad ?? '',
  gradoCodigo: reserva?.gradoCodigo ?? '',
  seccionId: reserva?.seccion.id ?? '',
  fecha: reserva?.fecha ?? '',
  horaInicio: reserva?.horaInicio ?? '',
  duracion: reserva
    ? String(duracionEnMinutos(reserva.horaInicio, reserva.horaFin))
    : '',
});

/** Los campos del backend que no se llaman igual que los del formulario (la hora de fin sale de la duración). */
const CAMPO_DEL_FORMULARIO: Record<string, keyof Valores> = {
  ambienteId: 'ambienteId',
  seccionId: 'seccionId',
  responsableId: 'responsableId',
  actividad: 'actividad',
  fecha: 'fecha',
  horaInicio: 'horaInicio',
  horaFin: 'duracion',
};

type ErrorDeEnvio = { mensaje: string; conflictoDeVersion: boolean };

// Figma > Coordinador > «Nueva reserva» (308:6867 y 308:7130). Figma no dibuja el modal de edición: es el mismo, con
// el título «Editar reserva» y el botón «Guardar cambios». Se monta solo mientras está abierto. No hay campo de año:
// al crear se usa el año ACTIVO (o, sin él, el más reciente que no esté cerrado) y al editar, el de la reserva.
export const DialogoReserva = ({ reserva, onCerrar }: DialogoReservaProps) => (
  <Dialog open onOpenChange={(open) => !open && onCerrar()}>
    {/* El calendario desplegable flota sobre el borde del modal en vez de agrandarlo (como en Configuración). */}
    <DialogContent className="flex max-h-[92vh] max-w-[670px] flex-col gap-0 overflow-visible rounded-xl border-stroke-tertiary bg-white p-0 [@media(max-height:700px)]:overflow-y-auto">
      <FormularioReserva reserva={reserva} onCerrar={onCerrar} />
    </DialogContent>
  </Dialog>
);

const FormularioReserva = ({ reserva, onCerrar }: DialogoReservaProps) => {
  const editando = !!reserva;
  const { addNotification } = useNotifications();
  const queryClient = useQueryClient();

  // La versión con la que se guarda: cambia al recargar tras un 409 de versión.
  const [reservaActual, setReservaActual] = React.useState(reserva);
  const [errorDeEnvio, setErrorDeEnvio] = React.useState<ErrorDeEnvio | null>(
    null,
  );
  const [recargando, setRecargando] = React.useState(false);

  // ---------- Datos de los selectores ----------
  const aniosQuery = useAniosParaHorarios();
  const anio = reserva
    ? { id: reserva.anioId }
    : anioParaReservar(aniosQuery.data ?? []);
  const gradosQuery = useGradosParaHorarios(anio?.id);
  const opcionesQuery = useOpcionesReserva();

  // ---------- Formulario ----------
  const jornadaRef = React.useRef<JornadaDelDia | undefined>(undefined);
  const form = useForm<Valores>({
    defaultValues: valoresDe(reserva),
    // El esquema depende de la jornada de la sección elegida: se arma en cada validación con la jornada de ese momento.
    resolver: (valores, contexto, opciones) =>
      zodResolver(construirEsquema(jornadaRef.current))(
        valores,
        contexto,
        opciones,
      ) as any,
  });
  const { control, formState, handleSubmit, setValue, setError, watch } = form;
  const errores = formState.errors;

  const gradoCodigo = watch('gradoCodigo');
  const seccionId = watch('seccionId');
  const horaInicio = watch('horaInicio');

  const horarioQuery = useHorarioSeccion(seccionId || undefined);
  const jornada = seccionId ? horarioQuery.data?.jornada : undefined;
  jornadaRef.current = jornada;

  const grados = gradosConSeccionesActivas(gradosQuery.data ?? []);
  const secciones = seccionesActivas(
    grados.find((g) => g.codigo === gradoCodigo),
  );

  // Lo guardado se conserva como opción aunque no coincida con los bloques de la jornada (no se pierde al editar).
  const mismaSeccionQueLaGuardada =
    !!reservaActual && seccionId === reservaActual.seccion.id;
  const opcionesDeLaHora = jornada
    ? opcionesDeHora(
        jornada,
        mismaSeccionQueLaGuardada ? reservaActual?.horaInicio : undefined,
      )
    : [];
  const opcionesDeLaDuracion = jornada
    ? opcionesDeDuracion(
        jornada,
        horaInicio || undefined,
        mismaSeccionQueLaGuardada && reservaActual
          ? duracionEnMinutos(reservaActual.horaInicio, reservaActual.horaFin)
          : undefined,
      )
    : [];

  // ---------- Envío ----------
  const alTerminar = (mensaje: string) => (guardada: ReservaDeAmbiente) => {
    addNotification({
      type: 'success',
      title: mensaje,
      message: `${guardada.ambiente.codigo} · ${formatDay(guardada.fecha)} · ${formatHora12(guardada.horaInicio)}`,
    });
    onCerrar();
  };
  const crear = useCrearReserva(alTerminar('Reserva registrada.'));
  const editar = useEditarReserva(
    reserva?.id ?? '',
    alTerminar('Reserva actualizada.'),
  );
  const enviando = crear.isPending || editar.isPending;

  const alFallar = (error: unknown) => {
    const delBackend = erroresDeCampos(error);
    if (estadoDeError(error) === 400 && delBackend) {
      // 400 de @Valid: cada mensaje va bajo su campo; lo que no sea de un campo del formulario, en el aviso.
      const sinCampo: string[] = [];
      Object.entries(delBackend).forEach(([campo, mensaje]) => {
        const delFormulario = CAMPO_DEL_FORMULARIO[campo];
        if (delFormulario) setError(delFormulario, { message: mensaje });
        else sinCampo.push(mensaje);
      });
      if (sinCampo.length > 0) {
        setErrorDeEnvio({ mensaje: sinCampo[0], conflictoDeVersion: false });
      }
      return;
    }
    setErrorDeEnvio({
      mensaje: mensajeDeError(error),
      conflictoDeVersion: esConflictoDeVersionDeReserva(error),
    });
  };

  const enviar = handleSubmit((valores) => {
    setErrorDeEnvio(null);
    const cuerpo: SolicitudReserva = {
      ambienteId: valores.ambienteId,
      seccionId: valores.seccionId,
      responsableId: valores.responsableId,
      actividad: valores.actividad.trim(),
      fecha: valores.fecha,
      horaInicio: valores.horaInicio,
      horaFin: calcularHoraFin(valores.horaInicio, Number(valores.duracion)),
    };
    if (reservaActual) {
      editar.mutate(
        { ...cuerpo, version: reservaActual.version },
        { onError: alFallar },
      );
    } else {
      crear.mutate(cuerpo, { onError: alFallar });
    }
  });

  // 409 de versión: se vuelve a leer la reserva y el formulario pasa a ser lo que hay guardado, con su versión nueva.
  const recargar = async () => {
    if (!reserva) return;
    setRecargando(true);
    try {
      const nueva = await getReserva(reserva.id);
      setReservaActual(nueva);
      form.reset(valoresDe(nueva));
      setErrorDeEnvio(null);
      await queryClient.invalidateQueries({ queryKey: CLAVE_RESERVAS });
    } catch (error) {
      setErrorDeEnvio({
        mensaje: mensajeDeError(error),
        conflictoDeVersion: false,
      });
    } finally {
      setRecargando(false);
    }
  };

  const sinAnio = !reserva && aniosQuery.isSuccess && !anio;
  const aviso = errorDeEnvio ? partirMensaje(errorDeEnvio.mensaje) : null;

  return (
    <form onSubmit={enviar} noValidate className="flex min-h-0 flex-1 flex-col">
      <div className="flex-1 space-y-6 p-8">
        <div className="flex items-start gap-3 border-b border-stroke-tertiary pb-3">
          <IconBadge color="accent" size="md" aria-hidden="true">
            <CalendarDays />
          </IconBadge>
          <div className="min-w-0 flex-1 space-y-2">
            <DialogDescription className="text-body-3 text-content-quartiary">
              Reservar espacios especiales
            </DialogDescription>
            <DialogTitle className="text-h6 font-medium text-content-primary">
              {editando ? 'Editar reserva' : 'Nueva reserva'}
            </DialogTitle>
          </div>
        </div>

        {sinAnio && (
          <Alert
            type="warning"
            size="sm"
            title="No hay un año escolar abierto para reservar espacios."
          />
        )}

        <div className="grid grid-cols-1 gap-x-8 gap-y-6 sm:grid-cols-2">
          <Controller
            control={control}
            name="ambienteId"
            render={({ field }) => (
              <Select
                label="Ambiente"
                value={field.value}
                onChange={(e) => field.onChange(e.target.value)}
                options={(opcionesQuery.data?.ambientes ?? []).map((a) => ({
                  label: a.nombre,
                  value: a.id,
                }))}
                error={errores.ambienteId}
              />
            )}
          />
          <Controller
            control={control}
            name="responsableId"
            render={({ field }) => (
              <Select
                label="Responsable"
                value={field.value}
                onChange={(e) => field.onChange(e.target.value)}
                options={(opcionesQuery.data?.responsables ?? []).map((r) => ({
                  label: r.nombre,
                  value: r.id,
                }))}
                error={errores.responsableId}
              />
            )}
          />

          <Input
            label="Actividad"
            wrapperClassName="sm:col-span-2"
            registration={form.register('actividad')}
            error={errores.actividad}
          />

          <Controller
            control={control}
            name="gradoCodigo"
            render={({ field }) => (
              <Select
                label="Grado"
                value={field.value}
                disabled={!anio}
                onChange={(e) => {
                  field.onChange(e.target.value);
                  // La sección elegida era de otro grado, y con ella cambia la jornada.
                  setValue('seccionId', '');
                  setValue('horaInicio', '');
                  setValue('duracion', '');
                }}
                options={grados.map((g) => ({
                  label: g.nombre,
                  value: g.codigo,
                }))}
                error={errores.gradoCodigo}
              />
            )}
          />
          <Controller
            control={control}
            name="seccionId"
            render={({ field }) => (
              <Select
                label="Sección"
                value={field.value}
                disabled={!gradoCodigo}
                onChange={(e) => {
                  field.onChange(e.target.value);
                  setValue('horaInicio', '');
                  setValue('duracion', '');
                }}
                options={secciones.map((s) => ({
                  label: s.nombre,
                  value: s.id,
                }))}
                error={errores.seccionId}
              />
            )}
          />

          <Controller
            control={control}
            name="fecha"
            render={({ field }) => (
              <DatePicker
                size="sm"
                label="Fecha"
                // No se reserva en el pasado; el backend vuelve a validar la fecha y la hora.
                min={dayjs().format('YYYY-MM-DD')}
                value={field.value}
                onChange={field.onChange}
                error={errores.fecha}
              />
            )}
          />
          <Controller
            control={control}
            name="horaInicio"
            render={({ field }) => (
              <Select
                label="Hora"
                value={field.value}
                disabled={!jornada}
                onChange={(e) => {
                  field.onChange(e.target.value);
                  // La duración elegida puede pasarse del fin de la jornada desde la hora nueva.
                  const elegida = form.getValues('duracion');
                  const sigueValida = opcionesDeDuracion(
                    jornada as JornadaDelDia,
                    e.target.value,
                    mismaSeccionQueLaGuardada && reservaActual
                      ? duracionEnMinutos(
                          reservaActual.horaInicio,
                          reservaActual.horaFin,
                        )
                      : undefined,
                  ).some((o) => String(o.value) === elegida && !o.disabled);
                  if (!sigueValida) setValue('duracion', '');
                }}
                options={opcionesDeLaHora}
                error={errores.horaInicio}
              />
            )}
          />

          <Controller
            control={control}
            name="duracion"
            render={({ field }) => (
              <Select
                label="Duración"
                value={field.value}
                disabled={!jornada || !horaInicio}
                onChange={(e) => field.onChange(e.target.value)}
                options={opcionesDeLaDuracion.map((o) => ({
                  label: o.label,
                  value: String(o.value),
                  disabled: o.disabled,
                }))}
                error={errores.duracion}
              />
            )}
          />
        </div>

        {seccionId && horarioQuery.isError && (
          <Alert
            type="warning"
            size="sm"
            title={mensajeDeError(horarioQuery.error)}
          />
        )}

        {aviso && errorDeEnvio && (
          <Alert
            type="error"
            size="sm"
            title={aviso.titulo ?? aviso.texto}
            action={
              errorDeEnvio.conflictoDeVersion ? (
                <Button
                  type="button"
                  size="sm"
                  variant="primary-light"
                  isLoading={recargando}
                  onClick={recargar}
                >
                  Recargar
                </Button>
              ) : undefined
            }
          >
            {aviso.titulo ? aviso.texto : undefined}
          </Alert>
        )}
      </div>

      <div className="flex items-center justify-end gap-3 border-t border-stroke-tertiary px-8 py-5">
        <Button
          type="button"
          variant="primary-light"
          onClick={onCerrar}
          disabled={enviando}
        >
          Cancelar
        </Button>
        <Button
          type="submit"
          variant="primary"
          isLoading={enviando}
          disabled={sinAnio}
        >
          {editando ? 'Guardar cambios' : 'Reservar'}
        </Button>
      </div>
    </form>
  );
};
