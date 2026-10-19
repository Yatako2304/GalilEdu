import { default as dayjs } from 'dayjs';
import 'dayjs/locale/es';

dayjs.locale('es');

export const formatDate = (date: number | string) =>
  dayjs(date).format('D [de] MMMM [de] YYYY, h:mm A');

export const formatDay = (date: number | string) =>
  dayjs(date).format('DD/MM/YYYY');
