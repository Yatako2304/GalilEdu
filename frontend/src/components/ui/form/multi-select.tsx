import { ChevronDown } from 'lucide-react';
import * as React from 'react';

import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
import { SelectableTag } from '@/components/ui/tag';
import { cn } from '@/utils/cn';

import {
  EtiquetaFlotante,
  FieldWrapper,
  FieldWrapperPassThroughProps,
  fieldClassName,
} from './field-wrapper';
import { SelectOption } from './select';

type MultiSelectProps = FieldWrapperPassThroughProps & {
  options: SelectOption[];
  value: string[];
  onValueChange: (value: string[]) => void;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
  wrapperClassName?: string;
};

// Figma > Componentes > Select múltiple con etiquetas removibles.
export const MultiSelect = ({
  label,
  hint,
  error,
  options,
  value,
  onValueChange,
  placeholder = 'Selecciona',
  disabled,
  className,
  wrapperClassName,
}: MultiSelectProps) => {
  const listaId = React.useId();
  const [abierto, setAbierto] = React.useState(false);
  const seleccionadas = options.filter((opcion) =>
    value.includes(String(opcion.value)),
  );

  const alternar = (opcion: SelectOption) => {
    const id = String(opcion.value);
    onValueChange(
      value.includes(id)
        ? value.filter((actual) => actual !== id)
        : [...value, id],
    );
  };

  return (
    <FieldWrapper hint={hint} error={error} className={wrapperClassName}>
      <DropdownMenu open={abierto} onOpenChange={setAbierto}>
        <div className="relative">
          <DropdownMenuTrigger asChild disabled={disabled}>
            <div
              role="combobox"
              aria-label={label}
              aria-controls={listaId}
              aria-expanded={abierto}
              aria-disabled={disabled || undefined}
              tabIndex={disabled ? -1 : 0}
              className={cn(
                fieldClassName(!!error),
                'flex min-h-11 cursor-pointer items-center gap-1.5 py-1.5 pr-10 text-left',
                abierto && 'border-stroke-focus ring-2 ring-deep-blue-100',
                disabled &&
                  'pointer-events-none cursor-not-allowed border-slate-300 bg-slate-200 text-content-tertiary',
                className,
              )}
            >
              {seleccionadas.length ? (
                seleccionadas.map((opcion) => (
                  <span
                    key={String(opcion.value)}
                    onPointerDown={(evento) => evento.stopPropagation()}
                  >
                    <SelectableTag
                      size="sm"
                      shape="circle"
                      onRemove={() => alternar(opcion)}
                    >
                      {opcion.label}
                    </SelectableTag>
                  </span>
                ))
              ) : (
                <span className="text-content-placeholder">{placeholder}</span>
              )}
            </div>
          </DropdownMenuTrigger>
          {label && (
            <EtiquetaFlotante error={!!error} reposo={false}>
              {label}
            </EtiquetaFlotante>
          )}
          <ChevronDown
            aria-hidden="true"
            className={cn(
              'pointer-events-none absolute right-3 top-[22px] size-4 -translate-y-1/2 text-content-tertiary transition-transform',
              abierto && 'rotate-180',
            )}
          />
        </div>
        <DropdownMenuContent
          id={listaId}
          align="start"
          className="max-h-64 w-[var(--radix-dropdown-menu-trigger-width)] overflow-y-auto rounded-lg border-stroke-secondary bg-white p-1"
        >
          {options.map((opcion) => {
            const id = String(opcion.value);
            const seleccionada = value.includes(id);
            return (
              <DropdownMenuCheckboxItem
                key={id}
                checked={seleccionada}
                disabled={opcion.disabled}
                onCheckedChange={() => alternar(opcion)}
                onSelect={(evento) => evento.preventDefault()}
                className="gap-2 py-3 pl-8 pr-2 text-label-2 text-content-secondary"
              >
                <span className="min-w-0 flex-1 truncate">{opcion.label}</span>
              </DropdownMenuCheckboxItem>
            );
          })}
        </DropdownMenuContent>
      </DropdownMenu>
    </FieldWrapper>
  );
};
