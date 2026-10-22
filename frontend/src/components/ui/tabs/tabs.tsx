import * as React from 'react';

import { cn } from '@/utils/cn';

export type TabItem<T extends string> = {
  value: T;
  label: React.ReactNode;
  disabled?: boolean;
  /** Texto de ayuda al pasar el mouse (por ejemplo, por qué está deshabilitada). */
  title?: string;
};

export type TabsProps<T extends string> = {
  items: TabItem<T>[];
  value: T;
  onValueChange: (value: T) => void;
  /** Nombre accesible del grupo de pestañas. */
  'aria-label'?: string;
  /**
   * underline: Figma > Componentes > Tab (línea inferior, para secciones de una ficha).
   * pill: Figma > Componentes > Tab v2 (botón relleno, para cambiar de vista).
   */
  variant?: 'underline' | 'pill';
  /** Solo pill: Large o Medium de Tab v2. */
  size?: 'lg' | 'md';
  /** Solo underline: las pestañas ocupan todo el ancho en partes iguales. */
  fullWidth?: boolean;
  className?: string;
};

const estilos = {
  underline: {
    base: 'px-2 py-2.5 text-center text-label-1 disabled:border-stroke-disabled disabled:bg-surface-disabled disabled:text-content-disabled',
    activa:
      'border-b-2 border-stroke-accent-primary text-content-accent-primary',
    inactiva:
      'border-b border-stroke-primary text-content-tertiary hover:bg-surface-primary-hover hover:text-content-tertiary-hover',
  },
  pill: {
    base: 'rounded-md text-center text-label-1 disabled:cursor-not-allowed disabled:text-content-placeholder',
    activa: 'bg-surface-accent-primary font-medium text-white',
    inactiva:
      'bg-surface-primary text-content-secondary hover:bg-surface-primary-hover hover:text-content-primary active:bg-surface-primary-pressed',
  },
};

// Figma > Componentes > Tab / Tabs (N° Tabs = 2 a 6) y Tab v2 / Tabs v2
export const Tabs = <T extends string>({
  items,
  value,
  onValueChange,
  variant = 'underline',
  size = 'lg',
  fullWidth = false,
  className,
  ...props
}: TabsProps<T>) => {
  const refs = React.useRef<(HTMLButtonElement | null)[]>([]);
  const estilo = estilos[variant];

  // Flechas izquierda/derecha para moverse entre pestañas (patrón WAI-ARIA de tabs).
  const alTeclear = (e: React.KeyboardEvent, indice: number) => {
    if (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft') return;
    e.preventDefault();
    const paso = e.key === 'ArrowRight' ? 1 : -1;
    for (let i = 1; i <= items.length; i++) {
      const siguiente = (indice + paso * i + items.length) % items.length;
      if (!items[siguiente].disabled) {
        refs.current[siguiente]?.focus();
        onValueChange(items[siguiente].value);
        return;
      }
    }
  };

  return (
    <div
      role="tablist"
      aria-label={props['aria-label']}
      className={cn('flex', variant === 'pill' && 'flex-wrap gap-2', className)}
    >
      {items.map((item, i) => {
        const seleccionada = item.value === value;
        return (
          <button
            key={item.value}
            ref={(el) => {
              refs.current[i] = el;
            }}
            type="button"
            role="tab"
            aria-selected={seleccionada}
            tabIndex={seleccionada ? 0 : -1}
            disabled={item.disabled}
            title={item.title}
            onClick={() => onValueChange(item.value)}
            onKeyDown={(e) => alTeclear(e, i)}
            className={cn(
              'outline-none transition-colors focus-visible:ring-2 focus-visible:ring-stroke-focus',
              estilo.base,
              variant === 'underline' &&
                (fullWidth ? 'flex-1' : 'min-w-[120px]'),
              variant === 'pill' &&
                (size === 'lg' ? 'px-6 py-3' : 'px-3 py-2.5'),
              seleccionada ? estilo.activa : estilo.inactiva,
            )}
          >
            {item.label}
          </button>
        );
      })}
    </div>
  );
};
