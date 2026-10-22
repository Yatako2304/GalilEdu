import { ChevronDown, Folder } from 'lucide-react';
import * as React from 'react';

import { cn } from '@/utils/cn';

type BaseProps = {
  title: React.ReactNode;
  children: React.ReactNode;
  /** Abierto al inicio (no controlado). */
  defaultOpen?: boolean;
  /** Abierto (controlado). Úsalo junto con onOpenChange. */
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  disabled?: boolean;
  className?: string;
};

const useAbierto = ({
  defaultOpen = false,
  open,
  onOpenChange,
}: Pick<BaseProps, 'defaultOpen' | 'open' | 'onOpenChange'>) => {
  const [interno, setInterno] = React.useState(defaultOpen);
  const abierto = open ?? interno;
  const cambiar = () => {
    if (open === undefined) setInterno(!abierto);
    onOpenChange?.(!abierto);
  };
  return [abierto, cambiar] as const;
};

export type AccordionProps = BaseProps & {
  /** Ícono a la izquierda del título (Figma: With Icon = true). */
  icon?: React.ReactNode;
};

// Figma > Componentes > Accordion (Open · State = Default, Hover, Disabled · With Icon)
export const Accordion = ({
  title,
  icon,
  children,
  disabled = false,
  className,
  ...estado
}: AccordionProps) => {
  const [abierto, cambiar] = useAbierto(estado);
  const id = React.useId();

  return (
    <div
      className={cn(
        'overflow-hidden rounded-lg border border-stroke-primary',
        disabled ? 'bg-surface-disabled' : 'bg-surface-primary',
        className,
      )}
    >
      <button
        type="button"
        aria-expanded={abierto}
        aria-controls={id}
        disabled={disabled}
        onClick={cambiar}
        className={cn(
          'flex w-full items-center gap-2 px-3 py-2.5 text-left outline-none transition-colors',
          'focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-stroke-focus',
          disabled
            ? 'cursor-not-allowed text-content-disabled'
            : 'text-content-primary hover:bg-surface-primary-hover hover:text-content-primary-hover',
        )}
      >
        {icon && (
          <span className="flex shrink-0 [&_svg]:size-5" aria-hidden="true">
            {icon}
          </span>
        )}
        <span className="min-w-0 flex-1 text-h6">{title}</span>
        <ChevronDown
          className={cn(
            'size-5 shrink-0 transition-transform',
            abierto && 'rotate-180',
          )}
          aria-hidden="true"
        />
      </button>
      {abierto && (
        <div
          id={id}
          className={cn(
            'flex flex-col gap-2 border-t p-3',
            disabled ? 'border-stroke-tertiary' : 'border-stroke-primary',
          )}
        >
          {children}
        </div>
      )}
    </div>
  );
};

export type AccordionFolderProps = BaseProps & {
  /** Texto bajo el título (Figma: "18 archivos"). */
  subtitle?: React.ReactNode;
  /** Muestra la carpeta a la izquierda (Figma: With Icon = true). */
  withIcon?: boolean;
  /** Botón extra junto a la flecha (Figma: Show Edition, ej. "Agregar archivo"). */
  action?: React.ReactNode;
};

// Figma > Componentes > Accordion v2: carpeta con contador, para agrupar archivos.
export const AccordionFolder = ({
  title,
  subtitle,
  withIcon = false,
  action,
  children,
  disabled = false,
  className,
  ...estado
}: AccordionFolderProps) => {
  const [abierto, cambiar] = useAbierto(estado);
  const id = React.useId();

  return (
    <div
      className={cn(
        'overflow-hidden rounded-lg border border-stroke-primary bg-surface-primary',
        className,
      )}
    >
      <div
        className={cn(
          'flex items-center gap-4 p-4 transition-colors',
          !abierto && !disabled && 'hover:bg-surface-primary-hover',
        )}
      >
        {withIcon && (
          <span
            className={cn(
              'flex shrink-0 rounded-xl p-2',
              abierto
                ? 'bg-surface-accent-secondary text-white'
                : 'bg-vibrant-blue-50 text-content-accent-secondary',
            )}
            aria-hidden="true"
          >
            <Folder className="size-6 fill-current" />
          </span>
        )}
        <button
          type="button"
          aria-expanded={abierto}
          aria-controls={id}
          disabled={disabled}
          onClick={cambiar}
          className="flex min-w-0 flex-1 flex-col gap-0.5 rounded-sm text-left outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus disabled:cursor-not-allowed"
        >
          <span className="text-h6 font-bold text-content-primary">
            {title}
          </span>
          {subtitle && (
            <span className="text-body-4 font-medium text-content-tertiary">
              {subtitle}
            </span>
          )}
        </button>
        {action}
        <ChevronDown
          className={cn(
            'size-7 shrink-0 text-content-tertiary transition-transform',
            abierto && 'rotate-180',
          )}
          aria-hidden="true"
        />
      </div>
      {abierto && (
        <div
          id={id}
          className="flex flex-col gap-2 border-t border-stroke-primary p-4"
        >
          {children}
        </div>
      )}
    </div>
  );
};
