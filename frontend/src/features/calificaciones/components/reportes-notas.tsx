import {
  BarChart3,
  FileDown,
  TrendingDown,
  TrendingUp,
  Users,
} from 'lucide-react';
import * as React from 'react';

import { Alert } from '@/components/ui/alert';
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
            Cuántas notas de competencia hay en cada nivel de logro, por grado,
            sección y área.
          </p>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-4 pb-6">
          {filtros}
        </CardContent>
      </Card>

      {isLoading || !reporte ? (
        <Cargando />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              icon={<Users className="size-5" />}
              value={reporte.estudiantes}
              label="Estudiantes evaluados"
            />
            <StatCard
              icon={<TrendingUp className="size-5" />}
              iconColor="success"
              value={
                reporte.total.porcentajeLogro === null
                  ? '—'
                  : `${reporte.total.porcentajeLogro} %`
              }
              label="Notas con logro esperado o destacado"
            />
            <StatCard
              icon={<BarChart3 className="size-5" />}
              iconColor="warning"
              value={reporte.total.b}
              label="Notas en proceso (B)"
            />
            <StatCard
              icon={<TrendingDown className="size-5" />}
              iconColor="error"
              value={reporte.total.c}
              label="Notas en inicio (C)"
            />
          </div>

          <Card>
            <CardHeader>
              <CardTitle>Por grado y sección · {reporte.periodo}</CardTitle>
            </CardHeader>
            <CardContent className="pb-6">
              {!reporte.filas.length ? (
                <Vacio texto="No hay secciones con plan de evaluación." />
              ) : (
                <TablaDistribucion
                  primera="Grado y sección"
                  conEstudiantes
                  filas={[
                    ...reporte.filas.map((f) => ({
                      clave: `${f.gradoCodigo}-${f.seccion ?? 'total'}`,
                      nombre: f.seccion
                        ? `${f.grado} · Sección ${f.seccion}`
                        : `${f.grado} (total)`,
                      total: f.seccion === null,
                      estudiantes: f.estudiantes,
                      distribucion: f.distribucion,
                    })),
                    {
                      clave: 'colegio',
                      nombre: 'Todo el colegio',
                      total: true,
                      estudiantes: reporte.estudiantes,
                      distribucion: reporte.total,
                    },
                  ]}
                />
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Por área</CardTitle>
            </CardHeader>
            <CardContent className="pb-6">
              {!reporte.areas.length ? (
                <Vacio texto="No hay áreas con competencias evaluadas." />
              ) : (
                <TablaDistribucion
                  primera="Área"
                  filas={reporte.areas.map((a) => ({
                    clave: a.area,
                    nombre: a.area,
                    distribucion: a.distribucion,
                  }))}
                />
              )}
            </CardContent>
          </Card>
          <p className="text-caption-1 text-content-quartiary">
            Cada nota oficial de competencia de cada estudiante cuenta una vez.
            Pendientes: falta registrar algún ítem, así que aún no hay nota
            (pídele al docente que lo complete). Sin notas: todavía no hay ítems
            que cuenten.
          </p>
        </>
      )}
    </div>
  );
};

type FilaDistribucion = {
  clave: string;
  nombre: string;
  total?: boolean;
  estudiantes?: number;
  distribucion: DistribucionNiveles;
};

const TablaDistribucion = ({
  primera,
  conEstudiantes,
  filas,
}: {
  primera: string;
  conEstudiantes?: boolean;
  filas: FilaDistribucion[];
}) => (
  <div className="overflow-x-auto rounded-lg border border-stroke-tertiary">
    <TableElement className="min-w-[720px]">
      <TableHeader>
        <TableRow>
          <TableHead className="w-[30%]">{primera}</TableHead>
          {conEstudiantes && <TableHead>Estudiantes</TableHead>}
          {(['AD', 'A', 'B', 'C'] as const).map((nivel) => (
            <TableHead key={nivel}>
              <NotaNivel nivel={nivel} size="sm" />
            </TableHead>
          ))}
          <TableHead title="Falta registrar algún ítem">Pendientes</TableHead>
          <TableHead title="Aún no hay ítems que cuenten">Sin notas</TableHead>
          <TableHead>% con A o AD</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {filas.map((fila) => {
          const d = fila.distribucion;
          return (
            <TableRow
              key={fila.clave}
              className={cn(fila.total && 'bg-surface-secondary font-semibold')}
            >
              <TableCell className="text-content-primary">
                {fila.nombre}
              </TableCell>
              {conEstudiantes && <TableCell>{fila.estudiantes}</TableCell>}
              <TableCell>{d.ad}</TableCell>
              <TableCell>{d.a}</TableCell>
              <TableCell>{d.b}</TableCell>
              <TableCell>{d.c}</TableCell>
              <TableCell
                className={cn(
                  d.pendientes
                    ? 'text-content-warning'
                    : 'text-content-quartiary',
                )}
              >
                {d.pendientes}
              </TableCell>
              <TableCell className="text-content-quartiary">
                {d.sinNotas}
              </TableCell>
              <TableCell>
                {d.porcentajeLogro === null ? '—' : `${d.porcentajeLogro} %`}
              </TableCell>
            </TableRow>
          );
        })}
      </TableBody>
    </TableElement>
  </div>
);
