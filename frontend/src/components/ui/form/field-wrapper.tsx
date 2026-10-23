import * as React from 'react';
import { type FieldError } from 'react-hook-form';

import { cn } from '@/utils/cn';

import { Error } from './error';
import { Label } from './label';

type FieldWrapperProps = {
  label?: string;
  hint?: string;
  className?: string;
  children: React.ReactNode;
  error?: FieldError | undefined;
  /** Va entre el campo y su mensaje de ayuda o error, fuera de la etiqueta (por ejemplo, el contador de Textarea). */
  afterField?: React.ReactNode;
};

export type FieldWrapperPassThroughProps = Omit<
  FieldWrapperProps,
  'className' | 'children' | 'afterField'
>;

// Figma > Componentes > Text Field: label (12px) + campo + mensaje de ayuda/error.
export const FieldWrapper = (props: FieldWrapperProps) => {
  const { label, hint, error, children, className, afterField } = props;
  return (
    <div className={cn('flex flex-col gap-1', className)}>
      <Label>
        {label && <span className="mb-1 block">{label}</span>}
        {children}
      </Label>
      {afterField}
      {error?.message ? (
        <Error errorMessage={error.message} />
      ) : (
        hint && <p className="text-body-4 text-content-quartiary">{hint}</p>
      )}
    </div>
  );
};

/** Clases compartidas por Input, Select y Textarea (estados de Figma). */
export const fieldClassName = (hasError?: boolean) =>
  cn(
    'w-full rounded-lg border bg-white px-3 text-body-3 text-content-primary transition-colors',
    'placeholder:text-content-placeholder',
    'hover:border-slate-500',
    'focus-visible:border-stroke-focus focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-deep-blue-100',
    'disabled:cursor-not-allowed disabled:border-slate-300 disabled:bg-slate-200 disabled:text-content-tertiary',
    hasError ? 'border-stroke-error' : 'border-stroke-primary',
  );

/**
 * Figma > Componentes > Text Field: la etiqueta (12 px) corta el borde superior del campo. `reposo` dice si descansa
 * dentro del campo, como si fuera el placeholder: `'segun-campo'` lo decide el CSS (el campo debe ser `peer` y tener
 * `placeholder-shown`), `true` la deja dentro y `false` la deja arriba (por ejemplo, si el campo muestra un placeholder
 * propio y la etiqueta lo taparía).
 */
export const EtiquetaFlotante = ({
  error,
  reposo,
  className,
  children,
}: {
  error?: boolean;
  reposo: boolean | 'segun-campo';
  className?: string;
  children: React.ReactNode;
}) => (
  <span
    className={cn(
      'pointer-events-none absolute left-2 top-[-7px] bg-white px-2 text-label-3 font-normal transition-all',
      reposo === true &&
        'left-2.5 top-3 bg-transparent px-0 text-body-3 text-content-placeholder',
      reposo === 'segun-campo' && [
        'peer-placeholder-shown:left-2.5 peer-placeholder-shown:top-3 peer-placeholder-shown:bg-transparent peer-placeholder-shown:px-0 peer-placeholder-shown:text-body-3 peer-placeholder-shown:text-content-placeholder',
        'peer-focus:left-2 peer-focus:top-[-7px] peer-focus:bg-white peer-focus:px-2 peer-focus:text-label-3 peer-focus:text-content-tertiary',
      ],
      error ? 'text-content-error' : 'text-content-tertiary',
      className,
    )}
  >
    {children}
  </span>
);
