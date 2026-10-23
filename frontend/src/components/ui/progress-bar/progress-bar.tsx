import * as React from 'react';

import { cn } from '@/utils/cn';

export type ProgressBarProps = {
  /** Porcentaje de avance. Se limita a 0–100. */
  value: number;
  /** Nombre accesible (Figma: "Avance del año escolar"). */
  label: string;
  /** Muestra la etiqueta arriba a la izquierda y el porcentaje a la derecha. */
  showLabel?: boolean;
  className?: string;
};

// Figma > CI-01 · Año escolar > "Progreso": pista de 8 px y relleno azul, ambos con esquinas de 4 px.
export const ProgressBar = ({
  value,
  label,
  showLabel = true,
  className,
}: ProgressBarProps) => {
  const porcentaje = Math.round(Math.min(100, Math.max(0, value)));
  const etiquetaId = React.useId();

  return (
    <div className={cn('flex flex-col gap-1.5', className)}>
      {showLabel && (
        <div className="flex items-start justify-between">
          <p id={etiquetaId} className="text-body-4 text-content-tertiary">
            {label}
          </p>
          <p className="text-label-2 font-semibold text-content-primary">
            {porcentaje} %
          </p>
        </div>
      )}
      <div
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={porcentaje}
        aria-label={showLabel ? undefined : label}
        aria-labelledby={showLabel ? etiquetaId : undefined}
        className="h-2 w-full overflow-hidden rounded bg-surface-tertiary"
      >
        <div
          className="h-full rounded bg-surface-accent-secondary"
          style={{ width: `${porcentaje}%` }}
        />
      </div>
    </div>
  );
};
