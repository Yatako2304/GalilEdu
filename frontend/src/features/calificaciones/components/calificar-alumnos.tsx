import { Download, FileText, Paperclip, Trash2 } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Select } from '@/components/ui/form';
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
import { Tag } from '@/components/ui/tag';
import {
  CursoDocente,
  FilaCalificacion,
  ItemEvaluacion,
  PeriodoCursoDocente,
} from '@/types/api';

import {
  descargarEvidencia,
  useCalificacionesItem,
  useEliminarEvidencia,
  useGuardarCalificaciones,
  useGuardarEvidencia,
} from '../api/calificaciones';

type Props = {
  curso: CursoDocente;
  periodo: PeriodoCursoDocente;
  items: ItemEvaluacion[];
};

export const CalificarAlumnos = ({ curso, periodo, items }: Props) => {
  const [itemId, setItemId] = React.useState('');
  const item = items.find((actual) => actual.id === itemId);

  React.useEffect(() => {
    if (!items.some((actual) => actual.id === itemId)) {
      setItemId(items[0]?.id ?? '');
    }
  }, [itemId, items]);

  return (
    <Card className="overflow-hidden">
      <div className="space-y-4 border-b border-stroke-tertiary px-5 py-4">
        <div>
          <h2 className="text-h6 text-content-primary">Calificar alumnos</h2>
          <p className="mt-1 text-body-4 text-content-quartiary">
            Selecciona el ítem de evaluación que deseas calificar.
          </p>
        </div>
        <div className="max-w-xl">
          <Select
            label="Ítem de evaluación"
            value={itemId}
            options={items.map((actual) => ({
              label: actual.nombre,
              value: actual.id,
            }))}
            onChange={(e) => setItemId(e.target.value)}
            disabled={!items.length}
            placeholder="No hay ítems en este periodo"
          />
        </div>
      </div>

      {!item ? (
        <div className="flex h-48 items-center justify-center px-5 text-center text-body-3 text-content-quartiary">
          Registra un ítem de evaluación para empezar a calificar.
        </div>
      ) : (
        <TablaCalificaciones curso={curso} periodo={periodo} item={item} />
      )}
    </Card>
  );
};

