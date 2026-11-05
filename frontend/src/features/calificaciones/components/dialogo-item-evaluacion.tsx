import { Pencil, Plus, Trash2 } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input, Select, Textarea } from '@/components/ui/form';
import { useNotifications } from '@/components/ui/notifications';
import {
  CompetenciaOpcion,
  ItemEvaluacion,
  SolicitudItemEvaluacion,
  TipoItemEvaluacion,
} from '@/types/api';
import { cn } from '@/utils/cn';

import {
  SeleccionPeriodo,
  useCrearItem,
  useEditarItem,
} from '../api/calificaciones';

const TIPOS: { value: TipoItemEvaluacion; label: string }[] = [
  { value: 'PRACTICA', label: 'Práctica' },
  { value: 'EXAMEN', label: 'Examen' },
  { value: 'PROYECTO', label: 'Proyecto' },
  { value: 'TRABAJO_INDIVIDUAL', label: 'Trabajo individual' },
  { value: 'EXPOSICION', label: 'Exposición' },
  { value: 'PORTAFOLIO', label: 'Portafolio' },
  { value: 'OTRO', label: 'Otro' },
];

type FilaCompetencia = { competenciaId: string; pesoPorcentaje: string };

type Props = {
  seleccion: SeleccionPeriodo;
  periodo: string;
  competencias: CompetenciaOpcion[];
  item?: ItemEvaluacion;
  onCerrar: () => void;
};

