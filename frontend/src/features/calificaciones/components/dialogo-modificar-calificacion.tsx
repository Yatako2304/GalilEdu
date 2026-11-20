import { History, Pencil } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input, Radio, Select, Textarea } from '@/components/ui/form';
import { useNotifications } from '@/components/ui/notifications';
import { Spinner } from '@/components/ui/spinner';
import {
  EscalaEvaluacion,
  FilaCalificacion,
  ModificacionCalificacion,
  NivelLogro,
} from '@/types/api';

import {
  useHistorialCalificacion,
  useModificarCalificacion,
} from '../api/calificaciones';

/** Igual que ServicioCalificaciones.MOTIVO_MINIMO. */
export const MOTIVO_MINIMO = 10;

type Props = {
  itemId: string;
  item: string;
  escala: EscalaEvaluacion;
  fila: FilaCalificacion;
  /** El periodo está dentro del plazo de registro (fecha límite de notas). */
  enPlazo: boolean;
  onCerrar: () => void;
};

type Resolucion = 'nota' | 'no-evaluado';

const valorTexto = (
  nota: number | null,
  nivel: NivelLogro | null,
  noEvaluado = false,
) => {
  if (noEvaluado) return 'No evaluado';
  if (nota !== null) return String(nota).replace('.', ',');
  return nivel ?? 'Sin nota';
};

const fechaHora = (iso: string) =>
  new Date(iso).toLocaleString('es-PE', {
    dateStyle: 'short',
    timeStyle: 'short',
  });

/**
 * HU-60 · Resolver una celda fuera del registro en lote. No hay frame en Figma: sigue el modal de «Ítem de
 * calificación». Sirve para modificar una nota, registrar fuera de plazo una que falta o marcarla «No evaluado» (el ítem
 * no cuenta para el estudiante; no es un 0). El motivo es obligatorio salvo al registrar por primera vez una nota
 * dentro del plazo, y cada cambio queda en el historial.
 */
