import { cn } from '@/utils/cn';

import { NivelLogro, SIGNIFICADO_NIVEL } from '../tag';

/** Cuántas calificaciones hay en cada nivel de logro. */
export type ConteoNiveles = { ad: number; a: number; b: number; c: number };

/** Los niveles de mayor a menor, como se leen en un reporte de logros. */
export const NIVELES_REPORTE: {
  nivel: NivelLogro;
  clave: keyof ConteoNiveles;
}[] = [
  { nivel: 'AD', clave: 'ad' },
  { nivel: 'A', clave: 'a' },
  { nivel: 'B', clave: 'b' },
  { nivel: 'C', clave: 'c' },
];

/** Colores de los niveles de Figma CI-23 (AD turquesa, A verde, B ámbar, C rojo). */
export const COLOR_BARRA_NIVEL: Record<NivelLogro, string> = {
  AD: 'bg-turquoise-600',
  A: 'bg-growth-green-400',
  B: 'bg-warm-yellow-200',
  C: 'bg-red-500',
};

/** Porcentaje entero de una parte (0,5 sube); null si no hay base. */
export const porcentajeDe = (parte: number, total: number) =>
  total === 0 ? null : Math.round((parte * 100) / total);

export type BarraDistribucionProps = {
  conteo: ConteoNiveles;
  size?: 'sm' | 'md';
  className?: string;
};

const altos = { sm: 'h-2.5', md: 'h-3.5' };

// Barra apilada con la proporción de calificaciones en cada nivel de logro (AD, A, B, C), con los colores de la
// escala de Figma CI-23. No está en el catálogo de Figma: se creó para el reporte institucional de notas (HU-64).
// Sin calificaciones queda la pista gris.
export const BarraDistribucion = ({
  conteo,
  size = 'sm',
  className,
}: BarraDistribucionProps) => {
  const total = conteo.ad + conteo.a + conteo.b + conteo.c;
  const descripcion =
    total === 0
      ? 'Sin calificaciones'
      : NIVELES_REPORTE.map(
          ({ nivel, clave }) =>
            `${nivel}: ${porcentajeDe(conteo[clave], total)} % (${conteo[clave]})`,
        ).join(', ');

  return (
    <div
      role="img"
      aria-label={descripcion}
      title={descripcion}
      className={cn(
        'flex w-full gap-px overflow-hidden rounded bg-surface-tertiary',
        altos[size],
        className,
      )}
    >
      {total > 0 &&
        NIVELES_REPORTE.filter(({ clave }) => conteo[clave] > 0).map(
          ({ nivel, clave }) => (
            <div
              key={nivel}
              className={COLOR_BARRA_NIVEL[nivel]}
              style={{ flexGrow: conteo[clave], flexBasis: 0 }}
            />
          ),
        )}
    </div>
  );
};

/** Leyenda de los colores de la barra: cada nivel con su significado. */
export const LeyendaNiveles = ({ className }: { className?: string }) => (
  <ul
    aria-label="Niveles de logro"
    className={cn(
      'flex flex-wrap gap-x-4 gap-y-1 text-caption-1 text-content-tertiary',
      className,
    )}
  >
    {NIVELES_REPORTE.map(({ nivel }) => (
      <li key={nivel} className="flex items-center gap-1.5">
        <span
          aria-hidden="true"
          className={cn('size-2.5 rounded-sm', COLOR_BARRA_NIVEL[nivel])}
        />
        {nivel} · {SIGNIFICADO_NIVEL[nivel]}
      </li>
    ))}
  </ul>
);
