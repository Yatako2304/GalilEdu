import { Alert } from '@/components/ui/alert';
import { Card } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import {
  TableBody,
  TableCell,
  TableElement,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { NotaFinalCompetencia } from '@/types/api';
import { cn } from '@/utils/cn';

import { SeleccionCurso, useNotaFinalCompetencia } from '../api/calificaciones';

const COMO_SE_CALCULA: Record<NotaFinalCompetencia['reglaNota'], string> = {
  PROMEDIO_PONDERADO: 'combina los cursos según su peso en el plan',
  PROMEDIO_SIMPLE: 'promedia los cursos, que pesan igual',
  NIVEL_MAS_FRECUENTE: 'toma el nivel más frecuente entre los cursos',
  ULTIMO_NIVEL: 'toma el nivel registrado más reciente entre los cursos',
};

const COMO_SE_CONSOLIDA: Record<NotaFinalCompetencia['consolidacion'], string> =
  {
    PROMEDIO_PERIODOS: 'consolida todos los periodos con notas',
    ULTIMO_PERIODO: 'usa el último periodo con notas',
  };

type Props = {
  seleccion: SeleccionCurso;
  competenciaId: string;
};

/** Nota de la competencia en el año, entre todos los cursos del grado que la evalúan. */
export const NotaFinalCompetenciaVista = ({
  seleccion,
  competenciaId,
}: Props) => {
  const { data: nota, isLoading } = useNotaFinalCompetencia(
    competenciaId ? { ...seleccion, competenciaId } : null,
  );

  if (!competenciaId) return null;

  return (
    <Card className="overflow-hidden">
      <div className="border-b border-stroke-tertiary px-5 py-4">
        <h2 className="text-label-1 font-medium text-content-primary">
          Nota final de la competencia
        </h2>
        {nota && (
          <p className="mt-1 text-body-4 text-content-quartiary">
            {nota.competencia}: {COMO_SE_CONSOLIDA[nota.consolidacion]} de cada
            curso y {COMO_SE_CALCULA[nota.reglaNota]}.
          </p>
        )}
      </div>

      {isLoading || !nota ? (
        <div className="flex h-40 items-center justify-center">
          {isLoading && <Spinner />}
        </div>
      ) : (
        <>
          {nota.reglaNota === 'PROMEDIO_PONDERADO' && !nota.pesosCompletos && (
            <div className="px-5 pt-4">
              <Alert
                type="warning"
                title="Los pesos de esta competencia están incompletos en el plan de evaluación."
              >
                La nota final se calcula cuando todos los cursos tengan peso y
                sumen 100 %.
              </Alert>
            </div>
          )}
          {nota.filas.length ? (
            <TablaNotaFinal nota={nota} />
          ) : (
            <div className="flex h-40 items-center justify-center px-5 text-center text-body-3 text-content-quartiary">
              No hay estudiantes matriculados para mostrar.
            </div>
          )}
        </>
      )}
    </Card>
  );
};

const TablaNotaFinal = ({ nota }: { nota: NotaFinalCompetencia }) => (
  <TableElement
    className="table-fixed"
    style={{ minWidth: 500 + nota.cursos.length * 160 }}
    aria-label={`Nota final de ${nota.competencia}`}
  >
    <colgroup>
      <col style={{ width: 260 }} />
      {nota.cursos.map((curso) => (
        <col key={curso.planCursoId} style={{ width: 160 }} />
      ))}
      <col style={{ width: 240 }} />
    </colgroup>
    <TableHeader className="bg-white">
      <TableRow>
        <TableHead>Estudiante</TableHead>
        {nota.cursos.map((curso) => (
          <TableHead key={curso.planCursoId}>
            {curso.curso}
            {nota.reglaNota === 'PROMEDIO_PONDERADO' && (
              <span className="block font-normal text-content-quartiary">
                {curso.pesoCurso == null
                  ? 'sin peso'
                  : `${formatearDecimal(String(curso.pesoCurso))} %`}
              </span>
            )}
          </TableHead>
        ))}
        <TableHead>Nota final</TableHead>
      </TableRow>
    </TableHeader>
    <TableBody>
      {nota.filas.map((fila) => (
        <TableRow key={fila.matriculaId}>
          <TableCell>
            <span className="font-medium text-content-primary">
              {fila.estudiante}
            </span>
          </TableCell>
          {nota.cursos.map((curso) => (
            <TableCell key={curso.planCursoId}>
              {formatearNota(fila.cursos[curso.planCursoId] ?? '—', nota)}
            </TableCell>
          ))}
          <TableCell>
            <span className="block font-semibold text-content-primary">
              {formatearNota(fila.notaFinal, nota)}
            </span>
            <span className="mt-1 flex flex-wrap items-center gap-2">
              <span
                className={cn(
                  'text-caption-1',
                  fila.estado === 'Aprobado'
                    ? 'text-content-success'
                    : fila.estado === 'Desaprobado'
                      ? 'text-content-error'
                      : 'text-content-quartiary',
                )}
              >
                {fila.estado}
              </span>
              {fila.parcial && (
                <Tag color="warning" appearance="soft" size="sm">
                  Parcial
                </Tag>
              )}
            </span>
          </TableCell>
        </TableRow>
      ))}
    </TableBody>
  </TableElement>
);

const formatearDecimal = (valor: string) => valor.replace('.', ',');

const formatearNota = (valor: string, nota: NotaFinalCompetencia) => {
  if (valor === '—' || nota.escala !== 'VIGESIMAL') return valor;
  return `${formatearDecimal(valor)}/20`;
};
