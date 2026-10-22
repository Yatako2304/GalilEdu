import * as React from 'react';

import { cn } from '@/utils/cn';

import { Checkbox, Radio } from './checkbox';

type OptionCardProps = Omit<
  React.InputHTMLAttributes<HTMLInputElement>,
  'type'
> & {
  label: React.ReactNode;
  /** Texto de ayuda bajo la etiqueta (Figma: Helper text). */
  helperText?: React.ReactNode;
  /** Ícono a la izquierda (Figma: With Icon = true). Con ícono, el control va a la derecha. */
  icon?: React.ReactNode;
  /** Lado del control (Figma: Location = left, right). */
  controlPosition?: 'left' | 'right';
};

// Clases comunes de Radio Card y Checkbox Card: el borde se oscurece cuando el control está marcado.
const tarjeta = (disabled?: boolean) =>
  cn(
    'flex w-full items-start gap-2 rounded-lg border p-4 transition-colors',
    disabled
      ? 'cursor-not-allowed border-transparent bg-surface-disabled'
      : 'cursor-pointer border-stroke-secondary bg-surface-primary hover:bg-surface-primary-hover has-[:checked]:border-stroke-accent-primary has-[:checked]:bg-surface-primary',
  );

const crear = (Control: typeof Radio, nombre: string) => {
  const Tarjeta = React.forwardRef<HTMLInputElement, OptionCardProps>(
    (
      {
        label,
        helperText,
        icon,
        controlPosition = icon ? 'right' : 'left',
        className,
        disabled,
        ...props
      },
      ref,
    ) => {
      const control = (
        <span className="flex h-[22px] items-center">
          <Control ref={ref} disabled={disabled} {...props} />
        </span>
      );
      return (
        <label className={cn(tarjeta(disabled), className)}>
          {icon && (
            <span
              className={cn(
                'flex h-[22px] items-center [&_svg]:size-5',
                disabled ? 'text-content-disabled' : 'text-content-tertiary',
              )}
              aria-hidden="true"
            >
              {icon}
            </span>
          )}
          {controlPosition === 'left' && control}
          <span className="flex min-w-0 flex-1 flex-col">
            <span
              className={cn(
                'text-label-1',
                disabled ? 'text-content-disabled' : 'text-content-primary',
              )}
            >
              {label}
            </span>
            {helperText && (
              <span
                className={cn(
                  'text-label-3',
                  disabled ? 'text-content-disabled' : 'text-content-tertiary',
                )}
              >
                {helperText}
              </span>
            )}
          </span>
          {controlPosition === 'right' && control}
        </label>
      );
    },
  );
  Tarjeta.displayName = nombre;
  return Tarjeta;
};

// Figma > Componentes > Radio Card (Location · State = Default, Hover, Active, Disabled · With Icon)
export const RadioCard = crear(Radio, 'RadioCard');

// Figma > Componentes > Checkbox Card (Location · State = Default, Hover, Active, Disabled · With Icon)
export const CheckboxCard = crear(Checkbox, 'CheckboxCard');

export type { OptionCardProps as RadioCardProps };
export type { OptionCardProps as CheckboxCardProps };