const TablaCalificaciones = ({
  curso,
  periodo,
  item,
}: {
  curso: CursoDocente;
  periodo: PeriodoCursoDocente;
  item: ItemEvaluacion;
}) => {
  const { data: filas = [], isLoading } = useCalificacionesItem(item.id);
  const [valores, setValores] = React.useState<Record<string, string>>({});
  const guardar = useGuardarCalificaciones();
  const subir = useGuardarEvidencia();
  const eliminar = useEliminarEvidencia();
  const { addNotification } = useNotifications();
  const editable = periodo.registroNotasAbierto && item.editable;

  React.useEffect(() => {
    setValores(
      Object.fromEntries(
        filas.map((fila) => [
          fila.matriculaId,
          fila.notaNumerica === null
            ? (fila.nivelLogro ?? '')
            : String(fila.notaNumerica),
        ]),
      ),
    );
  }, [filas]);

  const restaurar = () =>
    setValores(
      Object.fromEntries(
        filas.map((fila) => [
          fila.matriculaId,
          fila.notaNumerica === null
            ? (fila.nivelLogro ?? '')
            : String(fila.notaNumerica),
        ]),
      ),
    );

  const guardarNotas = async () => {
    try {
      await guardar.mutateAsync({
        itemId: item.id,
        calificaciones: filas.map((fila) => {
          const valor = valores[fila.matriculaId]?.trim() ?? '';
          return {
            matriculaId: fila.matriculaId,
            notaNumerica:
              curso.escala === 'VIGESIMAL' && valor ? Number(valor) : null,
            nivelLogro: curso.escala === 'CUALITATIVA' && valor ? valor : null,
          };
        }),
      });
      addNotification({
        type: 'success',
        title: 'Calificaciones guardadas',
        message: `${item.nombre} · ${periodo.nombre}`,
      });
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    }
  };

  const adjuntar = async (fila: FilaCalificacion, archivo?: File) => {
    if (!archivo) return;
    try {
      await subir.mutateAsync({
        itemId: item.id,
        matriculaId: fila.matriculaId,
        archivo,
      });
      addNotification({
        type: 'success',
        title: 'Evidencia adjuntada',
        message: fila.estudiante,
      });
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    }
  };

  const quitarEvidencia = async (fila: FilaCalificacion) => {
    try {
      await eliminar.mutateAsync({
        itemId: item.id,
        matriculaId: fila.matriculaId,
      });
      addNotification({ type: 'success', title: 'Evidencia eliminada' });
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    }
  };

  if (isLoading) {
    return (
      <div className="flex h-48 items-center justify-center">
        <Spinner />
      </div>
    );
  }

  if (!filas.length) {
    return (
      <div className="flex h-48 items-center justify-center px-5 text-center text-body-3 text-content-quartiary">
        No hay estudiantes matriculados en esta sección.
      </div>
    );
  }

  return (
    <>
      <TableElement className="min-w-[780px] table-fixed">
        <colgroup>
          <col className="w-[30%]" />
          <col className="w-[16%]" />
          <col className="w-[38%]" />
          <col className="w-[16%]" />
        </colgroup>
        <TableHeader>
          <TableRow>
            <TableHead>Estudiante</TableHead>
            <TableHead>Nota</TableHead>
            <TableHead>Evidencia</TableHead>
            <TableHead>Estado</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {filas.map((fila) => (
            <TableRow key={fila.matriculaId}>
              <TableCell className="font-medium text-content-primary">
                {fila.estudiante}
              </TableCell>
              <TableCell>
                {curso.escala === 'VIGESIMAL' ? (
                  <input
                    aria-label={`Calificación de ${fila.estudiante}`}
                    type="number"
                    min="0"
                    max="20"
                    step="0.01"
                    disabled={!editable}
                    value={valores[fila.matriculaId] ?? ''}
                    onChange={(e) =>
                      setValores((actuales) => ({
                        ...actuales,
                        [fila.matriculaId]: e.target.value,
                      }))
                    }
                    className="h-10 w-20 rounded-lg border border-stroke-secondary bg-white px-3 text-body-3 text-content-primary disabled:bg-surface-disabled"
                  />
                ) : (
                  <Select
                    aria-label={`Nivel de logro de ${fila.estudiante}`}
                    value={valores[fila.matriculaId] ?? ''}
                    options={['AD', 'A', 'B', 'C'].map((nivel) => ({
                      label: nivel,
                      value: nivel,
                    }))}
                    placeholder="Sin calificar"
                    vaciable
                    disabled={!editable}
                    onChange={(e) =>
                      setValores((actuales) => ({
                        ...actuales,
                        [fila.matriculaId]: e.target.value,
                      }))
                    }
                  />
                )}
              </TableCell>
              <TableCell>
                {fila.evidencia ? (
                  <div className="flex min-w-0 items-center gap-2">
                    <FileText className="size-8 shrink-0 rounded bg-vibrant-blue-500 p-1.5 text-white" />
                    <span
                      className="min-w-0 flex-1"
                      title={fila.evidencia.nombreArchivo}
                    >
                      <span className="block truncate text-body-4 text-content-secondary">
                        {fila.evidencia.nombreArchivo}
                      </span>
                      <span className="block text-caption-1 text-content-quartiary">
                        {tamanoArchivo(fila.evidencia.tamanoBytes)}
                      </span>
                    </span>
                    <div className="flex gap-1">
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label={`Descargar evidencia de ${fila.estudiante}`}
                        icon={<Download className="size-4" />}
                        onClick={() =>
                          descargarEvidencia(
                            item.id,
                            fila.matriculaId,
                            fila.evidencia!.nombreArchivo,
                          )
                        }
                      />
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label={`Eliminar evidencia de ${fila.estudiante}`}
                        disabled={!editable || eliminar.isPending}
                        icon={<Trash2 className="size-4" />}
                        onClick={() => quitarEvidencia(fila)}
                      />
                    </div>
                  </div>
                ) : (
                  <label className="inline-flex">
                    <input
                      type="file"
                      className="sr-only"
                      accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
                      disabled={!editable || !fila.id || subir.isPending}
                      onChange={(e) => adjuntar(fila, e.target.files?.[0])}
                    />
                    <span
                      className={`inline-flex h-9 items-center gap-2 rounded-lg px-3 text-body-4 ${
                        editable && fila.id
                          ? 'cursor-pointer bg-surface-accent-primary text-white hover:bg-deep-blue-950'
                          : 'cursor-not-allowed bg-surface-disabled text-content-disabled'
                      }`}
                      title={
                        fila.id
                          ? 'Adjuntar evidencia'
                          : 'Guarda primero la calificación.'
                      }
                    >
                      <Paperclip className="size-4" /> Adjuntar evidencia
                    </span>
                  </label>
                )}
              </TableCell>
              <TableCell>
                <Tag
                  color={fila.estado === 'CALIFICADO' ? 'success' : 'neutral'}
                  size="md"
                >
                  {fila.estado === 'CALIFICADO' ? 'Calificado' : 'Pendiente'}
                </Tag>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </TableElement>
      <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-5 py-4">
        <Button
          variant="primary-light"
          disabled={!editable || guardar.isPending}
          onClick={restaurar}
        >
          Cancelar
        </Button>
        <Button
          disabled={!editable || guardar.isPending}
          isLoading={guardar.isPending}
          onClick={guardarNotas}
        >
          Guardar calificaciones
        </Button>
      </div>
    </>
  );
};

const tamanoArchivo = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};
