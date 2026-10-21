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

/** 1 -> "1 año", 3 -> "3 años" (columna Antigüedad de P2). */
export const anios = (n: number) => `${n} ${n === 1 ? 'año' : 'años'}`;

/** Buscador de P2: por nombre (sin importar tildes ni mayúsculas) o por DNI. */
export const filtrarAlumnos = <
  T extends { nombre: string; numeroDocumento: string },
>(
  alumnos: T[],
  texto: string,
): T[] => {
  const normalizar = (s: string) =>
    s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
  const q = normalizar(texto.trim());
  if (!q) return alumnos;
  return alumnos.filter(
    (a) => normalizar(a.nombre).includes(q) || a.numeroDocumento.includes(q),
  );
};
