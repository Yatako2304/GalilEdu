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
import { formatDay } from '@/utils/format';

import { useMorosidadCompromiso } from '../api/morosidad';

import { soles } from './formato';

const nombrePension = (fecha: string) => {
  const mes = new Intl.DateTimeFormat('es-PE', {
    month: 'long',
    timeZone: 'UTC',
  })
    .format(new Date(`${fecha}T12:00:00Z`))
    .toLocaleLowerCase('es-PE');
  return `Pensión de ${mes}`;
};

export const MorosidadCompromiso = ({
  compromisoId,
}: {
  compromisoId: string;
}) => {
  const morosidad = useMorosidadCompromiso(compromisoId);

  if (morosidad.isLoading) {
    return (
      <div className="flex h-48 items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  const datos = morosidad.data;
  if (!datos?.cuotas.length) {
    return (
      <Card className="p-8 text-center text-body-3 text-content-tertiary">
        Sin cuotas vencidas
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      <p className="text-body-3 text-content-secondary">
        Gracia {datos.diasGracia} días · Recargo {datos.recargoPorcentaje} %
        cada 30 días · Tope {datos.topeRecargoPorcentaje} %
      </p>
      <TableElement className="whitespace-nowrap">
        <TableHeader>
          <TableRow>
            <TableHead>Cuota vencida</TableHead>
            <TableHead>Vencimiento</TableHead>
            <TableHead className="text-right">Días de atraso</TableHead>
            <TableHead className="text-right">Monto pendiente</TableHead>
            <TableHead className="text-right">Recargo</TableHead>
            <TableHead className="text-right">Total a la fecha</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {datos.cuotas.map((cuota) => (
            <TableRow key={cuota.cuotaId}>
              <TableCell>
                <p className="text-content-primary">
                  {nombrePension(cuota.fechaVencimiento)}
                </p>
                {cuota.numero && (
                  <p className="text-body-4 text-content-tertiary">
                    Cuota {cuota.numero}
                  </p>
                )}
              </TableCell>
              <TableCell>{formatDay(cuota.fechaVencimiento)}</TableCell>
              <TableCell className="text-right">
                {cuota.diasAtraso} días
              </TableCell>
              <TableCell className="text-right">
                {soles(cuota.capital)}
              </TableCell>
              <TableCell className="text-right text-content-error">
                + {soles(cuota.recargo)}
              </TableCell>
              <TableCell className="text-right font-semibold text-content-primary">
                {soles(cuota.total)}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
        <TableFooter>
          <TableRow>
            <TableCell
              colSpan={3}
              className="font-semibold text-content-primary"
            >
              Total adeudado al {formatDay(datos.fecha)}
            </TableCell>
            <TableCell className="text-right font-semibold text-content-primary">
              {soles(datos.capital)}
            </TableCell>
            <TableCell className="text-right font-semibold text-content-primary">
              + {soles(datos.recargo)}
            </TableCell>
            <TableCell className="text-right font-semibold text-content-primary">
              {soles(datos.total)}
            </TableCell>
          </TableRow>
        </TableFooter>
      </TableElement>
    </div>
  );
};
