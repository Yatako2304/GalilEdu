import { cva, type VariantProps } from 'class-variance-authority';
import * as React from 'react';

import { cn } from '@/utils/cn';

// Figma > Componentes > Stat Card > IconBadge (cuadrado) y el círculo con ícono de los modales y estados vacíos.
const iconBadgeVariants = cva(
  'inline-flex shrink-0 items-center justify-center',
  {
    variants: {
      color: {
        accent: 'bg-surface-accent-light text-content-accent-primary',
        'accent-secondary':
          'bg-surface-accent-light text-content-accent-secondary',
        success: 'bg-surface-success-light text-content-success',
        turquoise: 'bg-turquoise-50 text-turquoise-600',
        warning: 'bg-surface-warning-light text-content-warning',
        error: 'bg-surface-error-light text-content-error',
        neutral: 'bg-surface-tertiary text-content-quartiary',
      },
      shape: {
        circle: 'rounded-full',
        square: 'rounded-lg',
      },
      size: {
        sm: 'size-6 text-caption-1 font-semibold [&_svg]:size-3.5',
        md: 'size-10 [&_svg]:size-5',
        lg: 'size-12 [&_svg]:size-6',
        xl: 'size-16 [&_svg]:size-8',
      },
    },
    defaultVariants: {
      color: 'accent',
      shape: 'circle',
      size: 'md',
    },
  },
);

export type IconBadgeProps = Omit<
  React.HTMLAttributes<HTMLSpanElement>,
  'color'
> &
  VariantProps<typeof iconBadgeVariants>;

/** Ícono (o número corto) dentro de un fondo de color. El contenido es decorativo salvo que se pase texto. */
export const IconBadge = ({
  className,
  color,
  shape,
  size,
  children,
  ...props
}: IconBadgeProps) => (
  <span
    className={cn(iconBadgeVariants({ color, shape, size }), className)}
    {...props}
  >
    {children}
  </span>
);
