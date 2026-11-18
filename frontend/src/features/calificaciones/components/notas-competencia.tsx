import * as React from 'react';

import { Alert } from '@/components/ui/alert';
import { Card } from '@/components/ui/card';
import { Switch } from '@/components/ui/form';
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
import {
  CompetenciaNotas,
  NotaResultado,
  NotasCompetencias,
} from '@/types/api';
import { cn } from '@/utils/cn';

import { SeleccionCurso, useNotasCompetencia } from '../api/calificaciones';

import { colorLogro } from './logro';

const CONSOLIDACION: Record<NotasCompetencias['consolidacion'], string> = {
  PROMEDIO_PERIODOS: 'el promedio de los periodos',
  ULTIMO_PERIODO: 'la nota del último periodo',
};

type Props = {
  seleccion: SeleccionCurso;
  competenciaIds: string[];
};

/**
 * Nota oficial de la competencia: una por periodo y una final. Combina todos los cursos del grado que la evalúan con
 * el peso del plan de evaluación (solo en promedio ponderado). El «Promedio final» del consolidado es el aporte de
 * este curso en un periodo; esta tarjeta es la nota que va a la libreta.
 */
export const NotasCompetencia = ({ seleccion, competenciaIds }: Props) => {
  const { data, isLoading } = useNotasCompetencia(seleccion, competenciaIds);
  const [verAportes, setVerAportes] = React.useState(false);

  return (
    <Card className="overflow-hidden">
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-stroke-tertiary px-5 py-4">
        <div>
          <h2 className="text-label-1 font-medium text-content-primary">
            Nota de la competencia
          </h2>
          <p className="mt-1 max-w-2xl text-body-4 text-content-quartiary">
            Es la nota oficial: una por periodo y una final
            {data ? ` (${CONSOLIDACION[data.consolidacion]})` : ''}. Combina
            todos los cursos del grado que evalúan la competencia; tu «Promedio
            final» es el aporte de este curso.
          </p>
          <p className="mt-1 max-w-2xl text-body-4 text-content-quartiary">
            La nota final va en nivel de logro (AD, A, B, C): la nota se
            redondea al entero (0,5 sube) y se ubica en la tabla de conversión
            vigente.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <span
            id="etiqueta-ver-aportes"
            className="text-label-2 font-medium text-content-primary"
          >
            Ver el aporte de cada curso
          </span>
          <Switch
            checked={verAportes}
            onCheckedChange={setVerAportes}
            aria-labelledby="etiqueta-ver-aportes"
          />
        </div>
      </div>

      {isLoading || !data ? (
        <div className="flex h-40 items-center justify-center">
          <Spinner />
        </div>
      ) : (
        <div className="space-y-6 py-5">
          {data.competencias.map((competencia, indice) => (
            <TablaCompetencia
              key={competencia.competenciaId}
              datos={data}
              competencia={competencia}
              indice={indice}
              verAportes={verAportes}
            />
          ))}
        </div>
      )}
    </Card>
  );
};

const TablaCompetencia = ({
  datos,
  competencia,
  indice,
  verAportes,
}: {
  datos: NotasCompetencias;
  competencia: CompetenciaNotas;
  indice: number;
  verAportes: boolean;
}) => {
  const nombreCurso = Object.fromEntries(
    competencia.cursos.map((c) => [c.planCursoId, c.curso]),
  );
  const repartido = competencia.usaPesos
    ? competencia.cursos
        .map(
          (c) =>
            `${c.curso} ${c.pesoCurso == null ? 'sin peso' : `${String(c.pesoCurso).replace('.', ',')} %`}`,
        )
        .join(' · ')
    : `${competencia.cursos.map((c) => c.curso).join(', ')}: aportan por igual`;

  return (
    <section aria-label={competencia.competencia} className="space-y-3 px-5">
      <div className="flex flex-wrap items-center gap-2">
        <h3 className="text-label-2 font-semibold text-content-primary">
          {competencia.competencia}
        </h3>
        <Tag color="neutral" size="sm">
          {repartido}
        </Tag>
      </div>
      {competencia.usaPesos && !competencia.pesosCompletos && (
        <Alert
          type="warning"
          size="sm"
          title="Los pesos de los cursos no suman 100 %."
        >
          La nota de la competencia se calculará cuando el plan de evaluación
          complete el peso de cada curso. Mientras tanto se ve el aporte de cada
          uno.
        </Alert>
      )}
      <div className="overflow-x-auto">
        <TableElement
          className="table-fixed"
          style={{ minWidth: 260 + (datos.periodos.length + 1) * 150 }}
          aria-label={`Nota de la competencia ${competencia.competencia}`}
        >
          <colgroup>
            <col style={{ width: 260 }} />
            {datos.periodos.map((p) => (
              <col key={p.id} style={{ width: 150 }} />
            ))}
            <col style={{ width: 150 }} />
          </colgroup>
          <TableHeader className="bg-white">
            <TableRow>
              <TableHead>Estudiante</TableHead>
              {datos.periodos.map((p) => (
                <TableHead key={p.id}>{p.nombre}</TableHead>
              ))}
              <TableHead>Nota final (nivel de logro)</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {datos.filas.map((fila) => {
              const notas = fila.competencias[indice];
              return (
                <TableRow key={fila.matriculaId}>
                  <TableCell>
                    <span className="font-medium text-content-primary">
                      {fila.estudiante}
                    </span>
                  </TableCell>
                  {notas.periodos.map((periodo) => (
                    <TableCell key={periodo.periodoId}>
                      <Celda nota={periodo} escala={datos.escala} />
                      {verAportes && (
                        <ul className="mt-1 space-y-0.5 text-caption-1 text-content-tertiary">
                          {periodo.aportes.map((a) => (
                            <li key={a.planCursoId}>
                              {nombreCurso[a.planCursoId]}:{' '}
                              {formatearNota(a.nota, datos.escala)}
                            </li>
                          ))}
                        </ul>
                      )}
                    </TableCell>
                  ))}
                  <TableCell>
                    <Celda nota={notas.notaFinal} escala={datos.escala} final />
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </TableElement>
      </div>
    </section>
  );
};

const Celda = ({
  nota,
  escala,
  final = false,
}: {
  nota: NotaResultado;
  escala: NotasCompetencias['escala'];
  final?: boolean;
}) => (
  <>
    {final && nota.nivel ? (
      // La nota final de la competencia va en nivel de logro (AD, A, B, C); el número queda como referencia.
      <>
        <span
          className="block text-label-1 font-semibold text-content-primary"
          aria-label={`Nivel de logro ${nota.nivel}`}
        >
          {nota.nivel}
        </span>
        {escala === 'VIGESIMAL' && (
          <span className="block text-caption-1 text-content-tertiary">
            {formatearNota(nota.nota, escala)}
          </span>
        )}
      </>
    ) : (
      <span className="block text-content-primary">
        {formatearNota(nota.nota, escala)}
        {nota.nivel && escala === 'VIGESIMAL' && (
          <span className="text-content-tertiary"> · {nota.nivel}</span>
        )}
      </span>
    )}
    {nota.nota !== '—' && (
      <span className={cn('mt-1 block text-caption-1', colorLogro(nota.logro))}>
        {nota.logro}
        {nota.parcial && (
          <span className="text-content-quartiary"> · Parcial</span>
        )}
      </span>
    )}
  </>
);

const formatearNota = (valor: string, escala: NotasCompetencias['escala']) => {
  if (valor === '—' || escala !== 'VIGESIMAL') return valor;
  return `${valor.replace('.', ',')}/20`;
};
