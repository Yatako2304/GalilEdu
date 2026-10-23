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
    /**
     * Texto o ícono corto al inicio del campo (Figma: el "S/" de un monto). Es decorativo: no recibe clics y queda
     * fuera del nombre accesible del campo (el campo va dentro de su etiqueta), así que lo que importe para quien usa
     * un lector de pantalla debe ir en la etiqueta, por ejemplo "Monto (S/)".
     */
    startAdornment?: React.ReactNode;
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
      startAdornment,
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
                startAdornment && 'pl-10',
                className,
              )}
              aria-invalid={!!error}
              // Sin placeholder propio se usa un espacio: `placeholder-shown` indica que el campo está vacío.
              placeholder={label && !props.placeholder ? ' ' : undefined}
              ref={ref}
              {...registration}
              {...props}
            />
            {startAdornment && (
              <div
                aria-hidden="true"
                className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-label-2 text-content-tertiary"
              >
                {startAdornment}
              </div>
            )}
            {label && (
              <EtiquetaFlotante
                error={!!error}
                // Con un placeholder propio, un adorno al inicio ("S/") o `type="time"` (el navegador siempre dibuja
                // "--:--") la etiqueta se queda arriba: abajo taparía a uno u otro.
                reposo={
                  props.placeholder || startAdornment || type === 'time'
                    ? false
                    : 'segun-campo'
                }
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
