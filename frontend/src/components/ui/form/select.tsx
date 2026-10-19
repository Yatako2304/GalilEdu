import { ChevronDown } from 'lucide-react';
import * as React from 'react';
import { UseFormRegisterReturn } from 'react-hook-form';

import { cn } from '@/utils/cn';

import {
  FieldWrapper,
  FieldWrapperPassThroughProps,
  fieldClassName,
} from './field-wrapper';

export type SelectOption = {
  label: React.ReactNode;
  value: string | number;
};

type SelectFieldProps = FieldWrapperPassThroughProps &
  Omit<React.SelectHTMLAttributes<HTMLSelectElement>, 'children'> & {
    options: SelectOption[];
    placeholder?: string;
    className?: string;
    wrapperClassName?: string;
    registration?: Partial<UseFormRegisterReturn>;
  };

// Figma > Componentes > Select
export const Select = React.forwardRef<HTMLSelectElement, SelectFieldProps>(
  (
    {
      label,
      hint,
      options,
      error,
      className,
      wrapperClassName,
      placeholder,
      registration,
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
          <select
            ref={ref}
            className={cn(
              fieldClassName(!!error),
              'h-11 appearance-none pr-10',
              className,
            )}
            aria-invalid={!!error}
            {...registration}
            {...props}
          >
            {placeholder && (
              <option value="" disabled>
                {placeholder}
              </option>
            )}
            {options.map(({ label, value }) => (
              <option key={String(value)} value={value}>
                {label}
              </option>
            ))}
          </select>
          <ChevronDown
            className="pointer-events-none absolute right-3 top-1/2 size-4 -translate-y-1/2 text-content-tertiary"
            aria-hidden="true"
          />
        </div>
      </FieldWrapper>
    );
  },
);
Select.displayName = 'Select';
