import {
  EstadoAsistencia,
  FilaAsistencia,
  MotivoJustificacion,
} from '@/types/api';

/** Marca en edición de un estudiante: estado (null = sin marcar) y hora "HH:mm" (solo tardanza). */
export type Marca = { estado: EstadoAsistencia | null; hora: string };

export type Borrador = Record<string, Marca>;

export const OPCIONES_ASISTENCIA = [
  { value: 'PRESENTE' as const, label: 'Presente', color: 'success' as const },
  { value: 'TARDANZA' as const, label: 'Tardanza', color: 'warning' as const },
  { value: 'FALTA' as const, label: 'Falta', color: 'error' as const },
];

export const ETIQUETA_ESTADO: Record<EstadoAsistencia, string> = {
  PRESENTE: 'Presente',
  TARDANZA: 'Tardanza',
  FALTA: 'Falta',
};

export const COLOR_ESTADO: Record<
  EstadoAsistencia,
  'success' | 'warning' | 'error'
> = {
  PRESENTE: 'success',
  TARDANZA: 'warning',
  FALTA: 'error',
};

/** Hoy en Lima, en formato YYYY-MM-DD (el "hoy" del colegio, no el del navegador). */
export const hoyEnLima = (ahora = new Date()) =>
  new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Lima',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(ahora);

/** "2026-10-02" → "Viernes 02 oct 2026" (botón de fecha de AS-01). */
export const fechaLarga = (fecha: string) => {
  const [anio, mes, dia] = fecha.split('-').map(Number);
  const texto = new Intl.DateTimeFormat('es-PE', {
    weekday: 'long',
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    timeZone: 'UTC',
  })
    .format(new Date(Date.UTC(anio, mes - 1, dia)))
    .replace(/,/g, '')
    .replace(/\.(?=\s)/, '')
    .replace(' de ', ' ')
    .replace(' de ', ' ');
  return texto.charAt(0).toUpperCase() + texto.slice(1);
};

/** "2026-10-02T08:05:00-05:00" → "08:05" en hora de Lima. */
export const horaLima = (instante: string) =>
  new Intl.DateTimeFormat('es-PE', {
    timeZone: 'America/Lima',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(instante));

/** "08:10:00" → "08:10". */
export const horaCorta = (hora: string | null) =>
  hora ? hora.slice(0, 5) : '';

export const nombreCompleto = (fila: FilaAsistencia) =>
  `${fila.nombres} ${fila.apellidos}`;

/** Marcas iniciales: lo guardado (al corregir) o todo sin marcar (al registrar). */
export const borradorDe = (filas: FilaAsistencia[]): Borrador =>
  Object.fromEntries(
    filas.map((f) => [
      f.matriculaId,
      { estado: f.estado, hora: horaCorta(f.horaLlegada) },
    ]),
  );

export const contar = (filas: FilaAsistencia[], borrador: Borrador) => {
  const estados = filas.map((f) => borrador[f.matriculaId]?.estado ?? null);
  const presentes = estados.filter((e) => e === 'PRESENTE').length;
  const tardanzas = estados.filter((e) => e === 'TARDANZA').length;
  const faltas = estados.filter((e) => e === 'FALTA').length;
  return {
    presentes,
    tardanzas,
    faltas,
    sinMarcar: filas.length - presentes - tardanzas - faltas,
    total: filas.length,
  };
};

export type Cambio = {
  fila: FilaAsistencia;
  antes: { estado: EstadoAsistencia | null; hora: string };
  despues: Marca;
};

/** AS-06: las filas cuyo estado u hora cambió respecto de lo guardado. */
export const cambios = (filas: FilaAsistencia[], borrador: Borrador) =>
  filas.flatMap((fila): Cambio[] => {
    const despues = borrador[fila.matriculaId];
    if (!despues) return [];
    const antes = { estado: fila.estado, hora: horaCorta(fila.horaLlegada) };
    const horaDespues = despues.estado === 'TARDANZA' ? despues.hora : '';
    const horaAntes = antes.estado === 'TARDANZA' ? antes.hora : '';
    return despues.estado === antes.estado && horaDespues === horaAntes
      ? []
      : [{ fila, antes, despues }];
  });

/** Una tardanza necesita su hora de llegada (acordado el 19/11). */
export const tardanzaSinHora = (marca?: Marca) =>
  marca?.estado === 'TARDANZA' && !marca.hora;

/** HU-67: motivos acordados el 19/11 (Figma solo muestra "Cita Médica"). */
export const MOTIVOS_JUSTIFICACION: {
  value: MotivoJustificacion;
  label: string;
}[] = [
  { value: 'CITA_MEDICA', label: 'Cita médica' },
  { value: 'ENFERMEDAD', label: 'Enfermedad' },
  { value: 'TRAMITE_OFICIAL', label: 'Trámite oficial' },
  { value: 'OTRO', label: 'Otro' },
];

export const ETIQUETA_MOTIVO = Object.fromEntries(
  MOTIVOS_JUSTIFICACION.map((m) => [m.value, m.label]),
) as Record<MotivoJustificacion, string>;
