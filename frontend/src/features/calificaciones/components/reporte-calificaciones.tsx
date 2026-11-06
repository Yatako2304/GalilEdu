import { Download } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { MultiSelect, Select, Switch } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';
import {
  TableBody,
  TableCell,
  TableElement,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  CompetenciaOpcion,
  CursoDocente,
  ItemEvaluacion,
  PeriodoCursoDocente,
  ReporteCalificaciones,
} from '@/types/api';
import { cn } from '@/utils/cn';

import {
  exportarReporte,
  FiltrosReporte,
  SeleccionPeriodo,
  useReporteCalificaciones,
} from '../api/calificaciones';

import { NotaFinalCompetenciaVista } from './nota-final-competencia';

type OpcionGrado = { value: string; label: string };

type Props = {
  seleccion: SeleccionPeriodo;
  curso: CursoDocente;
  periodo: PeriodoCursoDocente;
  anios: number[];
  cursos: string[];
  grados: OpcionGrado[];
  competencias: CompetenciaOpcion[];
  items: ItemEvaluacion[];
  onAnioChange: (anio: number) => void;
  onCursoChange: (curso: string) => void;
  onGradoChange: (grado: string) => void;
  onPeriodoChange: (periodoId: string) => void;
};

export const ReporteCalificacionesVista = ({
  seleccion,
  curso,
  periodo,
  anios,
  cursos,
  grados,
  competencias,
  items,
  onAnioChange,
  onCursoChange,
  onGradoChange,
  onPeriodoChange,
}: Props) => {
  const [competenciaId, setCompetenciaId] = React.useState('');
  const [itemIds, setItemIds] = React.useState<string[]>([]);
  const [incluirPromedio, setIncluirPromedio] = React.useState(true);
  const [filtrosGenerados, setFiltrosGenerados] =
    React.useState<FiltrosReporte | null>(null);

  React.useEffect(() => {
    if (!competencias.some((competencia) => competencia.id === competenciaId)) {
      setCompetenciaId(competencias[0]?.id ?? '');
    }
  }, [competenciaId, competencias]);

  const itemsCompetencia = React.useMemo(
    () =>
      items.filter((item) =>
        item.competencias.some(
          (competencia) => competencia.competenciaId === competenciaId,
        ),
      ),
    [competenciaId, items],
  );
  const idsDisponibles = React.useMemo(
    () => itemsCompetencia.map((item) => item.id),
    [itemsCompetencia],
  );

  React.useEffect(() => {
    setItemIds(idsDisponibles);
    setFiltrosGenerados(null);
  }, [
    competenciaId,
    idsDisponibles,
    seleccion.periodoId,
    seleccion.planCursoId,
    seleccion.seccionId,
  ]);

  const { data: reporte, isLoading } =
    useReporteCalificaciones(filtrosGenerados);

  const generar = () => {
    if (!competenciaId || !itemIds.length) return;
    setFiltrosGenerados({
      ...seleccion,
      tipo: 'COMPETENCIA',
      objetivoId: competenciaId,
      itemIds: itemIds.join(','),
      incluirPromedio,
    });
  };

  const limpiar = () => {
    setItemIds([]);
    setIncluirPromedio(true);
    setFiltrosGenerados(null);
  };

  return (
    <div className="space-y-5">
      <h2 className="text-h5 font-semibold text-content-primary">
        Consolidado de notas
      </h2>
      <Card className="p-5">
        <h2 className="border-b border-stroke-tertiary pb-4 text-label-1 font-medium text-content-primary">
          Generar consolidado de notas
        </h2>

        <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <Select
            label="Año académico"
            value={String(curso.anio)}
            options={anios.map((anio) => ({
              label: String(anio),
              value: anio,
            }))}
            onChange={(evento) => onAnioChange(Number(evento.target.value))}
          />
          <Select
            label="Curso"
            value={curso.curso}
            options={cursos.map((nombre) => ({ label: nombre, value: nombre }))}
            onChange={(evento) => onCursoChange(evento.target.value)}
          />
          <Select
            label="Grado"
            value={curso.gradoCodigo}
            options={grados}
            onChange={(evento) => onGradoChange(evento.target.value)}
          />
          <Select
            label="Periodo lectivo"
            value={periodo.id}
            options={curso.periodos.map((actual) => ({
              label: actual.nombre,
              value: actual.id,
            }))}
            onChange={(evento) => onPeriodoChange(evento.target.value)}
          />
        </div>

        <div className="mt-4 grid gap-4 lg:grid-cols-2">
          <Select
            label="Competencia"
            value={competenciaId}
            options={competencias.map((competencia) => ({
              label: competencia.nombre,
              value: competencia.id,
            }))}
            onChange={(evento) => setCompetenciaId(evento.target.value)}
          />
          <MultiSelect
            label="Ítems de calificación"
            value={itemIds}
            options={itemsCompetencia.map((item) => ({
              label: item.nombre,
              value: item.id,
            }))}
            onValueChange={setItemIds}
            disabled={!itemsCompetencia.length}
            placeholder="Selecciona los ítems"
          />
        </div>

        <div className="mt-6 flex flex-wrap items-end justify-between gap-5">
          <div className="flex items-start gap-3">
            <div>
              <span
                id="etiqueta-promedio-final"
                className="block text-label-2 font-medium text-content-primary"
              >
                Incluir promedio final
              </span>
              <span className="mt-1 block max-w-md text-body-4 text-content-quartiary">
                Calcula el promedio final con las calificaciones registradas en
                los ítems seleccionados.
              </span>
            </div>
            <Switch
              checked={incluirPromedio}
              onCheckedChange={setIncluirPromedio}
              aria-labelledby="etiqueta-promedio-final"
            />
          </div>

          <div className="flex gap-3">
            <Button type="button" variant="primary-light" onClick={limpiar}>
              Limpiar
            </Button>
            <Button
              type="button"
              onClick={generar}
              disabled={!competenciaId || !itemIds.length}
              isLoading={isLoading}
            >
              Generar consolidado
            </Button>
          </div>
        </div>
      </Card>

      {(filtrosGenerados || isLoading) && (
        <Card className="overflow-hidden">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-stroke-tertiary px-5 py-4">
            <h2 className="text-label-1 font-medium text-content-primary">
              Consolidado de notas
            </h2>
            <Button
              type="button"
              variant="primary-light"
              size="sm"
              icon={<Download className="size-4" />}
              iconPosition="right"
              disabled={!filtrosGenerados || !reporte?.filas.length}
              onClick={() =>
                filtrosGenerados && exportarReporte(filtrosGenerados)
              }
            >
              Exportar consolidado
            </Button>
          </div>

          {isLoading ? (
            <div className="flex h-56 items-center justify-center">
              <Spinner />
            </div>
          ) : reporte && reporte.filas.length ? (
            <TablaConsolidado reporte={reporte} escala={curso.escala} />
          ) : (
            <div className="flex h-48 items-center justify-center px-5 text-center text-body-3 text-content-quartiary">
              No hay estudiantes matriculados para mostrar.
            </div>
          )}
        </Card>
      )}

      <NotaFinalCompetenciaVista
        seleccion={{
          planCursoId: seleccion.planCursoId,
          seccionId: seleccion.seccionId,
        }}
        competenciaId={competenciaId}
      />
    </div>
  );
};

