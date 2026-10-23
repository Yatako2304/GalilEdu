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

/** `md` es el Date picker de Figma. `sm` es una versión compacta (no está en Figma) para modales con varios campos. */
export type CalendarSize = 'md' | 'sm';

// Cada tamaño con sus clases completas (Tailwind solo genera clases escritas enteras).
const TAMANOS = {
  md: {
    caja: 'gap-2 p-6',
    titulo: 'text-h6',
    flecha: 'p-2',
    iconoFlecha: 'size-6',
    iconoTitulo: 'size-5',
    cuadricula: 'gap-x-5 gap-y-2',
    celda: 'size-8',
    opcion: 'w-20 p-2',
    anios: 'h-[168px] w-[280px]',
  },
  sm: {
    caja: 'gap-1 p-4',
    titulo: 'text-label-1',
    flecha: 'p-1',
    iconoFlecha: 'size-5',
    iconoTitulo: 'size-4',
    cuadricula: 'gap-x-3 gap-y-1',
    celda: 'size-7',
    opcion: 'w-16 p-1.5',
    anios: 'h-[140px] w-[220px]',
  },
} as const;

export type CalendarProps = {
  /** Fecha elegida en formato YYYY-MM-DD. */
  value?: string | null;
  onChange: (value: string) => void;
  /** Fechas límite (YYYY-MM-DD); fuera de ese rango los días quedan deshabilitados. */
  min?: string;
  max?: string;
  /** Tamaño: `md` (Figma, por defecto) o `sm` (compacto). */
  size?: CalendarSize;
  className?: string;
};

// Figma > Componentes > Picker Cell (State = Default, Hover, Selected, Disabled · Today)
const Celda = ({
  dia,
  seleccionado,
  hoy,
  fueraDeMes,
  deshabilitado,
  tamano,
  onClick,
}: {
  dia: Dayjs;
  seleccionado: boolean;
  hoy: boolean;
  fueraDeMes: boolean;
  deshabilitado: boolean;
  tamano: string;
  onClick: () => void;
}) => (
  <button
    type="button"
    onClick={onClick}
    disabled={deshabilitado}
    aria-pressed={seleccionado}
    aria-label={dia.format('D/MM/YYYY')}
    className={cn(
      'inline-flex items-center justify-center rounded-full text-label-3 outline-none transition-colors',
      tamano,
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
  tamano,
  onClick,
  children,
}: {
  activa: boolean;
  deshabilitada?: boolean;
  tamano: string;
  onClick: () => void;
  children: React.ReactNode;
}) => (
  <button
    type="button"
    onClick={onClick}
    disabled={deshabilitada}
    aria-pressed={activa}
    className={cn(
      'rounded-lg text-center text-label-2 outline-none transition-colors focus-visible:ring-2 focus-visible:ring-stroke-focus',
      tamano,
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
  tamano,
  onClick,
  children,
}: {
  etiqueta: string;
  tamano: string;
  onClick: () => void;
  children: React.ReactNode;
}) => (
  <button
    type="button"
    onClick={onClick}
    aria-label={etiqueta}
    className={cn(
      'inline-flex rounded-lg text-content-accent-primary outline-none hover:bg-surface-accent-light focus-visible:ring-2 focus-visible:ring-stroke-focus',
      tamano,
    )}
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
  size = 'md',
  className,
}: CalendarProps) => {
  const t = TAMANOS[size];
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
        ?.scrollIntoView?.({ block: 'center' });
    }
  }, [vista]);

  return (
    <div
      className={cn(
        // Card-shadow de Figma.
        'flex w-fit flex-col items-center rounded-lg border-b border-stroke-primary bg-surface-primary shadow-card-elevation',
        t.caja,
        className,
      )}
    >
      {vista === 'dias' && (
        <>
          <div className="flex w-full items-center gap-2">
            <BotonFlecha
              etiqueta="Mes anterior"
              tamano={t.flecha}
              onClick={() => setMes(mes.subtract(1, 'month'))}
            >
              <ChevronLeft className={t.iconoFlecha} aria-hidden="true" />
            </BotonFlecha>
            <button
              type="button"
              onClick={() => setVista('meses')}
              className={cn(
                'mx-auto inline-flex items-center gap-3 rounded-lg p-2 text-content-secondary outline-none hover:bg-surface-accent-light focus-visible:ring-2 focus-visible:ring-stroke-focus',
                t.titulo,
              )}
            >
              {MESES[mes.month()]} {mes.year()}
              <ChevronDown className={t.iconoTitulo} aria-hidden="true" />
            </button>
            <BotonFlecha
              etiqueta="Mes siguiente"
              tamano={t.flecha}
              onClick={() => setMes(mes.add(1, 'month'))}
            >
              <ChevronRight className={t.iconoFlecha} aria-hidden="true" />
            </BotonFlecha>
          </div>
          <div className={cn('grid grid-cols-7', t.cuadricula)}>
            {DIAS.map((d) => (
              <span
                key={d}
                className={cn(
                  'inline-flex items-center justify-center text-label-3 font-medium text-content-primary',
                  t.celda,
                )}
              >
                {d}
              </span>
            ))}
            {semanas.flat().map((d) => (
              <Celda
                key={d.format(FORMATO)}
                dia={d}
                tamano={t.celda}
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
            className={cn(
              'inline-flex items-center gap-3 self-start rounded-lg p-2 text-content-secondary outline-none hover:bg-surface-accent-light focus-visible:ring-2 focus-visible:ring-stroke-focus',
              t.titulo,
            )}
          >
            {mes.year()}
            <ChevronDown className={t.iconoTitulo} aria-hidden="true" />
          </button>
          <div className={cn('grid grid-cols-3', t.cuadricula)}>
            {MESES_CORTOS.map((m, i) => (
              <Opcion
                key={m}
                tamano={t.opcion}
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
          className={cn(
            'flex flex-col items-center gap-2 overflow-y-auto pr-2 [scrollbar-color:theme(colors.slate.700)_theme(colors.slate.200)] [scrollbar-width:thin]',
            t.anios,
          )}
        >
          {anios.map((a) => (
            <Opcion
              key={a}
              tamano={t.opcion}
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
