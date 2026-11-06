import * as React from 'react';
import { Link } from 'react-router';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { paths } from '@/config/paths';
import { OrdenPagoDetalle } from '@/types/api';

import { useCancelarOrdenPago, useOrdenPago } from '../api/pago-en-linea';

import { soles } from './formato';

// HU-41 · Pantalla a la que vuelve el apoderado desde la pasarela (CAHU41-6, 7, 8 y 9). Si la pasarela aún no avisa,
// el backend la consulta directamente y aquí se vuelve a preguntar cada 3 segundos.

const Volver = ({ estudianteId }: { estudianteId?: string }) => (
  <Button asChild variant="primary-light">
    <Link
      to={`${paths.app.apoderado.pensiones.getHref()}${estudianteId ? `?estudiante=${estudianteId}` : ''}`}
    >
      Volver a Pensiones
    </Link>
  </Button>
);

const Resumen = ({ orden }: { orden: OrdenPagoDetalle }) => (
  <ul className="divide-y divide-stroke-tertiary rounded-lg border border-stroke-tertiary">
    {orden.cuotas.map((c) => (
      <li
        key={c.cuotaId}
        className="flex items-center justify-between gap-4 px-4 py-3 text-body-3"
      >
        <span className="text-content-primary">
          {c.nombre}
          <span className="block text-body-4 text-content-tertiary">
            {c.estudiante}
            {c.recargo > 0 && ` · incluye ${soles(c.recargo)} de recargo`}
          </span>
        </span>
        <span className="shrink-0 text-content-primary">
          {soles(c.capital + c.recargo)}
        </span>
      </li>
    ))}
    <li className="flex justify-between px-4 py-3 text-label-2 font-semibold">
      <span>Total</span>
      <span>{soles(orden.total)}</span>
    </li>
  </ul>
);

export const ResultadoPago = ({
  codigo,
  cancelado,
}: {
  codigo: string;
  cancelado: boolean;
}) => {
  const consulta = useOrdenPago(codigo);
  const cancelar = useCancelarOrdenPago();
  const orden = consulta.data;

  // Volvió con "Atrás" desde la pasarela: se cierra la orden para liberar sus cuotas (una sola vez).
  const yaCancelada = React.useRef(false);
  React.useEffect(() => {
    if (cancelado && orden?.estado === 'EN_PROCESO' && !yaCancelada.current) {
      yaCancelada.current = true;
      cancelar.mutate(orden.codigo);
    }
  }, [cancelado, orden, cancelar]);

  if (consulta.isLoading) {
    return (
      <div className="flex min-h-40 items-center justify-center">
        <Spinner />
      </div>
    );
  }
  if (!orden) {
    return (
      <Alert type="error" title="No encontramos esta orden de pago.">
        <Volver />
      </Alert>
    );
  }

  // Si el pago fue de un solo estudiante, "Volver a Pensiones" abre las pensiones de ese estudiante.
  const estudiantes = [...new Set(orden.cuotas.map((c) => c.estudianteId))];
  const estudianteDelPago =
    estudiantes.length === 1 ? estudiantes[0] : undefined;

  return (
    <Card className="mx-auto max-w-2xl space-y-5 p-6">
      {orden.estado === 'EN_PROCESO' && (
        <div className="flex items-center gap-3 text-body-2 text-content-secondary">
          <Spinner size="sm" />
          Estamos confirmando tu pago con la pasarela…
        </div>
      )}
      {orden.estado === 'APROBADA' && (
        <Alert type="success" title="Pago realizado">
          Pagaste {soles(orden.montoConfirmado ?? orden.total)}. Ya puedes ver
          las cuotas como pagadas en tu cronograma.
        </Alert>
      )}
      {orden.estado === 'RECHAZADA' && (
        <Alert type="error" title="No se pudo completar el pago">
          {orden.motivo ?? 'La pasarela rechazó el pago.'} No se cobró nada y
          puedes intentarlo de nuevo.
        </Alert>
      )}
      {orden.estado === 'EXPIRADA' && (
        <Alert type="warning" title="El pago venció">
          Pasaron 30 minutos sin que se completara el pago. No se cobró nada y
          puedes intentarlo de nuevo.
        </Alert>
      )}

      <div className="space-y-2">
        <h2 className="text-label-2 font-semibold text-content-primary">
          Orden {orden.codigo}
        </h2>
        <Resumen orden={orden} />
      </div>

      {orden.constancias.length > 0 && (
        <div className="space-y-2">
          <h2 className="text-label-2 font-semibold text-content-primary">
            {orden.constancias.length === 1 ? 'Constancia' : 'Constancias'}
          </h2>
          <ul className="space-y-1 text-body-3 text-content-secondary">
            {orden.constancias.map((c) => (
              <li key={c.numero}>
                <span className="font-semibold text-content-primary">
                  {c.numero}
                </span>{' '}
                · {c.estudiante} · {soles(c.monto)}
                {c.estado === 'OBSERVADO' && ' · en revisión por el colegio'}
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="flex flex-wrap gap-3">
        <Volver estudianteId={estudianteDelPago} />
        <div className="ml-auto flex gap-3">
          {orden.estado === 'APROBADA' &&
            [
              ...new Set(
                orden.cuotas
                  .filter((c) => c.matricula)
                  .map((c) => c.estudianteId),
              ),
            ].map((estudianteId) => (
              <Button key={estudianteId} asChild>
                <Link
                  to={`${paths.app.apoderado.matricula.getHref()}?estudiante=${estudianteId}`}
                >
                  Ver matrícula
                </Link>
              </Button>
            ))}
        </div>
      </div>
    </Card>
  );
};
