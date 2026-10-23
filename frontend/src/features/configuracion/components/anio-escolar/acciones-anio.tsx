import { Lock, Pencil } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { ConfirmationDialog } from '@/components/ui/dialog';
import { useNotifications } from '@/components/ui/notifications';
import { Tooltip } from '@/components/ui/tooltip';
import { AnioEscolarDetalle } from '@/types/api';

import {
  anioActivo,
  useAniosEscolares,
  useRequisitosAnio,
} from '../../api/anios-escolares';
import { useActivarAnio, useCerrarAnio } from '../../api/mutaciones-anio';

import { DialogoAnio } from './dialogo-anio';

type ConfirmarAccionProps = {
  trigger: React.ReactElement;
  titulo: string;
  cuerpo: string;
  textoConfirmar: string;
  icono: 'danger' | 'info';
  pendiente: boolean;
  /** Ejecuta la acción; llama a `cerrarDialogo` cuando termina, salga bien o mal (el error se avisa con un toast). */
  onConfirmar: (cerrarDialogo: () => void) => void;
};

/** Confirmación con el modal compartido. Se cierra sola al terminar la acción, también si el backend rechaza. */
const ConfirmarAccion = ({
  trigger,
  titulo,
  cuerpo,
  textoConfirmar,
  icono,
  pendiente,
  onConfirmar,
}: ConfirmarAccionProps) => {
  const [terminada, setTerminada] = React.useState(false);
  // `isDone` cierra el modal; se baja enseguida para que la próxima vez se pueda volver a abrir.
  React.useEffect(() => {
    if (terminada) setTerminada(false);
  }, [terminada]);

  return (
    <ConfirmationDialog
      isDone={terminada}
      icon={icono}
      title={titulo}
      body={cuerpo}
      cancelButtonText="Cancelar"
      triggerButton={trigger}
      confirmButton={
        <Button
          isLoading={pendiente}
          disabled={pendiente}
          onClick={() => onConfirmar(() => setTerminada(true))}
        >
          {textoConfirmar}
        </Button>
      }
    />
  );
};

/**
 * Por qué "Activar año" está deshabilitado, o null si se puede activar. Los textos de "otro año activo" y de
 * requisitos son los del backend.
 */
export const motivoSinActivar = ({
  otroActivo,
  cargando,
  fallo,
  requisitos,
}: {
  otroActivo?: { anio: number };
  cargando: boolean;
  fallo: boolean;
  requisitos?: {
    puedeActivar: boolean;
    obligatorios: { cumplido: boolean; texto: string }[];
  };
}): string | null => {
  if (otroActivo) {
    return `El año ${otroActivo.anio} sigue activo. Ciérralo antes de activar este.`;
  }
  if (fallo) return 'No se pudieron verificar los requisitos.';
  if (cargando || !requisitos) return 'Verificando los requisitos…';
  if (!requisitos.puedeActivar) {
    const faltantes = requisitos.obligatorios
      .filter((r) => !r.cumplido)
      .map((r) => r.texto)
      .join(', ');
    return `Faltan requisitos obligatorios: ${faltantes}.`;
  }
  return null;
};

type AccionesAnioProps = { anio: AnioEscolarDetalle };

// Figma > CI-06 (Editar) y CI-12 (Cerrar año): botones a la derecha del título, según el estado del año.
export const AccionesAnio = ({ anio }: AccionesAnioProps) => {
  const { addNotification } = useNotifications();
  const [editando, setEditando] = React.useState(false);

  const planificado = anio.estado === 'PLANIFICADO';
  const anios = useAniosEscolares(planificado);
  const requisitos = useRequisitosAnio(anio.id, planificado);

  const activar = useActivarAnio((activado) =>
    addNotification({
      type: 'success',
      title: 'Año escolar activado',
      message: `El año escolar ${activado.anio} ya es el año en curso.`,
    }),
  );
  const cerrar = useCerrarAnio((cerrado) =>
    addNotification({
      type: 'success',
      title: 'Año escolar cerrado',
      message: `Se cerró el año escolar ${cerrado.anio}.`,
    }),
  );

  if (anio.estado === 'CERRADO') return null;

  if (anio.estado === 'ACTIVO') {
    return (
      <ConfirmarAccion
        trigger={
          <Button
            variant="tertiary"
            icon={<Lock className="size-4" aria-hidden="true" />}
          >
            Cerrar año
          </Button>
        }
        icono="danger"
        titulo={`¿Cerrar el año escolar ${anio.anio}?`}
        cuerpo="Pasará a ser un registro histórico y ya no podrás editar sus periodos."
        textoConfirmar="Cerrar año"
        pendiente={cerrar.isPending}
        onConfirmar={(cerrarDialogo) =>
          cerrar.mutate(anio.id, { onSettled: cerrarDialogo })
        }
      />
    );
  }

  const motivo = motivoSinActivar({
    otroActivo: anios.data ? anioActivo(anios.data) : undefined,
    cargando: anios.isLoading || requisitos.isLoading,
    fallo: anios.isError || requisitos.isError,
    requisitos: requisitos.data,
  });

  return (
    <>
      <Button
        variant="outline"
        icon={<Pencil className="size-4" aria-hidden="true" />}
        onClick={() => setEditando(true)}
      >
        Editar
      </Button>
      {motivo ? (
        <Tooltip content={motivo} side="bottom" align="end">
          {/* aria-disabled (y no disabled) deja el botón enfocable: así el tooltip también sale con el teclado. */}
          <Button aria-disabled="true">Activar año</Button>
        </Tooltip>
      ) : (
        <ConfirmarAccion
          trigger={<Button>Activar año</Button>}
          icono="info"
          titulo={`¿Activar el año escolar ${anio.anio}?`}
          cuerpo="Pasará a ser el año en curso y ya no podrás editar sus fechas ni su régimen."
          textoConfirmar="Activar año"
          pendiente={activar.isPending}
          onConfirmar={(cerrarDialogo) =>
            activar.mutate(anio.id, { onSettled: cerrarDialogo })
          }
        />
      )}
      <DialogoAnio
        abierto={editando}
        onCerrar={() => setEditando(false)}
        modo="editar"
        anio={anio}
      />
    </>
  );
};
