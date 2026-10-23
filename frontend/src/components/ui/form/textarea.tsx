import * as React from 'react';
import { UseFormRegisterReturn } from 'react-hook-form';

import { cn } from '@/utils/cn';

import {
  FieldWrapper,
  FieldWrapperPassThroughProps,
  fieldClassName,
} from './field-wrapper';

export type TextareaProps = React.TextareaHTMLAttributes<HTMLTextAreaElement> &
  FieldWrapperPassThroughProps & {
    className?: string;
    registration?: Partial<UseFormRegisterReturn>;
    /**
     * Muestra el contador "12/100" bajo el campo, a la derecha (Figma: Text Area con contador). Necesita
     * `maxLength`. Con `value` se mantiene al día solo; sin él cuenta lo que se escribe, pero un cambio hecho por
     * fuera (por ejemplo `reset` de react-hook-form) no lo actualiza: en ese caso usa un campo controlado.
     */
    showCount?: boolean;
  };

// Figma > Componentes > Text Area
const Textarea = React.forwardRef<HTMLTextAreaElement, TextareaProps>(
  (
    { className, label, hint, error, registration, showCount, ...props },
    ref,
  ) => {
    const [escrito, setEscrito] = React.useState(
      String(props.value ?? props.defaultValue ?? '').length,
    );
    const longitud =
      props.value !== undefined ? String(props.value).length : escrito;

    return (
      <FieldWrapper
        label={label}
        hint={hint}
        error={error}
        afterField={
          showCount &&
          props.maxLength !== undefined && (
            <span className="block text-right text-label-3 text-content-quartiary">
              {longitud}/{props.maxLength}
            </span>
          )
        }
      >
        <textarea
          className={cn(
            fieldClassName(!!error),
            'min-h-[96px] py-2',
            className,
          )}
          aria-invalid={!!error}
          ref={ref}
          {...registration}
          {...props}
          onChange={(e) => {
            setEscrito(e.target.value.length);
            registration?.onChange?.(e);
            props.onChange?.(e);
          }}
        />
      </FieldWrapper>
    );
  },
);
Textarea.displayName = 'Textarea';

export { Textarea };
