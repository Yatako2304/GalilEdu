import * as React from 'react';

import { cn } from '@/utils/cn';

export type Dato = {
  etiqueta: string;
  /** Vacío (null, undefined o '') se muestra como «—». */
  valor?: React.ReactNode;
  /** Ocupa toda la fila (direcciones y textos largos). */
  ancho?: boolean;
};

const COLUMNAS = {
  2: 'md:grid-cols-2',
  3: 'md:grid-cols-2 xl:grid-cols-3',
} as const;

/**
 * Datos de solo lectura en pares etiqueta / valor (lo que en un formulario serían campos). Mismo estilo que las fichas
 * de solo vista: etiqueta pequeña y gris arriba, valor debajo.
 */
export const ListaDatos = ({
  datos,
  titulo,
  columnas = 3,
  className,
}: {
  datos: Dato[];
  /** Subtítulo de la sección (por ejemplo, «Contacto»). */
  titulo?: string;
  columnas?: keyof typeof COLUMNAS;
  className?: string;
}) => (
  <section className={cn('space-y-3', className)}>
    {titulo && (
      <h3 className="text-label-3 uppercase tracking-wide text-content-tertiary">
        {titulo}
      </h3>
    )}
    <dl className={cn('grid grid-cols-1 gap-x-8 gap-y-4', COLUMNAS[columnas])}>
      {datos.map(({ etiqueta, valor, ancho }) => (
        <div
          key={etiqueta}
          className={cn('min-w-0', ancho && 'md:col-span-full')}
        >
          <dt className="text-body-4 text-content-quartiary">{etiqueta}</dt>
          <dd className="break-words text-body-3 text-content-primary">
            {valor === null || valor === undefined || valor === ''
              ? '—'
              : valor}
          </dd>
        </div>
      ))}
    </dl>
  </section>
);