export const DialogoItemEvaluacion = ({
  seleccion,
  periodo,
  competencias,
  item,
  onCerrar,
}: Props) => {
  const { addNotification } = useNotifications();
  const [nombre, setNombre] = React.useState(item?.nombre ?? '');
  const [descripcion, setDescripcion] = React.useState(item?.descripcion ?? '');
  const [tipo, setTipo] = React.useState<TipoItemEvaluacion>(
    item?.tipo ?? 'PRACTICA',
  );
  const [filas, setFilas] = React.useState<FilaCompetencia[]>(
    item?.competencias.map((c) => ({
      competenciaId: c.competenciaId,
      pesoPorcentaje: String(c.pesoPorcentaje),
    })) ?? [
      { competenciaId: competencias[0]?.id ?? '', pesoPorcentaje: '100' },
    ],
  );
  const [error, setError] = React.useState('');
  const crear = useCrearItem();
  const editar = useEditarItem();
  const guardando = crear.isPending || editar.isPending;

  const actualizarFila = (indice: number, cambio: Partial<FilaCompetencia>) =>
    setFilas((actuales) =>
      actuales.map((fila, i) => (i === indice ? { ...fila, ...cambio } : fila)),
    );

  const guardar = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!nombre.trim() || !descripcion.trim()) {
      setError('Completa el nombre y el criterio de evaluación.');
      return;
    }
    if (
      !filas.length ||
      filas.some(
        (f) =>
          !f.competenciaId ||
          !f.pesoPorcentaje ||
          Number(f.pesoPorcentaje) <= 0 ||
          Number(f.pesoPorcentaje) > 100,
      )
    ) {
      setError('Selecciona cada competencia e ingresa un peso entre 0 y 100.');
      return;
    }
    if (new Set(filas.map((f) => f.competenciaId)).size !== filas.length) {
      setError('Una competencia no puede repetirse en el mismo ítem.');
      return;
    }
    const datos: SolicitudItemEvaluacion = {
      nombre: nombre.trim(),
      descripcion: descripcion.trim(),
      tipo,
      competencias: filas.map((f) => ({
        competenciaId: f.competenciaId,
        pesoPorcentaje: Number(f.pesoPorcentaje),
      })),
    };
    try {
      if (item) await editar.mutateAsync({ id: item.id, datos });
      else await crear.mutateAsync({ seleccion, datos });
      addNotification({
        type: 'success',
        title: item ? 'Ítem actualizado' : 'Ítem registrado',
        message: datos.nombre,
      });
      onCerrar();
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    }
  };

  return (
    <Dialog open onOpenChange={(abierto) => !abierto && onCerrar()}>
      <DialogContent className="flex max-h-[92vh] max-w-[760px] flex-col gap-0 overflow-hidden rounded-xl border-stroke-tertiary bg-white p-0">
        <form onSubmit={guardar} className="flex min-h-0 flex-1 flex-col">
          <div className="flex-1 space-y-5 overflow-y-auto p-6">
            <div>
              <DialogDescription className="mb-2 flex items-center gap-2 text-body-4 text-content-quartiary">
                <Pencil className="size-4 text-icon-accent-primary" />
                {item
                  ? 'Editar ítem de calificación'
                  : 'Agregar ítem de calificación'}
              </DialogDescription>
              <DialogTitle className="text-h6 font-semibold text-content-primary">
                Ítem de calificación
              </DialogTitle>
            </div>

            <div className="grid gap-4 sm:grid-cols-[2fr_1fr]">
              <Input
                label="Nombre del ítem de calificación *"
                value={nombre}
                maxLength={120}
                onChange={(e) => setNombre(e.target.value)}
              />
              <Input
                label="Periodo lectivo"
                value={periodo}
                readOnly
                disabled
              />
            </div>
            <Textarea
              label="Descripción *"
              value={descripcion}
              maxLength={500}
              showCount
              onChange={(e) => setDescripcion(e.target.value)}
            />

            <fieldset className="space-y-2">
              <legend className="text-label-2 font-medium text-content-secondary">
                Tipo de ítem *
              </legend>
              <div className="flex flex-wrap gap-2">
                {TIPOS.map((opcion) => (
                  <button
                    type="button"
                    key={opcion.value}
                    onClick={() => setTipo(opcion.value)}
                    className={cn(
                      'rounded border px-2.5 py-1.5 text-caption-1 transition-colors',
                      tipo === opcion.value
                        ? 'border-stroke-accent-primary bg-surface-accent-light text-content-accent-primary'
                        : 'border-stroke-tertiary bg-white text-content-secondary hover:bg-surface-primary-hover',
                    )}
                  >
                    {opcion.label}
                  </button>
                ))}
              </div>
            </fieldset>

            <div className="space-y-3">
              <div>
                <h3 className="text-label-1 font-medium text-content-primary">
                  Competencias asociadas
                </h3>
              </div>
              {filas.map((fila, indice) => (
                <div
                  key={`${indice}-${fila.competenciaId}`}
                  className="grid items-end gap-3 sm:grid-cols-[1fr_140px_auto]"
                >
                  <Select
                    label="Competencia"
                    aria-label={`Competencia ${indice + 1}`}
                    value={fila.competenciaId}
                    options={competencias.map((c) => ({
                      label: c.nombre,
                      value: c.id,
                      disabled: filas.some(
                        (otra, otroIndice) =>
                          otroIndice !== indice && otra.competenciaId === c.id,
                      ),
                    }))}
                    onChange={(e) =>
                      actualizarFila(indice, { competenciaId: e.target.value })
                    }
                  />
                  <Input
                    label="Peso en % *"
                    type="number"
                    min="0.01"
                    max="100"
                    step="0.01"
                    value={fila.pesoPorcentaje}
                    onChange={(e) =>
                      actualizarFila(indice, {
                        pesoPorcentaje: e.target.value,
                      })
                    }
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    aria-label={`Quitar competencia ${indice + 1}`}
                    disabled={filas.length === 1}
                    onClick={() =>
                      setFilas((actuales) =>
                        actuales.filter((_, i) => i !== indice),
                      )
                    }
                    icon={<Trash2 className="size-4" />}
                  />
                </div>
              ))}
              <Button
                type="button"
                variant="primary-light"
                size="sm"
                icon={<Plus className="size-4" />}
                iconPosition="right"
                disabled={filas.length >= competencias.length}
                onClick={() =>
                  setFilas((actuales) => [
                    ...actuales,
                    {
                      competenciaId:
                        competencias.find(
                          (c) =>
                            !actuales.some(
                              (fila) => fila.competenciaId === c.id,
                            ),
                        )?.id ?? '',
                      pesoPorcentaje: '100',
                    },
                  ])
                }
              >
                Agregar competencia
              </Button>
            </div>

            {error && (
              <p role="alert" className="text-body-4 text-content-error">
                {error}
              </p>
            )}
          </div>
          <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
            <Button type="button" variant="primary-light" onClick={onCerrar}>
              Cancelar
            </Button>
            <Button type="submit" isLoading={guardando} disabled={guardando}>
              {item ? 'Guardar cambios' : 'Agregar ítem'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
};
