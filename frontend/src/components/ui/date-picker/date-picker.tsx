import dayjs from 'dayjs';
import { CalendarDays } from 'lucide-react';
import * as React from 'react';
import { type FieldError } from 'react-hook-form';

import { cn } from '@/utils/cn';

import { FieldWrapper, fieldClassName } from '../form/field-wrapper';

import { Calendar } from './calendar';

export type DatePickerProps = {
  label?: string;
  hint?: string;
  error?: FieldError;
  /** Fecha en formato YYYY-MM-DD (el mismo que guarda el backend). */
  value?: string | null;
  onChange: (value: string) => void;
  min?: string;
  max?: string;
  placeholder?: string;
  disabled?: boolean;
  className?: string;
};

// Campo de fecha: Text Field + Figma > Componentes > Date picker como desplegable.
// Con react-hook-form úsalo dentro de <Controller> (value / onChange).
export const DatePicker = ({
  label,
  hint,
  error,
  value,
  onChange,
  min,
  max,
  placeholder = 'DD/MM/AAAA',
  disabled = false,
  className,
}: DatePickerProps) => {
  const [abierto, setAbierto] = React.useState(false);
  const contenedor = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    if (!abierto) return;
    const alClickFuera = (e: MouseEvent) => {
      if (!contenedor.current?.contains(e.target as Node)) setAbierto(false);
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
  }, [abierto]);

  return (
    <FieldWrapper label={label} hint={hint} error={error} className={className}>
      <div ref={contenedor} className="relative">
        <button
          type="button"
          disabled={disabled}
          aria-haspopup="dialog"
          aria-expanded={abierto}
          onClick={() => setAbierto(!abierto)}
          className={cn(
            fieldClassName(!!error),
            'flex h-11 items-center justify-between text-left',
            !value && 'text-content-placeholder',
          )}
        >
          {value ? dayjs(value).format('DD/MM/YYYY') : placeholder}
          <CalendarDays
            className="size-5 text-content-tertiary"
            aria-hidden="true"
          />
        </button>
        {abierto && (
          <div
            role="dialog"
            aria-label="Elegir fecha"
            className="absolute left-0 top-full z-50 mt-1"
          >
            <Calendar
              value={value}
              min={min}
              max={max}
              onChange={(fecha) => {
                onChange(fecha);
                setAbierto(false);
              }}
            />
          </div>
        )}
      </div>
    </FieldWrapper>
  );
};
