import { X } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { Tabs } from '@/components/ui/tabs';
import { NotasApoderado } from '@/types/api';

import { useNotasMatricula } from '../api/reportes';

import {
  CursosDelPeriodo,
  Leyenda,
  NotasPorCompetencia,
} from './calificaciones-apoderado';

/** El periodo en curso; si no hay, el último que ya empezó. */
const periodoInicial = (notas: NotasApoderado) =>
  notas.periodos.find((p) => p.estado === 'EN_CURSO') ??
  [...notas.periodos].reverse().find((p) => p.estado !== 'PLANIFICADO') ??
  notas.periodos[0];

/**
 * Notas de un estudiante para el Coordinador, desde el detalle de una sección: cada curso con los ítems que calificó
 * el docente en el periodo elegido, y la nota oficial de cada competencia en todos los periodos y la final. Es la misma
 * información que ve el apoderado (HU-69). Solo lectura: las notas las registra y modifica el docente.
 */
export const NotasEstudiante = ({
  matriculaId,
  onCerrar,
}: {
  matriculaId: string;
  onCerrar: () => void;
}) => {
  const { data: notas, isLoading } = useNotasMatricula(matriculaId);
  const [periodoId, setPeriodoId] = React.useState<string>();
  const titulo = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    setPeriodoId(undefined);
    titulo.current?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
  }, [matriculaId]);

  const periodo =
    notas?.periodos.find((p) => p.id === periodoId) ??
    (notas ? periodoInicial(notas) : undefined);

  return (
    <div ref={titulo} className="space-y-4">
      <Card>
        <CardHeader
          action={
            <Button
              variant="ghost"
              size="sm"
              icon={<X className="size-4" />}
              aria-label="Cerrar las notas del estudiante"
              onClick={onCerrar}
            />
          }
        >
          <CardTitle>
            {notas
              ? `${notas.estudiante} · ${notas.grado} · Sección ${notas.seccion}`
              : 'Notas del estudiante'}
          </CardTitle>
          <p className="mt-1 text-body-4 text-content-quartiary">
            Ítems calificados por cada docente y nota oficial de cada
            competencia. Solo lectura: las notas las registra el docente.
          </p>
        </CardHeader>
        {notas && periodo && (
          <div className="px-6 pb-5">
            <Tabs
              variant="pill"
              size="md"
              aria-label="Periodo"
              items={notas.periodos.map((p) => ({
                value: p.id,
                label: p.nombre,
                disabled: p.estado === 'PLANIFICADO',
                title: p.estado === 'PLANIFICADO' ? 'Por empezar' : undefined,
              }))}
              value={periodo.id}
              onValueChange={setPeriodoId}
            />
          </div>
        )}
      </Card>

      {isLoading || !notas ? (
        <div className="flex h-40 items-center justify-center">
          <Spinner />
        </div>
      ) : !notas.competencias.length ? (
        <Card className="py-8 text-center text-body-3 text-content-quartiary">
          El grado todavía no tiene un plan de evaluación con competencias.
        </Card>
      ) : (
        <>
          <Leyenda vigesimal={notas.escala === 'VIGESIMAL'} />
          {periodo && <CursosDelPeriodo notas={notas} periodoId={periodo.id} />}
          <NotasPorCompetencia notas={notas} paraCoordinador />
        </>
      )}
    </div>
  );
};
