import { ChevronLeft, ChevronRight } from 'lucide-react';

import { cn } from '@/utils/cn';

type PaginationBarProps = {
  page: number;
  size: number;
  total: number;
  totalPages: number;
  onPageChange: (page: number) => void;
};

const paginasVisibles = (actual: number, total: number): (number | '…')[] => {
  if (total <= 5) return Array.from({ length: total }, (_, i) => i + 1);
  if (actual <= 3) return [1, 2, 3, '…', total];
  if (actual >= total - 2) return [1, '…', total - 2, total - 1, total];
  return [1, '…', actual, '…', total];
};

const flecha =
  'inline-flex size-8 items-center justify-center rounded-lg border border-stroke-primary bg-white text-content-tertiary transition-colors hover:bg-surface-primary-hover disabled:pointer-events-none disabled:opacity-40';

// Figma > Componentes > Count (State = Default, Hover, Selected · Page = número o "...")
const numero =
  'inline-flex min-w-8 items-center justify-center rounded-lg px-1 py-2 text-label-2 transition-colors';

// Figma > Componentes > Pagination: "Mostrando 1-10 de 200" + flechas y números (Count).
export const PaginationBar = ({
  page,
  size,
  total,
  totalPages,
  onPageChange,
}: PaginationBarProps) => {
  const desde = total === 0 ? 0 : (page - 1) * size + 1;
  const hasta = Math.min(page * size, total);

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 pt-4">
      <p className="text-body-3 text-content-tertiary">
        Mostrando{' '}
        <span className="font-semibold text-content-primary">
          {desde}-{hasta}
        </span>{' '}
        de <span className="font-semibold text-content-primary">{total}</span>
      </p>
      <div className="flex items-center gap-2">
        <button
          type="button"
          className={flecha}
          disabled={page <= 1}
          onClick={() => onPageChange(page - 1)}
        >
          <ChevronLeft className="size-4" aria-hidden="true" />
          <span className="sr-only">Página anterior</span>
        </button>
        {paginasVisibles(page, totalPages).map((p, i) =>
          p === '…' ? (
            <span key={`e${i}`} className={cn(numero, 'text-content-tertiary')}>
              ...
            </span>
          ) : (
            <button
              key={p}
              type="button"
              aria-current={p === page ? 'page' : undefined}
              onClick={() => onPageChange(p)}
              className={cn(
                numero,
                p === page
                  ? 'bg-surface-accent-primary font-medium text-white'
                  : 'text-content-tertiary hover:bg-surface-accent-light-hover hover:text-content-tertiary-hover',
              )}
            >
              {p}
            </button>
          ),
        )}
        <button
          type="button"
          className={flecha}
          disabled={page >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          <ChevronRight className="size-4" aria-hidden="true" />
          <span className="sr-only">Página siguiente</span>
        </button>
      </div>
    </div>
  );
};
