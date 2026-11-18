import {
  AnioParaHorarios,
  DiaCoordinador,
  GradoParaHorarios,
  JornadaDelDia,
  NOMBRE_DEL_DIA,
  SeccionParaHorarios,
} from '../types';
import { FiltroDeReservas } from '../types-reservas';

import { estadoDeError, mensajeDeError } from './formato';

// HU-48 · Reserva de espacios especiales: lógica pura de la pantalla (formato, errores del backend y opciones del
// formulario a partir de la jornada de la sección).

// ---------- Horas ----------

/** "08:00" -> 480. */
export const aMinutos = (hora: string): number => {
  const [h, m] = hora.split(':').map(Number);
  return h * 60 + m;
};

/** 480 -> "08:00". */
export const deMinutos = (minutos: number): string =>
  `${String(Math.floor(minutos / 60)).padStart(2, '0')}:${String(minutos % 60).padStart(2, '0')}`;

/** "13:30" -> "01:30 pm"; "08:00" -> "08:00 am" (Figma > Reserva de espacios especiales: dos dígitos y un espacio). */
export const formatHora12 = (hora: string): string => {
  const [h, m] = hora.split(':').map(Number);
  const sufijo = h >= 12 ? 'pm' : 'am';
  const h12 = h % 12 === 0 ? 12 : h % 12;
  return `${String(h12).padStart(2, '0')}:${String(m).padStart(2, '0')} ${sufijo}`;
};

/** `horaFin` = `horaInicio` + la duración, en «HH:mm». */
export const calcularHoraFin = (
  horaInicio: string,
  duracionMin: number,
): string => deMinutos(aMinutos(horaInicio) + duracionMin);

/** Cuántos minutos hay entre dos horas «HH:mm». */
export const duracionEnMinutos = (inicio: string, fin: string): number =>
  aMinutos(fin) - aMinutos(inicio);

// ---------- Opciones del formulario (a partir de la jornada) ----------

/** El fin del último bloque de la jornada, o null si no tiene bloques. */
export const finDeJornada = (jornada: JornadaDelDia): string | null =>
  jornada.bloques.at(-1)?.fin ?? null;

/** La duración de un bloque de esa jornada, en minutos (todos los bloques duran lo mismo); 0 si no hay bloques. */
export const duracionDelBloque = (jornada: JornadaDelDia): number => {
  const bloque = jornada.bloques[0];
  return bloque ? duracionEnMinutos(bloque.inicio, bloque.fin) : 0;
};

export type OpcionDeHora = { value: string; label: string };

/**
 * Las horas de inicio que ofrece el formulario: el inicio de cada bloque de la jornada. Al editar, si la hora guardada
 * no es el inicio de un bloque, se agrega para no perderla; las opciones salen ordenadas.
 */
export const opcionesDeHora = (
  jornada: JornadaDelDia,
  horaGuardada?: string,
): OpcionDeHora[] => {
  const horas = jornada.bloques.map((b) => b.inicio);
  if (horaGuardada && !horas.includes(horaGuardada)) horas.push(horaGuardada);
  return horas
    .sort((a, b) => aMinutos(a) - aMinutos(b))
    .map((hora) => ({ value: hora, label: formatHora12(hora) }));
};

export type OpcionDeDuracion = {
  value: number;
  label: string;
  disabled: boolean;
};

/** Cuántas veces la duración de un bloque se puede reservar (1×, 2× y 3×). */
export const MULTIPLOS_DE_DURACION = [1, 2, 3];

/**
 * Las duraciones que ofrece el formulario: 1×, 2× y 3× la duración del bloque de esa jornada. Las que pasan del fin de
 * la jornada, empezando en `horaInicio`, salen deshabilitadas. Al editar, si la duración guardada no es una de ellas se
 * agrega (habilitada) para no perderla. Sin `horaInicio` ninguna se deshabilita.
 */
export const opcionesDeDuracion = (
  jornada: JornadaDelDia,
  horaInicio?: string,
  duracionGuardada?: number,
): OpcionDeDuracion[] => {
  const bloque = duracionDelBloque(jornada);
  const fin = finDeJornada(jornada);
  const duraciones = bloque
    ? MULTIPLOS_DE_DURACION.map((veces) => veces * bloque)
    : [];
  if (duracionGuardada && !duraciones.includes(duracionGuardada)) {
    duraciones.push(duracionGuardada);
  }
  return duraciones
    .sort((a, b) => a - b)
    .map((duracion) => ({
      value: duracion,
      label: `${duracion} min`,
      disabled:
        !!horaInicio &&
        !!fin &&
        duracion !== duracionGuardada &&
        aMinutos(horaInicio) + duracion > aMinutos(fin),
    }));
};

