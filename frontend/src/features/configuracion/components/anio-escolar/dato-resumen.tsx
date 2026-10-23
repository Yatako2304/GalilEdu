import { DatoResumen } from './formato';

// Figma > CI-01 > "Dato": mini-tarjeta con etiqueta, valor y una línea de detalle.
export const DatoResumenCard = ({ etiqueta, valor, detalle }: DatoResumen) => (
  <div className="flex min-w-0 flex-1 flex-col gap-0.5 rounded-[10px] bg-surface-secondary px-3.5 py-2.5">
    <p className="text-body-4 text-content-tertiary">{etiqueta}</p>
    <p className="text-label-1 font-semibold text-content-primary">{valor}</p>
    {detalle && <p className="text-body-4 text-content-tertiary">{detalle}</p>}
  </div>
);
