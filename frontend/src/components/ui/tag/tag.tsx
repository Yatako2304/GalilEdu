import { cva, type VariantProps } from 'class-variance-authority';
import * as React from 'react';

import { cn } from '@/utils/cn';

// Figma > Componentes > Informative Tag
const tagVariants = cva(
  'inline-flex items-center gap-1 whitespace-nowrap rounded font-medium',
  {
    variants: {
      color: {
        primary: 'bg-surface-accent-light text-content-accent-primary',
        info: 'bg-vibrant-blue-50 text-vibrant-blue-500',
        success: 'bg-surface-success-light text-content-success',
        warning: 'bg-surface-warning-light text-content-warning',
        error: 'bg-surface-error-light text-content-error',
        neutral: 'bg-surface-tertiary text-content-quartiary',
      },
      size: {
        lg: 'px-2.5 py-1.5 text-body-4',
        md: 'px-2 py-1 text-caption-1',
        sm: 'px-1.5 py-1 text-caption-2',
      },
    },
    defaultVariants: {
      color: 'primary',
      size: 'lg',
    },
  },
);

export type TagProps = Omit<React.HTMLAttributes<HTMLSpanElement>, 'color'> &
  VariantProps<typeof tagVariants> & {
    icon?: React.ReactNode;
  };

export const Tag = ({
  className,
  color,
  size,
  icon,
  children,
  ...props
}: TagProps) => (
  <span className={cn(tagVariants({ color, size }), className)} {...props}>
    {icon}
    {children}
  </span>
);

// Figma > Componentes > Status (punto + texto)
type StatusProps = {
  active: boolean;
  activeLabel?: string;
  inactiveLabel?: string;
};

export const Status = ({
  active,
  activeLabel = 'Activo',
  inactiveLabel = 'Inactivo',
}: StatusProps) => (
  <span
    className={cn(
      'inline-flex items-center gap-2 text-label-3 font-medium',
      active ? 'text-content-success' : 'text-content-quartiary',
    )}
  >
    <span
      className={cn(
        'size-2 rounded-full',
        active ? 'bg-growth-green-500' : 'bg-slate-400',
      )}
      aria-hidden="true"
    />
    {active ? activeLabel : inactiveLabel}
  </span>
);
