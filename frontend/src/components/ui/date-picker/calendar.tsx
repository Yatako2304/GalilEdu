import dayjs, { type Dayjs } from 'dayjs';
import { ChevronDown, ChevronLeft, ChevronRight } from 'lucide-react';
import * as React from 'react';

import { cn } from '@/utils/cn';

const MESES = [
  'Enero',
  'Febrero',
  'Marzo',
  'Abril',
  'Mayo',
  'Junio',
  'Julio',
  'Agosto',
  'Setiembre',
  'Octubre',
  'Noviembre',
  'Diciembre',
];
const MESES_CORTOS = [
  'ENE',
  'FEB',
  'MAR',
  'ABR',
  'MAY',
  'JUN',
  'JUL',
  'AGO',
  'SEP',
  'OCT',
  'NOV',
  'DIC',
];
const DIAS = ['DO', 'LU', 'MA', 'MI', 'JU', 'VI', 'SA'];
const FORMATO = 'YYYY-MM-DD';

type Vista = 'dias' | 'meses' | 'anios';

export type CalendarProps = {
  /** Fecha elegida en formato YYYY-MM-DD. */
  value?: string | null;
  onChange: (value: string) => void;
  /** Fechas límite (YYYY-MM-DD); fuera de ese rango los días quedan deshabilitados. */
  min?: string;
  max?: string;
  className?: string;
};

// Figma > Componentes > Picker Cell (State = Default, Hover, Selected, Disabled · Today)
const Celda = ({
  dia,
  seleccionado,
  hoy,
  fueraDeMes,
  deshabilitado,
  onClick,
}: {
  dia: Dayjs;
  seleccionado: boolean;
  hoy: boolean;
  fueraDeMes: boolean;
  deshabilitado: boolean;
  onClick: () => void;
}) => (
  <button
    type="button"
    onClick={onClick}
    disabled={deshabilitado}
    aria-pressed={seleccionado}
    aria-label={dia.format('D/MM/YYYY')}
    className={cn(
      'inline-flex size-8 items-center justify-center rounded-full text-label-3 outline-none transition-colors',
      'focus-visible:border focus-visible:border-stroke-primary',
      seleccionado
        ? 'bg-surface-accent-primary font-medium text-white'
        : 'hover:bg-surface-primary-hover hover:text-content-primary-hover',
      !seleccionado && hoy && 'border border-stroke-accent-secondary',
      !seleccionado &&
        (fueraDeMes || deshabilitado
          ? 'text-content-disabled-alt'
          : 'text-content-primary'),
      deshabilitado && 'cursor-not-allowed hover:bg-transparent',
    )}
  >
    {dia.date()}
  </button>
);

