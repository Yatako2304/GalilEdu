import {
  BarChart3,
  FileDown,
  TrendingDown,
  TrendingUp,
  Users,
} from 'lucide-react';
import * as React from 'react';

import { Alert } from '@/components/ui/alert';
import {
  BarraDistribucion,
  LeyendaNiveles,
  NIVELES_REPORTE,
  porcentajeDe,
} from '@/components/ui/barra-distribucion';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Select } from '@/components/ui/form';
import { useNotifications } from '@/components/ui/notifications';
import { Spinner } from '@/components/ui/spinner';
import { StatCard } from '@/components/ui/stat-card';
import {
  TableBody,
  TableCell,
  TableElement,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Tabs } from '@/components/ui/tabs';
import { NotaNivel } from '@/components/ui/tag';
import {
  AnioReporte,
  CeldaResumen,
  DistribucionNiveles,
  EscalaEvaluacion,
  ReporteInstitucional,
} from '@/types/api';
import { cn } from '@/utils/cn';

import {
  descargarReporteGrado,
  descargarReporteInstitucional,
  FiltrosReporteGrado,
  FiltrosReporteInstitucional,
  useAniosReporte,
  useReporteGrado,
  useReporteInstitucional,
} from '../api/reportes';

import { DetalleSeccion } from './detalle-seccion';
import { colorLogro } from './logro';

export type VistaReporte = 'grado' | 'institucional';

const NOMBRES: Record<VistaReporte, string> = {
  grado: 'Por curso y grado',
  institucional: 'Institucional',
};

const Cargando = () => (
  <div className="flex h-40 items-center justify-center">
    <Spinner size="lg" />
  </div>
);

const Vacio = ({ texto }: { texto: string }) => (
  <p className="py-10 text-center text-body-3 text-content-quartiary">
    {texto}
  </p>
);

/**
 * Reportes de notas. HU-63 (por curso y grado, Coordinador) e HU-64 (institucional, Coordinador y Administrador). Figma
 * solo tiene el fragmento «Competencias reporte» del Coordinador (competencias × secciones con promedio y porcentaje):
 * de ahí sale la tabla de HU-63; el «% aprobados» de Figma se informa como «% con A o AD» (decisión del 18/11). El
 * reporte institucional no tiene frame y usa los mismos componentes.
 */
export const ReportesNotas = ({ vistas }: { vistas: VistaReporte[] }) => {
  const [vista, setVista] = React.useState<VistaReporte>(vistas[0]);
  const aniosQuery = useAniosReporte();
  const anios = aniosQuery.data ?? [];
  const [anioId, setAnioId] = React.useState<string>();
  const anio = anios.find((a) => a.id === anioId) ?? anios[0];
  // '' = nota final del año.
  const [periodoId, setPeriodoId] = React.useState('');

  if (aniosQuery.isLoading) return <Cargando />;
  if (!anio) {
    return (
      <Card>
        <Vacio texto="Todavía no hay un año escolar activo o cerrado." />
      </Card>
    );
  }

  const filtrosComunes = (
    <>
      <Select
        label="Año académico"
        wrapperClassName="w-40"
        value={anio.id}
        options={anios.map((a) => ({ label: a.anio, value: a.id }))}
        onChange={(e) => {
          setAnioId(e.target.value);
          setPeriodoId('');
        }}
      />
      <Select
        label="Periodo"
        wrapperClassName="w-48"
        value={periodoId}
        options={[
          { label: 'Nota final del año', value: '' },
          ...anio.periodos.map((p) => ({ label: p.nombre, value: p.id })),
        ]}
        onChange={(e) => setPeriodoId(e.target.value)}
      />
    </>
  );

  return (
    <div className="space-y-6">
      {vistas.length > 1 && (
        <Tabs
          variant="pill"
          value={vista}
          onValueChange={setVista}
          items={vistas.map((v) => ({ value: v, label: NOMBRES[v] }))}
        />
      )}
      {vista === 'grado' ? (
        <ReportePorGrado
          key={anio.id}
          anio={anio}
          periodoId={periodoId}
          filtros={filtrosComunes}
        />
      ) : (
        <ReporteDelColegio
          anio={anio}
          periodoId={periodoId}
          filtros={filtrosComunes}
        />
      )}
    </div>
  );
};

