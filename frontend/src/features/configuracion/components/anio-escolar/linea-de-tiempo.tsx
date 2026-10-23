import dayjs from 'dayjs';

import { AnioEscolarDetalle, EstadoPeriodo } from '@/types/api';
import { cn } from '@/utils/cn';
import { formatDay } from '@/utils/format';

import { ESTADO_PERIODO } from './formato';

type DatosAnio = Pick<
  AnioEscolarDetalle,
  'fechaInicio' | 'fechaFin' | 'estado' | 'periodos'
>;

/** Dos decimales bastan para un porcentaje de ancho y evitan 55.00000000000001. */
const redondear = (n: number) => Math.round(n * 100) / 100;

const dias = (desde: string, hasta: string) =>
  dayjs(hasta).diff(dayjs(desde), 'day');

/**
 * Posición (0 a 100) de un tramo dentro del año. Cada día ocupa el mismo ancho; `inclusivo` cuenta el último día
 * completo, para que el fin de un periodo llegue hasta el final de ese día.
 */
export const porcentajeDelAnio = (
  anio: Pick<DatosAnio, 'fechaInicio' | 'fechaFin'>,
  fecha: string,
  inclusivo = false,
) => {
  const total = dias(anio.fechaInicio, anio.fechaFin) + 1;
  return redondear(
    ((dias(anio.fechaInicio, fecha) + (inclusivo ? 1 : 0)) / total) * 100,
  );
};

/** Posición (0 a 100) del centro de un día: ahí va la marca "Hoy". */
const centroDelDia = (
  anio: Pick<DatosAnio, 'fechaInicio' | 'fechaFin'>,
  fecha: string,
) => {
  const total = dias(anio.fechaInicio, anio.fechaFin) + 1;
  return redondear(((dias(anio.fechaInicio, fecha) + 0.5) / total) * 100);
};

// Colores de los segmentos de Figma: Cerrado (gris), En curso (verde) y Planificado (azul).
const COLOR_SEGMENTO: Record<EstadoPeriodo, string> = {
  CERRADO: 'border-transparent bg-surface-quartiary text-content-tertiary',
  EN_CURSO:
    'border-stroke-success-primary bg-surface-success-light text-content-success',
  PLANIFICADO:
    'border-stroke-accent-secondary bg-surface-accent-light text-content-accent-secondary',
};

type LineaDeTiempoProps = {
  anio: DatosAnio;
  /** Día de hoy en yyyy-mm-dd; por defecto el del navegador. Solo coloca la marca "Hoy". */
  hoy?: string;
};

// Figma > CI-03 y CI-06 > "Línea de tiempo": los periodos sobre el año, con la marca "Hoy" si el año está activo.
export const LineaDeTiempo = ({
  anio,
  hoy = dayjs().format('YYYY-MM-DD'),
}: LineaDeTiempoProps) => {
  const hoyEnElAnio =
    anio.estado === 'ACTIVO' && hoy >= anio.fechaInicio && hoy <= anio.fechaFin;

  return (
    <div className="flex flex-col gap-2">
      <ol
        aria-label="Línea de tiempo de los periodos"
        className="relative h-[46px] rounded-[10px] bg-surface-secondary"
      >
        {anio.periodos.map((periodo) => {
          const inicio = porcentajeDelAnio(anio, periodo.fechaInicio);
          const fin = porcentajeDelAnio(anio, periodo.fechaFin, true);
          return (
            <li
              key={periodo.id}
              title={`${periodo.nombre} · ${ESTADO_PERIODO[periodo.estado].texto}`}
              style={{ left: `${inicio}%`, width: `${fin - inicio}%` }}
              className={cn(
                'absolute top-1.5 flex h-[34px] items-center overflow-hidden rounded-lg border px-2.5',
                COLOR_SEGMENTO[periodo.estado],
              )}
            >
              <span className="truncate text-label-2 font-semibold">
                {periodo.nombre}
              </span>
            </li>
          );
        })}
        {hoyEnElAnio && (
          <li
            aria-label="Hoy"
            style={{ left: `${centroDelDia(anio, hoy)}%` }}
            className="absolute top-[-5px] h-14 w-0.5 bg-surface-error"
          >
            <span className="absolute top-[-17px] -translate-x-1/2 text-body-4 text-content-error">
              Hoy
            </span>
          </li>
        )}
      </ol>
      <div className="flex justify-between text-body-4 text-content-tertiary">
        <p>Inicio {formatDay(anio.fechaInicio)}</p>
        <p>Fin {formatDay(anio.fechaFin)}</p>
      </div>
    </div>
  );
};
