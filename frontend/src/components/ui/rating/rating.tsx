import { Star } from 'lucide-react';

import { cn } from '@/utils/cn';

export type RatingProps = {
  /** Estrellas marcadas (0 a `max`). */
  value: number;
  /** Si se pasa, cada estrella es un botón para calificar. */
  onChange?: (value: number) => void;
  max?: number;
  size?: 'sm' | 'lg';
  /** Nombre accesible, ej. "Calificación del curso". */
  label?: string;
  className?: string;
};

// Figma > Componentes > Rating (N° Stars = 0 a 5 · Size = small 20px, large 36px)
export const Rating = ({
  value,
  onChange,
  max = 5,
  size = 'sm',
  label = 'Calificación',
  className,
}: RatingProps) => {
  const estrella = (i: number) => (
    <Star
      className={cn(
        size === 'lg' ? 'size-9' : 'size-5',
        i <= value
          ? 'fill-amber-400 text-amber-400'
          : 'fill-slate-400 text-slate-400',
      )}
      aria-hidden="true"
    />
  );
  const posiciones = Array.from({ length: max }, (_, i) => i + 1);

  if (!onChange) {
    return (
      <span
        role="img"
        aria-label={`${label}: ${value} de ${max}`}
        className={cn('inline-flex items-center', className)}
      >
        {posiciones.map((i) => (
          <span key={i}>{estrella(i)}</span>
        ))}
      </span>
    );
  }

  return (
    <span
      role="radiogroup"
      aria-label={label}
      className={cn('inline-flex items-center', className)}
    >
      {posiciones.map((i) => (
        <button
          key={i}
          type="button"
          role="radio"
          aria-checked={i === value}
          aria-label={`${i} de ${max}`}
          onClick={() => onChange(i)}
          className="rounded-md outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus"
        >
          {estrella(i)}
        </button>
      ))}
    </span>
  );
};
