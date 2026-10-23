import { cva } from 'class-variance-authority';
import { X } from 'lucide-react';
import * as React from 'react';

import { cn } from '@/utils/cn';

export type SelectableTagColor =
  'primary' | 'info' | 'error' | 'success' | 'warning';

// Figma > Componentes > Selectable Tag (Color · State = Default, Hover, Selected, Disabled · Size · Type = Square, Circle)
const tagVariants = cva(
  'inline-flex items-center justify-center gap-1 whitespace-nowrap border font-medium outline-none transition-colors focus-visible:ring-2 focus-visible:ring-stroke-focus disabled:cursor-not-allowed disabled:border-transparent disabled:bg-surface-disabled disabled:text-content-disabled',
  {
    variants: {
      color: {
        primary: '',
        info: '',
        error: '',
        success: '',
        warning: '',
      },
      selected: {
        true: 'border-transparent text-white',
        false: 'bg-surface-primary',
      },
      size: {
        lg: 'px-3 py-2 text-label-2 [&_svg]:size-4',
        md: 'px-3 py-1.5 text-label-3 [&_svg]:size-4',
        sm: 'px-2 py-1 text-label-3 [&_svg]:size-3',
      },
      shape: {
        square: 'rounded',
        circle: 'rounded-full',
      },
    },
    compoundVariants: [
      {
        color: 'primary',
        selected: false,
        class:
          'border-stroke-accent-primary text-content-accent-primary hover:bg-surface-accent-light',
      },
      {
        color: 'info',
        selected: false,
        class:
          'border-stroke-accent-secondary text-content-accent-secondary hover:bg-vibrant-blue-50',
      },
      {
        color: 'error',
        selected: false,
        class:
          'border-stroke-error text-content-error hover:bg-surface-error-light',
      },
      {
        color: 'success',
        selected: false,
        class:
          'border-stroke-success text-content-success hover:bg-surface-success-light',
      },
      {
        color: 'warning',
        selected: false,
        class:
          'border-stroke-warning text-content-warning hover:bg-surface-warning-light',
      },
      { color: 'primary', selected: true, class: 'bg-surface-accent-primary' },
      { color: 'info', selected: true, class: 'bg-surface-accent-secondary' },
      { color: 'error', selected: true, class: 'bg-surface-error' },
      { color: 'success', selected: true, class: 'bg-surface-success' },
      { color: 'warning', selected: true, class: 'bg-surface-warning' },
    ],
    defaultVariants: {
      color: 'primary',
      selected: false,
      size: 'lg',
      shape: 'square',
    },
  },
);

// Relleno claro de la pastilla informativa (`readOnly`): es el que Figma dibuja en los estados de una tabla.
const RELLENO_INFORMATIVO: Record<SelectableTagColor, string> = {
  primary: 'bg-surface-accent-light',
  info: 'bg-vibrant-blue-50',
  error: 'bg-surface-error-light',
  success: 'bg-surface-success-light',
  warning: 'bg-surface-warning-light',
};

export type SelectableTagProps = Omit<
  React.ButtonHTMLAttributes<HTMLButtonElement>,
  'color'
> & {
  color?: SelectableTagColor;
  selected?: boolean;
  /**
   * Solo informativa, con la forma de pastilla de Figma (estado "Programado", "Abierto", "Cerrado" de una tabla).
   * Se dibuja como un `<span>`: no es un botón ni se puede marcar. Para el resto de las etiquetas informativas
   * usa `Tag`.
   */
  readOnly?: boolean;
  size?: 'lg' | 'md' | 'sm';
  shape?: 'square' | 'circle';
  /** Ícono a la izquierda (Figma: IconPosition = Left). */
  icon?: React.ReactNode;
  /** Muestra la X a la derecha y la llama al hacer clic en ella (Figma: IconPosition = Right). */
  onRemove?: () => void;
};

/**
 * Etiqueta que se puede marcar (filtros, selección múltiple). Para una etiqueta solo informativa usa `Tag`.
 * Con `onRemove` deja de ser un botón y muestra una X para quitarla (por ejemplo, dentro de `TagField`).
 */
export const SelectableTag = ({
  color,
  selected = false,
  readOnly = false,
  size,
  shape,
  icon,
  onRemove,
  className,
  children,
  ...props
}: SelectableTagProps) => {
  const clases = cn(
    tagVariants({ color, selected: readOnly ? false : selected, size, shape }),
    readOnly && RELLENO_INFORMATIVO[color ?? 'primary'],
    className,
  );
  const contenido = (
    <>
      {icon && <span aria-hidden="true">{icon}</span>}
      {children}
    </>
  );

  if (readOnly) {
    return <span className={clases}>{contenido}</span>;
  }

  if (onRemove) {
    return (
      <span className={clases}>
        {contenido}
        <button
          type="button"
          onClick={onRemove}
          disabled={props.disabled}
          aria-label={`Quitar ${typeof children === 'string' ? children : ''}`.trim()}
          className="inline-flex rounded-full outline-none hover:opacity-70 focus-visible:ring-2 focus-visible:ring-stroke-focus"
        >
          <X aria-hidden="true" />
        </button>
      </span>
    );
  }

  return (
    <button type="button" aria-pressed={selected} className={clases} {...props}>
      {contenido}
    </button>
  );
};
