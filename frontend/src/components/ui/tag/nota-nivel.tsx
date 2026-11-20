import { cn } from '@/utils/cn';

export type NivelLogro = 'AD' | 'A' | 'B' | 'C';

/** Significado de cada nivel en la evaluación por competencias. No hay «aprobado» ni «desaprobado». */
export const SIGNIFICADO_NIVEL: Record<NivelLogro, string> = {
  AD: 'Logro destacado',
  A: 'Logro esperado',
  B: 'En proceso',
  C: 'En inicio',
};

const colores: Record<NivelLogro, string> = {
  AD: 'bg-surface-success text-white',
  A: 'bg-surface-success-light text-content-success',
  B: 'bg-surface-warning-light text-content-warning',
  C: 'bg-surface-error-light text-content-error',
};

const tamanos = {
  md: 'h-6 min-w-8 px-2 text-caption-1',
  sm: 'h-5 min-w-6 px-1.5 text-caption-2',
};

export type NotaNivelProps = {
  /** null: todavía sin nota (se muestra «—» en gris). */
  nivel: NivelLogro | null;
  size?: keyof typeof tamanos;
  className?: string;
};

// Figma > Apoderado > «❖ Nota (calificación)»: la letra del nivel de logro en una pastilla de su color
// (AD verde intenso, A verde, B ámbar, C rojo). Lleva su significado como nombre accesible.
export const NotaNivel = ({
  nivel,
  size = 'md',
  className,
}: NotaNivelProps) => (
  <span
    role="img"
    aria-label={nivel ? `${nivel} · ${SIGNIFICADO_NIVEL[nivel]}` : 'Sin nota'}
    title={nivel ? SIGNIFICADO_NIVEL[nivel] : 'Sin nota'}
    className={cn(
      'inline-flex items-center justify-center rounded-full font-semibold',
      tamanos[size],
      nivel ? colores[nivel] : 'bg-surface-tertiary text-content-quartiary',
      className,
    )}
  >
    {nivel ?? '—'}
  </span>
);