export const DialogoModificarCalificacion = ({
  itemId,
  item,
  escala,
  fila,
  enPlazo,
  onCerrar,
}: Props) => {
  const vigesimal = escala === 'VIGESIMAL';
  const registrada = fila.id !== null;
  const [resolucion, setResolucion] = React.useState<Resolucion>('nota');
  const [valor, setValor] = React.useState('');
  const [motivo, setMotivo] = React.useState('');
  const [error, setError] = React.useState<string>();
  const modificar = useModificarCalificacion();
  const historial = useHistorialCalificacion(itemId, fila.matriculaId);
  const { addNotification } = useNotifications();
  const noEvaluado = resolucion === 'no-evaluado';
  const pideMotivo = registrada || !enPlazo || noEvaluado;
  const titulo = registrada
    ? 'Modificar calificación'
    : 'Registrar calificación';

  const guardar = async (evento: React.FormEvent) => {
    evento.preventDefault();
    const limpio = valor.trim().replace(',', '.');
    const numero = Number(limpio);
    if (!noEvaluado && !limpio) {
      setError('Ingresa la calificación.');
      return;
    }
    if (
      !noEvaluado &&
      vigesimal &&
      (Number.isNaN(numero) || numero < 0 || numero > 20)
    ) {
      setError('La calificación debe estar entre 0 y 20.');
      return;
    }
    if (pideMotivo && motivo.trim().length < MOTIVO_MINIMO) {
      setError(
        `Explica el motivo (al menos ${MOTIVO_MINIMO} caracteres): queda en el historial.`,
      );
      return;
    }
    setError(undefined);
    try {
      await modificar.mutateAsync({
        itemId,
        matriculaId: fila.matriculaId,
        notaNumerica: !noEvaluado && vigesimal ? numero : null,
        nivelLogro: !noEvaluado && !vigesimal ? (limpio as NivelLogro) : null,
        noEvaluado,
        motivo: motivo.trim() || undefined,
      });
      addNotification({
        type: 'success',
        title: noEvaluado
          ? 'Marcada como «No evaluado»'
          : registrada
            ? 'Calificación modificada'
            : 'Calificación registrada',
        message: `${fila.estudiante} · ${item}`,
      });
      onCerrar();
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    }
  };

  return (
    <Dialog open onOpenChange={(abierto) => !abierto && onCerrar()}>
      <DialogContent className="flex max-h-[92vh] max-w-screen-sm flex-col gap-0 overflow-hidden rounded-xl border-stroke-tertiary bg-white p-0">
        <form
          noValidate
          onSubmit={guardar}
          className="flex min-h-0 flex-1 flex-col"
        >
          <div className="flex-1 space-y-5 overflow-y-auto p-6">
            <div>
              <DialogDescription className="mb-2 flex items-center gap-2 text-body-4 text-content-quartiary">
                <Pencil className="size-4 text-icon-accent-primary" />
                {item}
              </DialogDescription>
              <DialogTitle className="text-h6 font-semibold text-content-primary">
                {titulo}
              </DialogTitle>
              <p className="mt-1 text-body-4 text-content-tertiary">
                {fila.estudiante}
              </p>
            </div>

            <fieldset className="flex flex-wrap gap-6">
              <legend className="sr-only">Cómo se resuelve la celda</legend>
              <Radio
                name="resolucion"
                label="Con calificación"
                checked={resolucion === 'nota'}
                onChange={() => setResolucion('nota')}
              />
              <Radio
                name="resolucion"
                label="No evaluado"
                helperText="El ítem no cuenta para este estudiante; no es un 0."
                checked={noEvaluado}
                onChange={() => setResolucion('no-evaluado')}
              />
            </fieldset>

            <div className="grid gap-4 sm:grid-cols-2">
              <Input
                label="Valor actual"
                value={valorTexto(
                  fila.notaNumerica,
                  fila.nivelLogro,
                  fila.noEvaluado,
                )}
                readOnly
                disabled
              />
              {!noEvaluado &&
                (vigesimal ? (
                  <Input
                    label="Nueva calificación *"
                    type="number"
                    min="0"
                    max="20"
                    step="0.01"
                    value={valor}
                    onChange={(e) => setValor(e.target.value)}
                  />
                ) : (
                  <Select
                    label="Nuevo nivel de logro *"
                    value={valor}
                    placeholder="Selecciona"
                    options={['AD', 'A', 'B', 'C'].map((nivel) => ({
                      label: nivel,
                      value: nivel,
                    }))}
                    onChange={(e) => setValor(e.target.value)}
                  />
                ))}
            </div>
            <Textarea
              label={pideMotivo ? 'Motivo *' : 'Motivo (opcional)'}
              value={motivo}
              maxLength={500}
              showCount
              hint={
                pideMotivo
                  ? 'Queda registrado en el historial de la calificación.'
                  : 'Dentro del plazo, registrar una nota nueva no pide motivo.'
              }
              onChange={(e) => setMotivo(e.target.value)}
            />

            {error && (
              <p role="alert" className="text-body-4 text-content-error">
                {error}
              </p>
            )}

            <section aria-label="Historial de cambios" className="space-y-2">
              <h3 className="flex items-center gap-2 text-label-2 font-medium text-content-secondary">
                <History className="size-4 text-icon-accent-primary" />
                Historial de cambios
              </h3>
              <Historial
                cargando={historial.isLoading}
                cambios={historial.data ?? []}
              />
            </section>
          </div>
          <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
            <Button type="button" variant="primary-light" onClick={onCerrar}>
              Cancelar
            </Button>
            <Button
              type="submit"
              isLoading={modificar.isPending}
              disabled={modificar.isPending}
            >
              {registrada ? 'Guardar modificación' : 'Registrar'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};

const Historial = ({
  cargando,
  cambios,
}: {
  cargando: boolean;
  cambios: ModificacionCalificacion[];
}) => {
  if (cargando) return <Spinner size="sm" />;
  if (!cambios.length) {
    return (
      <p className="text-body-4 text-content-quartiary">
        Esta calificación no tiene cambios registrados.
      </p>
    );
  }
  return (
    <ol className="divide-y divide-stroke-tertiary rounded-lg border border-stroke-tertiary">
      {cambios.map((cambio) => (
        <li
          key={`${cambio.modificadoEn}-${cambio.motivo}`}
          className="space-y-1 px-4 py-3"
        >
          <p className="text-label-2 font-medium text-content-primary">
            {valorTexto(
              cambio.notaAnterior,
              cambio.nivelAnterior,
              cambio.noEvaluadoAnterior,
            )}{' '}
            →{' '}
            {valorTexto(
              cambio.notaNueva,
              cambio.nivelNuevo,
              cambio.noEvaluadoNuevo,
            )}
          </p>
          <p className="text-body-4 text-content-secondary">{cambio.motivo}</p>
          <p className="text-caption-1 text-content-quartiary">
            {cambio.modificadoPor} · {fechaHora(cambio.modificadoEn)}
          </p>
        </li>
      ))}
    </ol>
  );
};
