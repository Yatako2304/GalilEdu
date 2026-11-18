import { Search } from 'lucide-react';

import { Input, Select } from '@/components/ui/form';
import { SelectableTag } from '@/components/ui/tag';
import { TipoAmbiente } from '@/types/api';

/** `TODOS` no envía el filtro de estado: lista activos e inactivos. */
export type FiltroEstado = 'ACTIVO' | 'INACTIVO' | 'TODOS';

/** Con qué estado abre la pantalla: solo los activos. */
export const ESTADO_INICIAL: FiltroEstado = 'ACTIVO';

export type FiltroTipo = TipoAmbiente | '';

const CHIPS_TIPO: { valor: FiltroTipo; etiqueta: string }[] = [
  { valor: '', etiqueta: 'Todos' },
  { valor: 'AULA_REGULAR', etiqueta: 'Aulas regulares' },
  { valor: 'ESPACIO_ESPECIAL', etiqueta: 'Espacios especiales' },
];

const OPCIONES_ESTADO: { value: FiltroEstado; label: string }[] = [
  { value: 'TODOS', label: 'Todos los estados' },
  { value: 'ACTIVO', label: 'Activos' },
  { value: 'INACTIVO', label: 'Inactivos' },
];

type AmbientesFiltrosProps = {
  busqueda: string;
  onBusqueda: (texto: string) => void;
  tipo: FiltroTipo;
  onTipo: (tipo: FiltroTipo) => void;
  estado: FiltroEstado;
  onEstado: (estado: FiltroEstado) => void;
};

// Figma > Personal Administrativo > CI-26 · Ambientes físicos (nodo 857:10215): buscador, chips de tipo y estado.
export const AmbientesFiltros = ({
  busqueda,
  onBusqueda,
  tipo,
  onTipo,
  estado,
  onEstado,
}: AmbientesFiltrosProps) => (
  <div className="flex flex-wrap items-center gap-3">
    {/* Figma dibuja el campo sin texto de ayuda: solo el ícono de la lupa. */}
    <Input
      aria-label="Buscar ambiente por nombre"
      value={busqueda}
      onChange={(event) => onBusqueda(event.target.value)}
      startAdornment={<Search className="size-4" aria-hidden="true" />}
      wrapperClassName="w-full max-w-80"
    />
    <div
      role="group"
      aria-label="Filtrar por tipo de ambiente"
      className="flex flex-wrap gap-2"
    >
      {CHIPS_TIPO.map(({ valor, etiqueta }) => (
        <SelectableTag
          key={etiqueta}
          type="button"
          color="primary"
          size="md"
          selected={tipo === valor}
          aria-pressed={tipo === valor}
          onClick={() => onTipo(valor)}
        >
          {etiqueta}
        </SelectableTag>
      ))}
    </div>
    <Select
      aria-label="Filtrar por estado"
      wrapperClassName="w-52"
      value={estado}
      onChange={(event) => onEstado(event.target.value as FiltroEstado)}
      options={OPCIONES_ESTADO}
    />
  </div>
);
