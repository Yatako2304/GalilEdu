import * as React from 'react';

import { cn } from '@/utils/cn';

export type TooltipSide = 'top' | 'bottom' | 'left' | 'right';
export type TooltipAlign = 'start' | 'center' | 'end';

export type TooltipProps = {
  /** Texto corto (Figma: Size = Small) o título de la versión grande. */
  content: React.ReactNode;
  /** Texto de apoyo: si se pasa, se usa la versión grande (Figma: Size = Large, 224px). */
  description?: React.ReactNode;
  /** Lado donde aparece el globo respecto del elemento. */
  side?: TooltipSide;
  /** Posición de la flecha (Figma: Pointer = left / center / right). */
  align?: TooltipAlign;
  /** Elemento que muestra el globo al pasar el mouse o enfocarlo. */
  children: React.ReactElement;
  className?: string;
};

const posicion: Record<TooltipSide, Record<TooltipAlign, string>> = {
  top: {
    start: 'bottom-full left-0 mb-2',
    center: 'bottom-full left-1/2 mb-2 -translate-x-1/2',
    end: 'bottom-full right-0 mb-2',
  },
  bottom: {
    start: 'top-full left-0 mt-2',
    center: 'top-full left-1/2 mt-2 -translate-x-1/2',
    end: 'top-full right-0 mt-2',
  },
  left: {
    start: 'right-full top-0 mr-2',
    center: 'right-full top-1/2 mr-2 -translate-y-1/2',
    end: 'right-full bottom-0 mr-2',
  },
  right: {
    start: 'left-full top-0 ml-2',
    center: 'left-full top-1/2 ml-2 -translate-y-1/2',
    end: 'left-full bottom-0 ml-2',
  },
};

// La flecha es un cuadrado girado con el mismo borde del globo.
const flecha: Record<TooltipSide, Record<TooltipAlign, string>> = {
  top: {
    start: '-bottom-[5px] left-3 border-b border-r',
    center: '-bottom-[5px] left-1/2 -translate-x-1/2 border-b border-r',
    end: '-bottom-[5px] right-3 border-b border-r',
  },
  bottom: {
    start: '-top-[5px] left-3 border-l border-t',
    center: '-top-[5px] left-1/2 -translate-x-1/2 border-l border-t',
    end: '-top-[5px] right-3 border-l border-t',
  },
  left: {
    start: '-right-[5px] top-3 border-r border-t',
    center: '-right-[5px] top-1/2 -translate-y-1/2 border-r border-t',
    end: '-right-[5px] bottom-3 border-r border-t',
  },
  right: {
    start: '-left-[5px] top-3 border-b border-l',
    center: '-left-[5px] top-1/2 -translate-y-1/2 border-b border-l',
    end: '-left-[5px] bottom-3 border-b border-l',
  },
};

// Figma > Componentes > Tooltip (Pointer = 9 posiciones · Size = Small, Large)
export const Tooltip = ({
  content,
  description,
  side = 'top',
  align = 'center',
  children,
  className,
}: TooltipProps) => {
  const id = React.useId();
  const grande = description !== undefined;

  return (
    <span className="group/tooltip relative inline-flex">
      {React.cloneElement(children, { 'aria-describedby': id })}
      <span
        id={id}
        role="tooltip"
        className={cn(
          'pointer-events-none invisible absolute z-50 flex flex-col gap-2 rounded-lg border border-stroke-secondary bg-surface-primary p-3 opacity-0 shadow-card transition-opacity',
          'group-focus-within/tooltip:visible group-focus-within/tooltip:opacity-100 group-hover/tooltip:visible group-hover/tooltip:opacity-100',
          grande ? 'w-56' : 'whitespace-nowrap',
          posicion[side][align],
          className,
        )}
      >
        <span
          aria-hidden="true"
          className={cn(
            'absolute size-2.5 rotate-45 border-stroke-secondary bg-surface-primary',
            flecha[side][align],
          )}
        />
        <span className="text-body-3 text-content-primary">{content}</span>
        {grande && (
          <span className="text-body-4 text-content-tertiary">
            {description}
          </span>
        )}
      </span>
    </span>
  );
};
