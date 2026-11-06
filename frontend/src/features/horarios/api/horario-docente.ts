import { queryOptions, useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';

import { DiaCoordinador, JornadaDelDia } from '../types';

// Figma > Docente / Tutor > Horario (HU-49, 393:18119). Iguales a
// backend/.../horarios/aplicacion/DtosHorarioDocente.java. El docente es siempre el usuario que inició sesión.

/** Un año escolar. `estado` es PLANIFICADO, ACTIVO o CERRADO. */
export type AnioDelDocente = {
  id: string;
  anio: number;
  estado: 'PLANIFICADO' | 'ACTIVO' | 'CERRADO';
};

/** Un bloque que el docente dicta: curso, sección ("1.° A") y nombre del ambiente. */
export type ClaseDictada = {
  dia: DiaCoordinador;
  numeroBloque: number;
  curso: string;
  seccion: string;
  ambiente: string;
};

/**
 * Las clases que siguen una misma estructura horaria, con su jornada. `titulo` es «Primaria» o «Secundaria» (con el
 * código si hay dos del mismo nivel). Dos clases pueden caer en la misma celda si un cruce quedó guardado.
 */
export type HorarioPorEstructura = {
  nivel: 'PRIMARIA' | 'SECUNDARIA';
  titulo: string;
  jornada: JornadaDelDia;
  clases: ClaseDictada[];
};

/** `anio` es null si el docente no tiene clases en ningún año; `horarios` está vacío si no tiene clases en el año. */
export type HorarioDocente = {
  anio: AnioDelDocente | null;
  horarios: HorarioPorEstructura[];
};

// GET /horarios/docente/anios: los años en que tiene clases, del más reciente al más antiguo.
export const useAniosDocente = () =>
  useQuery(
    queryOptions({
      queryKey: ['horarios', 'docente', 'anios'],
      queryFn: (): Promise<AnioDelDocente[]> =>
        api.get('/horarios/docente/anios'),
    }),
  );

// GET /horarios/docente/horario?anioId=: sin `anioId`, el año ACTIVO si tiene clases y, si no, el más reciente con clases.
export const useHorarioDocente = (anioId?: string) =>
  useQuery(
    queryOptions({
      queryKey: ['horarios', 'docente', 'horario', { anioId }],
      queryFn: (): Promise<HorarioDocente> =>
        api.get('/horarios/docente/horario', {
          params: anioId ? { anioId } : undefined,
        }),
    }),
  );
