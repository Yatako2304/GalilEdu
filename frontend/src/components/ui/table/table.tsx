import { ArchiveX, ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react';
import * as React from 'react';

import { BaseEntity } from '@/types/api';
import { cn } from '@/utils/cn';

import { TablePagination, TablePaginationProps } from './pagination';

// Figma > Componentes > cell-table / cell-table-header
const TableElement = React.forwardRef<
  HTMLTableElement,
  React.HTMLAttributes<HTMLTableElement>
>(({ className, ...props }, ref) => (
  <div className="relative w-full overflow-auto">
    <table
      ref={ref}
      className={cn('w-full caption-bottom text-body-3', className)}
      {...props}
    />
  </div>
));
TableElement.displayName = 'Table';

const TableHeader = React.forwardRef<
  HTMLTableSectionElement,
  React.HTMLAttributes<HTMLTableSectionElement>
>(({ className, ...props }, ref) => (
  <thead
    ref={ref}
    className={cn('bg-surface-secondary [&_tr]:border-b', className)}
    {...props}
  />
));
TableHeader.displayName = 'TableHeader';

const TableBody = React.forwardRef<
  HTMLTableSectionElement,
  React.HTMLAttributes<HTMLTableSectionElement>
>(({ className, ...props }, ref) => (
  <tbody
    ref={ref}
    className={cn('[&_tr:last-child]:border-0', className)}
    {...props}
  />
));
TableBody.displayName = 'TableBody';

const TableFooter = React.forwardRef<
  HTMLTableSectionElement,
  React.HTMLAttributes<HTMLTableSectionElement>
>(({ className, ...props }, ref) => (
  <tfoot
    ref={ref}
    className={cn(
      'border-t bg-muted/50 font-medium [&>tr]:last:border-b-0',
      className,
    )}
    {...props}
  />
));
TableFooter.displayName = 'TableFooter';

const TableRow = React.forwardRef<
  HTMLTableRowElement,
  React.HTMLAttributes<HTMLTableRowElement>
>(({ className, ...props }, ref) => (
  <tr
    ref={ref}
    className={cn(
      'border-b border-stroke-tertiary transition-colors hover:bg-surface-secondary data-[state=selected]:bg-surface-accent-light',
      className,
    )}
    {...props}
  />
));
TableRow.displayName = 'TableRow';

const TableHead = React.forwardRef<
  HTMLTableCellElement,
  React.ThHTMLAttributes<HTMLTableCellElement>
>(({ className, ...props }, ref) => (
  <th
    ref={ref}
    className={cn(
      'h-11 px-4 text-left align-middle text-label-3 font-semibold text-content-tertiary [&:has([role=checkbox])]:pr-0',
      className,
    )}
    {...props}
  />
));
TableHead.displayName = 'TableHead';

const TableCell = React.forwardRef<
  HTMLTableCellElement,
  React.TdHTMLAttributes<HTMLTableCellElement>
>(({ className, ...props }, ref) => (
  <td
    ref={ref}
    className={cn(
      'px-4 py-3 align-middle text-content-secondary [&:has([role=checkbox])]:pr-0',
      className,
    )}
    {...props}
  />
));
TableCell.displayName = 'TableCell';

const TableCaption = React.forwardRef<
  HTMLTableCaptionElement,
  React.HTMLAttributes<HTMLTableCaptionElement>
>(({ className, ...props }, ref) => (
  <caption
    ref={ref}
    className={cn('mt-4 text-body-3 text-content-quartiary', className)}
    {...props}
  />
));
TableCaption.displayName = 'TableCaption';

export {
  TableElement,
  TableHeader,
  TableBody,
  TableFooter,
  TableHead,
  TableRow,
  TableCell,
  TableCaption,
};

type TableColumn<Entry> = {
  title: string;
  field: keyof Entry;
  /** Con la prop `sort` en la tabla, el encabezado se vuelve un botón que ordena por esta columna. */
  sortable?: boolean;
  Cell?({ entry }: { entry: Entry }): React.ReactElement;
};

export type TableSort<Entry> = {
  field: keyof Entry;
  direction: 'asc' | 'desc';
  onSort: (field: keyof Entry) => void;
};

export type TableProps<Entry> = {
  data: Entry[];
  sort?: TableSort<Entry>;
  columns: TableColumn<Entry>[];
  pagination?: TablePaginationProps;
  emptyMessage?: string;
};

export const Table = <Entry extends Pick<BaseEntity, 'id'>>({
  data,
  columns,
  sort,
  pagination,
  emptyMessage = 'No se encontraron registros',
}: TableProps<Entry>) => {
  if (!data?.length) {
    return (
      <div className="flex h-80 flex-col items-center justify-center gap-2 bg-white text-content-quartiary">
        <ArchiveX className="size-12" />
        <h4 className="text-body-3">{emptyMessage}</h4>
      </div>
    );
  }
  return (
    <>
      <TableElement>
        <TableHeader>
          <TableRow>
            {columns.map((column, index) => {
              const activa = sort?.field === column.field;
              return (
                <TableHead
                  key={column.title + index}
                  aria-sort={
                    activa
                      ? sort.direction === 'asc'
                        ? 'ascending'
                        : 'descending'
                      : undefined
                  }
                >
                  {sort && column.sortable ? (
                    <button
                      type="button"
                      className="inline-flex items-center gap-1 hover:text-content-primary focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus"
                      onClick={() => sort.onSort(column.field)}
                    >
                      {column.title}
                      {activa ? (
                        sort.direction === 'asc' ? (
                          <ArrowUp className="size-3.5" aria-hidden="true" />
                        ) : (
                          <ArrowDown className="size-3.5" aria-hidden="true" />
                        )
                      ) : (
                        <ArrowUpDown
                          className="size-3.5 opacity-50"
                          aria-hidden="true"
                        />
                      )}
                    </button>
                  ) : (
                    column.title
                  )}
                </TableHead>
              );
            })}
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.map((entry, entryIndex) => (
            <TableRow key={entry?.id || entryIndex}>
              {columns.map(({ Cell, field, title }, columnIndex) => (
                <TableCell key={title + columnIndex}>
                  {Cell ? <Cell entry={entry} /> : `${entry[field]}`}
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </TableElement>

      {pagination && <TablePagination {...pagination} />}
    </>
  );
};
