import * as React from 'react';
import { type UseFormRegisterReturn } from 'react-hook-form';

import { cn } from '@/utils/cn';

import { DateInput } from '../date-picker/date-picker';

import {
  EtiquetaFlotante,
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
    const esFecha = type === 'date';
    return (
      <FieldWrapper hint={hint} error={error} className={wrapperClassName}>
        {esFecha ? (
          // Figma > Componentes > Date picker en vez del calendario nativo del navegador.
          <DateInput
            ref={ref}
            label={label}
            registration={registration}
            invalid={!!error}
            {...props}
          />
        ) : (
          <div className="relative">
            <input
              type={type}
              className={cn(
                fieldClassName(!!error),
                'peer h-11',
                endAdornment && 'pr-10',
                className,
              )}
              aria-invalid={!!error}
              // Sin placeholder propio se usa un espacio: `placeholder-shown` indica que el campo está vacío.
              placeholder={label && !props.placeholder ? ' ' : undefined}
              ref={ref}
              {...registration}
              {...props}
            />
            {label && (
              <EtiquetaFlotante
                error={!!error}
                reposo={props.placeholder ? false : 'segun-campo'}
              >
                {label}
              </EtiquetaFlotante>
            )}
            {endAdornment && (
              <div className="absolute inset-y-0 right-0 flex items-center pr-3">
                {endAdornment}
              </div>
            )}
          </div>
        )}
      </FieldWrapper>
    );
  },
);
Input.displayName = 'Input';

export { Input };
