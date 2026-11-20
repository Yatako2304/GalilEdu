import { Eye, FileDown, X } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useNotifications } from '@/components/ui/notifications';
import { Spinner } from '@/components/ui/spinner';
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
import { NotaResultado, NotasCompetencias } from '@/types/api';
import { cn } from '@/utils/cn';

import { descargarLibretaMatricula, useNotasSeccion } from '../api/reportes';

import { NotasEstudiante } from './notas-estudiante';

type Props = {
  seccionId: string;
  /** '' = nota final del año. */
  periodoId: string;
  /** Las competencias del reporte, en su orden. */
  competenciaIds: string[];
  onCerrar: () => void;
};

type Vista = 'reporte' | 'anio';

const ROMANOS = ['I', 'II', 'III', 'IV', 'V', 'VI'];

/** «III» para el III Bimestre (con el nombre completo en el título de la columna). */
const corto = (periodo: { numero: number; nombre: string }) =>
  ROMANOS[periodo.numero - 1] ?? String(periodo.numero);

/**
 * Detalle de una sección del reporte por curso y grado (solo el Coordinador): la nota oficial de cada competencia de
 * cada estudiante, en el periodo del reporte o en todo el año (cada periodo y la final, como un registro consolidado),
 * su libreta y sus ítems. Solo lectura: las notas las cambia el docente. No hay frame en Figma; usa la tabla y la
 * pastilla de nivel del resto del módulo.
 */
