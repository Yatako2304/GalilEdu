import { queryOptions, useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';

import { HorarioDeSeccion, OpcionesDeHorario } from '../types';

// Figma > Coordinador > "HU-45: Asignación de docente y curso a un bloque de horario"

/** Todas las consultas de horarios del coordinador cuelgan de esta clave. */
export const CLAVE_HORARIOS = ['horarios'] as const;

export const claveHorarioSeccion = (seccionId: string | undefined) =>
  [...CLAVE_HORARIOS, 'seccion', seccionId] as const;

export const claveOpciones = (seccionId: string | undefined) =>
  [...CLAVE_HORARIOS, 'opciones', seccionId] as const;

// GET /horarios/secciones/{seccionId}: la matriz. 404 si la sección no existe; 409 «Configura la estructura horaria
// principal de …» si no hay horario ni estructura principal (la pantalla lo muestra como estado vacío, no como error,
// por eso se silencia la notificación).
export const useHorarioSeccion = (seccionId: string | undefined) =>
  useQuery(
    queryOptions({
      queryKey: claveHorarioSeccion(seccionId),
      queryFn: (): Promise<HorarioDeSeccion> =>
        api.get(`/horarios/secciones/${seccionId}`, {
          silenciarNotificacion: true,
        }),
      enabled: !!seccionId,
      retry: false,
    }),
  );

// GET /horarios/secciones/{seccionId}/opciones: cursos del plan con sus horas y ambientes activos. Solo se pide al
// editar, porque lo usa el modal «Asignar bloque horario».
export const useOpcionesHorario = (
  seccionId: string | undefined,
  habilitado: boolean,
) =>
  useQuery(
    queryOptions({
      queryKey: claveOpciones(seccionId),
      queryFn: (): Promise<OpcionesDeHorario> =>
        api.get(`/horarios/secciones/${seccionId}/opciones`),
      enabled: !!seccionId && habilitado,
    }),
  );
