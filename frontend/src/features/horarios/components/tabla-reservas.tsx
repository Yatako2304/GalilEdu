import { Pencil, Trash2 } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { PaginationBar, Table } from '@/components/ui/table';
import { SelectableTag } from '@/components/ui/tag';
import { Pagina } from '@/types/api';
import { formatDay } from '@/utils/format';

import { formatHora12 } from '../lib/reservas';
import {
  FILTROS_DE_RESERVAS,
  FiltroDeReservas,
  ReservaDeAmbiente,
} from '../types-reservas';

type TablaReservasProps = {
  filtro: FiltroDeReservas;
  onFiltro: (filtro: FiltroDeReservas) => void;
  /** undefined mientras llega la primera página. */
  pagina: Pagina<ReservaDeAmbiente> | undefined;
  cargando: boolean;
  error: boolean;
  onPagina: (page: number) => void;
  onEditar: (reserva: ReservaDeAmbiente) => void;
  onEliminar: (reserva: ReservaDeAmbiente) => void;
};

// Figma > Coordinador > «Reserva de espacios especiales» (308:1874): tarjeta «Historial de reservas» con los filtros de
// fecha como Selectable Tag (Color = Primary, Type = Circle, Size = Medium) y la tabla de reservas.
export const TablaReservas = ({
  filtro,
  onFiltro,
  pagina,
  cargando,
  error,
  onPagina,
  onEditar,
  onEliminar,
}: TablaReservasProps) => (
  <Card>
    <CardHeader>
      <CardTitle size="body-1">Historial de reservas</CardTitle>
    </CardHeader>
    <CardContent className="space-y-4 p-6">
      <div
        role="group"
        aria-label="Filtrar reservas por fecha"
        className="flex flex-wrap gap-2"
      >
        {FILTROS_DE_RESERVAS.map(({ valor, etiqueta }) => (
          <SelectableTag
            key={valor}
            color="primary"
            shape="circle"
            size="md"
            selected={filtro === valor}
            onClick={() => onFiltro(valor)}
          >
            {etiqueta}
          </SelectableTag>
        ))}
      </div>

      {cargando && !pagina ? (
        <div className="flex h-48 items-center justify-center">
          <Spinner size="lg" />
        </div>
      ) : error && !pagina ? (
        <p className="py-10 text-center text-body-3 text-content-quartiary">
          No se pudieron cargar las reservas.
        </p>
      ) : (
        <>
          <Table
            data={pagina?.data ?? []}
            emptyMessage="No hay reservas en este periodo."
            columns={[
              {
                title: 'Espacio',
                field: 'ambiente',
                Cell: ({ entry }) => (
                  <div className="flex items-center gap-3">
                    {/* El punto es el mismo azul oscuro en todas las filas: no depende del tipo de espacio. */}
                    <span
                      aria-hidden="true"
                      className="size-2.5 shrink-0 rounded-full bg-surface-accent-primary"
                    />
                    <div className="min-w-0">
                      <p className="text-body-3 font-semibold text-content-primary">
                        {entry.ambiente.codigo}
                      </p>
                      <p className="text-caption-1 text-content-quartiary">
                        {entry.ambiente.nombre}
                      </p>
                    </div>
                  </div>
                ),
              },
              {
                title: 'Actividad',
                field: 'actividad',
                Cell: ({ entry }) => (
                  <div className="min-w-0">
                    <p className="text-body-3 text-content-primary">
                      {entry.actividad}
                    </p>
                    <p className="text-caption-1 text-content-quartiary">
                      {entry.seccion.etiqueta} · {entry.responsable.nombre}
                    </p>
                  </div>
                ),
              },
              {
                title: 'Estado',
                field: 'estado',
                Cell: ({ entry }) => (
                  <SelectableTag
                    readOnly
                    shape="circle"
                    size="md"
                    color={entry.estado === 'PENDIENTE' ? 'primary' : 'info'}
                  >
                    {entry.estado === 'PENDIENTE' ? 'Pendiente' : 'Finalizado'}
                  </SelectableTag>
                ),
              },
              {
                title: 'Fecha',
                field: 'fecha',
                Cell: ({ entry }) => <span>{formatDay(entry.fecha)}</span>,
              },
              {
                title: 'Hora',
                field: 'horaInicio',
                Cell: ({ entry }) => (
                  <span className="whitespace-nowrap">
                    {formatHora12(entry.horaInicio)}
                  </span>
                ),
              },
              {
                title: 'Acciones',
                field: 'id',
                Cell: ({ entry }) => (
                  <div className="flex items-center gap-2">
                    <Button
                      variant="primary-light"
                      size="sm"
                      icon={<Pencil className="size-4" aria-hidden="true" />}
                      iconPosition="right"
                      disabled={!entry.editable}
                      onClick={() => onEditar(entry)}
                    >
                      Editar
                    </Button>
                    <Button
                      variant="primary-light"
                      size="sm"
                      className="w-8 px-0"
                      aria-label="Eliminar reserva"
                      icon={<Trash2 className="size-4" aria-hidden="true" />}
                      disabled={!entry.editable}
                      onClick={() => onEliminar(entry)}
                    />
                  </div>
                ),
              },
            ]}
          />
          {pagina && pagina.meta.total > 0 && (
            <PaginationBar {...pagina.meta} onPageChange={onPagina} />
          )}
        </>
      )}
    </CardContent>
  </Card>
);
