import { ClipboardCheck } from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogTitle,
} from '@/components/ui/dialog';
import { IconBadge } from '@/components/ui/icon-badge';
import { SolicitudTarifario } from '@/types/api';

import { soles } from '../periodo-matricula/formato';

type DialogoConfirmarTarifarioProps = {
  abierto: boolean;
  anio: number;
  /** Lo que se va a confirmar: los valores del formulario, ya validados. */
  tarifario: SolicitudTarifario | null;
  cantidadCuotas: number;
  pendiente: boolean;
  onConfirmar: () => void;
  onCancelar: () => void;
};

const plural = (cantidad: number, singular: string, plural: string) =>
  `${cantidad} ${cantidad === 1 ? singular : plural}`;

const Fila = ({ etiqueta, valor }: { etiqueta: string; valor: string }) => (
  <div className="flex items-start justify-between gap-6 py-1.5">
    <dt className="text-body-3 text-content-secondary">{etiqueta}</dt>
    <dd className="text-right text-label-2 font-semibold text-content-primary">
      {valor}
    </dd>
  </div>
);

// Figma > "Modal - ¿Confirmar el tarifario?" (1090:26770): resume lo que se va a confirmar antes de guardarlo.
export const DialogoConfirmarTarifario = ({
  abierto,
  anio,
  tarifario,
  cantidadCuotas,
  pendiente,
  onConfirmar,
  onCancelar,
}: DialogoConfirmarTarifarioProps) => (
  <Dialog open={abierto} onOpenChange={(open) => !open && onCancelar()}>
    <DialogContent className="flex max-w-[520px] flex-col gap-0 overflow-hidden rounded-xl border-stroke-tertiary bg-white p-0">
      <div className="flex flex-col gap-4 p-6">
        <IconBadge
          color="accent"
          shape="square"
          size="lg"
          className="mx-auto"
          aria-hidden="true"
        >
          <ClipboardCheck />
        </IconBadge>
        <DialogTitle className="text-center text-h6 font-semibold text-content-primary">
          ¿Confirmar el tarifario {anio}?
        </DialogTitle>
        <DialogDescription className="text-body-3 text-content-secondary">
          Con el tarifario confirmado se podrán generar los compromisos de pago{' '}
          {anio}. Después de generar el primer compromiso, los montos y el
          calendario ya no se podrán editar.
        </DialogDescription>
        {tarifario && (
          <dl className="rounded-xl bg-surface-secondary px-5 py-3">
            <Fila
              etiqueta="Cuota de ingreso"
              valor={
                tarifario.montoCuotaIngreso === null
                  ? '—'
                  : soles(tarifario.montoCuotaIngreso)
              }
            />
            <Fila
              etiqueta="Matrícula"
              valor={soles(tarifario.montoMatricula)}
            />
            <Fila
              etiqueta="Pensión mensual"
              valor={`${soles(tarifario.montoPension)} × ${plural(cantidadCuotas, 'cuota', 'cuotas')}`}
            />
            <Fila
              etiqueta="Morosidad"
              valor={`${plural(tarifario.diasGracia, 'día', 'días')} de gracia · ${tarifario.recargoPorcentaje} % c/30 días · tope ${tarifario.topeRecargoPorcentaje} %`}
            />
          </dl>
        )}
      </div>
      <DialogFooter className="gap-2 border-t border-stroke-tertiary px-6 py-4 sm:space-x-0">
        <Button variant="outline" onClick={onCancelar} disabled={pendiente}>
          Cancelar
        </Button>
        <Button
          onClick={onConfirmar}
          isLoading={pendiente}
          disabled={pendiente}
        >
          Confirmar
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
);
