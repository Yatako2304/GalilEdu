import { default as dayjs } from 'dayjs';
import 'dayjs/locale/es';

dayjs.locale('es');

export const formatDate = (date: number | string) =>
  dayjs(date).format('D [de] MMMM [de] YYYY, h:mm A');

export const formatDay = (date: number | string) =>
  dayjs(date).format('DD/MM/YYYY');

/** "01 oct. 2026" (Figma > Editar usuario). */
export const formatFecha = (date: number | string) =>
  dayjs(date).format('DD MMM YYYY');

/** Instante ISO → "02/02/2027" en la hora de Lima (los periodos del colegio se configuran en esa zona). */
export const formatFechaLima = (instante: string) =>
  new Intl.DateTimeFormat('es-PE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    timeZone: 'America/Lima',
  }).format(new Date(instante));
