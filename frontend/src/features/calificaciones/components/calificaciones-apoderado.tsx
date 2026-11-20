import { AlertTriangle, CalendarDays, FileDown, Trophy } from 'lucide-react';
import * as React from 'react';

import { ContentLayout } from '@/components/layouts';
import { Accordion } from '@/components/ui/accordion';
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
import { NotaNivel, SIGNIFICADO_NIVEL } from '@/components/ui/tag';
import { NotaResultado, NotasApoderado } from '@/types/api';
import { cn } from '@/utils/cn';

import {
  descargarLibreta,
  useHijosCalificaciones,
  useNotasApoderado,
} from '../api/reportes';

const Cargando = () => (
  <div className="flex h-48 w-full items-center justify-center">
    <Spinner size="lg" />
  </div>
);

const Vacio = ({ texto }: { texto: string }) => (
  <Card>
    <CardContent className="py-10 text-center text-body-3 text-content-quartiary">
      {texto}
    </CardContent>
  </Card>
);

const fecha = (iso: string) => {
  const [anio, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${anio}`;
};

/** «A · 14,2» en vigesimal; en la escala cualitativa basta la letra. */
const notaTexto = (nota: NotaResultado, vigesimal: boolean) =>
  vigesimal && nota.nivel ? nota.nota.replace('.', ',') : null;

/**
 * HU-69 · Figma > Apoderado > «HU-69 Consulta de notas propias por parte del apoderado» (467:7656) y «HU-69 ·
 * Resultado final del año escolar 2026» (1105:2091). Se respetan las decisiones del 16/11 y 18/11, que Figma aún no
 * refleja: la letra de cada curso es un resumen referencial (la nota oficial es de cada competencia y se muestra en
 * «Notas por competencia») y no hay «Aprobado» ni «cursos aprobados», sino niveles de logro.
 */
export const CalificacionesApoderado = () => {
  const { addNotification } = useNotifications();
  const hijosQuery = useHijosCalificaciones();
  const hijos = React.useMemo(() => hijosQuery.data ?? [], [hijosQuery.data]);
  const [estudianteId, setEstudianteId] = React.useState<string>();
  const [anio, setAnio] = React.useState<number>();
  const [periodoId, setPeriodoId] = React.useState<string>();
  const [descargando, setDescargando] = React.useState(false);

  const hijo = hijos.find((h) => h.estudianteId === estudianteId) ?? hijos[0];
  const anios = hijo?.matriculas.map((m) => m.anio) ?? [];
  const anioElegido = anio && anios.includes(anio) ? anio : anios[0];
  const notasQuery = useNotasApoderado(hijo?.estudianteId, anioElegido);
  const notas = notasQuery.data;

  // Empieza en el periodo en curso; si no hay, en el último que ya empezó.
  const periodoInicial =
    notas?.periodos.find((p) => p.estado === 'EN_CURSO') ??
    [...(notas?.periodos ?? [])]
      .reverse()
      .find((p) => p.estado !== 'PLANIFICADO') ??
    notas?.periodos[0];
  const periodo =
    notas?.periodos.find((p) => p.id === periodoId) ?? periodoInicial;

  const descargar = async () => {
    if (!hijo || !anioElegido) return;
    setDescargando(true);
    try {
      await descargarLibreta(hijo.estudianteId, anioElegido);
    } catch {
      addNotification({
        type: 'error',
        title: 'No se pudo descargar la libreta',
      });
    } finally {
      setDescargando(false);
    }
  };

  let contenido: React.ReactNode;
  if (hijosQuery.isLoading) {
    contenido = <Cargando />;
  } else if (!hijo) {
    contenido = <Vacio texto="No tienes estudiantes matriculados." />;
  } else {
    const matricula = hijo.matriculas.find((m) => m.anio === anioElegido);
    contenido = (
      <>
        <Card>
          <CardContent className="flex flex-wrap items-end gap-4 py-4">
            <Select
              label="Estudiante"
              wrapperClassName="min-w-72"
              value={hijo.estudianteId}
              options={hijos.map((h) => ({
                label: h.estudiante,
                value: h.estudianteId,
              }))}
              hint={
                matricula
                  ? `${matricula.grado} · Sección ${matricula.seccion}`
                  : undefined
              }
              onChange={(e) => {
                setEstudianteId(e.target.value);
                setAnio(undefined);
                setPeriodoId(undefined);
              }}
            />
          </CardContent>
        </Card>

        {notasQuery.isLoading || !notas ? (
          <Cargando />
        ) : (
          <>
            <AnioEscolar
              notas={notas}
              anios={anios}
              periodoId={periodo?.id}
              onAnio={(a) => {
                setAnio(a);
                setPeriodoId(undefined);
              }}
              onPeriodo={setPeriodoId}
            />
            {!notas.competencias.length ? (
              <Vacio texto="El grado todavía no tiene un plan de evaluación con competencias." />
            ) : (
              <>
                {notas.finalizado ? (
                  <ResultadoFinal notas={notas} />
                ) : (
                  periodo && (
                    <ResumenPeriodo notas={notas} periodoId={periodo.id} />
                  )
                )}
                <Leyenda vigesimal={notas.escala === 'VIGESIMAL'} />
                {periodo && (
                  <CursosDelPeriodo notas={notas} periodoId={periodo.id} />
                )}
                <NotasPorCompetencia notas={notas} />
              </>
            )}
          </>
        )}
      </>
    );
  }

  return (
    <ContentLayout
      title="Calificaciones"
      subtitle={
        notas
          ? notas.finalizado
            ? `Resultados finales del año escolar ${notas.anio}.`
            : `Notas por curso y competencia del año escolar ${notas.anio}.`
          : 'Notas por curso y competencia del año escolar.'
      }
      actions={
        <Button
          icon={<FileDown className="size-4" />}
          disabled={!notas || descargando}
          isLoading={descargando}
          onClick={descargar}
        >
          {notas?.finalizado
            ? 'Descargar libreta final (PDF)'
            : 'Descargar libreta (PDF)'}
        </Button>
      }
    >
      <div className="space-y-6">{contenido}</div>
    </ContentLayout>
  );
};

/** Figma: tarjeta «Año escolar 2026» con el selector del año y un botón por periodo. */
const AnioEscolar = ({
  notas,
  anios,
  periodoId,
  onAnio,
  onPeriodo,
}: {
  notas: NotasApoderado;
  anios: number[];
  periodoId: string | undefined;
  onAnio: (anio: number) => void;
  onPeriodo: (id: string) => void;
}) => {
  const primero = notas.periodos[0];
  const ultimo = notas.periodos[notas.periodos.length - 1];
  return (
    <Card>
      <CardHeader
        action={
          <Select
            label="Año académico"
            wrapperClassName="w-40"
            value={notas.anio}
            options={anios.map((a) => ({ label: a, value: a }))}
            onChange={(e) => onAnio(Number(e.target.value))}
          />
        }
      >
        <CardTitle>
          Año escolar {notas.anio}
          {notas.finalizado && ' · Finalizado'}
        </CardTitle>
        <p className="mt-1 text-body-4 text-content-quartiary">
          {notas.finalizado
            ? 'Todos los periodos están cerrados. Ya puedes ver la nota final.'
            : 'Selecciona un periodo para ver sus notas.'}
        </p>
      </CardHeader>
      <CardContent className="space-y-3 pb-5">
        <div
          role="group"
          aria-label="Periodos"
          className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4"
        >
          {notas.periodos.map((p) => {
            const elegido = p.id === periodoId;
            return (
              <button
                key={p.id}
                type="button"
                aria-pressed={elegido}
                disabled={p.estado === 'PLANIFICADO'}
                onClick={() => onPeriodo(p.id)}
                className={cn(
                  'rounded-lg border px-3 py-2 text-left text-body-4 transition-colors',
                  elegido
                    ? 'border-stroke-success bg-surface-success-light font-semibold text-content-success'
                    : p.estado === 'EN_CURSO'
                      ? 'border-stroke-accent-primary bg-surface-accent-light text-content-accent-primary'
                      : 'border-stroke-tertiary bg-surface-tertiary text-content-secondary hover:bg-surface-primary-hover',
                  'disabled:cursor-not-allowed disabled:opacity-60',
                )}
              >
                {p.nombre}
              </button>
            );
          })}
        </div>
        {primero && ultimo && (
          <div className="flex justify-between text-caption-1 text-content-quartiary">
            <span>Inicio {fecha(primero.fechaInicio)}</span>
            <span>Fin {fecha(ultimo.fechaFin)}</span>
          </div>
        )}
      </CardContent>
    </Card>
  );
};

const ESTADO_PERIODO = {
  PLANIFICADO: 'Por empezar',
  EN_CURSO: 'En curso',
  CERRADO: 'Cerrado',
} as const;

/** Figma: tres tarjetas del periodo. «Promedio del periodo» y «Cursos en proceso» se cuentan por competencia. */
const ResumenPeriodo = ({
  notas,
  periodoId,
}: {
  notas: NotasApoderado;
  periodoId: string;
}) => {
  const periodo = notas.periodos.find((p) => p.id === periodoId)!;
  const delPeriodo = notas.competencias.map((c) => ({
    competencia: c.competencia,
    nota: c.periodos.find((p) => p.periodoId === periodoId),
  }));
  const conNota = delPeriodo.filter((c) => c.nota?.nivel);
  const logradas = conNota.filter(
    (c) => c.nota?.nivel === 'A' || c.nota?.nivel === 'AD',
  );
  const porReforzar = conNota.filter(
    (c) => c.nota?.nivel === 'B' || c.nota?.nivel === 'C',
  );
  const pendientes = delPeriodo.filter((c) => c.nota?.pendiente).length;
  return (
    <div className="grid gap-4 md:grid-cols-3">
      <StatCard
        icon={<CalendarDays className="size-5" />}
        value={periodo.nombre}
        label={`${ESTADO_PERIODO[periodo.estado]} · ${fecha(periodo.fechaInicio)} – ${fecha(periodo.fechaFin)}`}
      />
      <StatCard
        icon={<Trophy className="size-5" />}
        iconColor="success"
        value={`${logradas.length} de ${delPeriodo.length}`}
        label={
          pendientes
            ? `Competencias con logro esperado o destacado · ${pendientes} con notas por registrar`
            : 'Competencias con logro esperado o destacado'
        }
      />
      <StatCard
        icon={<AlertTriangle className="size-5" />}
        iconColor="warning"
        value={porReforzar.length}
        label={
          porReforzar.length
            ? `Por reforzar: ${porReforzar.map((c) => c.competencia).join(', ')}`
            : 'Competencias en proceso o en inicio'
        }
      />
    </div>
  );
};

/**
 * Figma «Resultado final»: sin «Aprobado» (la situación final del año aún no se calcula, decisión del 18/11). Muestra
 * cuántas competencias terminaron en cada nivel de logro.
 */
const ResultadoFinal = ({ notas }: { notas: NotasApoderado }) => {
  const niveles = (['AD', 'A', 'B', 'C'] as const).map((nivel) => ({
    nivel,
    cantidad: notas.competencias.filter((c) => c.notaFinal.nivel === nivel)
      .length,
  }));
  const logradas = niveles
    .filter((n) => n.nivel === 'AD' || n.nivel === 'A')
    .reduce((suma, n) => suma + n.cantidad, 0);
  return (
    <Card className="border-stroke-success bg-surface-success-light">
      <CardContent className="flex flex-wrap items-center justify-between gap-6 py-5">
        <div className="flex items-center gap-4">
          <Trophy className="size-10 text-content-success" aria-hidden="true" />
          <div>
            <p className="text-body-4 text-content-tertiary">
              Resultado del año
            </p>
            <p className="text-h5 font-semibold text-content-success">
              {logradas} de {notas.competencias.length} competencias
            </p>
            <p className="text-body-4 text-content-secondary">
              con logro esperado o destacado en {notas.grado}.
            </p>
          </div>
        </div>
        <ul
          className="flex flex-wrap gap-3"
          aria-label="Competencias por nivel"
        >
          {niveles.map((n) => (
            <li
              key={n.nivel}
              className="flex items-center gap-2 rounded-lg bg-white px-3 py-2"
            >
              <NotaNivel nivel={n.nivel} size="sm" />
              <span className="text-label-2 font-semibold text-content-primary">
                {n.cantidad}
              </span>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  );
};

export const Leyenda = ({ vigesimal }: { vigesimal: boolean }) => (
  <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-caption-1 text-content-tertiary">
    <span>
      {vigesimal
        ? 'Las notas se registran de 0 a 20 y se convierten a letra:'
        : 'Las notas se registran en niveles de logro:'}
    </span>
    {(['AD', 'A', 'B', 'C'] as const).map((nivel) => (
      <span key={nivel} className="inline-flex items-center gap-1.5">
        <NotaNivel nivel={nivel} size="sm" />
        {SIGNIFICADO_NIVEL[nivel]}
      </span>
    ))}
  </div>
);

/** Figma: «Cursos · III Bimestre», cada curso desplegable con sus competencias e ítems. */
export const CursosDelPeriodo = ({
  notas,
  periodoId,
}: {
  notas: NotasApoderado;
  periodoId: string;
}) => {
  const periodo = notas.periodos.find((p) => p.id === periodoId)!;
  const vigesimal = notas.escala === 'VIGESIMAL';
  return (
    <Card>
      <CardHeader>
        <CardTitle>Cursos · {periodo.nombre}</CardTitle>
        <p className="mt-1 text-body-4 text-content-quartiary">
          La letra de cada curso es un resumen referencial de sus ítems. La nota
          oficial es la de cada competencia.
        </p>
      </CardHeader>
      <CardContent className="space-y-2 pb-6">
        {!notas.cursos.length && (
          <p className="py-6 text-center text-body-3 text-content-quartiary">
            El grado aún no tiene cursos con competencias.
          </p>
        )}
        {notas.cursos.map((curso) => {
          const resumen = curso.periodos.find(
            (p) => p.periodoId === periodoId,
          )?.resultado;
          return (
            <Accordion
              key={curso.planCursoId}
              title={
                <span className="flex w-full items-center justify-between gap-4">
                  <span>
                    <span className="block text-label-1 font-semibold text-content-primary">
                      {curso.curso}
                    </span>
                    <span className="block text-caption-1 font-normal text-content-quartiary">
                      {curso.docente
                        ? `Prof. ${curso.docente}`
                        : 'Docente por asignar'}
                    </span>
                  </span>
                  <span className="flex items-center gap-3 text-caption-1 font-normal text-content-tertiary">
                    {curso.competencias.length}{' '}
                    {curso.competencias.length === 1
                      ? 'competencia'
                      : 'competencias'}
                    {resumen?.pendiente ? (
                      <span className="text-content-quartiary">Pendiente</span>
                    ) : (
                      <NotaNivel nivel={resumen?.nivel ?? null} />
                    )}
                  </span>
                </span>
              }
            >
              <ul className="space-y-3 rounded-lg bg-surface-secondary p-3">
                {curso.competencias.map((competencia) => {
                  const aporte = competencia.periodos.find(
                    (p) => p.periodoId === periodoId,
                  );
                  return (
                    <li key={competencia.competenciaId}>
                      <div className="flex items-center justify-between gap-3">
                        <p className="text-label-2 font-medium text-content-primary">
                          {competencia.competencia}
                        </p>
                        {aporte?.aporte.pendiente ? (
                          <Pendiente nota={aporte.aporte} />
                        ) : (
                          <span className="flex items-center gap-2 text-caption-1 text-content-tertiary">
                            {aporte && notaTexto(aporte.aporte, vigesimal)}
                            <NotaNivel nivel={aporte?.aporte.nivel ?? null} />
                          </span>
                        )}
                      </div>
                      {aporte?.items.length ? (
                        <ul className="mt-2 space-y-1 pl-4">
                          {aporte.items.map((item) => (
                            <li
                              key={item.itemId}
                              className="flex items-center justify-between gap-3 text-body-4 text-content-secondary"
                            >
                              {item.item}
                              {item.nivel === null ? (
                                // «No evaluado» (no cuenta para el estudiante) o aún sin registrar.
                                <span className="text-caption-1 text-content-quartiary">
                                  {item.nota === '—'
                                    ? 'Por registrar'
                                    : item.nota}
                                </span>
                              ) : (
                                <span className="flex items-center gap-2 text-caption-1 text-content-quartiary">
                                  {vigesimal && item.nota.replace('.', ',')}
                                  <NotaNivel nivel={item.nivel} size="sm" />
                                </span>
                              )}
                            </li>
                          ))}
                        </ul>
                      ) : (
                        <p className="mt-1 pl-4 text-caption-1 text-content-quartiary">
                          Sin ítems de evaluación en este periodo.
                        </p>
                      )}
                    </li>
                  );
                })}
              </ul>
            </Accordion>
          );
        })}
      </CardContent>
    </Card>
  );
};

/** La nota oficial: cada competencia por periodo y la nota final (Figma «Notas finales por curso», por competencia). */
export const NotasPorCompetencia = ({
  notas,
  paraCoordinador,
}: {
  notas: NotasApoderado;
  /** El aviso de pesos incompletos le dice al Coordinador qué ajustar, en vez de hablarle al apoderado. */
  paraCoordinador?: boolean;
}) => {
  const vigesimal = notas.escala === 'VIGESIMAL';
  const sinCalcular = notas.competencias.some((c) => !c.calculable);
  return (
    <Card>
      <CardHeader>
        <CardTitle>
          {notas.finalizado
            ? 'Notas finales por competencia'
            : 'Notas por competencia'}
        </CardTitle>
        <p className="mt-1 text-body-4 text-content-quartiary">
          Nota oficial de cada competencia: combina todos los cursos que la
          evalúan. La nota final se expresa en nivel de logro.
        </p>
      </CardHeader>
      <CardContent className="space-y-4 pb-6">
        {sinCalcular && (
          <Alert type="warning" title="Notas oficiales pendientes">
            {paraCoordinador
              ? 'Algunas competencias no tienen nota oficial porque los pesos de sus cursos no suman 100 %. Ajústalos en el plan de evaluación.'
              : 'Algunas competencias todavía no tienen nota oficial: el colegio está terminando de configurar cuánto aporta cada curso.'}
          </Alert>
        )}
        <div className="overflow-x-auto rounded-lg border border-stroke-tertiary">
          <TableElement className="min-w-[720px]">
            <TableHeader>
              <TableRow>
                <TableHead className="w-[36%]">Competencia</TableHead>
                {notas.periodos.map((p) => (
                  <TableHead key={p.id}>{p.nombre}</TableHead>
                ))}
                <TableHead>Nota final</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {notas.competencias.map((c) => (
                <TableRow key={c.competenciaId}>
                  <TableCell>
                    <p className="font-medium text-content-primary">
                      {c.competencia}
                    </p>
                    <p className="text-caption-1 text-content-quartiary">
                      {c.area} · {c.cursos.join(', ')}
                    </p>
                  </TableCell>
                  {c.periodos.map((p) => (
                    <TableCell key={p.periodoId}>
                      <NotaCelda nota={p} vigesimal={vigesimal} />
                    </TableCell>
                  ))}
                  <TableCell>
                    <NotaCelda nota={c.notaFinal} vigesimal={vigesimal} final />
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </TableElement>
        </div>
      </CardContent>
    </Card>
  );
};

/** Una nota oficial que espera registros: sin número ni letra, con cuántos ítems ya tienen nota. */
const Pendiente = ({ nota }: { nota: NotaResultado }) => (
  <span className="text-caption-1 text-content-quartiary">
    Pendiente · {nota.calificadas} de {nota.total} ítems
  </span>
);

const NotaCelda = ({
  nota,
  vigesimal,
  final,
}: {
  nota: NotaResultado;
  vigesimal: boolean;
  final?: boolean;
}) =>
  nota.pendiente ? (
    <Pendiente nota={nota} />
  ) : (
    <span className="inline-flex items-center gap-2">
      <NotaNivel nivel={nota.nivel} size={final ? 'md' : 'sm'} />
      {notaTexto(nota, vigesimal) && (
        <span className="text-caption-1 text-content-tertiary">
          {notaTexto(nota, vigesimal)}
        </span>
      )}
    </span>
  );
