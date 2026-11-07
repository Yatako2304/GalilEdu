import { BookOpenCheck } from 'lucide-react';
import * as React from 'react';

import { Card } from '@/components/ui/card';
import { Select } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';
import { Tabs } from '@/components/ui/tabs';
import { Tag } from '@/components/ui/tag';
import { cn } from '@/utils/cn';

import {
  SeleccionPeriodo,
  useCompetenciasCurso,
  useCursosDocente,
  useItemsEvaluacion,
} from '../api/calificaciones';

import { CalificarAlumnos } from './calificar-alumnos';
import { ItemsEvaluacion } from './items-evaluacion';
import { ReporteCalificacionesVista } from './reporte-calificaciones';

type Vista = 'configuracion' | 'consolidado';
type Pestana = 'items' | 'calificar';

export const CalificacionesDocente = () => {
  const { data: cursos = [], isLoading } = useCursosDocente();
  const anios = React.useMemo(
    () => [...new Set(cursos.map((curso) => curso.anio))].sort((a, b) => b - a),
    [cursos],
  );
  const [anio, setAnio] = React.useState<number>();
  const [cursoClave, setCursoClave] = React.useState('');
  const [periodoId, setPeriodoId] = React.useState('');
  const [vista, setVista] = React.useState<Vista>('configuracion');
  const [pestana, setPestana] = React.useState<Pestana>('items');

  React.useEffect(() => {
    if (anio === undefined || !anios.includes(anio)) setAnio(anios[0]);
  }, [anio, anios]);

  const cursosDelAnio = cursos.filter((curso) => curso.anio === anio);
  React.useEffect(() => {
    if (!cursosDelAnio.some((curso) => claveCurso(curso) === cursoClave)) {
      setCursoClave(cursosDelAnio[0] ? claveCurso(cursosDelAnio[0]) : '');
    }
  }, [cursoClave, cursosDelAnio]);

  const curso = cursosDelAnio.find(
    (actual) => claveCurso(actual) === cursoClave,
  );
  const nombresCurso = unicos(cursosDelAnio.map((actual) => actual.curso));
  const cursosDelNombre = cursosDelAnio.filter(
    (actual) => actual.curso === curso?.curso,
  );
  const grados = unicosPor(
    cursosDelNombre.map((actual) => ({
      value: actual.gradoCodigo,
      label: actual.grado,
    })),
    (actual) => actual.value,
  );
  const secciones = cursosDelNombre.filter(
    (actual) => actual.gradoCodigo === curso?.gradoCodigo,
  );
  React.useEffect(() => {
    if (!curso) {
      setPeriodoId('');
      return;
    }
    if (!curso.periodos.some((periodo) => periodo.id === periodoId)) {
      setPeriodoId(
        curso.periodos.find((periodo) => periodo.estado === 'EN_CURSO')?.id ??
          curso.periodos.find((periodo) => periodo.registroNotasAbierto)?.id ??
          curso.periodos[0]?.id ??
          '',
      );
    }
  }, [curso, periodoId]);

  const periodo = curso?.periodos.find((actual) => actual.id === periodoId);
  const seleccionCurso = curso
    ? { planCursoId: curso.planCursoId, seccionId: curso.seccionId }
    : null;
  const seleccion: SeleccionPeriodo | null =
    seleccionCurso && periodo
      ? { ...seleccionCurso, periodoId: periodo.id }
      : null;
  const { data: competencias = [] } = useCompetenciasCurso(seleccionCurso);
  const { data: items = [] } = useItemsEvaluacion(seleccion);

  if (isLoading) {
    return (
      <div className="flex min-h-80 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (!cursos.length) {
    return (
      <Card className="flex min-h-72 flex-col items-center justify-center px-6 text-center">
        <BookOpenCheck className="size-12 text-icon-tertiary" />
        <h2 className="mt-4 text-h6 text-content-primary">
          No tienes cursos asignados
        </h2>
        <p className="mt-2 max-w-lg text-body-3 text-content-quartiary">
          Cuando el plan de evaluación tenga un curso y sección asignados a tu
          usuario, aparecerán aquí.
        </p>
      </Card>
    );
  }

  if (!curso || !periodo || !seleccion) return null;

  return (
    <>
      <Tabs
        aria-label="Vistas de calificaciones"
        variant="pill"
        value={vista}
        onValueChange={setVista}
        items={[
          { value: 'configuracion', label: 'Configuración de ítems' },
          { value: 'consolidado', label: 'Consolidado de notas' },
        ]}
      />

      {vista === 'configuracion' && (
        <>
          <Card className="p-5">
            <h2 className="mb-4 text-label-1 font-medium text-content-primary">
              Curso y sección
            </h2>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <Select
                label="Año académico"
                value={String(anio ?? '')}
                options={anios.map((valor) => ({
                  label: String(valor),
                  value: valor,
                }))}
                onChange={(e) => setAnio(Number(e.target.value))}
              />
              <Select
                label="Curso"
                value={curso.curso}
                options={nombresCurso.map((nombre) => ({
                  label: nombre,
                  value: nombre,
                }))}
                onChange={(e) => {
                  const siguiente = cursosDelAnio.find(
                    (actual) => actual.curso === e.target.value,
                  );
                  if (siguiente) setCursoClave(claveCurso(siguiente));
                }}
              />
              <Select
                label="Grado"
                value={curso.gradoCodigo}
                options={grados}
                onChange={(e) => {
                  const siguiente = cursosDelNombre.find(
                    (actual) => actual.gradoCodigo === e.target.value,
                  );
                  if (siguiente) setCursoClave(claveCurso(siguiente));
                }}
              />
              <Select
                label="Sección"
                value={curso.seccionId}
                options={secciones.map((actual) => ({
                  label: actual.seccion,
                  value: actual.seccionId,
                }))}
                onChange={(e) => {
                  const siguiente = secciones.find(
                    (actual) => actual.seccionId === e.target.value,
                  );
                  if (siguiente) setCursoClave(claveCurso(siguiente));
                }}
              />
              <Select
                label="Área curricular"
                value={curso.area}
                options={[{ label: curso.area, value: curso.area }]}
                disabled
                wrapperClassName="sm:col-span-2"
              />
            </div>
          </Card>

          <section aria-labelledby="periodos-titulo">
            <h2
              id="periodos-titulo"
              className="mb-3 text-label-1 font-medium text-content-primary"
            >
              Periodo lectivo
            </h2>
            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              {curso.periodos.map((actual) => (
                <button
                  key={actual.id}
                  type="button"
                  onClick={() => setPeriodoId(actual.id)}
                  className={cn(
                    'rounded-xl border bg-white p-4 text-left transition-colors',
                    actual.id === periodo.id
                      ? 'border-stroke-accent-primary bg-surface-accent-light'
                      : 'border-stroke-tertiary hover:bg-surface-primary-hover',
                  )}
                >
                  <div className="flex items-start justify-between gap-2">
                    <span className="text-label-2 font-medium text-content-primary">
                      {actual.nombre}
                    </span>
                    <Tag
                      size="sm"
                      color={
                        actual.estado === 'EN_CURSO'
                          ? 'success'
                          : actual.estado === 'PLANIFICADO'
                            ? 'info'
                            : 'neutral'
                      }
                    >
                      {actual.estado === 'EN_CURSO'
                        ? 'En curso'
                        : actual.estado === 'PLANIFICADO'
                          ? 'Planificado'
                          : 'Cerrado'}
                    </Tag>
                  </div>
                  <p className="mt-1 text-caption-1 text-content-quartiary">
                    {rangoFechas(actual.fechaInicio, actual.fechaFin)}
                  </p>
                  <p className="mt-5 text-body-4 text-content-secondary">
                    {actual.competencias} competencias
                  </p>
                  <p className="mt-1 text-body-4 text-content-accent-primary">
                    {actual.items} ítems de calificación
                  </p>
                </button>
              ))}
            </div>
          </section>
        </>
      )}

      {vista === 'configuracion' ? (
        <div>
          <Tabs
            aria-label="Secciones de calificaciones"
            fullWidth
            value={pestana}
            onValueChange={setPestana}
            items={[
              { value: 'items', label: 'Configurar ítems' },
              { value: 'calificar', label: 'Calificar alumnos' },
            ]}
          />
          <div className="mt-5">
            {pestana === 'items' && (
              <ItemsEvaluacion
                seleccion={seleccion}
                periodo={periodo}
                competencias={competencias}
                reglaNota={curso.reglaNota}
              />
            )}
            {pestana === 'calificar' && (
              <CalificarAlumnos curso={curso} periodo={periodo} items={items} />
            )}
          </div>
        </div>
      ) : (
        <ReporteCalificacionesVista
          seleccion={seleccion}
          curso={curso}
          periodo={periodo}
          anios={anios}
          cursos={nombresCurso}
          grados={grados}
          competencias={competencias}
          items={items}
          onAnioChange={setAnio}
          onCursoChange={(nombre) => {
            const siguiente = cursosDelAnio.find(
              (actual) => actual.curso === nombre,
            );
            if (siguiente) setCursoClave(claveCurso(siguiente));
          }}
          onGradoChange={(grado) => {
            const siguiente = cursosDelNombre.find(
              (actual) => actual.gradoCodigo === grado,
            );
            if (siguiente) setCursoClave(claveCurso(siguiente));
          }}
          onPeriodoChange={setPeriodoId}
        />
      )}
    </>
  );
};

const claveCurso = (curso: { planCursoId: string; seccionId: string }) =>
  `${curso.planCursoId}:${curso.seccionId}`;

const fechaCorta = (valor: string) =>
  new Intl.DateTimeFormat('es-PE', {
    day: '2-digit',
    month: 'short',
    timeZone: 'UTC',
  })
    .format(new Date(`${valor}T00:00:00Z`))
    .replace('.', '');

const rangoFechas = (inicio: string, fin: string) =>
  `${fechaCorta(inicio)} - ${fechaCorta(fin)}`;

const unicos = <T,>(valores: T[]) => [...new Set(valores)];

const unicosPor = <T,>(valores: T[], clave: (valor: T) => string) => [
  ...new Map(valores.map((valor) => [clave(valor), valor])).values(),
];
