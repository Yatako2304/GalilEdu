import { Card } from '@/components/ui/card';
import {
  TableBody,
  TableCell,
  TableElement,
  TableFooter,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { CuotaTarifarioVista } from '@/types/api';

import { soles } from '../periodo-matricula/formato';

import { fechaDeDia } from './formato';

type CalendarioCuotasProps = {
  cuotas: CuotaTarifarioVista[];
  /** Monto de cada cuota (la pensión del formulario); null mientras no hay un monto válido. */
  montoPorCuota: number | null;
  /** Pensión por la cantidad de cuotas; null mientras no hay un monto válido. */
  total: number | null;
};

const monto = (valor: number | null) => (valor === null ? '—' : soles(valor));

// Figma > P19 > tarjeta "Calendario de cuotas": el calendario lo genera el backend con las fechas del año y no se
// edita; el monto sigue a la pensión del formulario y la última fila suma las pensiones.
export const CalendarioCuotas = ({
  cuotas,
  montoPorCuota,
  total,
}: CalendarioCuotasProps) => (
  <Card className="flex flex-col gap-4 p-6">
    <h2 className="text-label-1 font-semibold text-content-primary">
      Calendario de cuotas
    </h2>
    <TableElement>
      <TableHeader>
        <TableRow>
          <TableHead>N.°</TableHead>
          <TableHead>Cuota</TableHead>
          <TableHead>Vencimiento</TableHead>
          <TableHead className="text-right">Monto</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {cuotas.map((cuota) => (
          <TableRow key={cuota.numero}>
            <TableCell>{cuota.numero}</TableCell>
            <TableCell>{cuota.nombre}</TableCell>
            <TableCell>{fechaDeDia(cuota.fechaVencimiento)}</TableCell>
            <TableCell className="text-right">{monto(montoPorCuota)}</TableCell>
          </TableRow>
        ))}
      </TableBody>
      <TableFooter className="bg-surface-secondary">
        <TableRow className="hover:bg-surface-secondary">
          <TableCell />
          <TableCell className="font-semibold text-content-primary">
            Total pensiones
          </TableCell>
          <TableCell />
          <TableCell className="text-right font-semibold text-content-primary">
            {monto(total)}
          </TableCell>
        </TableRow>
      </TableFooter>
    </TableElement>
  </Card>
);
