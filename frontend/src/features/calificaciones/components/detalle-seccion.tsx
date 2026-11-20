import { FileDown, X } from 'lucide-react';
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
import { NotaNivel } from '@/components/ui/tag';
import { NotaResultado } from '@/types/api';

import { descargarLibretaMatricula, useNotasSeccion } from '../api/reportes';

type Props = {
  seccionId: string;
  /** '' = nota final del año. */
  periodoId: string;
  /** Las competencias del reporte, en su orden. */
  competenciaIds: string[];
  onCerrar: () => void;
};

/**
 * Detalle de una sección del reporte por curso y grado (solo el Coordinador): la nota oficial de cada competencia de
 * cada estudiante, en el periodo del reporte o la final, y su libreta. Solo lectura: las notas las cambia el docente.
 * No hay frame en Figma; usa la tabla y la pastilla de nivel del resto del módulo.
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

  return (
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
          Nota oficial de cada competencia
          {periodo ? ` en el ${periodo.nombre}` : ' (nota final del año)'}.
          Combina todos los cursos que la evalúan. Solo lectura: las notas las
          registra el docente.
        </p>
      </CardHeader>
      <CardContent className="pb-6">
        {isLoading || !notas ? (
          <div className="flex h-40 items-center justify-center">
            <Spinner />
          </div>
        ) : !notas.filas.length ? (
          <p className="py-8 text-center text-body-3 text-content-quartiary">
            No hay estudiantes matriculados en esta sección.
          </p>
        ) : (
          <div className="overflow-x-auto rounded-lg border border-stroke-tertiary">
            <TableElement
              className="table-fixed"
              style={{ minWidth: 240 + competencias.length * 150 + 150 }}
              aria-label="Notas por estudiante"
            >
              <colgroup>
                <col style={{ width: 240 }} />
                {competencias.map((c) => (
                  <col key={c.competenciaId} style={{ width: 150 }} />
                ))}
                <col style={{ width: 150 }} />
              </colgroup>
              <TableHeader>
                <TableRow>
                  <TableHead>Estudiante</TableHead>
                  {competencias.map((c) => (
                    <TableHead key={c.competenciaId} className="text-caption-1">
                      {c.competencia}
                    </TableHead>
                  ))}
                  <TableHead>Libreta</TableHead>
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
                        ? suya?.periodos.find((p) => p.periodoId === periodoId)
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
                    <TableCell>
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
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </TableElement>
          </div>
        )}
      </CardContent>
    </Card>
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
