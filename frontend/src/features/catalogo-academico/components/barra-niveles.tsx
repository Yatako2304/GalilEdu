import { NivelLogro } from '@/types/api';
import { cn } from '@/utils/cn';

import { NIVELES_DE_MENOR_A_MAYOR } from './esquemas';

/** Colores de los niveles en Figma CI-23 (C rojo, B ámbar, A verde, AD turquesa). */
export const COLOR_NIVEL: Record<NivelLogro, string> = {
  C: 'bg-red-500 text-white',
  B: 'bg-warm-yellow-200 text-white',
  A: 'bg-growth-green-400 text-white',
  AD: 'bg-turquoise-600 text-white',
};

export const InsigniaNivel = ({ nivel }: { nivel: NivelLogro }) => (
  <span
    className={cn(
      'inline-flex h-6 min-w-8 items-center justify-center rounded px-1.5 text-caption-1 font-semibold',
      COLOR_NIVEL[nivel],
    )}
  >
    {nivel}
  </span>
);

type Rango = { nivel: NivelLogro; notaDesde: number; notaHasta: number };

// Barra de 0 a 20 con un tramo por nivel (Figma CI-23 y "Vista previa" del modal Nueva versión).
export const BarraNiveles = ({ niveles }: { niveles: Rango[] }) => {
  const tramos = NIVELES_DE_MENOR_A_MAYOR.map((codigo) =>
    niveles.find((n) => n.nivel === codigo),
  ).filter((n): n is Rango => !!n);

  return (
    <div>
      <div
        className="flex h-11 overflow-hidden rounded-lg"
        role="img"
        aria-label={tramos
          .map((t) => `${t.nivel} de ${t.notaDesde} a ${t.notaHasta}`)
          .join(', ')}
      >
        {tramos.map((t) => {
          const ancho = Math.max(0, t.notaHasta - t.notaDesde + 1);
          return (
            <div
              key={t.nivel}
              style={{ flexGrow: ancho, flexBasis: 0 }}
              className={cn(
                'flex min-w-0 flex-col items-center justify-center overflow-hidden text-caption-2',
                COLOR_NIVEL[t.nivel],
              )}
            >
              <span className="font-semibold">{t.nivel}</span>
              <span>
                {t.notaDesde}–{t.notaHasta}
              </span>
            </div>
          );
        })}
      </div>
      <div
        className="mt-1 flex justify-between text-caption-2 text-content-quartiary"
        aria-hidden="true"
      >
        <span>0</span>
        <span>10</span>
        <span>20</span>
      </div>
    </div>
  );
};
