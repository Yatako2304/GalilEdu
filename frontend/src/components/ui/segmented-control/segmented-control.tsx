import { cva } from 'class-variance-authority';

import { cn } from '@/utils/cn';

// Figma > Tutor > AS-01 a AS-06 · "Selector de asistencia" (Presente | Tardanza | Falta). No está en Componentes:
// se dibuja en la pantalla. Opción elegida con fondo sólido de su color; sin elegir, fondo blanco.
const opcionVariants = cva(
  'h-[34px] min-w-24 px-4 text-body-4 font-medium transition-colors focus-visible:relative focus-visible:z-10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus disabled:cursor-not-allowed',
  {
    variants: {
      color: {
        success: '',
        warning: '',
        error: '',
        accent: '',
      },
      seleccionada: {
        true: 'text-white',
        false: 'bg-white text-content-secondary hover:bg-slate-50',
      },
    },
    compoundVariants: [
      { color: 'success', seleccionada: true, className: 'bg-surface-success' },
      { color: 'warning', seleccionada: true, className: 'bg-surface-warning' },
      { color: 'error', seleccionada: true, className: 'bg-surface-error' },
      {
        color: 'accent',
        seleccionada: true,
        className: 'bg-surface-accent-primary',
      },
    ],
    defaultVariants: { color: 'accent', seleccionada: false },
  },
);

export type OpcionSegmentada<T extends string> = {
  value: T;
  label: string;
  color?: 'success' | 'warning' | 'error' | 'accent';
};

export type SegmentedControlProps<T extends string> = {
  /** Nombre accesible del grupo, por ejemplo "Asistencia de Valeria Acevedo". */
  label: string;
  options: OpcionSegmentada<T>[];
  value: T | null;
  onChange: (value: T) => void;
  /** Borde rojo (AS-02: estudiante sin marcar al intentar guardar). */
  invalid?: boolean;
  disabled?: boolean;
  className?: string;
};

export const SegmentedControl = <T extends string>({
  label,
  options,
  value,
  onChange,
  invalid,
  disabled,
  className,
}: SegmentedControlProps<T>) => (
  <div
    role="radiogroup"
    aria-label={label}
    aria-invalid={invalid || undefined}
    className={cn(
      'inline-flex overflow-hidden rounded-lg border',
      invalid ? 'border-stroke-error' : 'border-stroke-tertiary',
      className,
    )}
  >
    {options.map((opcion) => {
      const seleccionada = opcion.value === value;
      return (
        <button
          key={opcion.value}
          type="button"
          role="radio"
          aria-checked={seleccionada}
          disabled={disabled}
          onClick={() => onChange(opcion.value)}
          className={opcionVariants({
            color: opcion.color ?? 'accent',
            seleccionada,
          })}
        >
          {opcion.label}
        </button>
      );
    })}
  </div>
);