const BotonPdf = ({ descargar }: { descargar: () => Promise<void> }) => {
  const [descargando, setDescargando] = React.useState(false);
  const { addNotification } = useNotifications();
  return (
    <Button
      icon={<FileDown className="size-4" />}
      isLoading={descargando}
      disabled={descargando}
      onClick={async () => {
        setDescargando(true);
        try {
          await descargar();
        } catch {
          addNotification({
            type: 'error',
            title: 'No se pudo descargar el reporte',
          });
        } finally {
          setDescargando(false);
        }
      }}
    >
      Descargar PDF
    </Button>
  );
};

// ---------- HU-63 ----------

const ReportePorGrado = ({
  anio,
  periodoId,
  filtros,
}: {
  anio: AnioReporte;
  periodoId: string;
  filtros: React.ReactNode;
}) => {
  const [gradoCodigo, setGradoCodigo] = React.useState(
    anio.grados[0]?.codigo ?? '',
  );
  const [planCursoId, setPlanCursoId] = React.useState('');
  // Sección abierta en el detalle por estudiante (solo el Coordinador ve este reporte).
  const [seccionId, setSeccionId] = React.useState<string>();
  const grado = anio.grados.find((g) => g.codigo === gradoCodigo);
  const consulta: FiltrosReporteGrado | null = grado
    ? {
        anioEscolarId: anio.id,
        grado: grado.codigo,
        planCursoId: planCursoId || undefined,
        periodoId: periodoId || undefined,
      }
    : null;
  const { data: reporte, isLoading } = useReporteGrado(consulta);
  const sinCalcular = reporte?.filas.some((f) => !f.calculable);

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader
          action={
            consulta && (
              <BotonPdf descargar={() => descargarReporteGrado(consulta)} />
            )
          }
        >
          <CardTitle>Reporte de notas por curso y grado</CardTitle>
          <p className="mt-1 text-body-4 text-content-quartiary">
            Promedio de cada competencia por sección y en todo el grado, y
            cuántos estudiantes alcanzan el logro esperado (A o AD).
          </p>
        </CardHeader>
        <CardContent className="space-y-5 pb-6">
          <div className="flex flex-wrap gap-4">
            {filtros}
            <Select
              label="Grado"
              wrapperClassName="w-52"
              value={gradoCodigo}
              options={anio.grados.map((g) => ({
                label: g.nombre,
                value: g.codigo,
              }))}
              placeholder="Sin plan de evaluación"
              onChange={(e) => {
                setGradoCodigo(e.target.value);
                setSeccionId(undefined);
                setPlanCursoId('');
              }}
            />
            <Select
              label="Curso"
              wrapperClassName="w-64"
              value={planCursoId}
              options={[
                { label: 'Todos (nota oficial de la competencia)', value: '' },
                ...(grado?.cursos ?? []).map((c) => ({
                  label: c.curso,
                  value: c.planCursoId,
                })),
              ]}
              onChange={(e) => setPlanCursoId(e.target.value)}
            />
          </div>

          {reporte?.curso && (
            <Alert type="info" title={`Aporte de ${reporte.curso}`}>
              Cada celda resume lo que {reporte.curso} aporta a la competencia.
              La nota oficial combina todos los cursos del grado que la evalúan.
            </Alert>
          )}
          {sinCalcular && (
            <Alert type="warning" title="Pesos incompletos">
              Algunas competencias no tienen nota oficial porque los pesos de
              sus cursos no suman 100 %. Ajústalos en Plan de evaluación.
            </Alert>
          )}

          {!grado ? (
            <Vacio texto="El año no tiene grados con plan de evaluación." />
          ) : isLoading || !reporte ? (
            <Cargando />
          ) : !reporte.filas.length ? (
            <Vacio texto="No hay competencias para mostrar." />
          ) : (
            <div className="overflow-x-auto rounded-lg border border-stroke-tertiary">
              <TableElement className="min-w-[720px]">
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-[32%]">Competencia</TableHead>
                    {reporte.secciones.map((s) => (
                      <TableHead key={s.id}>
                        Sección {s.nombre}
                        <span className="block text-caption-2 font-normal text-content-quartiary">
                          {s.estudiantes} estudiantes
                        </span>
                        {s.estudiantes > 0 && (
                          <Button
                            variant="ghost"
                            size="sm"
                            icon={<Users className="size-4" />}
                            aria-label={`Ver estudiantes de la sección ${s.nombre}`}
                            aria-pressed={seccionId === s.id}
                            onClick={() =>
                              setSeccionId(
                                seccionId === s.id ? undefined : s.id,
                              )
                            }
                          >
                            Ver estudiantes
                          </Button>
                        )}
                      </TableHead>
                    ))}
                    <TableHead className="bg-surface-accent-light">
                      Todo el grado
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {reporte.filas.map((fila) => (
                    <TableRow key={fila.competenciaId}>
                      <TableCell>
                        <p className="font-medium text-content-primary">
                          {fila.competencia}
                        </p>
                        <p className="text-caption-1 text-content-quartiary">
                          {fila.area}
                        </p>
                      </TableCell>
                      {fila.secciones.map((celda, indice) => (
                        <TableCell key={reporte.secciones[indice].id}>
                          <Celda celda={celda} escala={reporte.escala} />
                        </TableCell>
                      ))}
                      <TableCell className="bg-surface-accent-light/40">
                        <Celda celda={fila.general} escala={reporte.escala} />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </TableElement>
            </div>
          )}
          {reporte && (
            <p className="text-caption-1 text-content-quartiary">
              {reporte.periodo} ·{' '}
              {reporte.escala === 'VIGESIMAL'
                ? 'Promedio de los estudiantes con nota (0 a 20).'
                : 'Nivel más frecuente entre los estudiantes con nota.'}{' '}
              «% con A o AD»: estudiantes con logro esperado o destacado. Solo
              cuentan las notas completas.
            </p>
          )}
        </CardContent>
      </Card>
      {seccionId && reporte && (
        <DetalleSeccion
          key={seccionId}
          seccionId={seccionId}
          periodoId={periodoId}
          competenciaIds={reporte.filas.map((f) => f.competenciaId)}
          onCerrar={() => setSeccionId(undefined)}
        />
      )}
    </div>
  );
};

const Celda = ({
  celda,
  escala,
}: {
  celda: CeldaResumen;
  escala: EscalaEvaluacion;
}) => {
  const porRegistrar = celda.pendientes > 0 && (
    <p className="text-caption-2 text-content-warning">
      {celda.pendientes} con notas por registrar
    </p>
  );
  if (celda.porcentajeLogro === null) {
    return (
      <div className="space-y-1">
        <span className="text-body-4 text-content-quartiary">— Sin notas</span>
        {porRegistrar}
      </div>
    );
  }
  return (
    <div className="space-y-1">
      <div className="flex items-center gap-2">
        {escala === 'VIGESIMAL' && (
          <span className="text-label-1 font-semibold text-content-primary">
            {celda.promedio.replace('.', ',')}/20
          </span>
        )}
        <NotaNivel nivel={celda.nivel} size="sm" />
      </div>
      <p className={cn('text-caption-1', colorLogro(celda.logro))}>
        {celda.porcentajeLogro} % con A o AD
      </p>
      <p className="text-caption-2 text-content-quartiary">
        {celda.logroEsperado} de {celda.conNota}
      </p>
      {porRegistrar}
    </div>
  );
};

// ---------- HU-64 ----------

/** «50 %» con la cantidad debajo; «—» si no hay calificaciones. */
const CeldaNivel = ({
  cantidad,
  calificaciones,
}: {
  cantidad: number;
  calificaciones: number;
}) => {
  const porcentaje = porcentajeDe(cantidad, calificaciones);
  if (porcentaje === null) {
    return <span className="text-content-quartiary">—</span>;
  }
  return (
    <span className="whitespace-nowrap">
      <span className="text-content-primary">{porcentaje} %</span>{' '}
      <span className="text-caption-2 text-content-quartiary">
        ({cantidad})
      </span>
    </span>
  );
};

const ReporteDelColegio = ({
  anio,
  periodoId,
  filtros,
}: {
  anio: AnioReporte;
  periodoId: string;
  filtros: React.ReactNode;
}) => {
  const consulta: FiltrosReporteInstitucional = {
    anioEscolarId: anio.id,
    periodoId: periodoId || undefined,
  };
  const { data: reporte, isLoading } = useReporteInstitucional(consulta);

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader
          action={
            <BotonPdf
              descargar={() => descargarReporteInstitucional(consulta)}
            />
          }
        >
          <CardTitle>Reporte institucional de notas</CardTitle>
          <p className="mt-1 text-body-4 text-content-quartiary">
            Resultados de las competencias por nivel de logro en todo el
            colegio: por grado, sección y área.
          </p>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-4 pb-6">
          {filtros}
        </CardContent>
      </Card>

      {isLoading || !reporte ? (
        <Cargando />
      ) : (
        <ResultadosDelColegio reporte={reporte} />
      )}
    </div>
  );
};

const ResultadosDelColegio = ({
  reporte,
}: {
  reporte: ReporteInstitucional;
}) => {
  const total = reporte.total;
  const porcentajeTotal = (cantidad: number) => {
    const p = porcentajeDe(cantidad, total.calificaciones);
    return p === null ? '—' : `${p} %`;
  };
  const deCalificaciones = (cantidad: number) =>
    total.calificaciones ? `${cantidad} de ${total.calificaciones}` : undefined;

  const seccionesPorCompletar = reporte.filas.filter(
    (f) => f.seccion !== null && f.distribucion.porCompletar > 0,
  );
  const areasConCalificaciones = reporte.areas.filter(
    (a) => a.distribucion.calificaciones > 0 || a.distribucion.porCompletar > 0,
  );
  const areasSinCalificaciones = reporte.areas
    .filter((a) => !areasConCalificaciones.includes(a))
    .map((a) => a.area);

  // Cada grado con su total arriba y sus secciones debajo; al final, todo el colegio.
  const grados = [...new Set(reporte.filas.map((f) => f.gradoCodigo))];
  const filasGrados: FilaDistribucion[] = grados.flatMap((codigo) => {
    const delGrado = reporte.filas.filter((f) => f.gradoCodigo === codigo);
    const totalGrado = delGrado.find((f) => f.seccion === null);
    return [
      ...(totalGrado
        ? [
            {
              clave: `${codigo}-total`,
              nombre: totalGrado.grado,
              tipo: 'grupo' as const,
              estudiantes: totalGrado.estudiantes,
              distribucion: totalGrado.distribucion,
            },
          ]
        : []),
      ...delGrado
        .filter((f) => f.seccion !== null)
        .map((f) => ({
          clave: `${codigo}-${f.seccion}`,
          nombre: `Sección ${f.seccion}`,
          tipo: 'detalle' as const,
          estudiantes: f.estudiantes,
          distribucion: f.distribucion,
        })),
    ];
  });

  return (
    <>
      {total.porCompletar > 0 && (
        <Alert
          type="warning"
          title={`${total.porCompletar} calificaciones por completar no se cuentan todavía`}
        >
          Falta registrar algún ítem en{' '}
          {seccionesPorCompletar
            .map(
              (f) =>
                `${f.grado} · Sección ${f.seccion} (${f.distribucion.porCompletar})`,
            )
            .join(', ')}
          . Se contarán cuando el docente las complete.
        </Alert>
      )}
      {reporte.sinCalcular.length > 0 && (
        <Alert type="warning" title="Competencias que no se pueden calcular">
          Los pesos de sus cursos no suman 100 %:{' '}
          {reporte.sinCalcular
            .map((c) => `${c.grado} · ${c.competencia}`)
            .join('; ')}
          . Ajústalos en el plan de evaluación.
        </Alert>
      )}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          icon={<Users className="size-5" />}
          value={reporte.estudiantes}
          label="Estudiantes"
          trend={`${total.calificaciones} calificaciones`}
          trendTone="neutral"
        />
        <StatCard
          icon={<TrendingUp className="size-5" />}
          iconColor="success"
          value={porcentajeTotal(total.ad + total.a)}
          label="Logro esperado o destacado"
          trend={deCalificaciones(total.ad + total.a)}
          trendTone="neutral"
        />
        <StatCard
          icon={<BarChart3 className="size-5" />}
          iconColor="warning"
          value={porcentajeTotal(total.b)}
          label="En proceso (B)"
          trend={deCalificaciones(total.b)}
          trendTone="neutral"
        />
        <StatCard
          icon={<TrendingDown className="size-5" />}
          iconColor="error"
          value={porcentajeTotal(total.c)}
          label="En inicio (C)"
          trend={deCalificaciones(total.c)}
          trendTone="neutral"
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Resultados por grado y sección</CardTitle>
          <p className="mt-1 text-body-4 text-content-quartiary">
            {reporte.periodo} · Año escolar {reporte.anio}
          </p>
        </CardHeader>
        <CardContent className="space-y-3 pb-6">
          {!reporte.filas.length ? (
            <Vacio texto="No hay secciones con plan de evaluación." />
          ) : (
            <>
              <LeyendaNiveles />
              <TablaDistribucion
                titulo="Resultados por grado y sección"
                primera="Grado y sección"
                conEstudiantes
                filas={[
                  ...filasGrados,
                  {
                    clave: 'colegio',
                    nombre: 'Todo el colegio',
                    tipo: 'total',
                    estudiantes: reporte.estudiantes,
                    distribucion: total,
                  },
                ]}
              />
            </>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Resultados por área</CardTitle>
          <p className="mt-1 text-body-4 text-content-quartiary">
            {reporte.periodo} · Año escolar {reporte.anio}
          </p>
        </CardHeader>
        <CardContent className="space-y-3 pb-6">
          {!areasConCalificaciones.length ? (
            <Vacio texto="Todavía no hay áreas con calificaciones." />
          ) : (
            <TablaDistribucion
              titulo="Resultados por área"
              primera="Área"
              filas={areasConCalificaciones.map((a) => ({
                clave: a.area,
                nombre: a.area,
                tipo: 'detalle',
                distribucion: a.distribucion,
              }))}
            />
          )}
          {areasSinCalificaciones.length > 0 && (
            <p className="text-body-4 text-content-quartiary">
              Aún sin calificaciones en este periodo:{' '}
              {areasSinCalificaciones.join(', ')}.
            </p>
          )}
        </CardContent>
      </Card>

      <p className="text-caption-1 text-content-quartiary">
        Cada estudiante recibe una calificación por competencia. Los porcentajes
        se calculan sobre las calificaciones completas; entre paréntesis, la
        cantidad. Notas calculadas al consultar el reporte.
      </p>
    </>
  );
};

type FilaDistribucion = {
  clave: string;
  nombre: string;
  /** grupo: total de un grado; detalle: una sección o un área; total: todo el colegio. */
  tipo: 'grupo' | 'detalle' | 'total';
  estudiantes?: number;
  distribucion: DistribucionNiveles;
};

const TablaDistribucion = ({
  titulo,
  primera,
  conEstudiantes,
  filas,
}: {
  titulo: string;
  primera: string;
  conEstudiantes?: boolean;
  filas: FilaDistribucion[];
}) => (
  <div className="overflow-x-auto rounded-lg border border-stroke-tertiary">
    <TableElement aria-label={titulo} className="min-w-[820px]">
      <TableHeader>
        <TableRow>
          <TableHead>{primera}</TableHead>
          {conEstudiantes && (
            <TableHead className="text-right">Estudiantes</TableHead>
          )}
          <TableHead
            className="text-right"
            title="Calificaciones de competencia completas"
          >
            Calificaciones
          </TableHead>
          <TableHead className="w-36">Distribución</TableHead>
          {NIVELES_REPORTE.map(({ nivel }) => (
            <TableHead key={nivel} className="text-right">
              <NotaNivel nivel={nivel} size="sm" />
            </TableHead>
          ))}
          <TableHead className="text-right" title="Logro esperado o destacado">
            A o AD
          </TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {filas.map((fila) => {
          const d = fila.distribucion;
          return (
            <TableRow
              key={fila.clave}
              className={cn(
                fila.tipo === 'grupo' && 'bg-surface-secondary font-semibold',
                fila.tipo === 'total' &&
                  'border-t-2 border-stroke-secondary bg-surface-tertiary font-semibold',
              )}
            >
              <TableCell
                className={cn(
                  'whitespace-nowrap text-content-primary',
                  fila.tipo === 'detalle' && conEstudiantes && 'pl-8',
                )}
              >
                {fila.nombre}
              </TableCell>
              {conEstudiantes && (
                <TableCell className="text-right">{fila.estudiantes}</TableCell>
              )}
              <TableCell className="text-right">
                <span className="flex flex-col items-end">
                  <span>{d.calificaciones}</span>
                  {d.porCompletar > 0 && (
                    <span className="whitespace-nowrap text-caption-2 font-normal text-content-warning">
                      +{d.porCompletar} por completar
                    </span>
                  )}
                </span>
              </TableCell>
              <TableCell>
                <BarraDistribucion conteo={d} />
              </TableCell>
              {NIVELES_REPORTE.map(({ nivel, clave }) => (
                <TableCell key={nivel} className="text-right">
                  <CeldaNivel
                    cantidad={d[clave]}
                    calificaciones={d.calificaciones}
                  />
                </TableCell>
              ))}
              <TableCell className="text-right font-semibold text-content-primary">
                {d.porcentajeLogro === null ? '—' : `${d.porcentajeLogro} %`}
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </TableElement>
  </div>
);
