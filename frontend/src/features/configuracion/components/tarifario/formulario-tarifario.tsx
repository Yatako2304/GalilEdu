import { ArrowLeft, CircleCheck } from 'lucide-react';
import * as React from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { useNotifications } from '@/components/ui/notifications';
import { paths } from '@/config/paths';
import { SolicitudTarifario, TarifarioVista } from '@/types/api';

import { useConfirmarTarifario } from '../../api/mutaciones-tarifario';

import { CalendarioCuotas } from './calendario-cuotas';
import { ConceptosMontos } from './conceptos-montos';
import { DialogoConfirmarTarifario } from './dialogo-confirmar-tarifario';
import {
  ValoresTarifario,
  aNumero,
  multiplicarMonto,
  resolverTarifario,
  solicitudDeTarifario,
  valoresDeTarifario,
} from './esquemas';
import { PoliticaMorosidad } from './politica-morosidad';

type FormularioTarifarioProps = {
  tarifario: TarifarioVista;
  /** Avisa si hay cambios sin confirmar, para que la pantalla los proteja al salir o al cambiar de año. */
  onPendientesChange?: (hayPendientes: boolean) => void;
};

const TEXTO_DESACTUALIZADO_EDITABLE =
  'Las fechas del año escolar cambiaron después de confirmar este tarifario. Vuelve a confirmarlo para actualizar el calendario.';
const TEXTO_DESACTUALIZADO_SOLO_LECTURA =
  'Las fechas del año escolar ya no coinciden con este calendario, pero no se puede regenerar porque ya hay compromisos de pago.';

// Figma > Personal Administrativo > "P19 · Tarifario y calendario" (HU-34): montos, política de morosidad y calendario
// de cuotas del año, con "Volver" y "Confirmar tarifario". Sin "Guardar": "Confirmar tarifario" guarda y confirma a la
// vez, después de mostrar el resumen en el modal de Figma. Con el tarifario en solo lectura (año cerrado o con
// compromisos de pago) los campos quedan deshabilitados y no hay botón de confirmar.
export const FormularioTarifario = ({
  tarifario,
  onPendientesChange,
}: FormularioTarifarioProps) => {
  const { addNotification } = useNotifications();
  const confirmar = useConfirmarTarifario();
  const [aConfirmar, setAConfirmar] = React.useState<SolicitudTarifario | null>(
    null,
  );
  const form = useForm<ValoresTarifario>({
    defaultValues: valoresDeTarifario(tarifario),
    resolver: resolverTarifario,
  });
  const { cuotas } = tarifario.calendario;

  const { isDirty } = form.formState;
  React.useEffect(() => {
    onPendientesChange?.(isDirty);
    return () => onPendientesChange?.(false);
  }, [isDirty, onPendientesChange]);

  // El calendario sigue a la pensión que se escribe; la cantidad de cuotas es la del backend, que genera el calendario.
  const pension = aNumero(form.watch('montoPension'));
  const total =
    pension === null ? null : multiplicarMonto(pension, cuotas.length);

  const deshabilitado = tarifario.soloLectura || confirmar.isPending;

  const abrirResumen = form.handleSubmit((valores) =>
    setAConfirmar(solicitudDeTarifario(valores)),
  );

  const confirmarTarifario = async () => {
    if (!aConfirmar) return;
    try {
      await confirmar.mutateAsync({
        anioEscolarId: tarifario.anioEscolarId,
        data: aConfirmar,
      });
      addNotification({
        type: 'success',
        title: 'Tarifario confirmado',
        message: `Los montos y el calendario del año escolar ${tarifario.anio} quedaron confirmados.`,
      });
    } catch {
      // El motivo (por ejemplo, que el año ya tiene compromisos) ya salió como notificación.
    } finally {
      setAConfirmar(null);
    }
  };

  return (
    <form
      noValidate
      onSubmit={abrirResumen}
      aria-label="Tarifario y calendario"
      className="flex flex-col gap-6"
    >
      {tarifario.soloLectura && tarifario.motivoSoloLectura && (
        <Alert type="info" title={tarifario.motivoSoloLectura} />
      )}
      {tarifario.calendarioDesactualizado && (
        <Alert
          type="warning"
          title={
            tarifario.soloLectura
              ? TEXTO_DESACTUALIZADO_SOLO_LECTURA
              : TEXTO_DESACTUALIZADO_EDITABLE
          }
        />
      )}

      <ConceptosMontos
        form={form}
        tarifario={tarifario}
        deshabilitado={deshabilitado}
      />
      <PoliticaMorosidad form={form} deshabilitado={deshabilitado} />
      <CalendarioCuotas cuotas={cuotas} montoPorCuota={pension} total={total} />

      <div className="flex justify-end gap-3">
        <Button asChild variant="outline">
          <Link to={paths.app.administrativo.pensiones.getHref()}>
            <ArrowLeft className="size-4" aria-hidden="true" />
            Volver
          </Link>
        </Button>
        {!tarifario.soloLectura && (
          <Button
            type="submit"
            disabled={confirmar.isPending}
            icon={<CircleCheck className="size-4" aria-hidden="true" />}
          >
            Confirmar tarifario
          </Button>
        )}
      </div>

      <DialogoConfirmarTarifario
        abierto={aConfirmar !== null}
        anio={tarifario.anio}
        tarifario={aConfirmar}
        cantidadCuotas={cuotas.length}
        pendiente={confirmar.isPending}
        onConfirmar={confirmarTarifario}
        onCancelar={() => setAConfirmar(null)}
      />
    </form>
  );
};