// ---------- Fecha ----------

const DIAS_DEL_CALENDARIO: (DiaCoordinador | null)[] = [
  null, // domingo
  'LUN',
  'MAR',
  'MIE',
  'JUE',
  'VIE',
  'SAB',
];

/** El día de la semana de una fecha «AAAA-MM-DD»; null el domingo (nunca es lectivo). Sin depender de la zona horaria. */
export const diaDeLaSemana = (fecha: string): DiaCoordinador | null => {
  const [anio, mes, dia] = fecha.split('-').map(Number);
  return DIAS_DEL_CALENDARIO[
    new Date(Date.UTC(anio, mes - 1, dia)).getUTCDay()
  ];
};

/** Si la fecha cae en un día de clases de la jornada (`jornada.dias`). El resto de las reglas las valida el backend. */
export const esDiaLectivo = (
  fecha: string,
  jornada: JornadaDelDia,
): boolean => {
  const dia = diaDeLaSemana(fecha);
  return dia !== null && jornada.dias.includes(dia);
};

/** «El sábado no es un día de clases de esta sección.» (el mismo texto que el backend responde en el 400). */
export const mensajeDeDiaNoLectivo = (fecha: string): string => {
  const dia = diaDeLaSemana(fecha);
  const nombre = dia ? NOMBRE_DEL_DIA[dia].toLowerCase() : 'domingo';
  return `El ${nombre} no es un día de clases de esta sección.`;
};

// ---------- Año, grado y sección ----------

/** Las secciones ACTIVAS de un grado (las INACTIVAS no se pueden elegir). */
export const seccionesActivas = (
  grado: GradoParaHorarios | undefined,
): SeccionParaHorarios[] =>
  (grado?.secciones ?? []).filter((s) => s.estado === 'ACTIVA');

/** Los grados que tienen al menos una sección activa. */
export const gradosConSeccionesActivas = (
  grados: GradoParaHorarios[],
): GradoParaHorarios[] => grados.filter((g) => seccionesActivas(g).length > 0);

/**
 * El año en el que se crea una reserva: el ACTIVO y, si no hay, el más reciente que no esté CERRADO. undefined si no
 * hay ninguno.
 */
export const anioParaReservar = (
  anios: AnioParaHorarios[],
): AnioParaHorarios | undefined =>
  anios.find((a) => a.estado === 'ACTIVO') ??
  anios
    .filter((a) => a.estado !== 'CERRADO')
    .sort((a, b) => b.anio - a.anio)[0];

// ---------- Errores del backend ----------

/** Los títulos que el backend antepone («Título: texto») a los 409 por cruce. Los demás mensajes van sin título. */
export const TITULOS_DE_CRUCE_DE_RESERVA = [
  'Ambiente no disponible',
  'Responsable no disponible',
  'Sección no disponible',
] as const;

export type MensajeDeReserva = { titulo?: string; texto: string };

/** «Ambiente no disponible: X ya…» -> `{titulo: 'Ambiente no disponible', texto: 'X ya…'}`; otro mensaje, solo `texto`. */
export const partirMensaje = (mensaje: string): MensajeDeReserva => {
  const titulo = TITULOS_DE_CRUCE_DE_RESERVA.find((t) =>
    mensaje.startsWith(`${t}: `),
  );
  return titulo
    ? { titulo, texto: mensaje.slice(titulo.length + 2) }
    : { texto: mensaje };
};

/** El 409 de versión: «La reserva cambió mientras la editabas. Vuelve a cargarla.». */
export const MENSAJE_DE_VERSION = 'La reserva cambió mientras la editabas.';

export const esConflictoDeVersionDeReserva = (error: unknown): boolean =>
  estadoDeError(error) === 409 &&
  mensajeDeError(error).startsWith(MENSAJE_DE_VERSION);

/** Los errores por campo de un 400 de `@Valid` (`{ campo: mensaje }`); undefined si el error no los trae. */
export const erroresDeCampos = (
  error: unknown,
): Record<string, string> | undefined => {
  const errores = (
    error as { response?: { data?: { errores?: Record<string, string> } } }
  )?.response?.data?.errores;
  return errores && Object.keys(errores).length > 0 ? errores : undefined;
};

// ---------- Filtro ----------

export const FILTRO_POR_DEFECTO: FiltroDeReservas = 'SEMANA';
