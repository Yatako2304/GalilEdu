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
    /**
     * Muestra el contador "12/100" bajo el campo, a la derecha (Figma: Text Area con contador). Necesita
     * `maxLength`. Con `value` se mantiene al día solo; sin él cuenta lo que se escribe, pero un cambio hecho por
     * fuera (por ejemplo `reset` de react-hook-form) no lo actualiza: en ese caso usa un campo controlado.
     */
    showCount?: boolean;
  };

// Figma > Componentes > Text Area (Size = Large, el predeterminado): 96 px de alto, relleno de 12/16 px y texto de 16 px.
// Sin dibujar todavía el contador de caracteres ni el ícono de redimensionar (el navegador pone el suyo).
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
        <div className="relative">
          <textarea
            className={cn(
              fieldClassName(!!error),
              'peer min-h-[96px] px-3 py-4 text-label-1 hover:bg-surface-primary-hover',
              className,
            )}
            aria-invalid={!!error}
            // Sin placeholder propio se usa un espacio: `placeholder-shown` indica que el campo está vacío.
            placeholder={label && !props.placeholder ? ' ' : undefined}
            ref={ref}
            {...registration}
            {...props}
            onChange={(e) => {
              setEscrito(e.target.value.length);
              registration?.onChange?.(e);
              props.onChange?.(e);
            }}
          />
          {label && (
            <EtiquetaFlotante
              error={!!error}
              reposo={props.placeholder ? false : 'segun-campo'}
              // En reposo la etiqueta es `tertiary` (como el Text Field de Figma) y baja a la altura del relleno; sube
              // con foco (`accent-primary`) y oscurece con el mouse encima (`tertiary-hover`).
              className={cn(
                'peer-hover:text-content-tertiary-hover peer-focus:text-content-accent-primary',
                !props.placeholder &&
                  'peer-placeholder-shown:left-3 peer-placeholder-shown:top-4 peer-placeholder-shown:text-label-1 peer-placeholder-shown:text-content-tertiary',
              )}
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
