import dayjs from 'dayjs';

import {
  AnioEscolarResumen,
  CuotaTarifarioVista,
  EstadoTarifario,
} from '@/types/api';

// Textos y fechas de la pantalla P19 "Tarifario y calendario" (HU-34).

type ColorPastilla = 'warning' | 'success';

// Figma > P19 > etiqueta junto al selector de año: "En configuración" (ámbar). "Confirmado" no está en Figma: es el
// mismo componente en verde.
export const ESTADO_TARIFARIO: Record<
  EstadoTarifario,
  { texto: string; color: ColorPastilla }
> = {
  EN_CONFIGURACION: { texto: 'En configuración', color: 'warning' },
  CONFIRMADO: { texto: 'Confirmado', color: 'success' },
};

const MESES = [
  'enero',
  'febrero',
  'marzo',
  'abril',
  'mayo',
  'junio',
  'julio',
  'agosto',
  'septiembre',
  'octubre',
  'noviembre',
  'diciembre',
];

/** "2027-02-28" -> "28/02/2027". */
export const fechaDeDia = (fecha: string) => {
  const [anio, mes, dia] = fecha.split('-');
  return `${dia}/${mes}/${anio}`;
};

/** "2027-03-31" -> "marzo". */
export const nombreDelMes = (fecha: string) =>
  MESES[Number(fecha.slice(5, 7)) - 1];

const esFinDeMes = (fecha: string) =>
  dayjs(fecha).endOf('month').format('YYYY-MM-DD') === fecha;

/**
 * Figma: "10 cuotas (marzo–diciembre)". La cantidad y los meses salen del calendario que manda el backend, no del
 * texto fijo del diseño.
 */
export const resumenDeCuotas = (cuotas: CuotaTarifarioVista[]) => {
  if (cuotas.length === 0) return 'Sin cuotas';
  const primera = nombreDelMes(cuotas[0].fechaVencimiento);
  if (cuotas.length === 1) return `1 cuota (${primera})`;
  const ultima = nombreDelMes(cuotas[cuotas.length - 1].fechaVencimiento);
  return `${cuotas.length} cuotas (${primera}–${ultima})`;
};

/**
 * Figma: "Último día de cada mes · diciembre: 15/12". La excepción sale de la última cuota del calendario: si no vence
 * el último día de su mes (el backend la pasa al 15 o al fin del año), se dice cuál es.
 */
export const vencimientoDePension = (cuotas: CuotaTarifarioVista[]) => {
  if (cuotas.length === 0) return '—';
  const ultima = cuotas[cuotas.length - 1].fechaVencimiento;
  if (esFinDeMes(ultima)) return 'Último día de cada mes';
  const [, mes, dia] = ultima.split('-');
  return `Último día de cada mes · ${nombreDelMes(ultima)}: ${dia}/${mes}`;
};

/**
 * El año que abre la pantalla: el que pide `?anioEscolarId=` si existe; si no, el PLANIFICADO más reciente (el que se
 * está preparando); si no, el ACTIVO; y, si todos están cerrados, el más reciente.
 */
export const anioPorDefecto = (
  anios: AnioEscolarResumen[],
  pedido: string | null,
): AnioEscolarResumen | undefined => {
  const porAnioDescendente = [...anios].sort((a, b) => b.anio - a.anio);
  return (
    porAnioDescendente.find((a) => a.id === pedido) ??
    porAnioDescendente.find((a) => a.estado === 'PLANIFICADO') ??
    porAnioDescendente.find((a) => a.estado === 'ACTIVO') ??
    porAnioDescendente[0]
  );
};
