import { Check } from 'lucide-react';
import * as React from 'react';

import { cn } from '@/utils/cn';

type ControlProps = Omit<
  React.InputHTMLAttributes<HTMLInputElement>,
  'type'
> & {
  /** Texto a la derecha (Figma: Checkbox Label / Radio Label). Si no se pasa, usar aria-label. */
  label?: React.ReactNode;
  /** Texto de ayuda debajo de la etiqueta (Figma: Helper text). */
  helperText?: React.ReactNode;
  /** Marca el control en rojo (Figma: State = Error). */
  invalid?: boolean;
};

const envolver = (
  control: React.ReactNode,
  { label, helperText, className, disabled }: ControlProps,
) =>
  label ? (
    <label
      className={cn(
        'inline-flex items-start gap-2',
        disabled ? 'cursor-not-allowed' : 'cursor-pointer',
        className,
      )}
    >
      <span className="flex h-5 items-center">{control}</span>
      <span className="flex flex-col">
        <span
          className={cn(
            'text-body-3',
            disabled ? 'text-content-disabled' : 'text-content-primary',
          )}
        >
          {label}
        </span>
        {helperText && (
          <span className="text-body-4 text-content-quartiary">
            {helperText}
          </span>
        )}
      </span>
    </label>
  ) : (
    control
  );

// Figma > Componentes > Checkbox y Checkbox Label (State = Default, Hover, Pressed, Disabled, Error)
export const Checkbox = React.forwardRef<HTMLInputElement, ControlProps>(
  (props, ref) => {
    const { label, helperText, invalid, className, ...input } = props;
    const control = (
      <span
        className={cn(
          'relative inline-flex size-4 shrink-0',
          !label && className,
        )}
      >
        <input
          ref={ref}
          type="checkbox"
          aria-invalid={invalid || undefined}
          className={cn(
            'peer size-4 cursor-[inherit] appearance-none rounded-md border-2 border-content-placeholder bg-surface-primary outline-none',
            'hover:border-content-tertiary checked:border-stroke-primary-solid checked:bg-stroke-primary-solid',
            'focus-visible:ring-2 focus-visible:ring-stroke-focus focus-visible:ring-offset-1',
            'disabled:border-stroke-primary disabled:checked:bg-stroke-primary',
            invalid &&
              'border-content-error checked:border-content-error checked:bg-content-error',
          )}
          {...input}
        />
        <Check
          className="pointer-events-none absolute inset-0 m-auto hidden size-3 stroke-[3] text-white peer-checked:block"
          aria-hidden="true"
        />
      </span>
    );
    return envolver(control, {
      label,
      helperText,
      className,
      disabled: input.disabled,
    });
  },
);
Checkbox.displayName = 'Checkbox';

// Figma > Componentes > Radio Buttons y Radio Label (State = Default, Hover, Pressed, Disabled)
export const Radio = React.forwardRef<HTMLInputElement, ControlProps>(
  (props, ref) => {
    const { label, helperText, invalid, className, ...input } = props;
    const control = (
      <span
        className={cn(
          'relative inline-flex size-4 shrink-0',
          !label && className,
        )}
      >
        <input
          ref={ref}
          type="radio"
          className={cn(
            'peer size-4 cursor-[inherit] appearance-none rounded-full border-2 border-content-placeholder bg-surface-primary outline-none',
            'hover:border-content-tertiary checked:border-stroke-primary-solid',
            'focus-visible:ring-2 focus-visible:ring-stroke-focus focus-visible:ring-offset-1',
            'disabled:border-stroke-primary',
            invalid && 'border-content-error',
          )}
          {...input}
        />
        <span
          className="pointer-events-none absolute inset-0 m-auto hidden size-2 rounded-full bg-stroke-primary-solid peer-checked:block peer-disabled:bg-stroke-primary"
          aria-hidden="true"
        />
      </span>
    );
    return envolver(control, {
      label,
      helperText,
      className,
      disabled: input.disabled,
    });
  },
);
Radio.displayName = 'Radio';
