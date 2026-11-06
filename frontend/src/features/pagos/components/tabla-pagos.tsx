import { Spinner } from '@/components/ui/spinner';
import {
  TableBody,
  TableCell,
  TableElement,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { PagoRealizado } from '@/types/api';
import { formatDate } from '@/utils/format';

import { soles } from './formato';

export const TablaPagos = ({
  pagos,
  isLoading,
}: {
  pagos?: PagoRealizado[];
  isLoading: boolean;
}) => {
  if (isLoading) {
    return (
      <div className="flex h-48 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  if (!pagos?.length) {
    return (
      <p className="py-10 text-center text-body-3 text-content-tertiary">
        Aún no hay pagos registrados.
      </p>
    );
  }

  return (
    <TableElement className="whitespace-nowrap">
      <TableHeader>
        <TableRow>
          <TableHead>Fecha</TableHead>
          <TableHead>Constancia</TableHead>
          <TableHead>Cuotas</TableHead>
          <TableHead className="text-right">Monto de cuotas</TableHead>
          <TableHead className="text-right">Recargo</TableHead>
          <TableHead className="text-right">Total pagado</TableHead>
          <TableHead>Estado</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {pagos.map((pago) => (
          <TableRow key={pago.id}>
            <TableCell>{formatDate(pago.fechaPago)}</TableCell>
            <TableCell>{pago.numeroConstancia}</TableCell>
            <TableCell className="max-w-64 whitespace-normal">
              {pago.cuotas.join(', ')}
            </TableCell>
            <TableCell className="text-right">{soles(pago.capital)}</TableCell>
            <TableCell className="text-right">{soles(pago.recargo)}</TableCell>
            <TableCell className="text-right font-semibold text-content-primary">
              {soles(pago.monto)}
            </TableCell>
            <TableCell>
              <Tag
                size="sm"
                color={pago.estado === 'CONFIRMADO' ? 'success' : 'warning'}
              >
                {pago.estado === 'CONFIRMADO' ? 'Confirmado' : 'Observado'}
              </Tag>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </TableElement>
  );
};
