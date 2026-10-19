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

const botonPagina =
  'inline-flex size-8 items-center justify-center rounded-lg border text-body-4 transition-colors disabled:pointer-events-none disabled:opacity-40';

// Figma > Componentes > Pagination: "Mostrando 1-8 de 253" + flechas y números.
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
      <div className="flex items-center gap-1">
        <button
          type="button"
          className={cn(botonPagina, 'border-stroke-primary bg-white')}
          disabled={page <= 1}
          onClick={() => onPageChange(page - 1)}
        >
          <ChevronLeft className="size-4" aria-hidden="true" />
          <span className="sr-only">Página anterior</span>
        </button>
        {paginasVisibles(page, totalPages).map((p, i) =>
          p === '…' ? (
            <span key={`e${i}`} className="px-1 text-content-quartiary">
              …
            </span>
          ) : (
            <button
              key={p}
              type="button"
              aria-current={p === page ? 'page' : undefined}
              onClick={() => onPageChange(p)}
              className={cn(
                botonPagina,
                p === page
                  ? 'border-transparent bg-surface-accent-primary text-white'
                  : 'border-stroke-primary bg-white text-content-secondary',
              )}
            >
              {p}
            </button>
          ),
        )}
        <button
          type="button"
          className={cn(botonPagina, 'border-stroke-primary bg-white')}
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
