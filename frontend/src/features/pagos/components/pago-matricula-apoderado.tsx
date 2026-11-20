import { Banknote } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Tag } from '@/components/ui/tag';
import { PagoMatricula } from '@/types/api';

import { useCrearOrdenPago } from '../api/pago-en-linea';

import { fechaDia, soles } from './formato';

// Figma > Apoderado > "HU-31 Derivación a pagos" > "Pago de matrícula".
// El pago usa el mismo flujo en línea de HU-41, solo con la cuota de matrícula.

const etiquetas: Record<
  PagoMatricula['estado'],
  { texto: string; color: 'success' | 'warning' | 'info' | 'neutral' }
> = {
  PENDIENTE: { texto: 'Pendiente de pago', color: 'warning' },
  PAGADA_PARCIAL: { texto: 'Pago parcial', color: 'warning' },
  EN_PROCESO: { texto: 'Pago en proceso', color: 'info' },
  PAGADA: { texto: 'Pagada', color: 'success' },
  ANULADA: { texto: 'Anulada', color: 'neutral' },
};

// Sin cuota (`pago` nulo) la tarjeta se muestra igual, como la del colegio: la cuota recién se genera
// cuando el colegio valida la documentación.
export const PagoMatriculaApoderado = ({
  pago,
  codigoMatricula,
}: {
  pago: PagoMatricula | null;
  /** Código de la matrícula, una vez matriculado (igual que en la tarjeta del colegio). */
  codigoMatricula?: string | null;
}) => {
  const crear = useCrearOrdenPago();

  if (!pago) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Pago de matrícula</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-body-3 text-content-quartiary">
            La cuota de matrícula se genera cuando el colegio valida la
            documentación. Te avisaremos para que puedas pagarla.
          </p>
        </CardContent>
      </Card>
    );
  }

  const etiqueta = etiquetas[pago.estado];
  const pendiente =
    pago.estado === 'PENDIENTE' || pago.estado === 'PAGADA_PARCIAL';

  return (
    <Card>
      <CardHeader>
        <CardTitle>Pago de matrícula</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <dl className="space-y-2 rounded-[10px] bg-surface-secondary px-5 py-4 text-body-3">
          <Dato nombre="Concepto" valor={pago.concepto} />
          <Dato nombre="Monto" valor={soles(pago.monto)} />
          {pago.montoDescuento > 0 && (
            <Dato
              nombre="Descuento"
              valor={`− ${soles(pago.montoDescuento)}`}
            />
          )}
          <div className="flex items-start justify-between gap-4">
            <dt className="text-content-quartiary">Estado</dt>
            <dd>
              <Tag size="sm" color={etiqueta.color}>
                {etiqueta.texto}
                {pago.fechaPago ? ` · ${fechaDia(pago.fechaPago)}` : ''}
              </Tag>
            </dd>
          </div>
          <Dato nombre="Vencimiento" valor={fechaDia(pago.fechaVencimiento)} />
          <Dato nombre="Responsable" valor={pago.responsable} />
          {codigoMatricula && (
            <Dato nombre="Código de matrícula" valor={codigoMatricula} />
          )}
        </dl>
        {pendiente && (
          <div className="flex flex-wrap items-center gap-3">
            <Button
              icon={<Banknote className="size-4" />}
              disabled={!pago.puedePagar}
              isLoading={crear.isPending}
              onClick={() => crear.mutate([pago.cuotaId])}
            >
              Pagar matrícula
            </Button>
            {!pago.puedePagar && pago.motivo && (
              <p className="text-body-3 text-content-quartiary">
                {pago.motivo}
              </p>
            )}
          </div>
        )}
        {!pendiente && pago.motivo && (
          <p className="text-body-3 text-content-quartiary">{pago.motivo}</p>
        )}
      </CardContent>
    </Card>
  );
};

const Dato = ({ nombre, valor }: { nombre: string; valor: string }) => (
  <div className="flex items-start justify-between gap-4">
    <dt className="text-content-quartiary">{nombre}</dt>
    <dd className="text-right font-medium text-content-primary">{valor}</dd>
  </div>
);
