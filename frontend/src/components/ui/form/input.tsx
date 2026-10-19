import * as React from 'react';
import { type UseFormRegisterReturn } from 'react-hook-form';

import { cn } from '@/utils/cn';

import {
  FieldWrapper,
  FieldWrapperPassThroughProps,
  fieldClassName,
} from './field-wrapper';

export type InputProps = React.InputHTMLAttributes<HTMLInputElement> &
  FieldWrapperPassThroughProps & {
    className?: string;
    wrapperClassName?: string;
    registration?: Partial<UseFormRegisterReturn>;
    /** Ícono o botón al final del campo (ej. mostrar contraseña). */
    endAdornment?: React.ReactNode;
  };

const Input = React.forwardRef<HTMLInputElement, InputProps>(
  (
    {
      className,
      wrapperClassName,
      type,
      label,
      hint,
      error,
      registration,
      endAdornment,
      ...props
    },
    ref,
  ) => {
    return (
      <FieldWrapper
        label={label}
        hint={hint}
        error={error}
        className={wrapperClassName}
      >
        <div className="relative">
          <input
            type={type}
            className={cn(
              fieldClassName(!!error),
              'h-11',
              endAdornment && 'pr-10',
              className,
            )}
            aria-invalid={!!error}
            ref={ref}
            {...registration}
            {...props}
          />
          {endAdornment && (
            <div className="absolute inset-y-0 right-0 flex items-center pr-3">
              {endAdornment}
            </div>
          )}
        </div>
      </FieldWrapper>
    );
  },
);
Input.displayName = 'Input';

export { Input };
