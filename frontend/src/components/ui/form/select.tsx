import { Check, ChevronDown } from 'lucide-react';
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

const unirRefs =
  <T,>(...refs: (React.Ref<T> | undefined)[]) =>
  (el: T | null) => {
    refs.forEach((r) => {
      if (typeof r === 'function') r(el);
      else if (r) (r as React.MutableRefObject<T | null>).current = el;
    });
  };

// Figma > Componentes > Select + Dropdown Item List / Dropdown Item (Size = Medium).
// El <select> nativo sigue en el formulario (oculto) para que react-hook-form, `value` y `onChange`
// funcionen igual; encima se dibuja el botón y la lista de Figma, porque la lista nativa no se puede estilizar.
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
      disabled,
      ...props
    },
    ref,
  ) => {
    const nativo = React.useRef<HTMLSelectElement | null>(null);
    const contenedor = React.useRef<HTMLDivElement>(null);
    const lista = React.useRef<HTMLUListElement>(null);
    const id = React.useId();
    const [abierto, setAbierto] = React.useState(false);
    const [activo, setActivo] = React.useState(0);
    const [valor, setValor] = React.useState('');

    // El valor puede cambiar por fuera (reset, setValue, prop `value`): se lee del <select> en cada render.
    // Va sin dependencias a propósito; el if evita el bucle.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    React.useLayoutEffect(() => {
      const actual = nativo.current?.value ?? '';
      if (actual !== valor) setValor(actual);
    });

    React.useEffect(() => {
      if (!abierto) return;
      const alClickFuera = (e: MouseEvent) => {
        if (!contenedor.current?.contains(e.target as Node)) setAbierto(false);
      };
      document.addEventListener('mousedown', alClickFuera);
      return () => document.removeEventListener('mousedown', alClickFuera);
    }, [abierto]);

    React.useEffect(() => {
      if (abierto)
        lista.current
          ?.querySelector(`[data-indice="${activo}"]`)
          ?.scrollIntoView?.({ block: 'nearest' });
    }, [abierto, activo]);

    const elegido = options.find((o) => String(o.value) === valor);

    const abrir = () => {
      if (disabled) return;
      const i = options.findIndex((o) => String(o.value) === valor);
      setActivo(i < 0 ? 0 : i);
      setAbierto(true);
    };

    const elegir = (opcion: SelectOption) => {
      const select = nativo.current;
      if (select) {
        select.value = String(opcion.value);
        // Dispara el mismo evento que el <select> nativo: lo reciben register() y onChange.
        select.dispatchEvent(new Event('change', { bubbles: true }));
        setValor(select.value);
      }
      setAbierto(false);
    };

    const alTeclear = (e: React.KeyboardEvent) => {
      if (!abierto) {
        if (['ArrowDown', 'ArrowUp', 'Enter', ' '].includes(e.key)) {
          e.preventDefault();
          abrir();
        }
        return;
      }
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        setActivo((i) => Math.min(i + 1, options.length - 1));
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        setActivo((i) => Math.max(i - 1, 0));
      } else if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        if (options[activo]) elegir(options[activo]);
      } else if (e.key === 'Escape' || e.key === 'Tab') {
        setAbierto(false);
      }
    };

    return (
      <FieldWrapper
        label={label}
        hint={hint}
        error={error}
        className={wrapperClassName}
      >
        <div ref={contenedor} className="relative">
          <select
            tabIndex={-1}
            className="sr-only"
            aria-invalid={!!error}
            disabled={disabled}
            {...registration}
            {...props}
            ref={unirRefs(ref, nativo, registration?.ref)}
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
          <button
            type="button"
            disabled={disabled}
            role="combobox"
            aria-haspopup="listbox"
            aria-expanded={abierto}
            aria-controls={abierto ? id : undefined}
            aria-activedescendant={abierto ? `${id}-${activo}` : undefined}
            onClick={() => (abierto ? setAbierto(false) : abrir())}
            onKeyDown={alTeclear}
            onBlur={(e) => registration?.onBlur?.(e as never)}
            className={cn(
              fieldClassName(!!error),
              'flex h-11 items-center pr-10 text-left',
              abierto && 'border-stroke-focus ring-2 ring-deep-blue-100',
              !elegido && 'text-content-placeholder',
              className,
            )}
          >
            <span className="truncate">{elegido?.label ?? placeholder}</span>
          </button>
          <ChevronDown
            className={cn(
              'pointer-events-none absolute right-3 top-[22px] size-4 -translate-y-1/2 text-content-tertiary transition-transform',
              abierto && 'rotate-180',
            )}
            aria-hidden="true"
          />
          {abierto && (
            <ul
              ref={lista}
              id={id}
              role="listbox"
              className="absolute inset-x-0 top-full z-50 mt-1 max-h-64 overflow-y-auto rounded-lg border border-stroke-secondary bg-surface-primary p-1 shadow-card"
            >
              {options.map((opcion, i) => {
                const seleccionada = String(opcion.value) === valor;
                return (
                  // El teclado se maneja en el botón (patrón listbox con aria-activedescendant).
                  // eslint-disable-next-line jsx-a11y/click-events-have-key-events
                  <li
                    key={String(opcion.value)}
                    id={`${id}-${i}`}
                    role="option"
                    aria-selected={seleccionada}
                    data-indice={i}
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => elegir(opcion)}
                    onMouseEnter={() => setActivo(i)}
                    className={cn(
                      'flex cursor-pointer items-center gap-2 rounded px-2 py-3 text-label-2 text-content-secondary',
                      i === activo &&
                        'bg-surface-primary-hover text-content-primary',
                    )}
                  >
                    <span className="min-w-0 flex-1">{opcion.label}</span>
                    {seleccionada && (
                      <Check
                        className="size-4 shrink-0 text-content-accent-primary"
                        aria-hidden="true"
                      />
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </FieldWrapper>
    );
  },
);
Select.displayName = 'Select';
