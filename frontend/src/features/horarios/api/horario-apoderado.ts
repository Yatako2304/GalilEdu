import { queryOptions, useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';

import { DiaCoordinador, JornadaDelDia } from '../types';

// Figma > Apoderado (real) > "Horario" y Apoderado > "HU-51 Consulta de horario de clases de sección para el
// Apoderado". Iguales a backend/.../horarios/aplicacion/DtosHorarioApoderado.java.

/** Un año en que el hijo está matriculado, con su grado y sección. */
export type MatriculaDelHijo = {
  anio: number;
  grado: string;
  nivel: 'PRIMARIA' | 'SECUNDARIA';
  seccion: string;
};

/** Un hijo matriculado, con sus matrículas del año más reciente al más antiguo. */
export type HijoConHorario = {
  estudianteId: string;
  nombreCompleto: string;
  matriculas: MatriculaDelHijo[];
};

export type TutorDelHorario = {
  nombre: string;
  celular: string | null;
  correo: string | null;
};

/** Un bloque con clase. `docente` es null si el curso aún no tiene docente. */
export type ClaseDelHorario = {
  dia: DiaCoordinador;
  numeroBloque: number;
  curso: string;
  docente: string | null;
  aula: string | null;
};

/** `disponible` es false si el horario de la sección aún no se ha armado: entonces no hay jornada ni clases. */
export type HorarioDelEstudiante = {
  estudianteId: string;
  nombreCompleto: string;
  anio: number;
  grado: string;
  nivel: 'PRIMARIA' | 'SECUNDARIA';
  seccion: string;
  tutor: TutorDelHorario | null;
  disponible: boolean;
  jornada: JornadaDelDia | null;
  clases: ClaseDelHorario[];
};

const CLAVE = ['horarios', 'apoderado'] as const;

// GET /horarios/apoderado/estudiantes: los hijos matriculados del apoderado (selector de Figma).
export const useHijosConHorario = () =>
  useQuery(
    queryOptions({
      queryKey: [...CLAVE, 'estudiantes'],
      queryFn: (): Promise<HijoConHorario[]> =>
        api.get('/horarios/apoderado/estudiantes'),
    }),
  );

// GET /horarios/apoderado/estudiantes/{id}/horario?anio=: el horario de la sección del hijo ese año.
export const useHorarioDelEstudiante = (
  estudianteId: string | undefined,
  anio: number | undefined,
) =>
  useQuery(
    queryOptions({
      queryKey: [...CLAVE, 'horario', estudianteId, anio],
      queryFn: (): Promise<HorarioDelEstudiante> =>
        api.get(`/horarios/apoderado/estudiantes/${estudianteId}/horario`, {
          params: { anio },
        }),
      enabled: !!estudianteId && !!anio,
    }),
  );
