import {
  DiaLectivo,
  EstructuraHorariaResumen,
  Nivel,
  RecesoResumen,
} from '@/types/api';

// Figma > CI-12 · Estructura horaria (HU-44): los textos de las tarjetas, los datos y la grilla.

/** Motivo del botón "Nueva" (y de Editar y Eliminar) cuando el año ya no es PLANIFICADO. Es el mensaje del backend. */
export const MOTIVO_ANIO_NO_MODIFICABLE =
  'Solo se puede modificar la estructura horaria de un año planificado.';

/** Los días en el orden de la semana. */
export const DIAS_LECTIVOS: DiaLectivo[] = [
  'LUN',
  'MAR',
  'MIE',
  'JUE',
  'VIE',
  'SAB',
];

export const NOMBRE_DIA_CORTO: Record<DiaLectivo, string> = {
  LUN: 'Lun',
  MAR: 'Mar',
  MIE: 'Mié',
  JUE: 'Jue',
  VIE: 'Vie',
  SAB: 'Sáb',
};

/** Encabezados de la grilla semanal. */
export const NOMBRE_DIA_LARGO: Record<DiaLectivo, string> = {
  LUN: 'Lunes',
  MAR: 'Martes',
  MIE: 'Miércoles',
  JUE: 'Jueves',
  VIE: 'Viernes',
  SAB: 'Sábado',
};

export const NOMBRE_NIVEL: Record<Nivel, string> = {
  PRIMARIA: 'Primaria',
  SECUNDARIA: 'Secundaria',
};

/** Los días de la estructura, siempre en el orden de la semana (el backend los puede mandar en cualquier orden). */
export const ordenarDias = (dias: DiaLectivo[]): DiaLectivo[] =>
  DIAS_LECTIVOS.filter((dia) => dias.includes(dia));

/** Figma > CI-12 > Datos: "Lun – Vie" si son seguidos; si no, "Lun, Mié, Vie". Un solo día va solo. */
export const textoDias = (dias: DiaLectivo[]) => {
  const ordenados = ordenarDias(dias);
  if (ordenados.length === 0) return '—';
  const posiciones = ordenados.map((dia) => DIAS_LECTIVOS.indexOf(dia));
  const sonSeguidos = posiciones.every(
    (posicion, i) => i === 0 || posicion === posiciones[i - 1] + 1,
  );
  if (ordenados.length > 2 && sonSeguidos) {
    return `${NOMBRE_DIA_CORTO[ordenados[0]]} – ${NOMBRE_DIA_CORTO[ordenados[ordenados.length - 1]]}`;
  }
  return ordenados.map((dia) => NOMBRE_DIA_CORTO[dia]).join(', ');
};

/** Figma > CI-12 > Lista de estructuras: "5 días" (en singular, "1 día"). */
export const textoCantidadDias = (cantidad: number) =>
  cantidad === 1 ? '1 día' : `${cantidad} días`;

/** Figma > CI-12 > Lista de estructuras: "2 registradas" (en singular, "1 registrada"). */
export const textoRegistradas = (cantidad: number) =>
  cantidad === 1 ? '1 registrada' : `${cantidad} registradas`;

/** Figma > CI-12 > Lista de estructuras: "07:45 – 14:20 · 8 bloques de 45 min". */
export const textoJornada = (
  e: Pick<
    EstructuraHorariaResumen,
    'horaEntrada' | 'horaSalida' | 'bloquesPorDia' | 'duracionBloqueMin'
  >,
) =>
  `${e.horaEntrada} – ${e.horaSalida} · ${e.bloquesPorDia} ${e.bloquesPorDia === 1 ? 'bloque' : 'bloques'} de ${e.duracionBloqueMin} min`;

/** Figma > CI-12 > Datos > "Bloques por día": "8 × 45 min". */
export const textoBloquesPorDia = (
  e: Pick<EstructuraHorariaResumen, 'bloquesPorDia' | 'duracionBloqueMin'>,
) => `${e.bloquesPorDia} × ${e.duracionBloqueMin} min`;

/** Figma > CI-12 > Datos > "Recesos": "20 + 15 min", en el orden en que ocurren en la jornada. */
export const textoRecesos = (recesos: RecesoResumen[]) =>
  recesos.length === 0
    ? 'Sin recesos'
    : `${[...recesos]
        .sort((a, b) => a.despuesDeBloque - b.despuesDeBloque)
        .map((receso) => receso.duracionMin)
        .join(' + ')} min`;

/** Figma > CI-12 > Grilla semanal: "10:00 – 10:20". */
export const textoRango = (inicio: string, fin: string) => `${inicio} – ${fin}`;
