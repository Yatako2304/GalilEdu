// HU-48 · Reserva de espacios especiales (coordinador). Contrato del backend de /horarios/reservas
// (`ReservaAmbienteController`, `DtosReservas`). Los tipos de la HU-45 que se reutilizan se importan de `./types`.

import type { AmbienteOpcion, Referencia } from './types';

/** Filtros de fecha del historial (Figma: «Esta semana», «Este mes», «Este año»). Por defecto, SEMANA. */
export type FiltroDeReservas = 'SEMANA' | 'MES' | 'ANIO';

export const FILTROS_DE_RESERVAS: {
  valor: FiltroDeReservas;
  etiqueta: string;
}[] = [
  { valor: 'SEMANA', etiqueta: 'Esta semana' },
  { valor: 'MES', etiqueta: 'Este mes' },
  { valor: 'ANIO', etiqueta: 'Este año' },
];

/** Lo calcula el backend al leer, con la hora de fin: no se guarda. */
export type EstadoDeReserva = 'PENDIENTE' | 'FINALIZADO';

/** El código («LAB-01») y el nombre («Laboratorio de ciencias») se ven en la tabla. */
export type AmbienteDeReserva = { id: string; codigo: string; nombre: string };

/** `etiqueta` es «4.° B», como en el resto del módulo. */
export type SeccionDeReserva = { id: string; etiqueta: string };

/**
 * Una reserva. `fecha` es «AAAA-MM-DD» y las horas «HH:mm». `editable` es true solo si está activa y no ha finalizado;
 * `version` es la que debe volver en el PUT.
 */
export type ReservaDeAmbiente = {
  id: string;
  ambiente: AmbienteDeReserva;
  actividad: string;
  anioId: string;
  gradoCodigo: string;
  seccion: SeccionDeReserva;
  responsable: Referencia;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  estado: EstadoDeReserva;
  editable: boolean;
  version: number;
};

/** Cuerpo del POST: todos los campos son obligatorios; `actividad` admite hasta 120 caracteres. */
export type SolicitudReserva = {
  ambienteId: string;
  seccionId: string;
  responsableId: string;
  actividad: string;
  fecha: string;
  horaInicio: string;
  horaFin: string;
};

/** Cuerpo del PUT: lo mismo más la `version` que devolvió la lectura. */
export type SolicitudEdicionReserva = SolicitudReserva & { version: number };

/** GET /horarios/reservas/opciones: los espacios especiales activos y quienes pueden ser responsables. */
export type OpcionesDeReserva = {
  ambientes: AmbienteOpcion[];
  responsables: Referencia[];
};
