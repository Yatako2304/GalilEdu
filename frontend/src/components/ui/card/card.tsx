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

// `h6` (20 px, regular) es el título de CardHeader de Figma. `body-1` es Body/Body-1/Medium (18 px, medium): el título
// que dibujan las tarjetas de «HU-45: Asignación de horarios» («Datos del Horario», «Matriz horaria»).
const TAMANOS_TITULO = {
  h6: 'text-h6 font-normal',
  'body-1': 'text-body-1 font-medium',
} as const;

const CardTitle = React.forwardRef<
  HTMLHeadingElement,
  React.HTMLAttributes<HTMLHeadingElement> & {
    size?: keyof typeof TAMANOS_TITULO;
  }
>(({ className, children, size = 'h6', ...props }, ref) => (
  <h2
    ref={ref}
    className={cn(TAMANOS_TITULO[size], 'text-content-primary', className)}
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
