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
const alertVariants = cva(
  'flex items-center gap-2 rounded-lg border-2 px-0.5 py-3',
  {
    variants: {
      type: {
        default: 'border-stroke-primary bg-surface-secondary',
        success: 'border-stroke-success-secondary bg-surface-success-light',
        warning: 'border-stroke-warning-secondary bg-surface-warning-light',
        error: 'border-stroke-error-secondary bg-surface-error-light',
        info: 'border-stroke-accent-secondary bg-vibrant-blue-50',
      },
    },
    defaultVariants: { type: 'info' },
  },
);

const iconos: Record<AlertType, React.ReactNode> = {
  default: <Info className="size-6 text-content-quartiary" />,
  success: <CircleCheck className="size-6 text-growth-green-600" />,
  warning: <TriangleAlert className="size-6 text-content-warning" />,
  error: <OctagonAlert className="size-6 text-surface-error" />,
  info: <ClipboardList className="size-6 text-vibrant-blue-500" />,
};

export type AlertProps = Omit<React.HTMLAttributes<HTMLDivElement>, 'title'> & {
  type?: AlertType;
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
  title,
  children,
  action,
  onClose,
  className,
  ...props
}: AlertProps) => (
  <div
    role={type === 'error' || type === 'warning' ? 'alert' : 'status'}
    className={cn(alertVariants({ type }), className)}
    {...props}
  >
    <span className="flex items-center px-2 py-1" aria-hidden="true">
      {iconos[type]}
    </span>
    <div className="flex min-w-0 flex-1 flex-col gap-0.5">
      <p className="text-body-1 text-content-primary">{title}</p>
      {children && (
        <div className="text-body-3 text-content-tertiary">{children}</div>
      )}
    </div>
    {action && (
      <div className={cn('shrink-0', !onClose && 'pr-3')}>{action}</div>
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
