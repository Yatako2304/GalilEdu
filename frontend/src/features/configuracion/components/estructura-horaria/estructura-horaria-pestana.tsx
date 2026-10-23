import * as React from 'react';

import { Alert } from '@/components/ui/alert';
import { Spinner } from '@/components/ui/spinner';
import { DetalleEstructuraHoraria, EstadoAnioEscolar } from '@/types/api';

import { useEstructurasHorarias } from '../../api/estructuras-horarias';

import { DialogoEliminarEstructura } from './dialogo-eliminar-estructura';
import { DialogoEstructura } from './dialogo-estructura';
import { ListaEstructuras } from './lista-estructuras';
import { TarjetaEstructura } from './tarjeta-estructura';

export type EstructuraHorariaPestanaProps = {
  anioId: string;
  estadoAnio: EstadoAnioEscolar;
};

// Figma > Personal Administrativo > CI-12 · Estructura horaria (HU-44): a la izquierda la lista "Estructuras del año" y
// a la derecha la tarjeta de la estructura elegida con su jornada semanal. Si "Nueva" está bloqueado no es por
// `estadoAnio` sino por `anioModificable`, que decide el backend (solo un año PLANIFICADO admite cambios).
export const EstructuraHorariaPestana = ({
  anioId,
}: EstructuraHorariaPestanaProps) => {
  const consulta = useEstructurasHorarias(anioId);
  const [elegidaId, setElegidaId] = React.useState<string>();
  const [creando, setCreando] = React.useState(false);
  // Editar y Eliminar actúan sobre la estructura elegida, con el detalle que ya cargó su tarjeta.
  const [accion, setAccion] = React.useState<{
    tipo: 'editar' | 'eliminar';
    estructura: DetalleEstructuraHoraria;
  } | null>(null);

  if (!consulta.data) {
    return consulta.isError ? (
      <Alert type="error" title="No pudimos cargar las estructuras horarias.">
        Vuelve a intentarlo en unos minutos.
      </Alert>
    ) : (
      <div className="flex h-60 items-center justify-center">
        <Spinner />
      </div>
    );
  }

  const { anioModificable, estructuras } = consulta.data;
  // Si aún no se eligió ninguna (o la elegida ya no existe, por ejemplo tras eliminarla), se muestra la primera.
  const elegida = estructuras.find((e) => e.id === elegidaId) ?? estructuras[0];

  return (
    <div className="grid items-start gap-5 lg:grid-cols-[300px_minmax(0,1fr)]">
      <ListaEstructuras
        estructuras={estructuras}
        seleccionadaId={elegida?.id}
        onElegir={setElegidaId}
        anioModificable={anioModificable}
        onNueva={() => setCreando(true)}
      />
      {elegida && (
        <TarjetaEstructura
          estructuraId={elegida.id}
          onEditar={(estructura) => setAccion({ tipo: 'editar', estructura })}
          onEliminar={(estructura) =>
            setAccion({ tipo: 'eliminar', estructura })
          }
        />
      )}
      {creando && anioModificable && (
        <DialogoEstructura
          anioId={anioId}
          onCerrar={() => setCreando(false)}
          // La estructura creada queda elegida en la lista.
          onGuardada={setElegidaId}
        />
      )}
      {accion?.tipo === 'editar' && anioModificable && (
        <DialogoEstructura
          anioId={anioId}
          estructura={accion.estructura}
          onCerrar={() => setAccion(null)}
          onGuardada={setElegidaId}
        />
      )}
      {accion?.tipo === 'eliminar' && anioModificable && (
        <DialogoEliminarEstructura
          estructura={accion.estructura}
          onCerrar={() => setAccion(null)}
          // Sin la eliminada, la lista vuelve a mostrar la primera estructura.
          onEliminada={() => setElegidaId(undefined)}
        />
      )}
    </div>
  );
};
