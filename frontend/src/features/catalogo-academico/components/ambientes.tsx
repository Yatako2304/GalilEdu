import { CirclePlus } from 'lucide-react';
import * as React from 'react';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { useDebouncedValue } from '@/hooks/use-debounced-value';
import { AmbienteResumen } from '@/types/api';

import { useAmbientes, useResumenAmbientes } from '../api/ambientes';

import { AmbientesFiltros, ESTADO_INICIAL } from './ambientes-filtros';
import type { FiltroEstado, FiltroTipo } from './ambientes-filtros';
import { Orden, ordenarAmbientes } from './ambientes-orden';
import { AmbientesResumen } from './ambientes-resumen';
import { AmbientesTabla } from './ambientes-tabla';
import {
  CambioEstadoAmbiente,
  DialogoCambiarEstadoAmbiente,
} from './dialogo-cambiar-estado-ambiente';

const MENSAJE_SIN_AMBIENTES = 'No hay ambientes disponibles.';
const MENSAJE_SIN_COINCIDENCIAS =
  'No hay ambientes que coincidan con los filtros.';

type AmbientesProps = {
  /** Abre el modal "Nuevo ambiente físico". Sin esta función, el botón no se muestra. */
  onNuevo?: () => void;
  /** Abre el modal de edición. Sin esta función, "Editar" aparece deshabilitado en el menú de la fila. */
  onEditar?: (ambiente: AmbienteResumen) => void;
};

// Figma > Personal Administrativo > "CI-26 · Ambientes físicos (HU-43)" (nodo 857:10215).
// El título, las migas de pan y la descripción ("Registra las aulas y espacios especiales…") los pone la ruta.
export const Ambientes = ({ onNuevo, onEditar }: AmbientesProps) => {
  const [busqueda, setBusqueda] = React.useState('');
  const [tipo, setTipo] = React.useState<FiltroTipo>('');
  const [estado, setEstado] = React.useState<FiltroEstado>(ESTADO_INICIAL);
  const [orden, setOrden] = React.useState<Orden>({
    campo: 'nombre',
    direccion: 'asc',
  });
  const [cambio, setCambio] = React.useState<CambioEstadoAmbiente | null>(null);

  // El buscador espera a que se haga una pausa para no consultar en cada tecla.
  const q = useDebouncedValue(busqueda.trim());
  const lista = useAmbientes({
    tipo: tipo || undefined,
    estado: estado === 'TODOS' ? undefined : estado,
    q,
  });
  const resumen = useResumenAmbientes();

  const ambientes = React.useMemo(
    () => ordenarAmbientes(lista.data ?? [], orden),
    [lista.data, orden],
  );
  // Con el estado de apertura (solo activos) y sin nada más, una lista vacía significa que no hay ambientes.
  const hayFiltros = q !== '' || tipo !== '' || estado !== ESTADO_INICIAL;
  // "de M": todos los ambientes registrados (activos e inactivos), como la tarjeta "de N registrados".
  const registrados = Math.max(resumen.data?.total ?? 0, ambientes.length);

  return (
    <>
      {onNuevo && (
        <div className="flex justify-end">
          <Button icon={<CirclePlus className="size-4" />} onClick={onNuevo}>
            Nuevo ambiente
          </Button>
        </div>
      )}

      <AmbientesResumen />

      <AmbientesFiltros
        busqueda={busqueda}
        onBusqueda={setBusqueda}
        tipo={tipo}
        onTipo={setTipo}
        estado={estado}
        onEstado={setEstado}
      />

      <Card className="p-6">
        {lista.isError && !lista.data ? (
          <Alert
            type="error"
            title="No pudimos cargar los ambientes."
            action={
              <Button
                size="sm"
                variant="tertiary"
                onClick={() => lista.refetch()}
              >
                Reintentar
              </Button>
            }
          />
        ) : !lista.data ? (
          <div
            role="status"
            aria-label="Cargando ambientes"
            className="flex h-60 items-center justify-center"
          >
            <Spinner />
          </div>
        ) : (
          <>
            <AmbientesTabla
              ambientes={ambientes}
              orden={orden}
              onOrden={setOrden}
              mensajeVacio={
                hayFiltros ? MENSAJE_SIN_COINCIDENCIAS : MENSAJE_SIN_AMBIENTES
              }
              onEditar={onEditar}
              onCambioEstado={setCambio}
            />
            <p className="mt-4 text-body-4 text-content-tertiary">
              Mostrando {ambientes.length} de {registrados}{' '}
              {registrados === 1 ? 'ambiente' : 'ambientes'}
            </p>
          </>
        )}
      </Card>

      <DialogoCambiarEstadoAmbiente
        cambio={cambio}
        onCerrar={() => setCambio(null)}
      />
    </>
  );
};
