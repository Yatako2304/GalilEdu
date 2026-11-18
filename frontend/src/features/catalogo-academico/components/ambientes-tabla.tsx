import { CircleCheck, CircleOff, EllipsisVertical, Pencil } from 'lucide-react';

import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
import { Table } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { AmbienteResumen } from '@/types/api';
import { cn } from '@/utils/cn';

import {
  ETIQUETA_ESTADO,
  ETIQUETA_TIPO,
  Orden,
  esCampoOrden,
} from './ambientes-orden';
import { CambioEstadoAmbiente } from './dialogo-cambiar-estado-ambiente';

type AmbientesTablaProps = {
  /** Ya ordenados. */
  ambientes: AmbienteResumen[];
  orden: Orden;
  onOrden: (orden: Orden) => void;
  /** Mensaje cuando no hay filas (distinto si hay filtros puestos). */
  mensajeVacio: string;
  /** Sin esta función, "Editar" aparece deshabilitado (el modal de edición no existe todavía). */
  onEditar?: (ambiente: AmbienteResumen) => void;
  onCambioEstado: (cambio: CambioEstadoAmbiente) => void;
};

const plural = (n: number, singular: string, varios: string) =>
  `${n} ${n === 1 ? singular : varios}`;

// Figma > Personal Administrativo > CI-26 · Ambientes físicos (nodo 857:10215): tabla de ambientes.
// Figma dibuja primero una columna "Código"; no se muestra porque el código es interno (lo genera el backend).
export const AmbientesTabla = ({
  ambientes,
  orden,
  onOrden,
  mensajeVacio,
  onEditar,
  onCambioEstado,
}: AmbientesTablaProps) => (
  <Table<AmbienteResumen>
    data={ambientes}
    emptyMessage={mensajeVacio}
    sort={{
      field: orden.campo,
      direction: orden.direccion,
      onSort: (campo) => {
        if (!esCampoOrden(campo)) return;
        onOrden({
          campo,
          direccion:
            campo === orden.campo && orden.direccion === 'asc' ? 'desc' : 'asc',
        });
      },
    }}
    columns={[
      {
        title: 'Nombre',
        field: 'nombre',
        sortable: true,
        Cell: ({ entry }) => (
          <Atenuado ambiente={entry}>
            <span className="text-body-3 text-content-primary">
              {entry.nombre}
            </span>
          </Atenuado>
        ),
      },
      {
        title: 'Tipo',
        field: 'tipo',
        sortable: true,
        Cell: ({ entry }) => (
          <Atenuado ambiente={entry}>
            {/* Figma: aula regular en azul y espacio especial en naranja. */}
            <Tag
              size="sm"
              color={entry.tipo === 'AULA_REGULAR' ? 'info' : 'warning'}
            >
              {ETIQUETA_TIPO[entry.tipo]}
            </Tag>
          </Atenuado>
        ),
      },
      {
        title: 'Aforo máx.',
        field: 'aforoMaximo',
        sortable: true,
        Cell: ({ entry }) => (
          <Atenuado ambiente={entry}>
            <span className="text-body-3 text-content-secondary">
              {plural(entry.aforoMaximo, 'persona', 'personas')}
            </span>
          </Atenuado>
        ),
      },
      {
        title: 'Recursos',
        field: 'recursos',
        sortable: true,
        Cell: ({ entry }) => (
          <Atenuado ambiente={entry}>
            {entry.recursos.length === 0 ? (
              <span className="text-body-3 text-content-tertiary">—</span>
            ) : (
              <ul
                aria-label={`Recursos de ${entry.nombre}`}
                className="flex flex-wrap gap-1"
              >
                {entry.recursos.map((recurso) => (
                  <li key={recurso}>
                    <Tag size="sm" color="neutral">
                      {recurso}
                    </Tag>
                  </li>
                ))}
              </ul>
            )}
          </Atenuado>
        ),
      },
      {
        title: 'Estado',
        field: 'estado',
        sortable: true,
        Cell: ({ entry }) => (
          <Atenuado ambiente={entry}>
            <Tag
              size="sm"
              color={entry.estado === 'ACTIVO' ? 'success' : 'neutral'}
            >
              {ETIQUETA_ESTADO[entry.estado]}
            </Tag>
          </Atenuado>
        ),
      },
      {
        title: '',
        field: 'id',
        Cell: ({ entry }) => (
          <Atenuado ambiente={entry}>
            <AccionesAmbiente
              ambiente={entry}
              onEditar={onEditar}
              onCambioEstado={onCambioEstado}
            />
          </Atenuado>
        ),
      },
    ]}
  />
);

/** Fila inactiva: el contenido de cada celda se ve atenuado, como en Figma (la fila de A-205). */
const Atenuado = ({
  ambiente,
  children,
}: {
  ambiente: AmbienteResumen;
  children: React.ReactNode;
}) => (
  <div className={cn(ambiente.estado === 'INACTIVO' && 'opacity-50')}>
    {children}
  </div>
);

const AccionesAmbiente = ({
  ambiente,
  onEditar,
  onCambioEstado,
}: {
  ambiente: AmbienteResumen;
  onEditar?: (ambiente: AmbienteResumen) => void;
  onCambioEstado: (cambio: CambioEstadoAmbiente) => void;
}) => {
  const activo = ambiente.estado === 'ACTIVO';
  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="inline-flex size-8 items-center justify-center rounded-xl text-content-tertiary hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus">
        <EllipsisVertical className="size-4" aria-hidden="true" />
        <span className="sr-only">Acciones de {ambiente.nombre}</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-40">
        <DropdownMenuItem
          disabled={!onEditar}
          onClick={() => onEditar?.(ambiente)}
        >
          <Pencil className="mr-2 size-4" aria-hidden="true" /> Editar
        </DropdownMenuItem>
        <DropdownMenuItem
          onClick={() =>
            onCambioEstado({
              ambiente,
              accion: activo ? 'desactivar' : 'reactivar',
            })
          }
          className={
            activo ? 'text-content-error focus:text-content-error' : ''
          }
        >
          {activo ? (
            <CircleOff className="mr-2 size-4" aria-hidden="true" />
          ) : (
            <CircleCheck className="mr-2 size-4" aria-hidden="true" />
          )}
          {activo ? 'Desactivar' : 'Reactivar'}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
