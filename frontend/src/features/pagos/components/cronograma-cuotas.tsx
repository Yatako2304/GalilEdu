import { Ban } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import {
  TableBody,
  TableCell,
  TableElement,
  TableFooter,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { CompromisoDetalle } from '@/types/api';
import { cn } from '@/utils/cn';
import { formatDay } from '@/utils/format';

import { etiquetaCuota, soles } from './formato';

const PESTANAS = [
  'Cronograma de cuotas',
  'Pagos',
  'Descuento',
  'Morosidad',
] as const;

const Indicador = ({
  etiqueta,
  valor,
  detalle,
}: {
  etiqueta: string;
  valor: string;
  detalle: string;
}) => (
  <div className="flex flex-1 flex-col gap-1 px-5 py-4">
    <p className="text-body-4 text-content-tertiary">{etiqueta}</p>
    <p className="text-h6 font-semibold text-content-primary">{valor}</p>
    <p className="text-body-4 text-content-tertiary">{detalle}</p>
  </div>
);

const ResumenCompromiso = ({
  compromiso,
}: {
  compromiso: CompromisoDetalle;
}) => {
  const r = compromiso.resumen;
  return (
    <Card className="flex flex-col divide-y divide-stroke-tertiary md:flex-row md:divide-x md:divide-y-0">
      <Indicador
        etiqueta="Total anual"
        valor={soles(r.totalAnual)}
        detalle={`${r.cuotasActivas} cuotas activas`}
      />
      <Indicador
        etiqueta="Capital pagado"
        valor={soles(r.capitalPagado)}
        detalle={`${r.pensionesPagadas} de ${r.pensionesTotal} pensiones`}
      />
      <Indicador
        etiqueta="Descuento"
        valor={r.descuentoPorcentaje ? `${r.descuentoPorcentaje} %` : '—'}
        detalle={r.descuentoNombre ?? 'Sin descuento'}
      />
      <Indicador
        etiqueta="Pensión mensual"
        valor={soles(r.pensionMensual)}
        detalle={`Tarifario ${compromiso.anio}`}
      />
      <Indicador
        etiqueta="Saldo pendiente"
        valor={soles(r.saldoPendiente)}
        detalle={
          r.cuotasVencidas > 0
            ? `${r.cuotasVencidas} cuota(s) vencida(s)`
            : 'Sin cuotas vencidas'
        }
      />
    </Card>
  );
};

const TablaCuotas = ({ compromiso }: { compromiso: CompromisoDetalle }) => (
  <TableElement className="whitespace-nowrap">
    <TableHeader>
      <TableRow>
        <TableHead>Cuota</TableHead>
        <TableHead>Vencimiento</TableHead>
        <TableHead className="text-right">Monto base</TableHead>
        <TableHead className="text-right">Descuento</TableHead>
        <TableHead className="text-right">Monto final</TableHead>
        <TableHead className="text-right">Capital pagado</TableHead>
        <TableHead>Estado</TableHead>
      </TableRow>
    </TableHeader>
    <TableBody>
      {compromiso.cuotas.map((cuota) => {
        const etiqueta = etiquetaCuota(cuota);
        return (
          <TableRow key={cuota.id}>
            <TableCell>
              <p className="text-content-primary">{cuota.nombre}</p>
              <p className="text-body-4 text-content-tertiary">
                {cuota.numero
                  ? `Cuota ${cuota.numero} de ${compromiso.resumen.pensionesTotal}`
                  : 'Cuota de matrícula'}
              </p>
            </TableCell>
            <TableCell>{formatDay(cuota.fechaVencimiento)}</TableCell>
            <TableCell className="text-right">
              {soles(cuota.montoBase)}
            </TableCell>
            <TableCell className="text-right text-content-success">
              {cuota.montoDescuento > 0
                ? `− ${soles(cuota.montoDescuento)}${
                    cuota.porcentajeDescuento
                      ? ` (${cuota.porcentajeDescuento} %)`
                      : ''
                  }`
                : '—'}
            </TableCell>
            <TableCell className="text-right font-semibold text-content-primary">
              {soles(cuota.montoFinal)}
            </TableCell>
            <TableCell className="text-right">
              {soles(cuota.montoPagado)}
            </TableCell>
            <TableCell>
              <span className="flex items-center gap-2">
                <Tag size="sm" color={etiqueta.color}>
                  {etiqueta.texto}
                </Tag>
                {cuota.fechaPago && (
                  <span className="text-body-4 text-content-tertiary">
                    {formatDay(cuota.fechaPago)}
                  </span>
                )}
              </span>
            </TableCell>
          </TableRow>
        );
      })}
    </TableBody>
    <TableFooter>
      <TableRow>
        <TableCell colSpan={4} className="font-semibold text-content-primary">
          Total anual comprometido
        </TableCell>
        <TableCell className="text-right font-semibold text-content-primary">
          {soles(compromiso.resumen.totalAnual)}
        </TableCell>
        <TableCell className="text-right font-semibold text-content-primary">
          {soles(compromiso.resumen.capitalPagado)}
        </TableCell>
        <TableCell />
      </TableRow>
    </TableFooter>
  </TableElement>
);

// Figma > Personal Administrativo > "P8 · Compromiso 2026 — Cronograma de cuotas" (HU-37)
export const CronogramaCuotas = ({
  compromiso,
  isLoading,
}: {
  compromiso?: CompromisoDetalle;
  isLoading: boolean;
}) => {
  const [pestana, setPestana] = React.useState<(typeof PESTANAS)[number]>(
    'Cronograma de cuotas',
  );

  if (isLoading) {
    return (
      <div className="flex h-48 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }
  if (!compromiso) {
    return (
      <Card className="p-8 text-center text-body-3 text-content-tertiary">
        El estudiante no tiene compromisos de pago.
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <ResumenCompromiso compromiso={compromiso} />

      <Card className="flex flex-col gap-4 p-6">
        <div className="flex flex-wrap gap-2 border-b border-stroke-tertiary pb-4">
          {PESTANAS.map((p) => (
            <button
              key={p}
              type="button"
              onClick={() => setPestana(p)}
              className={cn(
                'rounded-lg px-4 py-2.5 text-label-2',
                p === pestana
                  ? 'bg-surface-accent-primary text-white'
                  : 'text-content-secondary hover:bg-surface-secondary',
              )}
            >
              {p}
            </button>
          ))}
        </div>
        {pestana === 'Cronograma de cuotas' ? (
          <TablaCuotas compromiso={compromiso} />
        ) : (
          <p className="py-10 text-center text-body-3 text-content-tertiary">
            Disponible en la iteración 2 (HU-38 a HU-42).
          </p>
        )}
      </Card>

      <Card className="flex flex-wrap items-center gap-4 px-6 py-5">
        <div className="min-w-0 flex-1">
          <p className="text-label-1 font-semibold text-content-primary">
            Acciones sobre el compromiso
          </p>
          <p className="text-body-4 text-content-tertiary">
            Usa estas opciones solo en casos excepcionales. Quedan registradas
            en el historial.
          </p>
        </div>
        <Button
          variant="primary-light"
          disabled
          icon={<Ban className="size-4" />}
        >
          Anular compromiso
        </Button>
      </Card>
    </div>
  );
};
