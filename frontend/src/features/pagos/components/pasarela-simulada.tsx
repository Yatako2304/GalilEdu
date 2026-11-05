import { CreditCard } from 'lucide-react';
import { useNavigate } from 'react-router';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { paths } from '@/config/paths';

import { useOrdenPago, useSimularPago } from '../api/pago-en-linea';

import { soles } from './formato';

// Página de pago de la pasarela SIMULADA (backend con galiledu.pagos.pasarela=simulada). Reemplaza a la página de
// Stripe mientras no hay cuenta: no pide datos de tarjeta ni cobra nada.
export const PasarelaSimulada = ({ codigo }: { codigo: string }) => {
  const consulta = useOrdenPago(codigo);
  const simular = useSimularPago(codigo);
  const navigate = useNavigate();
  const orden = consulta.data;

  const decidir = (aprobar: boolean) =>
    simular.mutate(aprobar, {
      onSuccess: () =>
        navigate(
          `${paths.app.apoderado.resultadoPago.getHref()}?orden=${codigo}`,
        ),
    });

  if (consulta.isLoading) {
    return (
      <div className="flex min-h-40 items-center justify-center">
        <Spinner />
      </div>
    );
  }
  if (!orden) {
    return <Alert type="error" title="No encontramos esta orden de pago." />;
  }

  return (
    <Card className="mx-auto max-w-md space-y-5 p-6">
      <div className="flex items-center gap-3">
        <CreditCard
          className="size-6 text-content-accent-primary"
          aria-hidden="true"
        />
        <div>
          <h2 className="text-label-1 font-semibold text-content-primary">
            Pasarela de pagos (simulación)
          </h2>
          <p className="text-body-4 text-content-tertiary">
            Orden {orden.codigo} · no se cobra dinero real
          </p>
        </div>
      </div>
      <p className="text-h5 font-semibold text-content-primary">
        {soles(orden.total)}
      </p>
      {orden.estado === 'EN_PROCESO' ? (
        <div className="flex flex-col gap-2">
          <Button onClick={() => decidir(true)} isLoading={simular.isPending}>
            Pagar
          </Button>
          <Button
            variant="error-light"
            onClick={() => decidir(false)}
            disabled={simular.isPending}
          >
            Simular tarjeta rechazada
          </Button>
        </div>
      ) : (
        <Alert type="info" title="Esta orden ya se resolvió." />
      )}
    </Card>
  );
};
