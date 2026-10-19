import { Slot } from '@radix-ui/react-slot';
import { cva, type VariantProps } from 'class-variance-authority';
import * as React from 'react';

import { cn } from '@/utils/cn';

import { Spinner } from '../spinner';

// Figma > Componentes > Button
// Type:  Primary | Secondary | Primary-Light | Tertiary | Error | Error-Light
// Size:  Large (46px) | Medium (44px) | Small (32px) · radius 12px
const buttonVariants = cva(
  'inline-flex items-center justify-center gap-1 whitespace-nowrap rounded-[12px] font-normal transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus focus-visible:ring-offset-2 disabled:pointer-events-none disabled:border-transparent disabled:bg-surface-disabled disabled:text-content-disabled',
  {
    variants: {
      variant: {
        primary:
          'bg-surface-accent-primary text-white hover:bg-deep-blue-950 active:bg-deep-blue-950',
        secondary:
          'bg-surface-accent-secondary text-white hover:bg-vibrant-blue-600 active:bg-vibrant-blue-700',
        'primary-light':
          'border border-stroke-primary bg-white text-content-primary hover:bg-slate-50 active:bg-slate-200',
        tertiary:
          'border border-stroke-tertiary bg-surface-tertiary text-content-tertiary hover:bg-slate-200 active:bg-slate-300',
        error: 'bg-surface-error text-white hover:bg-red-600 active:bg-red-700',
        'error-light':
          'border border-stroke-error-secondary bg-surface-error-light text-red-400 hover:bg-red-100 active:bg-red-200',
        ghost: 'text-content-secondary hover:bg-slate-100',
        link: 'h-auto px-0 text-content-accent-secondary underline-offset-4 hover:underline',
        // Alias para los componentes heredados de bulletproof-react
        default:
          'bg-surface-accent-primary text-white hover:bg-deep-blue-950 active:bg-deep-blue-950',
        outline:
          'border border-stroke-primary bg-white text-content-primary hover:bg-slate-50 active:bg-slate-200',
        destructive:
          'bg-surface-error text-white hover:bg-red-600 active:bg-red-700',
      },
      size: {
        lg: 'h-[46px] px-6 text-body-2',
        md: 'h-11 px-4 text-body-3',
        sm: 'h-8 px-3 text-body-4',
        icon: 'size-10',
        default: 'h-11 px-4 text-body-3',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'md',
    },
  },
);

export type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> &
  VariantProps<typeof buttonVariants> & {
    asChild?: boolean;
    isLoading?: boolean;
    icon?: React.ReactNode;
    iconPosition?: 'left' | 'right';
  };

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      variant,
      size,
      asChild = false,
      children,
      isLoading,
      icon,
      iconPosition = 'left',
      ...props
    },
    ref,
  ) => {
    const Comp = asChild ? Slot : 'button';

    if (asChild) {
      return (
        <Comp
          className={cn(buttonVariants({ variant, size, className }))}
          ref={ref}
          {...props}
        >
          {children}
        </Comp>
      );
    }

    return (
      <Comp
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      >
        {isLoading && <Spinner size="sm" className="text-current" />}
        {!isLoading && icon && iconPosition === 'left' && icon}
        {children && <span className="px-1">{children}</span>}
        {!isLoading && icon && iconPosition === 'right' && icon}
      </Comp>
    );
  },
);
Button.displayName = 'Button';

export { Button, buttonVariants };
