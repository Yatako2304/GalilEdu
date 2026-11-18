import { Plus } from 'lucide-react';
import * as React from 'react';

import { ContentLayout } from '@/components/layouts';
import { Button } from '@/components/ui/button';
import { DialogoConfirmacion } from '@/components/ui/dialog/dialogo-confirmacion';
import { useNotifications } from '@/components/ui/notifications';
import { formatDay } from '@/utils/format';

import { useEliminarReserva, useReservas } from '../api/reservas';
import { FILTRO_POR_DEFECTO, formatHora12 } from '../lib/reservas';
import { FiltroDeReservas, ReservaDeAmbiente } from '../types-reservas';

import { DialogoReserva } from './dialogo-reserva';
import { TablaReservas } from './tabla-reservas';

type Dialogo = { reserva?: ReservaDeAmbiente } | null;

// Figma > Coordinador > «Reserva de espacios especiales» (308:1874), HU-48: historial de reservas, «Nueva reserva» y
// edición. Figma no dibuja la edición ni la confirmación de eliminar: se usa el mismo modal («Editar reserva») y el
// modal de confirmación de los demás módulos.
export const ReservasEspacios = () => {
  const { addNotification } = useNotifications();
  const [filtro, setFiltro] =
    React.useState<FiltroDeReservas>(FILTRO_POR_DEFECTO);
  const [page, setPage] = React.useState(1);
  const reservas = useReservas(filtro, page);

  const [dialogo, setDialogo] = React.useState<Dialogo>(null);
  const [porEliminar, setPorEliminar] =
    React.useState<ReservaDeAmbiente | null>(null);

  // Al eliminar la última fila de una página (que no es la primera) se vuelve a la anterior. Se decide al confirmar:
  // cuando llega la respuesta, la lista ya se volvió a pedir y la página puede estar vacía.
  const eraLaUltimaDeLaPagina = React.useRef(false);
  const eliminar = useEliminarReserva(() => {
    addNotification({
      type: 'success',
      title: 'Reserva eliminada.',
      message:
        `${porEliminar?.ambiente.codigo ?? ''} · ${formatDay(porEliminar?.fecha ?? '')}`.trim(),
    });
    setPorEliminar(null);
    if (eraLaUltimaDeLaPagina.current) setPage((actual) => actual - 1);
  });

  const cambiarFiltro = (nuevo: FiltroDeReservas) => {
    setFiltro(nuevo);
    setPage(1);
  };

  return (
    <ContentLayout
      title="Reserva de espacios especiales"
      breadcrumbs={['Horarios', 'Reserva de espacios especiales']}
      actions={
        <Button
          variant="secondary"
          icon={<Plus className="size-4" aria-hidden="true" />}
          iconPosition="right"
          onClick={() => setDialogo({})}
        >
          Nueva reserva
        </Button>
      }
    >
      <TablaReservas
        filtro={filtro}
        onFiltro={cambiarFiltro}
        pagina={reservas.data}
        cargando={reservas.isLoading}
        error={reservas.isError}
        onPagina={setPage}
        onEditar={(reserva) => setDialogo({ reserva })}
        onEliminar={setPorEliminar}
      />

      {dialogo && (
        <DialogoReserva
          // Una reserva distinta es otro formulario: no arrastra lo escrito en el anterior.
          key={dialogo.reserva?.id ?? 'nueva'}
          reserva={dialogo.reserva}
          onCerrar={() => setDialogo(null)}
        />
      )}

      <DialogoConfirmacion
        open={porEliminar !== null}
        onOpenChange={(abierto) => !abierto && setPorEliminar(null)}
        tipo="eliminar"
        titulo="¿Eliminar la reserva?"
        descripcion={
          porEliminar
            ? `Se eliminará la reserva de ${porEliminar.ambiente.nombre} (${porEliminar.ambiente.codigo}) del ${formatDay(porEliminar.fecha)} a las ${formatHora12(porEliminar.horaInicio)}. Esta acción no se puede deshacer.`
            : ''
        }
        textoConfirmar="Eliminar"
        cargando={eliminar.isPending}
        onConfirmar={() => {
          if (!porEliminar) return;
          eraLaUltimaDeLaPagina.current =
            page > 1 && reservas.data?.data.length === 1;
          eliminar.mutate({ id: porEliminar.id, version: porEliminar.version });
        }}
      />
    </ContentLayout>
  );
};
