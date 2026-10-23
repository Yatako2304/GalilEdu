import { EllipsisVertical, Eye, Pencil, Trash2, Users } from 'lucide-react';
import * as React from 'react';
import { useNavigate } from 'react-router';

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
import { Table } from '@/components/ui/table';
import { paths } from '@/config/paths';
import { FilaPeriodoMatricula } from '@/types/api';

import { DialogoEliminarPeriodo } from './dialogo-eliminar-periodo';
import {
  MATRICULAS_DEL_PERIODO_DISPONIBLE,
  TEXTO_DISPONIBLE_PRONTO,
} from './enlaces';
import { EstadoPeriodoMatricula } from './estado-periodo-matricula';
import { fechaLima, matriculasDeLaFila } from './formato';

const AccionesFila = ({ fila }: { fila: FilaPeriodoMatricula }) => {
  const navigate = useNavigate();
  const rutas = paths.app.administrativo;
  const [eliminando, setEliminando] = React.useState(false);
  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger className="inline-flex size-8 items-center justify-center rounded-lg text-content-tertiary hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus">
          <EllipsisVertical className="size-4" aria-hidden="true" />
          <span className="sr-only">Acciones de {fila.nombre}</span>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-56">
          <DropdownMenuItem
            onSelect={() => navigate(rutas.periodoMatricula.getHref(fila.id))}
          >
            <Eye className="mr-2 size-4" aria-hidden="true" /> Ver
          </DropdownMenuItem>
          {/* La pantalla "Matrículas del periodo" es de Matrícula: hasta que exista, la opción queda deshabilitada. */}
          <DropdownMenuItem
            disabled={!MATRICULAS_DEL_PERIODO_DISPONIBLE}
            onSelect={() =>
              navigate(rutas.matriculasDelPeriodo.getHref(fila.id))
            }
          >
            <Users className="mr-2 size-4" aria-hidden="true" /> Ver matrículas
            {!MATRICULAS_DEL_PERIODO_DISPONIBLE && (
              <span className="ml-auto pl-3 text-label-3 text-content-quartiary">
                {TEXTO_DISPONIBLE_PRONTO}
              </span>
            )}
          </DropdownMenuItem>
          {/* Un periodo abierto también ofrece "Editar": abre su vista de solo lectura con el aviso. */}
          {fila.estado !== 'CERRADO' && (
            <DropdownMenuItem
              onSelect={() =>
                navigate(rutas.periodoMatriculaEditar.getHref(fila.id))
              }
            >
              <Pencil className="mr-2 size-4" aria-hidden="true" /> Editar
            </DropdownMenuItem>
          )}
          {/* Solo un periodo que aún no abre se puede eliminar. */}
          {fila.estado === 'PROGRAMADO' && (
            <DropdownMenuItem
              className="text-content-error"
              onSelect={() => setEliminando(true)}
            >
              <Trash2 className="mr-2 size-4" aria-hidden="true" /> Eliminar
            </DropdownMenuItem>
          )}
        </DropdownMenuContent>
      </DropdownMenu>
      <DialogoEliminarPeriodo
        periodo={fila}
        abierto={eliminando}
        onCerrar={() => setEliminando(false)}
      />
    </>
  );
};

type PeriodosMatriculaTablaProps = {
  filas: FilaPeriodoMatricula[];
  emptyMessage: string;
};

// Figma > pantalla 13 > listado: Periodo, Año escolar, Inicio, Fin, Matrículas, Estado y menú ⋮.
export const PeriodosMatriculaTabla = ({
  filas,
  emptyMessage,
}: PeriodosMatriculaTablaProps) => (
  <Table
    data={filas}
    emptyMessage={emptyMessage}
    columns={[
      { title: 'Periodo', field: 'nombre' },
      { title: 'Año escolar', field: 'anio' },
      {
        title: 'Inicio',
        field: 'inicio',
        Cell: ({ entry }) => <span>{fechaLima(entry.inicio)}</span>,
      },
      {
        title: 'Fin',
        field: 'cierre',
        Cell: ({ entry }) => <span>{fechaLima(entry.cierre)}</span>,
      },
      {
        title: 'Matrículas',
        field: 'matriculas',
        Cell: ({ entry }) => <span>{matriculasDeLaFila(entry)}</span>,
      },
      {
        title: 'Estado',
        field: 'estado',
        Cell: ({ entry }) => <EstadoPeriodoMatricula estado={entry.estado} />,
      },
      {
        title: '',
        field: 'id',
        Cell: ({ entry }) => <AccionesFila fila={entry} />,
      },
    ]}
  />
);
