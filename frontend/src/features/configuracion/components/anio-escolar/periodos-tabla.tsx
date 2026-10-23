import { EllipsisVertical, Pencil } from 'lucide-react';

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
import { Table } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { PeriodoDetalle } from '@/types/api';
import { formatDay } from '@/utils/format';

import { ESTADO_PERIODO } from './formato';

type PeriodosTablaProps = {
  periodos: PeriodoDetalle[];
  /** Con el año CERRADO los periodos son de solo lectura: no hay menú ⋮. */
  editable: boolean;
  onEditar: (periodo: PeriodoDetalle) => void;
};

// Figma > CI-03 y CI-06 > "Table": N.°, Periodo, Inicio, Fin, Límite de notas, Duración, Estado y menú ⋮.
export const PeriodosTabla = ({
  periodos,
  editable,
  onEditar,
}: PeriodosTablaProps) => (
  <Table
    data={periodos}
    emptyMessage="Este año aún no tiene periodos."
    columns={[
      { title: 'N.°', field: 'numero' },
      {
        title: 'Periodo',
        field: 'nombre',
        Cell: ({ entry }) => (
          <span className="text-label-2 font-medium text-content-primary">
            {entry.nombre}
          </span>
        ),
      },
      {
        title: 'Inicio',
        field: 'fechaInicio',
        Cell: ({ entry }) => <span>{formatDay(entry.fechaInicio)}</span>,
      },
      {
        title: 'Fin',
        field: 'fechaFin',
        Cell: ({ entry }) => <span>{formatDay(entry.fechaFin)}</span>,
      },
      {
        title: 'Límite de notas',
        field: 'fechaLimiteNotas',
        Cell: ({ entry }) => <span>{formatDay(entry.fechaLimiteNotas)}</span>,
      },
      {
        title: 'Duración',
        field: 'duracionSemanas',
        Cell: ({ entry }) => <span>{entry.duracionSemanas} sem.</span>,
      },
      {
        title: 'Estado',
        field: 'estado',
        Cell: ({ entry }) => {
          const { texto, color } = ESTADO_PERIODO[entry.estado];
          return (
            <Tag color={color} size="md">
              {texto}
            </Tag>
          );
        },
      },
      ...(editable
        ? [
            {
              title: '',
              field: 'id' as const,
              Cell: ({ entry }: { entry: PeriodoDetalle }) => (
                <DropdownMenu>
                  <DropdownMenuTrigger className="inline-flex size-8 items-center justify-center rounded-lg text-content-tertiary hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus">
                    <EllipsisVertical className="size-4" aria-hidden="true" />
                    <span className="sr-only">Acciones de {entry.nombre}</span>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" className="w-44">
                    <DropdownMenuItem onClick={() => onEditar(entry)}>
                      <Pencil className="mr-2 size-4" aria-hidden="true" />{' '}
                      Editar periodo
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              ),
            },
          ]
        : []),
    ]}
  />
);
