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
  exportarResumenAnual,
  FiltrosReporte,
  FiltrosResumenAnual,
  SeleccionPeriodo,
  useReporteCalificaciones,
  useResumenAnual,
} from '../api/calificaciones';

import { colorLogro } from './logro';
import { NotasCompetencia } from './notas-competencia';

type OpcionGrado = { value: string; label: string };

/** Valor del selector de periodo para el resumen del año. */
const TODO_EL_ANIO = 'ANIO';

/** Igual que ServicioCalificaciones.FALTAN_REGISTROS y POR_REGISTRAR. */
const FALTAN_REGISTROS = 'Faltan registros';
const POR_REGISTRAR = 'Por registrar';

const NOMBRES_REGLA: Record<string, string> = {
  PROMEDIO_SIMPLE: 'promedio simple',
  PROMEDIO_PONDERADO: 'promedio ponderado',
  NIVEL_MAS_FRECUENTE: 'nivel más frecuente',
  ULTIMO_NIVEL: 'último nivel',
};

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
  const [competenciaIds, setCompetenciaIds] = React.useState<string[]>([]);
  const [itemIds, setItemIds] = React.useState<string[]>([]);
  const [incluirPromedio, setIncluirPromedio] = React.useState(true);
  const [filtrosGenerados, setFiltrosGenerados] =
    React.useState<FiltrosReporte | null>(null);
  // «Todo el año»: el consolidado de un periodo no trae datos de otros periodos; el año va en su propio resumen.
  const [todoElAnio, setTodoElAnio] = React.useState(false);
  const [filtrosAnio, setFiltrosAnio] =
    React.useState<FiltrosResumenAnual | null>(null);

  const catalogoCompetencias = competencias
    .map((competencia) => competencia.id)
    .join(',');
  React.useEffect(() => {
    setCompetenciaIds(
      catalogoCompetencias ? [catalogoCompetencias.split(',')[0]] : [],
    );
    setFiltrosGenerados(null);
    setFiltrosAnio(null);
  }, [catalogoCompetencias, seleccion.planCursoId, seleccion.seccionId]);

  const itemsCompetencia = React.useMemo(
    () =>
      items.filter((item) =>
        item.competencias.some((competencia) =>
          competenciaIds.includes(competencia.competenciaId),
        ),
      ),
    [competenciaIds, items],
  );
  const idsDisponibles = itemsCompetencia.map((item) => item.id).join(',');
  const seleccionCompetencias = competenciaIds.join(',');

  React.useEffect(() => {
    setItemIds(idsDisponibles ? idsDisponibles.split(',') : []);
    setFiltrosGenerados(null);
  }, [
    seleccionCompetencias,
    idsDisponibles,
    seleccion.periodoId,
    seleccion.planCursoId,
    seleccion.seccionId,
  ]);

  const { data: reporte, isLoading } =
    useReporteCalificaciones(filtrosGenerados);
  const resumen = useResumenAnual(filtrosAnio);

  React.useEffect(() => {
    setFiltrosAnio(null);
  }, [seleccionCompetencias]);

  const generar = () => {
    if (todoElAnio) {
      if (!competenciaIds.length) return;
      setFiltrosAnio({
        planCursoId: seleccion.planCursoId,
        seccionId: seleccion.seccionId,
        competenciaIds: competenciaIds.join(','),
      });
      return;
    }
    if (!competenciaIds.length || !itemIds.length) return;
    setFiltrosGenerados({
      ...seleccion,
      tipo: 'COMPETENCIA',
      competenciaIds: competenciaIds.join(','),
      itemIds: itemIds.join(','),
      incluirPromedio,
    });
  };

  const limpiar = () => {
    setCompetenciaIds([]);
    setItemIds([]);
    setIncluirPromedio(true);
    setFiltrosGenerados(null);
    setFiltrosAnio(null);
  };

  const elegirPeriodo = (valor: string) => {
    setFiltrosGenerados(null);
    setFiltrosAnio(null);
    if (valor === TODO_EL_ANIO) {
      setTodoElAnio(true);
      return;
    }
    setTodoElAnio(false);
    onPeriodoChange(valor);
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
            value={todoElAnio ? TODO_EL_ANIO : periodo.id}
            options={[
              ...curso.periodos.map((actual) => ({
                label: actual.nombre,
                value: actual.id,
              })),
              { label: 'Todo el año', value: TODO_EL_ANIO },
            ]}
            onChange={(evento) => elegirPeriodo(evento.target.value)}
          />
        </div>

        <div className="mt-4 grid gap-4 lg:grid-cols-2">
          <MultiSelect
            label="Competencias"
            value={competenciaIds}
            options={competencias.map((competencia) => ({
              label: competencia.nombre,
              value: competencia.id,
            }))}
            onValueChange={setCompetenciaIds}
            placeholder="Selecciona las competencias"
          />
          {!todoElAnio && (
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
          )}
        </div>

        <div className="mt-6 flex flex-wrap items-end justify-between gap-5">
          {todoElAnio ? (
            <p className="max-w-xl text-body-4 text-content-quartiary">
              Por competencia: lo que aporta este curso en cada periodo y su
              aporte final (referencial), y la nota de la competencia en cada
              periodo y la final, que combina todos los cursos del grado.
            </p>
          ) : (
            <div className="flex items-start gap-3">
              <div>
                <span
                  id="etiqueta-promedio-periodo"
                  className="block text-label-2 font-medium text-content-primary"
                >
                  Incluir promedio del periodo
                </span>
                <span className="mt-1 block max-w-md text-body-4 text-content-quartiary">
                  Calcula el promedio de este curso en cada competencia durante
                  el periodo (su aporte a la nota de la competencia) y un
                  promedio general de referencia, con la regla del año escolar:{' '}
                  {NOMBRES_REGLA[curso.reglaNota] ?? curso.reglaNota}.
                </span>
              </div>
              <Switch
                checked={incluirPromedio}
                onCheckedChange={setIncluirPromedio}
                aria-labelledby="etiqueta-promedio-periodo"
              />
            </div>
          )}

          <div className="flex gap-3">
            <Button type="button" variant="primary-light" onClick={limpiar}>
              Limpiar
            </Button>
            <Button
              type="button"
              onClick={generar}
              disabled={
                !competenciaIds.length || (!todoElAnio && !itemIds.length)
              }
              isLoading={todoElAnio ? resumen.isLoading : isLoading}
            >
              {todoElAnio ? 'Generar resumen del año' : 'Generar consolidado'}
            </Button>
          </div>
        </div>
      </Card>

      {todoElAnio && filtrosAnio && (
        <Card className="overflow-hidden">
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-stroke-tertiary px-5 py-4">
            <div>
              <h2 className="text-label-1 font-medium text-content-primary">
                Resumen del año
              </h2>
              <p className="mt-1 max-w-2xl text-body-4 text-content-quartiary">
                El aporte final de {curso.curso} es referencial. La nota de la
                competencia combina todos los cursos en cada periodo: no es el
                promedio de los aportes finales. La consolida la coordinación.
              </p>
            </div>
            <Button
              type="button"
              variant="primary-light"
              size="sm"
              icon={<Download className="size-4" />}
              iconPosition="right"
              disabled={!resumen.data?.filas.length}
              onClick={() => exportarResumenAnual(filtrosAnio)}
            >
              Descargar PDF
            </Button>
          </div>
          {resumen.isLoading ? (
            <div className="flex h-56 items-center justify-center">
              <Spinner />
            </div>
          ) : resumen.data && resumen.data.filas.length ? (
            <TablaConsolidado reporte={resumen.data} escala={curso.escala} />
          ) : (
            <div className="flex h-48 items-center justify-center px-5 text-center text-body-3 text-content-quartiary">
              No hay estudiantes matriculados para mostrar.
            </div>
          )}
        </Card>
      )}

      {!todoElAnio && (filtrosGenerados || isLoading) && (
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
              Descargar PDF
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

      {!todoElAnio && filtrosGenerados?.competenciaIds && (
        <NotasCompetencia
          seleccion={{
            planCursoId: filtrosGenerados.planCursoId,
            seccionId: filtrosGenerados.seccionId,
          }}
          competenciaIds={filtrosGenerados.competenciaIds.split(',')}
        />
      )}
    </div>
  );
};

const TablaConsolidado = ({
  reporte,
  escala,
}: {
  reporte: ReporteCalificaciones;
  escala: CursoDocente['escala'];
}) => {
  const grupos: { clave: string; titulo: string; columnas: number }[] = [];
  reporte.columnas.forEach((columna) => {
    // En el resumen del año una competencia tiene dos grupos: el aporte del curso y la nota de la competencia.
    const clave = `${columna.competenciaId ?? 'resumen'}|${columna.competencia ?? ''}`;
    const ultimo = grupos.at(-1);
    if (ultimo?.clave === clave) {
      ultimo.columnas++;
    } else {
      grupos.push({
        clave,
        titulo:
          columna.competencia ??
          (columna.clave === 'resultado_general'
            ? 'Resumen del curso (referencial)'
            : reporte.titulo),
        columnas: 1,
      });
    }
  });

  return (
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
          {grupos.map((grupo) => (
            <TableHead
              key={grupo.clave}
              colSpan={grupo.columnas}
              scope="colgroup"
              className="text-center text-caption-1"
            >
              {grupo.titulo}
            </TableHead>
          ))}
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
              const esPromedio =
                columna.promedio ?? columna.clave === 'resultado';
              const estado = fila.valores[`${columna.clave}_estado`];
              // Un promedio al que le faltan registros es solo avance: en gris, sin letra ni color de logro.
              const incompleto = estado?.startsWith(FALTAN_REGISTROS);
              const sinNota = valor === POR_REGISTRAR || valor === 'NE';
              return (
                <TableCell key={columna.clave}>
                  <span
                    className={cn(
                      'block',
                      esPromedio &&
                        !incompleto &&
                        'font-semibold text-content-primary',
                      (incompleto || sinNota) && 'text-content-quartiary',
                    )}
                  >
                    {formatearNota(valor, escala)}
                  </span>
                  {esPromedio && estado && (
                    <span
                      className={cn(
                        'mt-1 block text-caption-1',
                        colorLogro(estado),
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
};

const formatearNota = (valor: string, escala: CursoDocente['escala']) => {
  // Solo los números llevan «/20»: «—», «NE» y «Pendiente (3 de 4)» quedan igual.
  if (escala !== 'VIGESIMAL' || !/^\d+(\.\d+)?$/.test(valor)) return valor;
  return `${valor.replace('.', ',')}/20`;
};
