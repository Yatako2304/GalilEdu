import * as React from 'react';
import { type FieldError } from 'react-hook-form';

import { cn } from '@/utils/cn';

import { SelectableTag } from '../tag/selectable-tag';

import { FieldWrapper } from './field-wrapper';

export type TagFieldProps = {
  label?: string;
  hint?: string;
  error?: FieldError;
  /** Etiquetas actuales. */
  value: string[];
  onChange: (value: string[]) => void;
  placeholder?: string;
  disabled?: boolean;
  /** Ícono al final del campo (Figma: Icon = true). */
  endAdornment?: React.ReactNode;
  className?: string;
};

// Figma > Componentes > Tag Field: campo que acumula etiquetas (Enter o coma para agregar).
// Con react-hook-form úsalo dentro de <Controller> (value / onChange).
export const TagField = ({
  label,
  hint,
  error,
  value,
  onChange,
  placeholder,
  disabled = false,
  endAdornment,
  className,
}: TagFieldProps) => {
  const [texto, setTexto] = React.useState('');

  const agregar = () => {
    const nuevo = texto.trim();
    if (nuevo && !value.includes(nuevo)) onChange([...value, nuevo]);
    setTexto('');
  };

  return (
    <FieldWrapper label={label} hint={hint} error={error} className={className}>
      <div
        className={cn(
          'flex min-h-11 w-full flex-wrap items-center gap-2 rounded-lg border bg-white px-3 py-1.5 transition-colors',
          'focus-within:border-stroke-focus focus-within:ring-2 focus-within:ring-deep-blue-100',
          error ? 'border-stroke-error' : 'border-stroke-primary',
          disabled
            ? 'cursor-not-allowed border-slate-300 bg-surface-disabled'
            : 'hover:border-slate-500',
        )}
      >
        {value.map((tag) => (
          <SelectableTag
            key={tag}
            shape="circle"
            size="md"
            color={error ? 'error' : 'primary'}
            disabled={disabled}
            onRemove={() => onChange(value.filter((t) => t !== tag))}
          >
            {tag}
          </SelectableTag>
        ))}
        <input
          value={texto}
          disabled={disabled}
          placeholder={value.length ? undefined : placeholder}
          aria-invalid={!!error}
          onChange={(e) => setTexto(e.target.value)}
          onBlur={agregar}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ',') {
              e.preventDefault();
              agregar();
            } else if (e.key === 'Backspace' && !texto && value.length) {
              onChange(value.slice(0, -1));
            }
          }}
          className="min-w-24 flex-1 bg-transparent text-body-3 text-content-primary outline-none placeholder:text-content-placeholder disabled:cursor-not-allowed"
        />
        {endAdornment && (
          <span className="flex shrink-0 text-content-tertiary">
            {endAdornment}
          </span>
        )}
      </div>
    </FieldWrapper>
  );
};
