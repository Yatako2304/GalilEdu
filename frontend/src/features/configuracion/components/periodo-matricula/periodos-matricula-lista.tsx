import * as React from 'react';

import { Alert } from '@/components/ui/alert';
import { Card } from '@/components/ui/card';
import { Select } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';
import { PaginationBar } from '@/components/ui/table';
import { EstadoVisiblePeriodoMatricula } from '@/types/api';

import {
  usePeriodosMatricula,
  usePeriodosMatriculaTodos,
} from '../../api/periodos-matricula';

import { ESTADO_PERIODO_MATRICULA } from './formato';
import { PeriodosMatriculaTabla } from './periodos-matricula-tabla';

const OPCIONES_ESTADO = [
  { label: 'Estado: Todos', value: '' },
  ...(
    Object.entries(ESTADO_PERIODO_MATRICULA) as [
      EstadoVisiblePeriodoMatricula,
      { texto: string },
    ][]
  ).map(([valor, { texto }]) => ({ label: texto, value: valor })),
];

// Figma > pantalla 13 "Periodos de matrícula" > listado con los filtros "Año escolar" y "Estado" (HU-23).
export const PeriodosMatriculaLista = () => {
  const [anio, setAnio] = React.useState('');
  const [estado, setEstado] = React.useState<
    EstadoVisiblePeriodoMatricula | ''
  >('');
  const [page, setPage] = React.useState(1);

  React.useEffect(() => setPage(1), [anio, estado]);

  const periodos = usePeriodosMatricula({
    filtros: { anio: anio === '' ? '' : Number(anio), estado, page },
  });
  // Si se elimina la última fila de una página, se vuelve a la anterior en lugar de mostrar una página vacía.
  const filasDePagina = periodos.data?.data.length;
  React.useEffect(() => {
    if (filasDePagina === 0 && page > 1) setPage(page - 1);
  }, [filasDePagina, page]);

  // Los años del filtro salen de todos los periodos, no solo de la página que se ve.
  const todos = usePeriodosMatriculaTodos();
  const opcionesAnio = [
    { label: 'Año escolar: Todos', value: '' },
    ...[...new Set((todos.data?.data ?? []).map((fila) => fila.anio))]
      .sort((a, b) => b - a)
      .map((valor) => ({ label: String(valor), value: String(valor) })),
  ];
  const conFiltros = anio !== '' || estado !== '';

  return (
    <Card className="p-6">
      <div className="mb-6 flex flex-wrap gap-3">
        <Select
          aria-label="Filtrar por año escolar"
          options={opcionesAnio}
          value={anio}
          onChange={(e) => setAnio(e.target.value)}
          wrapperClassName="w-60"
          className="h-10"
        />
        <Select
          aria-label="Filtrar por estado"
          options={OPCIONES_ESTADO}
          value={estado}
          onChange={(e) =>
            setEstado(e.target.value as EstadoVisiblePeriodoMatricula | '')
          }
          wrapperClassName="w-60"
          className="h-10"
        />
      </div>

      {periodos.isLoading ? (
        <div className="flex h-80 items-center justify-center">
          <Spinner />
        </div>
      ) : periodos.isError || !periodos.data ? (
        <Alert
          type="error"
          title="No pudimos cargar los periodos de matrícula."
        >
          Intenta de nuevo en unos minutos.
        </Alert>
      ) : (
        <>
          <PeriodosMatriculaTabla
            filas={periodos.data.data}
            emptyMessage={
              conFiltros
                ? 'No se encontraron periodos de matrícula con esos filtros.'
                : 'Aún no hay periodos de matrícula.'
            }
          />
          {periodos.data.meta.total > 0 && (
            <PaginationBar {...periodos.data.meta} onPageChange={setPage} />
          )}
        </>
      )}
    </Card>
  );
};
