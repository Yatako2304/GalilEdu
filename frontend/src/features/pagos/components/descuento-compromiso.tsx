import { MinusCircle } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { Tag } from '@/components/ui/tag';
import { formatDate, formatDay } from '@/utils/format';

import { useDescuentoCompromiso } from '../api/descuentos';

import { DialogoRevocarDescuento } from './dialogo-revocar-descuento';

const vigencia = (desde: string, hasta: string | null) =>
  hasta
    ? `${formatDay(desde)} – ${formatDay(hasta)}`
    : `Desde ${formatDay(desde)}`;

export const DescuentoCompromiso = ({
  compromisoId,
}: {
  compromisoId: string;
}) => {
  const descuento = useDescuentoCompromiso(compromisoId);
  const [revocar, setRevocar] = React.useState(false);

  if (descuento.isLoading) {
    return (
      <div className="flex h-48 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  const datos = descuento.data;
  if (!datos?.activo) {
    return (
      <Card className="p-8 text-center text-body-3 text-content-tertiary">
        El estudiante no tiene un descuento activo.
      </Card>
    );
  }

  const activo = datos.activo;
  return (
    <div className="space-y-4">
      <div className="rounded-xl bg-surface-secondary p-5">
        <dl className="space-y-3">
          <div className="flex items-start justify-between gap-4">
            <dt className="text-body-3 text-content-tertiary">Descuento</dt>
            <dd className="flex items-center gap-2 text-right text-body-3 font-medium text-content-primary">
              {activo.nombre}
              <Tag size="sm" color="success">
                Activo
              </Tag>
            </dd>
          </div>
          <div className="flex items-start justify-between gap-4">
            <dt className="text-body-3 text-content-tertiary">
              Porcentaje aplicado
            </dt>
            <dd className="text-right text-body-3 font-medium text-content-primary">
              {activo.porcentaje} %
            </dd>
          </div>
          <div className="flex items-start justify-between gap-4">
            <dt className="text-body-3 text-content-tertiary">Vigencia</dt>
            <dd className="text-right text-body-3 font-medium text-content-primary">
              {vigencia(activo.vigenciaDesde, activo.vigenciaHasta)} (
              {datos.cuotasConDescuento} periodos)
            </dd>
          </div>
          <div className="flex items-start justify-between gap-4">
            <dt className="text-body-3 text-content-tertiary">
              Fecha de aplicación
            </dt>
            <dd className="text-right text-body-3 font-medium text-content-primary">
              {formatDate(activo.aplicadoEn)}
            </dd>
          </div>
          <div className="flex items-start justify-between gap-4">
            <dt className="text-body-3 text-content-tertiary">Motivo</dt>
            <dd className="max-w-[70%] text-right text-body-3 font-medium text-content-primary">
              {activo.motivo}
            </dd>
          </div>
        </dl>
      </div>

      <Button
        variant="error-light"
        icon={<MinusCircle className="size-4" />}
        onClick={() => setRevocar(true)}
      >
        Revocar descuento
      </Button>

      <DialogoRevocarDescuento
        abierto={revocar}
        onOpenChange={setRevocar}
        compromisoId={compromisoId}
        descuentoAplicadoId={activo.id}
        nombre={activo.nombre}
      />
    </div>
  );
};
