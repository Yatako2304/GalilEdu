import * as React from 'react';

import { cn } from '@/utils/cn';

// Contenedor blanco usado en todas las pantallas (Figma: "Card - ...", "Config", "CTA").
const Card = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn(
      'rounded-xl border border-stroke-tertiary bg-white',
      className,
    )}
    {...props}
  />
));
Card.displayName = 'Card';

// Figma > Componentes > CardHeader (título 20px + divisor)
const CardHeader = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement> & { action?: React.ReactNode }
>(({ className, children, action, ...props }, ref) => (
  <div
    ref={ref}
    className={cn(
      'flex items-center justify-between gap-4 border-b border-stroke-tertiary px-5 py-4',
      className,
    )}
    {...props}
  >
    <div className="min-w-0">{children}</div>
    {action}
  </div>
));
CardHeader.displayName = 'CardHeader';

const CardTitle = React.forwardRef<
  HTMLHeadingElement,
  React.HTMLAttributes<HTMLHeadingElement>
>(({ className, children, ...props }, ref) => (
  <h2
    ref={ref}
    className={cn('text-h6 font-normal text-content-primary', className)}
    {...props}
  >
    {children}
  </h2>
));
CardTitle.displayName = 'CardTitle';

const CardDescription = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => (
  <p
    ref={ref}
    className={cn('mt-1 text-body-3 text-content-quartiary', className)}
    {...props}
  />
));
CardDescription.displayName = 'CardDescription';

const CardContent = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div ref={ref} className={cn('px-5 py-4', className)} {...props} />
));
CardContent.displayName = 'CardContent';

export { Card, CardHeader, CardTitle, CardDescription, CardContent };
