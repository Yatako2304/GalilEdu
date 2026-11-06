import { cva } from 'class-variance-authority';
import {
  CircleCheck,
  ClipboardList,
  Info,
  OctagonAlert,
  TriangleAlert,
  X,
} from 'lucide-react';
import * as React from 'react';

import { cn } from '@/utils/cn';

export type AlertType = 'default' | 'success' | 'warning' | 'error' | 'info';

// Figma > Componentes > Alert y Toast (Type = Default, Success, Warning, Error, Info).
// El Toast es el mismo Alert con la X para cerrarlo (`onClose`).
const alertVariants = cva('flex items-center rounded-lg', {
  variants: {
    // `sm` es el aviso compacto para espacios chicos (un modal): menos relleno, borde fino y letra pequeña.
    size: {
      md: 'gap-2 border-2 px-0.5 py-3',
      sm: 'gap-1 border px-0.5 py-1.5',
    },
    type: {
      default: 'border-stroke-primary bg-surface-secondary',
      success: 'border-stroke-success-secondary bg-surface-success-light',
      warning: 'border-stroke-warning-secondary bg-surface-warning-light',
      error: 'border-stroke-error-secondary bg-surface-error-light',
      info: 'border-stroke-accent-secondary bg-vibrant-blue-50',
    },
  },
  defaultVariants: { type: 'info', size: 'md' },
});

const iconos: Record<AlertType, { Icono: typeof Info; color: string }> = {
  default: { Icono: Info, color: 'text-content-quartiary' },
  success: { Icono: CircleCheck, color: 'text-growth-green-600' },
  warning: { Icono: TriangleAlert, color: 'text-content-warning' },
  error: { Icono: OctagonAlert, color: 'text-surface-error' },
  info: { Icono: ClipboardList, color: 'text-vibrant-blue-500' },
};

export type AlertProps = Omit<React.HTMLAttributes<HTMLDivElement>, 'title'> & {
  type?: AlertType;
  /** `sm`: aviso compacto (menos relleno, ícono y letra pequeños) para modales y otros espacios chicos. */
  size?: 'md' | 'sm';
  /** Texto principal (Figma: Title). */
  title: React.ReactNode;
  /** Texto secundario opcional (Figma: Detail). */
  children?: React.ReactNode;
  /** Acción opcional a la derecha (por ejemplo, un botón "Pagar deuda"). */
  action?: React.ReactNode;
  /** Muestra la X para cerrarlo (Figma: Toast). */
  onClose?: () => void;
};

export const Alert = ({
  type = 'info',
  size = 'md',
  title,
  children,
  action,
  onClose,
  className,
  ...props
}: AlertProps) => {
  const compacto = size === 'sm';
  const { Icono, color } = iconos[type];
  return (
    <div
      role={type === 'error' || type === 'warning' ? 'alert' : 'status'}
      className={cn(alertVariants({ type, size }), className)}
      {...props}
    >
      <span
        className={cn('flex items-center', compacto ? 'px-1.5' : 'px-2 py-1')}
        aria-hidden="true"
      >
        <Icono className={cn(compacto ? 'size-4' : 'size-6', color)} />
      </span>
      <div className="flex min-w-0 flex-1 flex-col gap-0.5">
        <p
          className={cn(
            'text-content-primary',
            compacto ? 'text-label-3' : 'text-body-1',
          )}
        >
          {title}
        </p>
        {children && (
          <div
            className={cn(
              'text-content-tertiary',
              compacto ? 'text-caption-1' : 'text-body-3',
            )}
          >
            {children}
          </div>
        )}
      </div>
      {action && (
        <div
          className={cn('shrink-0', !onClose && (compacto ? 'pr-2' : 'pr-3'))}
        >
          {action}
        </div>
      )}
      {onClose && (
        <button
          type="button"
          onClick={onClose}
          aria-label="Cerrar"
          className="mr-1 inline-flex shrink-0 rounded-xl p-2 text-content-tertiary outline-none hover:bg-black/5 focus-visible:ring-2 focus-visible:ring-stroke-focus"
        >
          <X className="size-4" aria-hidden="true" />
        </button>
      )}
    </div>
  );
};