export const DetalleSeccion = ({
  seccionId,
  periodoId,
  competenciaIds,
  onCerrar,
}: Props) => {
  const { data, isLoading } = useNotasSeccion(seccionId);
  const { addNotification } = useNotifications();
  const [descargando, setDescargando] = React.useState<string>();
  const [vista, setVista] = React.useState<Vista>('reporte');
  const [estudiante, setEstudiante] = React.useState<string | null>(null);

  React.useEffect(() => setEstudiante(null), [seccionId]);

  const descargar = async (matriculaId: string) => {
    setDescargando(matriculaId);
    try {
      await descargarLibretaMatricula(matriculaId);
    } catch {
      addNotification({
        type: 'error',
        title: 'No se pudo descargar la libreta',
      });
    } finally {
      setDescargando(undefined);
    }
  };

  const notas = data?.notas;
  const periodo = notas?.periodos.find((p) => p.id === periodoId);
  const competencias =
    notas?.competencias.filter((c) =>
      competenciaIds.includes(c.competenciaId),
    ) ?? [];
  const vigesimal = notas?.escala === 'VIGESIMAL';

  const acciones = (fila: NotasCompetencias['filas'][number]) => (
    <span className="flex items-center gap-1">
      <Button
        variant="ghost"
        size="sm"
        icon={<Eye className="size-4" />}
        aria-label={`Ver los ítems de ${fila.estudiante}`}
        aria-pressed={estudiante === fila.matriculaId}
        onClick={() => setEstudiante(fila.matriculaId)}
      >
        Ítems
      </Button>
      <Button
        variant="ghost"
        size="sm"
        icon={<FileDown className="size-4" />}
        aria-label={`Descargar la libreta de ${fila.estudiante}`}
        isLoading={descargando === fila.matriculaId}
        disabled={!!descargando}
        onClick={() => descargar(fila.matriculaId)}
      >
        PDF
      </Button>
    </span>
  );

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader
          action={
            <Button
              variant="ghost"
              size="sm"
              icon={<X className="size-4" />}
              aria-label="Cerrar el detalle de la sección"
              onClick={onCerrar}
            />
          }
        >
          <CardTitle>
            {data
              ? `${data.grado} · Sección ${data.seccion} · notas por estudiante`
              : 'Notas por estudiante'}
          </CardTitle>
          <p className="mt-1 text-body-4 text-content-quartiary">
            {vista === 'anio'
              ? 'Nota oficial de cada competencia en cada periodo y la nota final del año.'
              : `Nota oficial de cada competencia${
                  periodo ? ` en el ${periodo.nombre}` : ' (nota final del año)'
                }.`}{' '}
            Combina todos los cursos que la evalúan. Solo lectura: las notas las
            registra el docente.
          </p>
        </CardHeader>
        <CardContent className="space-y-4 pb-6">
          <Tabs
            variant="pill"
            size="md"
            aria-label="Vista del detalle"
            items={[
              {
                value: 'reporte',
                label: periodo ? periodo.nombre : 'Nota final',
              },
              { value: 'anio', label: 'Todo el año' },
            ]}
            value={vista}
            onValueChange={setVista}
          />
          {isLoading || !notas ? (
            <div className="flex h-40 items-center justify-center">
              <Spinner />
            </div>
          ) : !notas.filas.length ? (
            <p className="py-8 text-center text-body-3 text-content-quartiary">
              No hay estudiantes matriculados en esta sección.
            </p>
          ) : vista === 'anio' ? (
            <TablaAnio
              notas={notas}
              competencias={competencias}
              vigesimal={vigesimal}
              acciones={acciones}
            />
          ) : (
            <div className="overflow-x-auto rounded-lg border border-stroke-tertiary">
              <TableElement
                className="table-fixed"
                style={{ minWidth: 240 + competencias.length * 150 + 190 }}
                aria-label="Notas por estudiante"
              >
                <colgroup>
                  <col style={{ width: 240 }} />
                  {competencias.map((c) => (
                    <col key={c.competenciaId} style={{ width: 150 }} />
                  ))}
                  <col style={{ width: 190 }} />
                </colgroup>
                <TableHeader>
                  <TableRow>
                    <TableHead>Estudiante</TableHead>
                    {competencias.map((c) => (
                      <TableHead
                        key={c.competenciaId}
                        className="text-caption-1"
                      >
                        {c.competencia}
                      </TableHead>
                    ))}
                    <TableHead>Acciones</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {notas.filas.map((fila) => (
                    <TableRow key={fila.matriculaId}>
                      <TableCell className="font-medium text-content-primary">
                        {fila.estudiante}
                      </TableCell>
                      {competencias.map((c) => {
                        const suya = fila.competencias.find(
                          (x) => x.competenciaId === c.competenciaId,
                        );
                        const nota = periodoId
                          ? suya?.periodos.find(
                              (p) => p.periodoId === periodoId,
                            )
                          : suya?.notaFinal;
                        return (
                          <TableCell key={c.competenciaId}>
                            {nota ? (
                              <Nota nota={nota} vigesimal={vigesimal} />
                            ) : (
                              '—'
                            )}
                          </TableCell>
                        );
                      })}
                      <TableCell>{acciones(fila)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </TableElement>
            </div>
          )}
        </CardContent>
      </Card>

      {estudiante && (
        <NotasEstudiante
          matriculaId={estudiante}
          onCerrar={() => setEstudiante(null)}
        />
      )}
    </div>
  );
};

/**
 * «Todo el año»: por competencia, el nivel de cada periodo y la nota final, como un registro consolidado. Solo la letra
 * (el número va en el título de la celda) para que quepan todos los periodos.
 */
const TablaAnio = ({
  notas,
  competencias,
  vigesimal,
  acciones,
}: {
  notas: NotasCompetencias;
  competencias: NotasCompetencias['competencias'];
  vigesimal: boolean;
  acciones: (fila: NotasCompetencias['filas'][number]) => React.ReactNode;
}) => {
  const columnas = notas.periodos.length + 1;
  return (
    <div className="overflow-x-auto rounded-lg border border-stroke-tertiary">
      <TableElement
        aria-label="Notas de todo el año por estudiante"
        style={{ minWidth: 240 + competencias.length * columnas * 52 + 190 }}
      >
        <TableHeader>
          <TableRow>
            <TableHead rowSpan={2} className="align-bottom">
              Estudiante
            </TableHead>
            {competencias.map((c) => (
              <TableHead
                key={c.competenciaId}
                colSpan={columnas}
                className="border-l border-stroke-tertiary text-center text-caption-1"
              >
                {c.competencia}
              </TableHead>
            ))}
            <TableHead rowSpan={2} className="align-bottom">
              Acciones
            </TableHead>
          </TableRow>
          <TableRow>
            {competencias.flatMap((c) => [
              ...notas.periodos.map((p, i) => (
                <TableHead
                  key={`${c.competenciaId}-${p.id}`}
                  title={p.nombre}
                  className={cn(
                    'px-1 text-center text-caption-1',
                    i === 0 && 'border-l border-stroke-tertiary',
                  )}
                >
                  {corto(p)}
                </TableHead>
              )),
              <TableHead
                key={`${c.competenciaId}-final`}
                className="bg-surface-secondary px-1 text-center text-caption-1"
              >
                Final
              </TableHead>,
            ])}
          </TableRow>
        </TableHeader>
        <TableBody>
          {notas.filas.map((fila) => (
            <TableRow key={fila.matriculaId}>
              <TableCell className="whitespace-nowrap font-medium text-content-primary">
                {fila.estudiante}
              </TableCell>
              {competencias.flatMap((c) => {
                const suya = fila.competencias.find(
                  (x) => x.competenciaId === c.competenciaId,
                );
                return [
                  ...notas.periodos.map((p, i) => (
                    <TableCell
                      key={`${c.competenciaId}-${p.id}`}
                      className={cn(
                        'px-1 text-center',
                        i === 0 && 'border-l border-stroke-tertiary',
                      )}
                    >
                      <NotaCorta
                        nota={suya?.periodos.find((x) => x.periodoId === p.id)}
                        periodo={p.nombre}
                        vigesimal={vigesimal}
                      />
                    </TableCell>
                  )),
                  <TableCell
                    key={`${c.competenciaId}-final`}
                    className="bg-surface-secondary px-1 text-center"
                  >
                    <NotaCorta
                      nota={suya?.notaFinal}
                      periodo="Nota final"
                      vigesimal={vigesimal}
                    />
                  </TableCell>,
                ];
              })}
              <TableCell>{acciones(fila)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </TableElement>
    </div>
  );
};

/** Solo la letra; «Pend.» si falta registrar algún ítem; «—» sin notas. El detalle va en el título. */
const NotaCorta = ({
  nota,
  periodo,
  vigesimal,
}: {
  nota: NotaResultado | undefined;
  periodo: string;
  vigesimal: boolean;
}) => {
  if (nota?.pendiente) {
    return (
      <span
        className="text-caption-2 text-content-quartiary"
        title={`${periodo}: pendiente · ${nota.calificadas} de ${nota.total} ítems`}
      >
        Pend.
      </span>
    );
  }
  if (!nota?.nivel) {
    return (
      <span className="text-content-quartiary" title={`${periodo}: sin notas`}>
        —
      </span>
    );
  }
  return (
    <span
      title={`${periodo}: ${nota.nivel}${vigesimal ? ` (${nota.nota.replace('.', ',')})` : ''}`}
    >
      <NotaNivel nivel={nota.nivel} size="sm" />
    </span>
  );
};

const Nota = ({
  nota,
  vigesimal,
}: {
  nota: NotaResultado;
  vigesimal: boolean;
}) => {
  if (nota.pendiente) {
    return (
      <span className="text-caption-1 text-content-quartiary">
        Pendiente · {nota.calificadas} de {nota.total} ítems
      </span>
    );
  }
  if (!nota.nivel) {
    return <span className="text-content-quartiary">—</span>;
  }
  return (
    <span className="inline-flex items-center gap-2">
      <NotaNivel nivel={nota.nivel} size="sm" />
      {vigesimal && (
        <span className="text-caption-1 text-content-tertiary">
          {nota.nota.replace('.', ',')}
        </span>
      )}
    </span>
  );
};