const TablaConsolidado = ({
  reporte,
  escala,
}: {
  reporte: ReporteCalificaciones;
  escala: CursoDocente['escala'];
}) => (
  <TableElement
    className="table-fixed"
    style={{ minWidth: Math.max(760, 260 + reporte.columnas.length * 180) }}
    aria-label={`${reporte.titulo}. ${reporte.detalle}`}
  >
    <colgroup>
      <col style={{ width: 260 }} />
      {reporte.columnas.map((columna) => (
        <col key={columna.clave} style={{ width: 180 }} />
      ))}
    </colgroup>
    <TableHeader className="bg-white">
      <TableRow>
        <TableHead />
        <TableHead
          colSpan={reporte.columnas.length}
          className="text-center text-caption-1"
        >
          {reporte.titulo}
        </TableHead>
      </TableRow>
      <TableRow>
        <TableHead>Estudiante</TableHead>
        {reporte.columnas.map((columna) => (
          <TableHead key={columna.clave}>{columna.titulo}</TableHead>
        ))}
      </TableRow>
    </TableHeader>
    <TableBody>
      {reporte.filas.map((fila) => (
        <TableRow key={fila.matriculaId}>
          <TableCell>
            <span className="flex items-center gap-3 font-medium text-content-primary">
              <span
                aria-hidden="true"
                className="size-2 shrink-0 rounded-full bg-vibrant-blue-500"
              />
              {fila.estudiante}
            </span>
          </TableCell>
          {reporte.columnas.map((columna) => {
            const valor = fila.valores[columna.clave] ?? '—';
            const esPromedio = columna.clave === 'resultado';
            const estado = fila.valores.resultado_estado;
            return (
              <TableCell key={columna.clave}>
                <span
                  className={cn(
                    'block',
                    esPromedio && 'font-semibold text-content-primary',
                  )}
                >
                  {formatearNota(valor, escala)}
                </span>
                {esPromedio && estado && (
                  <span
                    className={cn(
                      'mt-1 block text-caption-1',
                      estado === 'Aprobado'
                        ? 'text-content-success'
                        : estado === 'Desaprobado'
                          ? 'text-content-error'
                          : 'text-content-quartiary',
                    )}
                  >
                    {estado}
                  </span>
                )}
              </TableCell>
            );
          })}
        </TableRow>
      ))}
    </TableBody>
  </TableElement>
);

const formatearNota = (valor: string, escala: CursoDocente['escala']) => {
  if (valor === '—' || escala !== 'VIGESIMAL') return valor;
  return `${valor.replace('.', ',')}/20`;
};
