import { ChevronRight } from 'lucide-react';
import * as React from 'react';
import { Link } from 'react-router';

import { cn } from '@/utils/cn';

export type ShortcutCardProps = {
  icon: React.ReactNode;
  /** Nombre del módulo. */
  title: string;
  /** Descripción breve. */
  description?: string;
  /** Ruta del módulo (usa `paths` de `@/config/paths`). */
  to: string;
  className?: string;
};

// Figma > Componentes > Shortcut Card: acceso rápido a un módulo en los inicios.
export const ShortcutCard = ({
  icon,
  title,
  description,
  to,
  className,
}: ShortcutCardProps) => (
  <Link
    to={to}
    className={cn(
      'flex items-center gap-3 rounded-xl border border-stroke-tertiary bg-white px-4 py-3.5 outline-none transition-colors',
      'hover:bg-surface-primary-hover focus-visible:ring-2 focus-visible:ring-stroke-focus',
      className,
    )}
  >
    <span
      className="flex shrink-0 rounded-xl border border-stroke-tertiary bg-surface-tertiary p-2 text-content-tertiary [&_svg]:size-4"
      aria-hidden="true"
    >
      {icon}
    </span>
    <span className="flex min-w-0 flex-1 flex-col gap-0.5">
      <span className="truncate text-[14px] font-semibold text-content-primary">
        {title}
      </span>
      {description && (
        <span className="truncate text-[12px] text-content-tertiary">
          {description}
        </span>
      )}
    </span>
    <ChevronRight
      className="size-[18px] shrink-0 text-content-primary"
      aria-hidden="true"
    />
  </Link>
);
