import type { AmbienteOpcion, Referencia } from '@/features/horarios/types';
import type {
  OpcionesDeReserva,
  ReservaDeAmbiente,
} from '@/features/horarios/types-reservas';

import {
  ANIO_2026,
  SECCION_4A,
  SECCION_5A,
  gradosCoordinador,
} from './horarios-coordinador';

// HU-48 (coordinador): los mismos datos que siembra `R__datos_prueba.sql` para las reservas de espacios especiales.
// Los endpoints de año, grado y sección y la jornada de una sección los responde `horarios-coordinador` (HU-45).

/** «Hoy» de los simuladores: el miércoles 18 de noviembre de 2026, 10:00. Las pruebas lo cambian con `fijarAhora`. */
export const HOY_SIMULADO = '2026-11-18';
export const AHORA_SIMULADO = `${HOY_SIMULADO}T10:00`;

export const AMBIENTE_LAB01 = 'amb-lab-01';
export const AMBIENTE_LAB02 = 'amb-lab-02';
export const AMBIENTE_BIB01 = 'amb-bib-01';

/** Los espacios especiales activos de los datos de prueba (el backend los manda por código). */
export const espaciosDeReserva: AmbienteOpcion[] = [
  {
    id: AMBIENTE_BIB01,
    nombre: 'Biblioteca',
    codigo: 'BIB-01',
    tipo: 'ESPACIO_ESPECIAL',
    aforoMaximo: 40,
  },
  {
    id: AMBIENTE_LAB01,
    nombre: 'Laboratorio de ciencias',
    codigo: 'LAB-01',
    tipo: 'ESPACIO_ESPECIAL',
    aforoMaximo: 30,
  },
  {
    id: AMBIENTE_LAB02,
    nombre: 'Laboratorio de cómputo',
    codigo: 'LAB-02',
    tipo: 'ESPACIO_ESPECIAL',
    aforoMaximo: 35,
  },
];

export const RESPONSABLE_MARIA: Referencia = {
  id: 'doc-maria',
  nombre: 'María Fernández Ríos',
};

export const opcionesDeReserva: OpcionesDeReserva = {
  ambientes: espaciosDeReserva,
  responsables: [RESPONSABLE_MARIA],
};

// Secciones de las reservas sembradas. 4.° A y 5.° A son las de `gradosCoordinador` (así el modal de edición las
// encuentra en los selectores); las demás solo existen para dibujar la tabla.
export const SECCION_1A = 'sec-1a';
export const SECCION_1B = 'sec-1b';
export const SECCION_2A = 'sec-2a';
export const SECCION_3A = 'sec-3a';

type DatosDeSeccion = { gradoCodigo: string; etiqueta: string };

const seccionesSembradas: Record<string, DatosDeSeccion> = {
  [SECCION_1A]: { gradoCodigo: 'SEC-1', etiqueta: '1.° A' },
  [SECCION_1B]: { gradoCodigo: 'SEC-1', etiqueta: '1.° B' },
  [SECCION_2A]: { gradoCodigo: 'SEC-2', etiqueta: '2.° A' },
  [SECCION_3A]: { gradoCodigo: 'SEC-3', etiqueta: '3.° A' },
};

/** El grado y la etiqueta («4.° B») de una sección conocida por los simuladores; undefined si no existe. */
export const buscarSeccionDeReserva = (
  id: string,
): DatosDeSeccion | undefined => {
  if (seccionesSembradas[id]) return seccionesSembradas[id];
  for (const grado of gradosCoordinador) {
    const seccion = grado.secciones.find((s) => s.id === id);
    if (seccion) {
      return {
        gradoCodigo: grado.codigo,
        etiqueta: `${grado.numero}.° ${seccion.nombre}`,
      };
    }
  }
  return undefined;
};

/** Una reserva como la guarda el backend: el estado y `editable` se calculan al leerla. */
export type ReservaGuardada = Omit<ReservaDeAmbiente, 'estado' | 'editable'> & {
  estadoGuardado: 'ACTIVA' | 'CANCELADA';
};

const espacio = (id: string) => {
  const e = espaciosDeReserva.find((a) => a.id === id) as AmbienteOpcion;
  return { id: e.id, codigo: e.codigo, nombre: e.nombre };
};

const reserva = (
  numero: number,
  ambienteId: string,
  seccionId: string,
  actividad: string,
  fecha: string,
  horaInicio: string,
  horaFin: string,
  estadoGuardado: ReservaGuardada['estadoGuardado'] = 'ACTIVA',
): ReservaGuardada => {
  const seccion = buscarSeccionDeReserva(seccionId) as DatosDeSeccion;
  return {
    id: `reserva-${numero}`,
    ambiente: espacio(ambienteId),
    actividad,
    anioId: ANIO_2026,
    gradoCodigo: seccion.gradoCodigo,
    seccion: { id: seccionId, etiqueta: seccion.etiqueta },
    responsable: RESPONSABLE_MARIA,
    fecha,
    horaInicio,
    horaFin,
    version: 0,
    estadoGuardado,
  };
};

/**
 * Las seis reservas de los datos de prueba: tres futuras (19/11, 20/11 y 9/12), dos pasadas que se ven «Finalizado»
 * (3/11 y 12/11) y una cancelada (27/11) que no sale en ningún listado.
 */
export const reservasSembradas = (): ReservaGuardada[] => [
  reserva(
    1,
    AMBIENTE_LAB01,
    SECCION_1A,
    'Práctica de laboratorio de química',
    '2026-11-19',
    '08:15',
    '09:45',
  ),
  reserva(
    2,
    AMBIENTE_LAB02,
    SECCION_4A,
    'Taller de programación',
    '2026-11-20',
    '10:05',
    '11:35',
  ),
  reserva(
    3,
    AMBIENTE_BIB01,
    SECCION_5A,
    'Sesión de lectura y búsqueda de fuentes',
    '2026-12-09',
    '08:15',
    '09:45',
  ),
  reserva(
    4,
    AMBIENTE_LAB01,
    SECCION_2A,
    'Experimento de densidad',
    '2026-11-03',
    '07:30',
    '09:00',
  ),
  reserva(
    5,
    AMBIENTE_LAB02,
    SECCION_1B,
    'Taller de hojas de cálculo',
    '2026-11-12',
    '10:05',
    '11:35',
  ),
  reserva(
    6,
    AMBIENTE_LAB01,
    SECCION_3A,
    'Práctica de microscopía',
    '2026-11-27',
    '08:15',
    '09:45',
    'CANCELADA',
  ),
];

export const MENSAJE_DE_VERSION_SIMULADO =
  'La reserva cambió mientras la editabas. Vuelve a cargarla.';
