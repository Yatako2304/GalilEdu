import * as React from 'react';
import { UseFormRegisterReturn } from 'react-hook-form';

import { cn } from '@/utils/cn';

import {
  EtiquetaFlotante,
  FieldWrapper,
  FieldWrapperPassThroughProps,
  fieldClassName,
} from './field-wrapper';

export type TextareaProps = React.TextareaHTMLAttributes<HTMLTextAreaElement> &
  FieldWrapperPassThroughProps & {
    className?: string;
    registration?: Partial<UseFormRegisterReturn>;
  };

// Figma > Componentes > Text Area
const Textarea = React.forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, label, hint, error, registration, ...props }, ref) => {
    return (
      <FieldWrapper hint={hint} error={error}>
        <div className="relative">
          <textarea
            className={cn(
              fieldClassName(!!error),
              'peer min-h-[96px] py-3',
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
        </div>
      </FieldWrapper>
    );
  },
);
Textarea.displayName = 'Textarea';

export { Textarea };
