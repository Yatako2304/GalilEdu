import { Coffee } from 'lucide-react';

import { VistaPreviaJornada as Vista } from '@/types/api';
import { cn } from '@/utils/cn';

import {
  MENSAJE_JORNADA_MUY_TARDE,
  esMensajeDelEsquema,
} from './esquemas-estructura';
import { textoRango } from './formato';

type VistaPreviaJornadaProps = {
  /** La última respuesta del backend; undefined mientras no haya llegado ninguna. */
  vista: Vista | undefined;
  /** Hay una respuesta nueva en camino: se sigue mostrando la anterior, algo atenuada. */
  actualizando: boolean;
  /** Los datos del formulario no alcanzan (o no son válidos) para calcular: lo que se ve es de la última vez que sí. */
  desactualizada?: boolean;
};

// Figma > "Modal - Nueva estructura horaria" > "Vista previa de la jornada": los bloques y recesos en orden, la hora
// de salida, los bloques por semana y el aviso de las 18:00. Todo lo calcula el backend (`vista-previa`): aquí solo se
// dibuja. La jornada llega aunque pase de las 18:00, para que se vea la salida real.
export const VistaPreviaJornada = ({
  vista,
  actualizando,
  desactualizada = false,
}: VistaPreviaJornadaProps) => {
  const jornada = vista?.jornada ?? null;
  const atenuada = actualizando || desactualizada;
  const pasaDeLas18 =
    vista?.errores.includes(MENSAJE_JORNADA_MUY_TARDE) ?? false;
  // Los demás errores del backend ya salen junto a su campo; aquí solo los que el formulario no cubre.
  const otrosErrores = (vista?.errores ?? []).filter(
    (error) =>
      error !== MENSAJE_JORNADA_MUY_TARDE && !esMensajeDelEsquema(error),
  );

  // Las claves siguen al bloque (no a la hora): al agregar un receso o cambiar un tiempo, las filas se actualizan en
  // su lugar en vez de volver a crearse, y el receso nuevo aparece entre los bloques que ya estaban.
  let bloqueAnterior = 0;
  const filas = (jornada?.filas ?? []).map((fila) => {
    if (fila.tipo === 'BLOQUE') {
      bloqueAnterior = fila.numero ?? bloqueAnterior;
      return (
        <li
          key={`bloque-${fila.numero}`}
          className="flex items-center justify-between rounded-lg border border-stroke-tertiary bg-white px-3 py-2"
        >
          <span className="text-label-2 font-medium text-content-primary">
            Bloque {fila.numero}
          </span>
          <span className="text-body-4 text-content-primary">
            {textoRango(fila.inicio, fila.fin)}
          </span>
        </li>
      );
    }
    return (
      <li
        key={`receso-despues-${bloqueAnterior}`}
        className="flex items-center justify-between rounded-lg border border-stroke-warning-secondary bg-surface-warning-light px-3 py-2 text-body-4 text-content-warning"
      >
        <span className="flex items-center gap-1.5">
          <Coffee className="size-4" aria-hidden="true" />
          Receso · {fila.duracionMin} min
        </span>
        <span>{textoRango(fila.inicio, fila.fin)}</span>
      </li>
    );
  });

  return (
    <section
      aria-label="Vista previa de la jornada"
      aria-busy={actualizando}
      className="flex flex-col gap-2.5"
    >
      <h3 className="text-label-2 font-semibold text-content-primary">
        Vista previa de la jornada
      </h3>

      {jornada ? (
        <>
          <ul
            className={cn(
              'flex flex-col gap-1.5 rounded-xl bg-surface-secondary p-3 transition-opacity',
              atenuada && 'opacity-60',
            )}
          >
            {filas}
          </ul>
          <dl
            className={cn(
              'flex flex-col gap-2 rounded-[10px] bg-surface-secondary px-4 py-3.5 transition-opacity',
              atenuada && 'opacity-60',
            )}
          >
            <div className="flex items-start justify-between">
              <dt className="text-body-3 text-content-tertiary">
                Hora de salida
              </dt>
              <dd
                className={cn(
                  'text-label-2 font-medium',
                  pasaDeLas18 ? 'text-content-error' : 'text-content-primary',
                )}
              >
                {jornada.horaSalida}
              </dd>
            </div>
            <div className="flex items-start justify-between">
              <dt className="text-body-3 text-content-tertiary">
                Bloques por semana
              </dt>
              <dd className="text-label-2 font-medium text-content-primary">
                {jornada.bloquesPorSemana}
              </dd>
            </div>
          </dl>
          {desactualizada && (
            <p className="text-body-4 text-content-tertiary">
              Corrige los datos marcados para actualizar la vista previa.
            </p>
          )}
        </>
      ) : (
        <p className="rounded-xl bg-surface-secondary px-3 py-4 text-body-4 text-content-tertiary">
          Completa la hora de entrada, los bloques por día y la duración para
          ver la jornada.
        </p>
      )}

      {otrosErrores.length > 0 && (
        <ul className="space-y-0.5 text-body-4 text-content-error">
          {otrosErrores.map((error) => (
            <li key={error}>{error}</li>
          ))}
        </ul>
      )}
      <p
        role={pasaDeLas18 ? 'alert' : undefined}
        className={cn(
          'text-body-4',
          pasaDeLas18 ? 'text-content-error' : 'text-content-tertiary',
        )}
      >
        {MENSAJE_JORNADA_MUY_TARDE}
      </p>
    </section>
  );
};
