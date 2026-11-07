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
  ponderado: boolean;
  pesoAsignadoPorCompetencia: ReadonlyMap<string, number>;
  item?: ItemEvaluacion;
  onCerrar: () => void;
};

const redondearPeso = (peso: number) => Math.round(peso * 100) / 100;

export const DialogoItemEvaluacion = ({
  seleccion,
  periodo,
  competencias,
  ponderado,
  pesoAsignadoPorCompetencia,
  item,
  onCerrar,
}: Props) => {
  const { addNotification } = useNotifications();
  const pesoActualPorCompetencia = React.useMemo(
    () =>
      new Map(
        item?.competencias.map(
          (competencia) =>
            [competencia.competenciaId, competencia.pesoPorcentaje] as const,
        ) ?? [],
      ),
    [item],
  );
  const pesoMaximo = React.useCallback(
    (competenciaId: string) =>
      redondearPeso(
        Math.max(
          0,
          100 -
            (pesoAsignadoPorCompetencia.get(competenciaId) ?? 0) +
            (pesoActualPorCompetencia.get(competenciaId) ?? 0),
        ),
      ),
    [pesoActualPorCompetencia, pesoAsignadoPorCompetencia],
  );
  const primeraCompetenciaDisponible = competencias.find(
    (competencia) => !ponderado || pesoMaximo(competencia.id) > 0,
  );
  const [nombre, setNombre] = React.useState(item?.nombre ?? '');
  const [descripcion, setDescripcion] = React.useState(item?.descripcion ?? '');
  const [tipo, setTipo] = React.useState<TipoItemEvaluacion>(
    item?.tipo ?? 'PRACTICA',
  );
  const [filas, setFilas] = React.useState<FilaCompetencia[]>(
    item?.competencias.map((c) => ({
      competenciaId: c.competenciaId,
      pesoPorcentaje: c.pesoPorcentaje == null ? '' : String(c.pesoPorcentaje),
    })) ?? [
      {
        competenciaId: primeraCompetenciaDisponible?.id ?? '',
        pesoPorcentaje:
          ponderado && primeraCompetenciaDisponible
            ? String(pesoMaximo(primeraCompetenciaDisponible.id))
            : '',
      },
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
    if (!filas.length || filas.some((fila) => !fila.competenciaId)) {
      setError('Selecciona cada competencia del ítem.');
      return;
    }
    if (
      ponderado &&
      filas.some(
        (fila) =>
          !fila.pesoPorcentaje ||
          !Number.isFinite(Number(fila.pesoPorcentaje)) ||
          Number(fila.pesoPorcentaje) < 0.01 ||
          Number(fila.pesoPorcentaje) > 100,
      )
    ) {
      setError('Ingresa un peso entre 0,01 y 100 % para cada competencia.');
      return;
    }
    if (new Set(filas.map((f) => f.competenciaId)).size !== filas.length) {
      setError('Una competencia no puede repetirse en el mismo ítem.');
      return;
    }
    const filaSinCupo =
      ponderado &&
      filas.find(
        (fila) => Number(fila.pesoPorcentaje) > pesoMaximo(fila.competenciaId),
      );
    if (filaSinCupo) {
      const competencia = competencias.find(
        (actual) => actual.id === filaSinCupo.competenciaId,
      );
      setError(
        `${competencia?.nombre ?? 'La competencia'} solo tiene ${pesoMaximo(
          filaSinCupo.competenciaId,
        )} % de peso disponible.`,
      );
      return;
    }
    const datos: SolicitudItemEvaluacion = {
      nombre: nombre.trim(),
      descripcion: descripcion.trim(),
      tipo,
      competencias: filas.map((f) => ({
        competenciaId: f.competenciaId,
        ...(ponderado ? { pesoPorcentaje: Number(f.pesoPorcentaje) } : {}),
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
        <form
          noValidate
          onSubmit={guardar}
          className="flex min-h-0 flex-1 flex-col"
        >
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
                  className={cn(
                    'grid items-end gap-3',
                    ponderado
                      ? 'sm:grid-cols-[1fr_140px_auto]'
                      : 'sm:grid-cols-[1fr_auto]',
                  )}
                >
                  <Select
                    label="Competencia"
                    aria-label={`Competencia ${indice + 1}`}
                    value={fila.competenciaId}
                    options={competencias.map((c) => ({
                      label: c.nombre,
                      value: c.id,
                      disabled:
                        (ponderado &&
                          pesoMaximo(c.id) <= 0 &&
                          c.id !== fila.competenciaId) ||
                        filas.some(
                          (otra, otroIndice) =>
                            otroIndice !== indice &&
                            otra.competenciaId === c.id,
                        ),
                    }))}
                    onChange={(e) => {
                      const competenciaId = e.target.value;
                      actualizarFila(indice, {
                        competenciaId,
                        pesoPorcentaje: ponderado
                          ? String(pesoMaximo(competenciaId))
                          : '',
                      });
                    }}
                  />
                  {ponderado && (
                    <Input
                      label="Peso en % *"
                      type="number"
                      min="0.01"
                      max={pesoMaximo(fila.competenciaId)}
                      step="0.01"
                      value={fila.pesoPorcentaje}
                      onChange={(e) =>
                        actualizarFila(indice, {
                          pesoPorcentaje: e.target.value,
                        })
                      }
                    />
                  )}
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
                disabled={
                  !competencias.some(
                    (competencia) =>
                      (!ponderado || pesoMaximo(competencia.id) > 0) &&
                      !filas.some(
                        (fila) => fila.competenciaId === competencia.id,
                      ),
                  )
                }
                onClick={() =>
                  setFilas((actuales) => {
                    const competencia = competencias.find(
                      (actual) =>
                        (!ponderado || pesoMaximo(actual.id) > 0) &&
                        !actuales.some(
                          (fila) => fila.competenciaId === actual.id,
                        ),
                    );
                    if (!competencia) return actuales;
                    return [
                      ...actuales,
                      {
                        competenciaId: competencia.id,
                        pesoPorcentaje: ponderado
                          ? String(pesoMaximo(competencia.id))
                          : '',
                      },
                    ];
                  })
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
