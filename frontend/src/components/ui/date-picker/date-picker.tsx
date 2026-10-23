import dayjs from 'dayjs';
import { CalendarDays } from 'lucide-react';
import * as React from 'react';
import { type FieldError, type UseFormRegisterReturn } from 'react-hook-form';

import { cn } from '@/utils/cn';

import {
  EtiquetaFlotante,
  FieldWrapper,
  fieldClassName,
} from '../form/field-wrapper';

import { Calendar, type CalendarSize } from './calendar';

type CampoFechaProps = {
  /** Fecha en formato YYYY-MM-DD (el mismo que guarda el backend). */
  value?: string | null;
  onChange: (value: string) => void;
  min?: string;
  max?: string;
  placeholder?: string;
  disabled?: boolean;
  invalid?: boolean;
  onBlur?: () => void;
  /** Tamaño del calendario desplegable: `md` (Figma, por defecto) o `sm` (compacto, para modales). */
  size?: CalendarSize;
  /** Etiqueta flotante, como la del Text Field. Descansa dentro si no hay fecha, el campo está cerrado y no hay placeholder. */
  label?: string;
};

// Botón con la fecha elegida + el calendario de Figma desplegable.
const CampoFecha = ({
  value,
  onChange,
  min,
  max,
  placeholder: placeholderPropio,
  disabled = false,
  invalid = false,
  onBlur,
  size,
  label,
}: CampoFechaProps) => {
  const [abierto, setAbierto] = React.useState(false);
  // Con etiqueta y sin placeholder propio, el campo vacío no muestra "DD/MM/AAAA": la etiqueta ocupa su lugar. El texto
  // sigue ahí (transparente) para no cambiar el nombre accesible del botón.
  const etiquetaDescansa = !!label && !value && !abierto && !placeholderPropio;
  const placeholder = placeholderPropio ?? 'DD/MM/AAAA';
  const contenedor = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    if (!abierto) return;
    const alClickFuera = (e: MouseEvent) => {
      if (!contenedor.current?.contains(e.target as Node)) {
        setAbierto(false);
        onBlur?.();
      }
    };
    const alTeclear = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setAbierto(false);
    };
    document.addEventListener('mousedown', alClickFuera);
    document.addEventListener('keydown', alTeclear);
    return () => {
      document.removeEventListener('mousedown', alClickFuera);
      document.removeEventListener('keydown', alTeclear);
    };
  }, [abierto, onBlur]);

  return (
    <div ref={contenedor} className="relative">
      <button
        type="button"
        disabled={disabled}
        aria-haspopup="dialog"
        aria-expanded={abierto}
        onClick={() => setAbierto(!abierto)}
        className={cn(
          fieldClassName(invalid),
          'flex h-11 items-center justify-between text-left',
          abierto && 'border-stroke-focus ring-2 ring-deep-blue-100',
          !value &&
            (etiquetaDescansa
              ? 'text-transparent'
              : 'text-content-placeholder'),
        )}
      >
        {value ? dayjs(value).format('DD/MM/YYYY') : placeholder}
        <CalendarDays
          className="size-5 text-content-tertiary"
          aria-hidden="true"
        />
      </button>
      {label && (
        <EtiquetaFlotante error={invalid} reposo={etiquetaDescansa}>
          {label}
        </EtiquetaFlotante>
      )}
      {abierto && (
        <div
          role="dialog"
          aria-label="Elegir fecha"
          className="absolute left-0 top-full z-50 mt-1 w-max"
        >
          <Calendar
            value={value}
            min={min}
            max={max}
            size={size}
            onChange={(fecha) => {
              onChange(fecha);
              setAbierto(false);
            }}
          />
        </div>
      )}
    </div>
  );
};

export type DatePickerProps = Omit<CampoFechaProps, 'invalid' | 'onBlur'> & {
  label?: string;
  hint?: string;
  error?: FieldError;
  className?: string;
};

// Campo de fecha controlado: Text Field + Figma > Componentes > Date picker como desplegable.
// Con react-hook-form usa <Input type="date" registration={register(...)} /> o este dentro de <Controller>.
export const DatePicker = ({
  label,
  hint,
  error,
  className,
  ...campo
}: DatePickerProps) => (
  <FieldWrapper hint={hint} error={error} className={className}>
    <CampoFecha {...campo} label={label} invalid={!!error} />
  </FieldWrapper>
);

const asignarValor = Object.getOwnPropertyDescriptor(
  HTMLInputElement.prototype,
  'value',
)?.set;

export type DateInputProps = React.InputHTMLAttributes<HTMLInputElement> & {
  registration?: Partial<UseFormRegisterReturn>;
  invalid?: boolean;
  /** Etiqueta flotante del campo (la misma que la de los demás campos de texto). */
  label?: string;
};

/**
 * Lo que dibuja `<Input type="date">`: el <input type="date"> sigue en el formulario (oculto) para que
 * register(), `value` y `onChange` funcionen igual, y encima va el calendario de Figma
 * (el calendario nativo del navegador no se puede estilizar).
 */
export const DateInput = React.forwardRef<HTMLInputElement, DateInputProps>(
  (
    { registration, invalid, disabled, min, max, placeholder, label, ...props },
    ref,
  ) => {
    const nativo = React.useRef<HTMLInputElement | null>(null);
    const [valor, setValor] = React.useState('');

    // El valor puede cambiar por fuera (reset, setValue): se lee del input en cada render.
    // Va sin dependencias a propósito; el if evita el bucle.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    React.useLayoutEffect(() => {
      const actual = nativo.current?.value ?? '';
      if (actual !== valor) setValor(actual);
    });

    const unirRef = (el: HTMLInputElement | null) => {
      nativo.current = el;
      if (typeof ref === 'function') ref(el);
      else if (ref) ref.current = el;
      registration?.ref?.(el);
    };

    return (
      <>
        <input
          type="date"
          tabIndex={-1}
          className="sr-only"
          disabled={disabled}
          min={min}
          max={max}
          aria-invalid={invalid}
          {...registration}
          {...props}
          onChange={(e) => {
            setValor(e.target.value);
            registration?.onChange?.(e);
            props.onChange?.(e);
          }}
          ref={unirRef}
        />
        <CampoFecha
          value={valor}
          min={min === undefined ? undefined : String(min)}
          max={max === undefined ? undefined : String(max)}
          placeholder={placeholder}
          label={label}
          disabled={disabled}
          invalid={invalid}
          onChange={(fecha) => {
            const input = nativo.current;
            if (!input) return;
            // Igual que si se escribiera en el input: React y react-hook-form reciben el evento.
            asignarValor?.call(input, fecha);
            input.dispatchEvent(new Event('input', { bubbles: true }));
          }}
        />
      </>
    );
  },
);
DateInput.displayName = 'DateInput';