// Figma > Componentes > Picker Months / Picker Years
const Opcion = ({
  activa,
  deshabilitada,
  onClick,
  children,
}: {
  activa: boolean;
  deshabilitada?: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) => (
  <button
    type="button"
    onClick={onClick}
    disabled={deshabilitada}
    aria-pressed={activa}
    className={cn(
      'w-20 rounded-lg p-2 text-center text-label-2 outline-none transition-colors focus-visible:ring-2 focus-visible:ring-stroke-focus',
      activa
        ? 'bg-surface-accent-primary text-white'
        : 'text-content-primary hover:bg-surface-accent-light',
      deshabilitada && 'cursor-not-allowed text-content-disabled-alt',
    )}
  >
    {children}
  </button>
);

const BotonFlecha = ({
  etiqueta,
  onClick,
  children,
}: {
  etiqueta: string;
  onClick: () => void;
  children: React.ReactNode;
}) => (
  <button
    type="button"
    onClick={onClick}
    aria-label={etiqueta}
    className="inline-flex rounded-lg p-2 text-content-accent-primary outline-none hover:bg-surface-accent-light focus-visible:ring-2 focus-visible:ring-stroke-focus"
  >
    {children}
  </button>
);

// Figma > Componentes > Date picker (Type = Month v2, Year, Years)
export const Calendar = ({
  value,
  onChange,
  min,
  max,
  className,
}: CalendarProps) => {
  const elegida = value ? dayjs(value) : null;
  const [mes, setMes] = React.useState<Dayjs>(
    (elegida ?? dayjs()).startOf('month'),
  );
  const [vista, setVista] = React.useState<Vista>('dias');
  const listaAnios = React.useRef<HTMLDivElement>(null);

  const fueraDeRango = (d: Dayjs, unidad: 'day' | 'month' | 'year') =>
    (min !== undefined && d.isBefore(dayjs(min), unidad)) ||
    (max !== undefined && d.isAfter(dayjs(max), unidad));

  // Seis semanas desde el domingo anterior al día 1 (Figma muestra los días vecinos en gris).
  const inicio = mes.subtract(mes.day(), 'day');
  const dias = Array.from({ length: 42 }, (_, i) => inicio.add(i, 'day'));
  const semanas = Array.from({ length: 6 }, (_, i) =>
    dias.slice(i * 7, i * 7 + 7),
  ).filter((semana) => semana.some((d) => d.month() === mes.month()));

  const anioActual = mes.year();
  const anios = Array.from({ length: 101 }, (_, i) => anioActual - 80 + i);

  React.useEffect(() => {
    if (vista === 'anios') {
      listaAnios.current
        ?.querySelector('[aria-pressed="true"]')
        ?.scrollIntoView({ block: 'center' });
    }
  }, [vista]);

  return (
    <div
      className={cn(
        'flex w-fit flex-col items-center gap-2 rounded-lg border-b border-stroke-primary bg-surface-primary p-6 shadow-card',
        className,
      )}
    >
      {vista === 'dias' && (
        <>
          <div className="flex w-full items-center gap-2">
            <BotonFlecha
              etiqueta="Mes anterior"
              onClick={() => setMes(mes.subtract(1, 'month'))}
            >
              <ChevronLeft className="size-6" aria-hidden="true" />
            </BotonFlecha>
            <button
              type="button"
              onClick={() => setVista('meses')}
              className="mx-auto inline-flex items-center gap-3 rounded-lg p-2 text-h6 text-content-secondary outline-none hover:bg-surface-accent-light focus-visible:ring-2 focus-visible:ring-stroke-focus"
            >
              {MESES[mes.month()]} {mes.year()}
              <ChevronDown className="size-5" aria-hidden="true" />
            </button>
            <BotonFlecha
              etiqueta="Mes siguiente"
              onClick={() => setMes(mes.add(1, 'month'))}
            >
              <ChevronRight className="size-6" aria-hidden="true" />
            </BotonFlecha>
          </div>
          <div className="grid grid-cols-7 gap-x-5 gap-y-2">
            {DIAS.map((d) => (
              <span
                key={d}
                className="inline-flex size-8 items-center justify-center text-label-3 font-medium text-content-primary"
              >
                {d}
              </span>
            ))}
            {semanas.flat().map((d) => (
              <Celda
                key={d.format(FORMATO)}
                dia={d}
                seleccionado={!!elegida && d.isSame(elegida, 'day')}
                hoy={d.isSame(dayjs(), 'day')}
                fueraDeMes={d.month() !== mes.month()}
                deshabilitado={fueraDeRango(d, 'day')}
                onClick={() => {
                  onChange(d.format(FORMATO));
                  if (d.month() !== mes.month()) setMes(d.startOf('month'));
                }}
              />
            ))}
          </div>
        </>
      )}

      {vista === 'meses' && (
        <>
          <button
            type="button"
            onClick={() => setVista('anios')}
            className="inline-flex items-center gap-3 self-start rounded-lg p-2 text-h6 text-content-secondary outline-none hover:bg-surface-accent-light focus-visible:ring-2 focus-visible:ring-stroke-focus"
          >
            {mes.year()}
            <ChevronDown className="size-5" aria-hidden="true" />
          </button>
          <div className="grid grid-cols-3 gap-x-5 gap-y-2">
            {MESES_CORTOS.map((m, i) => (
              <Opcion
                key={m}
                activa={!!elegida && elegida.isSame(mes.month(i), 'month')}
                deshabilitada={fueraDeRango(mes.month(i), 'month')}
                onClick={() => {
                  setMes(mes.month(i));
                  setVista('dias');
                }}
              >
                {m}
              </Opcion>
            ))}
          </div>
        </>
      )}

      {vista === 'anios' && (
        <div
          ref={listaAnios}
          className="flex h-[168px] w-[280px] flex-col items-center gap-2 overflow-y-auto pr-2 [scrollbar-color:theme(colors.slate.700)_theme(colors.slate.200)] [scrollbar-width:thin]"
        >
          {anios.map((a) => (
            <Opcion
              key={a}
              activa={a === anioActual}
              deshabilitada={fueraDeRango(mes.year(a), 'year')}
              onClick={() => {
                setMes(mes.year(a));
                setVista('meses');
              }}
            >
              {a}
            </Opcion>
          ))}
        </div>
      )}
    </div>
  );
};
