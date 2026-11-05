import { FileDown, FileText } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Select } from '@/components/ui/form';
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
import { Tag } from '@/components/ui/tag';
import {
  CompetenciaOpcion,
  ItemEvaluacion,
  TipoReporteCalificaciones,
} from '@/types/api';

import {
  exportarReporte,
  FiltrosReporte,
  SeleccionPeriodo,
  useReporteCalificaciones,
} from '../api/calificaciones';

type Props = {
  seleccion: SeleccionPeriodo;
  competencias: CompetenciaOpcion[];
  items: ItemEvaluacion[];
};

export const ReporteCalificacionesVista = ({
  seleccion,
  competencias,
  items,
}: Props) => {
  const [tipo, setTipo] = React.useState<TipoReporteCalificaciones>('ITEM');
  const [competenciaId, setCompetenciaId] = React.useState('');
  const itemsCompetencia = competenciaId
    ? items.filter((item) =>
        item.competencias.some((c) => c.competenciaId === competenciaId),
      )
    : items;
  const [itemId, setItemId] = React.useState('');

  React.useEffect(() => {
    if (!competencias.some((c) => c.id === competenciaId)) {
      setCompetenciaId(competencias[0]?.id ?? '');
    }
  }, [competenciaId, competencias]);

  React.useEffect(() => {
    if (!itemsCompetencia.some((item) => item.id === itemId)) {
      setItemId(itemsCompetencia[0]?.id ?? '');
    }
  }, [itemId, itemsCompetencia]);

  const objetivoId =
    tipo === 'ITEM'
      ? itemId
      : tipo === 'COMPETENCIA'
        ? competenciaId
        : undefined;
  const filtros: FiltrosReporte | null =
    tipo === 'PERIODO' || objetivoId
      ? { ...seleccion, tipo, objetivoId }
      : null;
  const { data: reporte, isLoading } = useReporteCalificaciones(filtros);

  return (
    <Card className="overflow-hidden">
      <div className="space-y-4 border-b border-stroke-tertiary px-5 py-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-h6 text-content-primary">Ver calificaciones</h2>
          </div>
          <Button
            icon={<FileDown className="size-4" />}
            disabled={!filtros || !reporte?.filas.length}
            onClick={() => filtros && exportarReporte(filtros)}
          >
            Exportar calificaciones
          </Button>
        </div>
        <Tabs
          aria-label="Tipo de reporte"
          fullWidth
          value={tipo}
          onValueChange={setTipo}
          items={[
            { value: 'ITEM', label: 'Por ítem' },
            { value: 'COMPETENCIA', label: 'Por competencia' },
            { value: 'PERIODO', label: 'Por periodo' },
          ]}
        />
        {tipo !== 'PERIODO' && (
          <div className="grid gap-4 md:grid-cols-2">
            <Select
              label="Competencia"
              value={competenciaId}
              options={competencias.map((c) => ({
                label: c.nombre,
                value: c.id,
              }))}
              onChange={(e) => setCompetenciaId(e.target.value)}
            />
            {tipo === 'ITEM' && (
              <Select
                label="Ítem de calificación"
                value={itemId}
                options={itemsCompetencia.map((item) => ({
                  label: item.nombre,
                  value: item.id,
                }))}
                onChange={(e) => setItemId(e.target.value)}
                disabled={!itemsCompetencia.length}
              />
            )}
          </div>
        )}
      </div>

      {isLoading ? (
        <div className="flex h-56 items-center justify-center">
          <Spinner />
        </div>
      ) : !reporte ? (
        <div className="flex h-56 items-center justify-center px-5 text-center text-body-3 text-content-quartiary">
          Selecciona una opción con ítems registrados para ver el reporte.
        </div>
      ) : (
        <>
          {!reporte.filas.length ? (
            <div className="flex h-48 items-center justify-center px-5 text-center text-body-3 text-content-quartiary">
              No hay estudiantes matriculados para mostrar.
            </div>
          ) : (
            <TableElement
              className="table-fixed"
              style={{
                minWidth: Math.max(760, 260 + reporte.columnas.length * 180),
              }}
              aria-label={`${reporte.titulo}. ${reporte.detalle}`}
            >
              <colgroup>
                <col style={{ width: 260 }} />
                {reporte.columnas.map((columna) => (
                  <col key={columna.clave} style={{ width: 180 }} />
                ))}
              </colgroup>
              <TableHeader>
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
                    <TableCell className="font-medium text-content-primary">
                      {fila.estudiante}
                    </TableCell>
                    {reporte.columnas.map((columna) => (
                      <TableCell key={columna.clave}>
                        <ValorReporte
                          clave={columna.clave}
                          valor={fila.valores[columna.clave] ?? '—'}
                        />
                      </TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </TableElement>
          )}
        </>
      )}
    </Card>
  );
};

const ValorReporte = ({ clave, valor }: { clave: string; valor: string }) => {
  if (clave === 'estado') {
    return (
      <Tag color={valor === 'Calificado' ? 'success' : 'neutral'} size="md">
        {valor}
      </Tag>
    );
  }
  if (clave === 'evidencia' && valor !== '—') {
    return (
      <span className="flex min-w-0 items-center gap-2">
        <FileText className="size-7 shrink-0 rounded bg-vibrant-blue-500 p-1.5 text-white" />
        <span className="truncate" title={valor}>
          {valor}
        </span>
      </span>
    );
  }
  return valor;
};
