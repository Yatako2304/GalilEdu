import dayjs from 'dayjs';

import {
  EstadoVisiblePeriodoMatricula,
  FilaPeriodoMatricula,
  PeriodoMatriculaVigente,
} from '@/types/api';

// Textos y fechas de la pantalla 13 "Periodos de matrícula" (HU-23).

type ColorPastilla = 'warning' | 'success' | 'primary';

// Figma > pantalla 13 > estados de la tabla: Programado (ámbar), Abierto (verde) y Cerrado (azul oscuro).
export const ESTADO_PERIODO_MATRICULA: Record<
  EstadoVisiblePeriodoMatricula,
  { texto: string; color: ColorPastilla }
> = {
  PROGRAMADO: { texto: 'Programado', color: 'warning' },
  ABIERTO: { texto: 'Abierto', color: 'success' },
  CERRADO: { texto: 'Cerrado', color: 'primary' },
};

// El listado trae `inicio` y `cierre` como instantes UTC; el colegio las ve en hora de Lima.
const ZONA_DEL_COLEGIO = 'America/Lima';

const partesEnLima = (
  instante: string,
  opciones: Intl.DateTimeFormatOptions,
): Record<string, string> =>
  Object.fromEntries(
    new Intl.DateTimeFormat('es-PE', {
      timeZone: ZONA_DEL_COLEGIO,
      ...opciones,
    })
      .formatToParts(new Date(instante))
      .map((parte) => [parte.type, parte.value]),
  );

/** "2026-12-01T15:00:00Z" -> "01/12/2026" (en hora de Lima). */
export const fechaLima = (instante: string) => {
  const p = partesEnLima(instante, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
  return `${p.day}/${p.month}/${p.year}`;
};

/** "2026-12-01T15:00:00Z" -> "10:00 a.m." (en hora de Lima, como en Figma). */
export const horaLima = (instante: string) => {
  const p = partesEnLima(instante, {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  });
  const periodo = (p.dayPeriod ?? '').toLowerCase().replace(/\s/g, '');
  return `${p.hour}:${p.minute} ${periodo}`;
};

export const fechaHoraLima = (instante: string) =>
  `${fechaLima(instante)} ${horaLima(instante)}`;

/**
 * Línea bajo el nombre de la tarjeta vigente. Figma solo dibuja la del periodo abierto ("Abierto el 01/12/2026
 * 10:00 a.m. · cierra el 15/02/2027 10:00 a.m."); la de uno programado sigue el mismo tono.
 */
export const lineaDeApertura = ({
  estado,
  inicio,
  cierre,
}: Pick<PeriodoMatriculaVigente, 'estado' | 'inicio' | 'cierre'>) => {
  const cierra = `cierra el ${fechaHoraLima(cierre)}`;
  if (estado === 'PROGRAMADO') {
    return `Abre el ${fechaHoraLima(inicio)} · ${cierra}`;
  }
  if (estado === 'CERRADO') return `Cerró el ${fechaHoraLima(cierre)}`;
  return `Abierto el ${fechaHoraLima(inicio)} · ${cierra}`;
};

/** Columna "Matrículas": un periodo que aún no abre no tiene matrículas que mostrar ("-" en Figma). */
export const matriculasDeLaFila = (
  fila: Pick<FilaPeriodoMatricula, 'estado' | 'matriculas'>,
) => (fila.estado === 'PROGRAMADO' ? '-' : String(fila.matriculas));

/** "8 de 300 vacantes" (dato "Matrículas registradas" de la tarjeta vigente). */
export const matriculasDeVacantes = (registradas: number, vacantes: number) =>
  `${registradas} de ${vacantes} ${vacantes === 1 ? 'vacante' : 'vacantes'}`;

const FORMATO_SOLES = new Intl.NumberFormat('es-PE', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

/** 450 -> "S/ 450.00". */
export const soles = (monto: number) => `S/ ${FORMATO_SOLES.format(monto)}`;

/** "15/09/2026 – 31/10/2026" (modal "Configuración · Matrícula"), con fechas sin hora. */
export const rangoDeMatricula = (inicio: string, cierre: string) => {
  const dia = (fecha: string) => fecha.split('-').reverse().join('/');
  return `${dia(inicio)} – ${dia(cierre)}`;
};

export const siONo = (valor: boolean) => (valor ? 'Sí' : 'No');

/** "Matrícula extemporánea" del modal: la regla se muestra, no se edita. */
export const textoExtemporanea = (permitida: boolean) =>
  permitida ? 'Permitida tras el cierre' : 'No permitida';

// ---------- Formulario "Nuevo periodo de matrícula" ----------

const MESES = [
  'ene',
  'feb',
  'mar',
  'abr',
  'may',
  'jun',
  'jul',
  'ago',
  'sep',
  'oct',
  'nov',
  'dic',
];

/** "2026-12-01" -> "01 dic" (tarjeta "Periodo de matrícula"). */
export const diaYMes = (fecha: string) => {
  const [, mes, dia] = fecha.split('-');
  return `${dia} ${MESES[Number(mes) - 1]}`;
};

/** "2026-12-01" y "2027-02-15" -> "01 dic – 15 feb". */
export const rangoCorto = (inicio: string, fin: string) =>
  `${diaYMes(inicio)} – ${diaYMes(fin)}`;

/** "2026-12-01" -> "01-dic" (extremos de la barra de duración). */
export const diaYMesCorto = (fecha: string) => diaYMes(fecha).replace(' ', '-');

/** Días entre dos fechas yyyy-mm-dd (el 01/12 y el 15/02 están a 76 días, como `diasDelPeriodo` del backend). */
export const diasEntre = (inicio: string, fin: string) =>
  dayjs(fin).diff(dayjs(inicio), 'day');

/** "13:30" -> "1:30 p.m.", "00:00" -> "12:00 a.m." (como en Figma). */
export const etiquetaHora = (hora: string) => {
  const [h, m] = hora.split(':').map(Number);
  const hora12 = h % 12 === 0 ? 12 : h % 12;
  return `${hora12}:${String(m).padStart(2, '0')} ${h < 12 ? 'a.m.' : 'p.m.'}`;
};

/** El backend manda "08:00:00" y recibe "08:00". */
export const normalizarHora = (hora: string) => hora.slice(0, 5);

/** Cada 30 minutos del día, más las 11:59 p. m. (el cierre de la matrícula suele ser al final del día). */
const HORAS = [
  ...Array.from({ length: 48 }, (_, i) => {
    const h = String(Math.floor(i / 2)).padStart(2, '0');
    return `${h}:${i % 2 === 0 ? '00' : '30'}`;
  }),
  '23:59',
];

/** Opciones de los selectores de hora; si el valor actual no está en la lista (por ejemplo "08:15") se agrega. */
export const opcionesDeHora = (actual?: string | null) => {
  const horas = new Set(HORAS);
  const valor = actual ? normalizarHora(actual) : '';
  if (valor) horas.add(valor);
  return [...horas]
    .sort()
    .map((hora) => ({ label: etiquetaHora(hora), value: hora }));
};
