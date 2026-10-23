import { zodResolver } from '@hookform/resolvers/zod';
import { CirclePlus, Clock, Trash2 } from 'lucide-react';
import * as React from 'react';
import { useFieldArray, useForm } from 'react-hook-form';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Checkbox, Input, Select } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { useNotifications } from '@/components/ui/notifications';
import { SelectableTag } from '@/components/ui/tag';
import { useDebouncedValue } from '@/hooks/use-debounced-value';
import {
  DetalleEstructuraHoraria,
  DiaLectivo,
  EstructuraGuardada,
  SolicitudVistaPrevia,
  VistaPreviaJornada as RespuestaVistaPrevia,
} from '@/types/api';

import { useAnioEscolar } from '../../api/anios-escolares';
import { useVistaPreviaJornada } from '../../api/estructuras-horarias';
import {
  useCrearEstructura,
  useEditarEstructura,
} from '../../api/mutaciones-estructuras';

import {
  BLOQUES_MAXIMOS_POR_DIA,
  BLOQUES_MINIMOS_POR_DIA,
  ValoresEstructura,
  aSolicitudEdicion,
  aSolicitudEstructura,
  aSolicitudVistaPrevia,
  estructuraSchema,
  PREFIJO_CODIGO_REPETIDO,
  normalizarCodigo,
  puedeCalcularseLaJornada,
  valoresDeEstructura,
  valoresInicialesEstructura,
} from './esquemas-estructura';
import { DIAS_LECTIVOS, NOMBRE_DIA_CORTO, NOMBRE_NIVEL } from './formato';
import { VistaPreviaJornada } from './vista-previa-jornada';

type DialogoEstructuraProps = {
  anioId: string;
  onCerrar: () => void;
  /** Con una estructura el modal edita esa estructura; sin ella crea una nueva. */
  estructura?: DetalleEstructuraHoraria;
  /** Recibe el id de la estructura creada o editada, para dejarla elegida en la lista. */
  onGuardada?: (estructuraId: string) => void;
};

const OPCIONES_DE_NIVEL = Object.entries(NOMBRE_NIVEL).map(
  ([valor, texto]) => ({
    label: texto,
    value: valor,
  }),
);

// Figma > Personal Administrativo > "Modal - Nueva estructura horaria" (HU-44). Se monta solo mientras está abierto.
// Editar no está en Figma: es el mismo modal con los datos de la estructura, sin poder cambiar el nivel.
export const DialogoEstructura = (props: DialogoEstructuraProps) => (
  <Dialog open onOpenChange={(open) => !open && props.onCerrar()}>
    <DialogContent className="flex max-h-[92vh] max-w-[860px] flex-col gap-0 overflow-hidden rounded-xl border-stroke-tertiary bg-white p-0">
      <FormularioEstructura {...props} />
    </DialogContent>
  </Dialog>
);

const ETIQUETA_SECCION = 'text-label-2 font-medium text-content-primary';

