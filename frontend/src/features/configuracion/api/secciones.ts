import { queryOptions, useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { DocenteElegible, GradoConSecciones } from '@/types/api';

// Figma > Personal Administrativo > CI-09 · Secciones por grado y tutores (HU-75)

/** Todas las consultas de secciones cuelgan de esta clave; las mutaciones la invalidan. */
export const CLAVE_SECCIONES = ['configuracion', 'secciones'] as const;

// GET /configuracion/anios-escolares/{anioId}/secciones: los grados del año, también los que no tienen secciones
export const useGradosConSecciones = (anioId: string | undefined) =>
  useQuery(
    queryOptions({
      queryKey: [...CLAVE_SECCIONES, 'grados', anioId],
      queryFn: (): Promise<GradoConSecciones[]> =>
        api.get(`/configuracion/anios-escolares/${anioId}/secciones`),
      enabled: !!anioId,
    }),
  );

/**
 * GET /configuracion/secciones/tutores-disponibles?anioEscolarId=: los docentes y tutores activos, marcando con
 * `esTutorEsteAnio` a quienes ya tienen una sección. Solo se pide cuando el modal Asignar tutor está abierto.
 */
export const useDocentesElegibles = (
  anioId: string | undefined,
  habilitado = true,
) =>
  useQuery(
    queryOptions({
      queryKey: [...CLAVE_SECCIONES, 'docentes-elegibles', anioId],
      queryFn: (): Promise<DocenteElegible[]> =>
        api.get('/configuracion/secciones/tutores-disponibles', {
          params: { anioEscolarId: anioId },
        }),
      enabled: !!anioId && habilitado,
    }),
  );
