import { CuotaDetalle } from '@/types/api';

const SOLES = new Intl.NumberFormat('es-PE', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/** 5820 -> "S/ 5,820.00" (Figma P8). */
export const soles = (monto: number) => `S/ ${SOLES.format(monto)}`;

type ColorTag = 'success' | 'error' | 'neutral' | 'warning' | 'info';

/** Etiqueta de la columna "Estado". "Vencida" se calcula en el backend (RF-91). */
export const etiquetaCuota = (
  cuota: Pick<CuotaDetalle, 'estado' | 'vencida'>,
): { texto: string; color: ColorTag } => {
  if (cuota.estado === 'PAGADA') return { texto: 'Pagada', color: 'success' };
  if (cuota.estado === 'ANULADA') return { texto: 'Anulada', color: 'neutral' };
  if (cuota.vencida) return { texto: 'Vencida', color: 'error' };
  if (cuota.estado === 'EN_PROCESO')
    return { texto: 'En proceso', color: 'info' };
  if (cuota.estado === 'PAGADA_PARCIAL')
    return { texto: 'Pago parcial', color: 'warning' };
  return { texto: 'Pendiente', color: 'neutral' };
};
