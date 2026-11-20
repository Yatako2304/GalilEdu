import {
  EllipsisVertical,
  Pencil,
  SlidersHorizontal,
  ToggleLeft,
  ToggleRight,
} from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
import { useNotifications } from '@/components/ui/notifications';
import { Spinner } from '@/components/ui/spinner';
import { Table } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { DescuentoVista } from '@/types/api';

import {
  useCambiarEstadoDescuento,
  useConfiguracionDescuentos,
  useDescuentos,
} from '../api/descuentos';

import { DialogoDescuento } from './dialogo-descuento';
import { DialogoLimiteDescuento } from './dialogo-limite-descuento';

const porcentaje = (descuento: DescuentoVista) => {
  if (descuento.tipo === 'PORCENTAJE_UNICO')
    return `${descuento.porcentaje ?? 0} %`;
  return descuento.tramos
    .map((tramo) =>
      tramo.hermanosHasta === null
        ? `${tramo.hermanosDesde}+: ${tramo.porcentaje} %`
        : tramo.hermanosDesde === tramo.hermanosHasta
          ? `${tramo.hermanosDesde} hermanos: ${tramo.porcentaje} %`
          : `${tramo.hermanosDesde}–${tramo.hermanosHasta} hermanos: ${tramo.porcentaje} %`,
    )
    .join(' · ');
};

const conceptos = (valores: string[]) =>
  valores
    .map((valor) =>
      valor === 'PENSION'
        ? 'Pensiones'
        : valor === 'MATRICULA'
          ? 'Matrícula'
          : 'Cuota de ingreso',
    )
    .join(' · ');

const AccionesDescuento = ({
  descuento,
  onEditar,
  onCambiarEstado,
}: {
  descuento: DescuentoVista;
  onEditar: () => void;
  onCambiarEstado: () => void;
}) => (
  <DropdownMenu>
    <DropdownMenuTrigger className="inline-flex size-8 items-center justify-center rounded-xl text-content-tertiary hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus">
      <EllipsisVertical className="size-4" aria-hidden="true" />
      <span className="sr-only">Acciones de {descuento.nombre}</span>
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end" className="w-48">
      <DropdownMenuItem onClick={onEditar}>
        <Pencil className="mr-2 size-4" aria-hidden="true" /> Editar
      </DropdownMenuItem>
      <DropdownMenuItem onClick={onCambiarEstado}>
        {descuento.estado === 'ACTIVO' ? (
          <ToggleLeft className="mr-2 size-4" aria-hidden="true" />
        ) : (
          <ToggleRight className="mr-2 size-4" aria-hidden="true" />
        )}
        {descuento.estado === 'ACTIVO' ? 'Desactivar' : 'Activar'}
      </DropdownMenuItem>
    </DropdownMenuContent>
  </DropdownMenu>
);

export const CatalogoDescuentos = ({
  nuevoAbierto,
  onNuevoAbiertoChange,
}: {
  nuevoAbierto: boolean;
  onNuevoAbiertoChange: (abierto: boolean) => void;
}) => {
  const catalogo = useDescuentos();
  const configuracion = useConfiguracionDescuentos();
  const { addNotification } = useNotifications();
  const [editando, setEditando] = React.useState<DescuentoVista | null>(null);
  const [limiteAbierto, setLimiteAbierto] = React.useState(false);
  const cambiarEstado = useCambiarEstadoDescuento((descuento) =>
    addNotification({
      type: 'success',
      title:
        descuento.estado === 'ACTIVO'
          ? 'Descuento activado'
          : 'Descuento desactivado',
      message: descuento.nombre,
    }),
  );

  if (catalogo.isLoading) {
    return (
      <div className="flex h-60 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <>
      <Card className="mb-6 flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-3">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-surface-accent-light text-content-accent-primary">
            <SlidersHorizontal className="size-5" aria-hidden="true" />
          </span>
          <div>
            <p className="text-label-1 font-semibold text-content-primary">
              Límite institucional de descuento
            </p>
            <p className="mt-1 text-body-4 text-content-tertiary">
              Porcentaje máximo permitido al crear o editar descuentos.
            </p>
          </div>
        </div>
        <div className="flex items-center gap-3 self-end sm:self-auto">
          <strong className="text-h6 text-content-primary">
            {configuracion.data
              ? `${configuracion.data.porcentajeMaximo} %`
              : '—'}
          </strong>
          <Button
            type="button"
            size="sm"
            variant="primary-light"
            icon={<Pencil className="size-4" aria-hidden="true" />}
            disabled={!configuracion.data}
            onClick={() => setLimiteAbierto(true)}
          >
            Cambiar límite
          </Button>
        </div>
      </Card>

      <Card className="p-6">
        <Table<DescuentoVista>
          data={catalogo.data ?? []}
          emptyMessage="Aún no hay descuentos en el catálogo"
          columns={[
            {
              title: 'Descuento',
              field: 'nombre',
              Cell: ({ entry }) => (
                <div>
                  <p className="text-content-primary">{entry.nombre}</p>
                  <p className="text-body-4 text-content-tertiary">
                    {entry.descripcion ?? 'Sin descripción'}
                  </p>
                </div>
              ),
            },
            {
              title: 'Tipo',
              field: 'aplicacion',
              Cell: ({ entry }) => (
                <span>
                  {entry.aplicacion === 'MANUAL'
                    ? 'Manual'
                    : 'Automático por condición'}
                </span>
              ),
            },
            {
              title: 'Porcentaje',
              field: 'porcentaje',
              Cell: ({ entry }) => <span>{porcentaje(entry)}</span>,
            },
            {
              title: 'Aplica a',
              field: 'conceptos',
              Cell: ({ entry }) => <span>{conceptos(entry.conceptos)}</span>,
            },
            {
              title: 'Estado',
              field: 'estado',
              Cell: ({ entry }) => (
                <Tag
                  size="sm"
                  color={entry.estado === 'ACTIVO' ? 'success' : 'neutral'}
                >
                  {entry.estado === 'ACTIVO' ? 'Activo' : 'Inactivo'}
                </Tag>
              ),
            },
            {
              title: '',
              field: 'id',
              Cell: ({ entry }) => (
                <AccionesDescuento
                  descuento={entry}
                  onEditar={() => setEditando(entry)}
                  onCambiarEstado={() =>
                    cambiarEstado.mutate({
                      id: entry.id,
                      activo: entry.estado === 'INACTIVO',
                    })
                  }
                />
              ),
            },
          ]}
        />
      </Card>

      <DialogoDescuento
        abierto={nuevoAbierto}
        onOpenChange={onNuevoAbiertoChange}
      />
      <DialogoDescuento
        abierto={!!editando}
        onOpenChange={(abierto) => !abierto && setEditando(null)}
        descuento={editando}
      />
      {configuracion.data && (
        <DialogoLimiteDescuento
          abierto={limiteAbierto}
          porcentajeMaximo={configuracion.data.porcentajeMaximo}
          onOpenChange={setLimiteAbierto}
        />
      )}
    </>
  );
};
