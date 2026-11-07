import { EllipsisVertical, Pencil, Plus, Trash2 } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmationDialog } from '@/components/ui/dialog';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
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
import { Tag } from '@/components/ui/tag';
import {
  CompetenciaOpcion,
  ItemEvaluacion,
  PeriodoCursoDocente,
} from '@/types/api';

import {
  SeleccionPeriodo,
  useEliminarItem,
  useItemsEvaluacion,
} from '../api/calificaciones';

import { DialogoItemEvaluacion } from './dialogo-item-evaluacion';

const NOMBRES_TIPO: Record<ItemEvaluacion['tipo'], string> = {
  PRACTICA: 'Práctica',
  EXAMEN: 'Evaluación',
  PROYECTO: 'Proyecto',
  TRABAJO_INDIVIDUAL: 'Trabajo individual',
  EXPOSICION: 'Exposición',
  PORTAFOLIO: 'Portafolio',
  OTRO: 'Otro',
};

type Props = {
  seleccion: SeleccionPeriodo;
  periodo: PeriodoCursoDocente;
  competencias: CompetenciaOpcion[];
  reglaNota: string;
};

type VistaItems = 'items' | 'competencias';

export const ItemsEvaluacion = ({
  seleccion,
  periodo,
  competencias,
  reglaNota,
}: Props) => {
  const ponderado = reglaNota === 'PROMEDIO_PONDERADO';
  const { data: items = [], isLoading } = useItemsEvaluacion(seleccion);
  const [vista, setVista] = React.useState<VistaItems>('items');
  const [editando, setEditando] = React.useState<ItemEvaluacion | 'nuevo'>();
  const eliminar = useEliminarItem();
  const { addNotification } = useNotifications();
  const pesoAsignadoPorCompetencia = React.useMemo(() => {
    const totales = new Map<string, number>();
    items.forEach((item) =>
      item.competencias.forEach((competencia) =>
        totales.set(
          competencia.competenciaId,
          (totales.get(competencia.competenciaId) ?? 0) +
            (competencia.pesoPorcentaje ?? 0),
        ),
      ),
    );
    return totales;
  }, [items]);
  const competenciasCompletas =
    ponderado &&
    competencias.length > 0 &&
    competencias.every(
      (competencia) =>
        (pesoAsignadoPorCompetencia.get(competencia.id) ?? 0) >= 100,
    );

  const quitar = async (item: ItemEvaluacion) => {
    try {
      await eliminar.mutateAsync(item.id);
      addNotification({
        type: 'success',
        title: 'Ítem eliminado',
        message: item.nombre,
      });
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-end">
        <Button
          icon={<Plus className="size-4" />}
          iconPosition="right"
          disabled={
            isLoading ||
            !periodo.registroNotasAbierto ||
            !competencias.length ||
            competenciasCompletas
          }
          title={
            !periodo.registroNotasAbierto
              ? 'El periodo no está habilitado para registrar calificaciones.'
              : competenciasCompletas
                ? 'Todas las competencias ya tienen el 100 % de peso asignado.'
                : undefined
          }
          onClick={() => setEditando('nuevo')}
        >
          Agregar ítem de evaluación
        </Button>
      </div>

      <Card className="overflow-hidden">
        <Tabs
          aria-label="Vista de ítems de evaluación"
          fullWidth
          value={vista}
          onValueChange={setVista}
          items={[
            { value: 'items', label: 'Ítems de evaluación' },
            { value: 'competencias', label: 'Ítems por competencia' },
          ]}
          className="px-5 pt-3"
        />

        {isLoading ? (
          <div className="flex h-48 items-center justify-center">
            <Spinner />
          </div>
        ) : !items.length ? (
          <div className="flex h-48 flex-col items-center justify-center px-5 text-center">
            <p className="text-body-3 text-content-secondary">
              Aún no hay ítems en {periodo.nombre}.
            </p>
            <p className="mt-1 text-body-4 text-content-quartiary">
              {periodo.registroNotasAbierto
                ? 'Agrega el primer ítem para empezar a calificar.'
                : 'Este periodo se encuentra en modo de consulta.'}
            </p>
          </div>
        ) : vista === 'items' ? (
          <TablaItems
            items={items}
            eliminando={eliminar.isPending}
            eliminacionLista={eliminar.isSuccess}
            onEditar={setEditando}
            onEliminar={quitar}
          />
        ) : (
          <ItemsPorCompetencia
            competencias={competencias}
            items={items}
            ponderado={ponderado}
            eliminando={eliminar.isPending}
            eliminacionLista={eliminar.isSuccess}
            onEditar={setEditando}
            onEliminar={quitar}
          />
        )}
      </Card>

      {editando && (
        <DialogoItemEvaluacion
          seleccion={seleccion}
          periodo={periodo.nombre}
          competencias={competencias}
          ponderado={ponderado}
          pesoAsignadoPorCompetencia={pesoAsignadoPorCompetencia}
          item={editando === 'nuevo' ? undefined : editando}
          onCerrar={() => setEditando(undefined)}
        />
      )}
    </div>
  );
};

type AccionesProps = {
  item: ItemEvaluacion;
  eliminando: boolean;
  eliminacionLista: boolean;
  onEditar: (item: ItemEvaluacion) => void;
  onEliminar: (item: ItemEvaluacion) => void;
};

const AccionesItem = ({
  item,
  eliminando,
  eliminacionLista,
  onEditar,
  onEliminar,
}: AccionesProps) => (
  <DropdownMenu>
    <DropdownMenuTrigger
      aria-label={`Acciones de ${item.nombre}`}
      disabled={!item.editable}
      className="inline-flex size-8 items-center justify-center rounded-lg text-content-tertiary hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus disabled:text-content-disabled"
    >
      <EllipsisVertical className="size-4" aria-hidden="true" />
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end" className="w-40">
      <DropdownMenuItem onSelect={() => onEditar(item)}>
        <Pencil className="mr-2 size-4" />
        Editar
      </DropdownMenuItem>
      <ConfirmationDialog
        title="Eliminar ítem de evaluación"
        body={`Se eliminará “${item.nombre}”. Esta acción solo está disponible mientras no tenga calificaciones.`}
        cancelButtonText="Cancelar"
        isDone={eliminacionLista}
        triggerButton={
          <DropdownMenuItem
            className="text-content-error"
            onSelect={(evento) => evento.preventDefault()}
          >
            <Trash2 className="mr-2 size-4" />
            Eliminar
          </DropdownMenuItem>
        }
        confirmButton={
          <Button
            variant="error"
            isLoading={eliminando}
            onClick={() => onEliminar(item)}
          >
            Eliminar
          </Button>
        }
      />
    </DropdownMenuContent>
  </DropdownMenu>
);

const TipoItem = ({ tipo }: { tipo: ItemEvaluacion['tipo'] }) => (
  <span className="inline-flex rounded border border-stroke-accent-primary bg-surface-accent-light px-2 py-1 text-caption-1 text-content-accent-primary">
    {NOMBRES_TIPO[tipo]}
  </span>
);

const TablaItems = ({
  items,
  ...acciones
}: { items: ItemEvaluacion[] } & Omit<AccionesProps, 'item'>) => (
  <TableElement className="table-fixed">
    <colgroup>
      <col />
      <col className="w-52" />
      <col className="w-16" />
    </colgroup>
    <TableHeader className="bg-white">
      <TableRow>
        <TableHead className="px-5">Ítem</TableHead>
        <TableHead>Tipo</TableHead>
        <TableHead>
          <span className="sr-only">Acciones</span>
        </TableHead>
      </TableRow>
    </TableHeader>
    <TableBody>
      {items.map((item) => (
        <TableRow key={item.id}>
          <TableCell className="px-5 py-4">
            <p className="font-medium text-content-primary">{item.nombre}</p>
            <p className="mt-1 text-body-4 text-content-quartiary">
              {item.descripcion}
            </p>
          </TableCell>
          <TableCell>
            <TipoItem tipo={item.tipo} />
          </TableCell>
          <TableCell className="text-right">
            <AccionesItem item={item} {...acciones} />
          </TableCell>
        </TableRow>
      ))}
    </TableBody>
  </TableElement>
);

const ItemsPorCompetencia = ({
  competencias,
  items,
  ponderado,
  ...acciones
}: {
  competencias: CompetenciaOpcion[];
  items: ItemEvaluacion[];
  ponderado: boolean;
} & Omit<AccionesProps, 'item'>) => {
  const grupos = competencias
    .map((competencia, indice) => ({
      competencia,
      indice: indice + 1,
      items: items.flatMap((item) =>
        item.competencias
          .filter((actual) => actual.competenciaId === competencia.id)
          .map((actual) => ({ item, peso: actual.pesoPorcentaje })),
      ),
    }))
    .filter((grupo) => grupo.items.length > 0);

  return (
    <div className="divide-y divide-stroke-tertiary px-5">
      {grupos.map((grupo) => {
        const pesoTotal = grupo.items.reduce(
          (suma, actual) => suma + (actual.peso ?? 0),
          0,
        );
        const completo = Math.abs(pesoTotal - 100) < 0.01;
        const excedido = pesoTotal > 100;
        return (
          <section key={grupo.competencia.id} className="py-6">
            <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
              <div className="flex items-start gap-3">
                <span className="flex size-7 shrink-0 items-center justify-center rounded-lg bg-surface-accent-light text-label-2 text-content-accent-primary">
                  {grupo.indice}
                </span>
                <div>
                  <h3 className="text-label-1 font-medium text-content-primary">
                    {grupo.competencia.nombre}
                  </h3>
                  <p className="mt-1 text-body-4 text-content-quartiary">
                    {grupo.items.length}{' '}
                    {grupo.items.length === 1 ? 'ítem' : 'ítems'}
                  </p>
                </div>
              </div>
              {ponderado && (
                <Tag
                  color={completo ? 'success' : 'error'}
                  appearance={completo ? 'soft' : 'outline'}
                  size="md"
                >
                  {completo
                    ? 'Pesos completos'
                    : excedido
                      ? 'Pesos excedidos'
                      : 'Pesos incompletos'}
                </Tag>
              )}
            </div>

            <TableElement className="table-fixed">
              <colgroup>
                <col />
                <col className="w-52" />
                {ponderado && <col className="w-36" />}
                <col className="w-16" />
              </colgroup>
              <TableHeader className="bg-white">
                <TableRow>
                  <TableHead className="px-3">Ítem</TableHead>
                  <TableHead>Tipo</TableHead>
                  {ponderado && <TableHead>Peso en %</TableHead>}
                  <TableHead>
                    <span className="sr-only">Acciones</span>
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {grupo.items.map(({ item, peso }) => (
                  <TableRow key={item.id}>
                    <TableCell className="px-3 py-4">
                      <p className="font-medium text-content-primary">
                        {item.nombre}
                      </p>
                      <p className="mt-1 text-body-4 text-content-quartiary">
                        {item.descripcion}
                      </p>
                    </TableCell>
                    <TableCell>
                      <TipoItem tipo={item.tipo} />
                    </TableCell>
                    {ponderado && (
                      <TableCell>{peso == null ? '—' : `${peso}%`}</TableCell>
                    )}
                    <TableCell className="text-right">
                      <AccionesItem item={item} {...acciones} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </TableElement>
          </section>
        );
      })}
    </div>
  );
};
