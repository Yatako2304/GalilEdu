import { Banknote } from 'lucide-react';

import { Button } from '@/components/ui/button';

import { soles } from './formato';
import { CuotaSeleccionable, unirNombres } from './seleccion-cuotas';

// Figma > Apoderado > "HU-41 Pago en línea de cuotas pendientes y HU-40" > "Barra de selección".
// Va sobre la tabla del cronograma (HU-40), que marca las cuotas con useSeleccionCuotas.

const detalle = (cuotas: CuotaSeleccionable[]) => {
  const variosHijos = new Set(cuotas.map((c) => c.estudiante)).size > 1;
  const nombres = cuotas.map((c) =>
    variosHijos ? `${c.nombre} (${c.estudiante.split(' ')[0]})` : c.nombre,
  );
  const vencidas = cuotas.every((c) => c.vencida) ? ' (vencidas)' : '';
  const recargo = cuotas.reduce((suma, c) => suma + c.recargo, 0);
  const conRecargo =
    recargo > 0 ? ` · incluye ${soles(recargo)} de recargo por mora` : '';
  return `${unirNombres(nombres)}${vencidas}${conRecargo}`;
};

export type BarraPagoCuotasProps = {
  seleccionadas: CuotaSeleccionable[];
  onPagar: () => void;
  isLoading?: boolean;
};

export const BarraPagoCuotas = ({
  seleccionadas,
  onPagar,
  isLoading,
}: BarraPagoCuotasProps) => {
  if (seleccionadas.length === 0) return null;
  const total = seleccionadas.reduce((suma, c) => suma + c.total, 0);
  const n = seleccionadas.length;
  return (
    <div className="flex flex-wrap items-center gap-4 rounded-lg border border-stroke-accent-primary bg-surface-accent-light px-4 py-3">
      <div className="flex min-w-0 flex-1 flex-col gap-0.5">
        <p className="text-label-2 font-semibold text-content-primary">
          {n} {n === 1 ? 'cuota seleccionada' : 'cuotas seleccionadas'} · Total:{' '}
          {soles(total)}
        </p>
        <p className="text-body-4 text-content-secondary">
          {detalle(seleccionadas)}
        </p>
      </div>
      <Button
        icon={<Banknote className="size-4" aria-hidden="true" />}
        onClick={onPagar}
        isLoading={isLoading}
      >
        Pagar seleccionadas
      </Button>
    </div>
  );
};