const FormularioEstructura = ({
  anioId,
  onCerrar,
  estructura,
  onGuardada,
}: DialogoEstructuraProps) => {
  const editando = estructura !== undefined;
  const { addNotification } = useNotifications();
  // El año ya está en la caché de la página (la cabecera lo pidió): no es una petición nueva.
  const anio = useAnioEscolar(anioId);

  // Los errores salen en vivo, mientras se escribe, y no solo al enviar.
  const form = useForm<ValoresEstructura>({
    resolver: zodResolver(estructuraSchema),
    mode: 'onChange',
    defaultValues: estructura
      ? valoresDeEstructura(estructura)
      : valoresInicialesEstructura(),
  });
  const { register, control, setValue, watch, formState } = form;
  const errores = formState.errors;
  const recesos = useFieldArray({ control, name: 'recesos' });
  const valores = watch();

  // La posición de cada receso depende de los bloques por día, y dos recesos no pueden ir en el mismo bloque: al
  // cambiar esos campos hay que revisar los demás recesos. Solo los que ya tienen algo escrito o ya muestran un error
  // (para limpiarlo): un campo vacío que el usuario todavía no tocó espera a su primera entrada (o al envío), como
  // cualquier otro campo, en vez de salir en rojo apenas se agrega la fila o se elige el bloque.
  const revalidarRecesos = () => {
    const camposARevisar = form
      .getValues('recesos')
      .flatMap((receso, indice) => {
        const posicion = `recesos.${indice}.despuesDeBloque` as const;
        const duracion = `recesos.${indice}.duracionMin` as const;
        return [
          ...(receso.despuesDeBloque.trim() !== '' ||
          form.getFieldState(posicion).error
            ? [posicion]
            : []),
          ...(receso.duracionMin.trim() !== '' ||
          form.getFieldState(duracion).error
            ? [duracion]
            : []),
        ];
      });
    if (camposARevisar.length > 0) void form.trigger(camposARevisar);
  };

  // Los errores del backend (código repetido, año no planificado, planes que no caben, desmarcar la principal) salen
  // por el interceptor.
  const alGuardar = (titulo: string) => (guardada: EstructuraGuardada) => {
    addNotification({
      type: 'success',
      title: titulo,
      message: `${guardada.estructura.codigo} · ${NOMBRE_NIVEL[guardada.estructura.nivel]}`,
    });
    // Un aviso por cada advertencia del backend, con su texto.
    guardada.advertencias.forEach((advertencia) =>
      addNotification({
        type: 'warning',
        title: 'Advertencia',
        message: advertencia,
      }),
    );
    onGuardada?.(guardada.estructura.id);
    onCerrar();
  };
  const crear = useCrearEstructura(anioId, alGuardar('Estructura creada'));
  const editar = useEditarEstructura(alGuardar('Estructura actualizada'));
  const guardando = crear.isPending || editar.isPending;

  // La vista previa se pide al backend cuando el usuario hace una pausa, no en cada tecla. La clave en texto evita que
  // un objeto nuevo en cada render reinicie la espera. Todo (si hay con qué calcular y qué se manda) sale de la misma
  // copia retrasada: decidirlo con lo escrito ahora y mandar la copia vieja pediría una vista previa desactualizada.
  const clave = JSON.stringify(aSolicitudVistaPrevia(valores));
  const claveRetardada = useDebouncedValue(clave);
  const solicitudRetardada = React.useMemo(
    () => JSON.parse(claveRetardada) as SolicitudVistaPrevia,
    [claveRetardada],
  );
  const hayConQueCalcular = puedeCalcularseLaJornada(solicitudRetardada);
  const vistaPrevia = useVistaPreviaJornada(
    anioId,
    hayConQueCalcular ? solicitudRetardada : null,
  );

  const enviar = form.handleSubmit((datos) =>
    estructura
      ? editar.mutate({ id: estructura.id, data: aSolicitudEdicion(datos) })
      : crear.mutate(aSolicitudEstructura(datos)),
  );

  // Al editar, el backend avisa que el código ya existe aunque sea el de esta misma estructura: ese aviso no se muestra
  // mientras el código no cambie. Si se cambia por el de otra estructura, el aviso es cierto y sí se muestra.
  const sinAvisoDeSuPropioCodigo = (
    vista: RespuestaVistaPrevia | undefined,
  ): RespuestaVistaPrevia | undefined =>
    vista && estructura && valores.codigo.trim() === estructura.codigo
      ? {
          ...vista,
          errores: vista.errores.filter(
            (error) => !error.startsWith(PREFIJO_CODIGO_REPETIDO),
          ),
        }
      : vista;

  // Al editar, la vista previa arranca con la jornada que ya tiene la estructura, sin esperar al backend.
  const vistaInicial: RespuestaVistaPrevia | undefined = estructura && {
    valida: true,
    errores: [],
    jornada: {
      filas: estructura.jornada,
      horaSalida: estructura.horaSalida,
      bloquesPorSemana: estructura.bloquesPorSemana,
    },
  };

  // Una respuesta sin jornada no tapa la que la estructura ya tiene (solo al editar), pero sus errores sí se muestran.
  const vistaMostrada: RespuestaVistaPrevia | undefined = vistaPrevia.data
    ? vistaInicial && !vistaPrevia.data.jornada
      ? { ...vistaPrevia.data, jornada: vistaInicial.jornada }
      : vistaPrevia.data
    : vistaInicial;

  // Un receso va después de un bloque entre 1 y (bloques - 1).
  const bloques = /^\d+$/.test(valores.bloquesPorDia.trim())
    ? Number(valores.bloquesPorDia.trim())
    : 0;
  const opcionesDeReceso =
    bloques >= BLOQUES_MINIMOS_POR_DIA && bloques <= BLOQUES_MAXIMOS_POR_DIA
      ? Array.from({ length: Math.max(bloques - 1, 0) }, (_, i) => ({
          label: `Después del bloque ${i + 1}`,
          value: String(i + 1),
        }))
      : [];

  const alternarDia = (dia: DiaLectivo) => {
    const marcados = valores.diasLectivos;
    setValue(
      'diasLectivos',
      marcados.includes(dia)
        ? marcados.filter((d) => d !== dia)
        : [...marcados, dia],
      { shouldValidate: true, shouldDirty: true },
    );
  };

  return (
    <form onSubmit={enviar} noValidate className="flex min-h-0 flex-1 flex-col">
      <div className="flex-1 space-y-4 overflow-y-auto p-8">
        <div className="space-y-3">
          <IconBadge color="accent" size="lg" aria-hidden="true">
            <Clock />
          </IconBadge>
          <div className="space-y-2">
            <DialogTitle className="text-h6 font-semibold text-content-primary">
              {editando
                ? 'Editar estructura horaria'
                : 'Nueva estructura horaria'}
            </DialogTitle>
            <DialogDescription className="text-body-3 text-content-tertiary">
              {anio.data ? `Año escolar ${anio.data.anio}. ` : ''}Los bloques se
              calculan a partir de la hora de entrada, la duración y los
              recesos.
            </DialogDescription>
          </div>
        </div>

        <div className="grid gap-6 pt-1 lg:grid-cols-[minmax(0,1fr)_320px]">
          <div className="space-y-3.5">
            <div className="grid grid-cols-2 gap-3">
              <Input
                label="Código *"
                autoComplete="off"
                // El código se pasa a mayúsculas al recibir lo escrito; sin tope de largo en el campo para que el aviso
                // "hasta 20 caracteres" pueda verse en vivo.
                registration={register('codigo', {
                  onChange: (evento) =>
                    setValue('codigo', normalizarCodigo(evento.target.value), {
                      shouldValidate: true,
                    }),
                })}
                error={errores.codigo}
              />
              <Input
                label="Descripción"
                autoComplete="off"
                registration={register('descripcion')}
                error={errores.descripcion}
              />
            </div>

            {/* El nivel y la casilla no están en Figma, pero la BD los exige. */}
            <div className="grid grid-cols-2 items-start gap-3">
              {editando ? (
                // El nivel no se cambia (para otro nivel se elimina y se crea de nuevo). Va sin `register`: un campo
                // deshabilitado y registrado quedaría sin valor en el formulario.
                <Select
                  label="Nivel *"
                  disabled
                  value={estructura.nivel}
                  onChange={() => undefined}
                  options={OPCIONES_DE_NIVEL}
                  hint="El nivel no se puede cambiar."
                />
              ) : (
                <Select
                  label="Nivel *"
                  placeholder="Elige el nivel"
                  options={OPCIONES_DE_NIVEL}
                  registration={register('nivel')}
                  error={errores.nivel}
                />
              )}
              {editando && estructura.esPrincipal ? (
                // La principal no se puede desmarcar (409): para cambiar de principal se marca otra estructura.
                <Checkbox
                  label="Estructura principal"
                  helperText="Para cambiar de principal, marca otra estructura del nivel."
                  className="pt-2"
                  checked
                  disabled
                  onChange={() => undefined}
                />
              ) : (
                <Checkbox
                  label="Estructura principal"
                  helperText={
                    editando
                      ? 'Si la marcas, la principal actual del nivel deja de serlo.'
                      : 'La primera de cada nivel nace como principal.'
                  }
                  className="pt-2"
                  {...register('esPrincipal')}
                />
              )}
            </div>

            <div className="space-y-2">
              <p id="dias-lectivos" className={ETIQUETA_SECCION}>
                Días lectivos *
              </p>
              <div
                role="group"
                aria-labelledby="dias-lectivos"
                className="flex flex-wrap gap-2"
              >
                {DIAS_LECTIVOS.map((dia) => {
                  const marcado = valores.diasLectivos.includes(dia);
                  return (
                    <SelectableTag
                      key={dia}
                      type="button"
                      color="info"
                      size="md"
                      selected={marcado}
                      aria-pressed={marcado}
                      onClick={() => alternarDia(dia)}
                    >
                      {NOMBRE_DIA_CORTO[dia]}
                    </SelectableTag>
                  );
                })}
              </div>
              {errores.diasLectivos && (
                <p role="alert" className="text-body-4 text-content-error">
                  {errores.diasLectivos.message}
                </p>
              )}
            </div>

            <div className="grid grid-cols-3 gap-3">
              <Input
                type="time"
                label="Hora de entrada *"
                registration={register('horaEntrada')}
                error={errores.horaEntrada}
              />
              <Input
                label="Bloques por día *"
                inputMode="numeric"
                autoComplete="off"
                // La posición de cada receso depende de los bloques por día: al cambiarlos se revisan los recesos.
                registration={register('bloquesPorDia', {
                  onChange: revalidarRecesos,
                })}
                error={errores.bloquesPorDia}
              />
              <Input
                label="Duración del bloque *"
                inputMode="numeric"
                autoComplete="off"
                // El "min" aparece cuando ya hay un valor ("45 min"); vacío chocaría con la etiqueta.
                endAdornment={
                  valores.duracionBloqueMin ? (
                    <span className="text-body-4">min</span>
                  ) : undefined
                }
                registration={register('duracionBloqueMin')}
                error={errores.duracionBloqueMin}
              />
            </div>

            <div className="space-y-2">
              <p className={ETIQUETA_SECCION}>Recesos</p>
              {recesos.fields.map((campo, indice) => (
                <div key={campo.id} className="flex items-start gap-2.5">
                  <Select
                    aria-label={`Receso ${indice + 1}: después de qué bloque`}
                    placeholder="Después del bloque…"
                    wrapperClassName="flex-1"
                    options={opcionesDeReceso}
                    registration={register(
                      `recesos.${indice}.despuesDeBloque` as const,
                      { onChange: revalidarRecesos },
                    )}
                    error={errores.recesos?.[indice]?.despuesDeBloque}
                  />
                  <Input
                    label="Duración"
                    inputMode="numeric"
                    autoComplete="off"
                    wrapperClassName="w-[130px]"
                    endAdornment={
                      valores.recesos[indice]?.duracionMin ? (
                        <span className="text-body-4">min</span>
                      ) : undefined
                    }
                    registration={register(
                      `recesos.${indice}.duracionMin` as const,
                    )}
                    error={errores.recesos?.[indice]?.duracionMin}
                  />
                  <Button
                    type="button"
                    variant="error-light"
                    size="icon"
                    className="mt-1.5 size-8"
                    aria-label={`Quitar el receso ${indice + 1}`}
                    onClick={() => recesos.remove(indice)}
                  >
                    <Trash2 className="size-4" aria-hidden="true" />
                  </Button>
                </div>
              ))}
              <Button
                type="button"
                variant="tertiary"
                size="sm"
                icon={<CirclePlus className="size-3.5" aria-hidden="true" />}
                onClick={() =>
                  recesos.append({ despuesDeBloque: '', duracionMin: '' })
                }
              >
                Agregar receso
              </Button>
            </div>
          </div>

          <VistaPreviaJornada
            // Con datos que no alcanzan para calcular no se pide nada y se sigue viendo la última vista previa (la
            // consulta conserva la respuesta anterior), atenuada y avisando que hay datos por corregir.
            vista={sinAvisoDeSuPropioCodigo(vistaMostrada)}
            actualizando={
              hayConQueCalcular &&
              (vistaPrevia.isFetching || clave !== claveRetardada)
            }
            desactualizada={!hayConQueCalcular}
          />
        </div>
      </div>

      <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-8 py-4">
        <Button type="button" variant="tertiary" onClick={onCerrar}>
          Cancelar
        </Button>
        <Button type="submit" isLoading={guardando} disabled={guardando}>
          {editando ? 'Guardar cambios' : 'Crear estructura'}
        </Button>
      </div>
    </form>
  );
};
