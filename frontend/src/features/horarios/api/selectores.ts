import { queryOptions, useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';

import { AnioParaHorarios, GradoParaHorarios } from '../types';

// Selectores de año, grado y sección de la pantalla del coordinador. Usan los endpoints de Horarios, no los de
// Configuración: esos (HU-74 y HU-75) son solo del Personal administrativo y el coordinador recibiría 403.

// GET /horarios/anios-escolares: del más reciente al más antiguo
export const useAniosParaHorarios = () =>
  useQuery(
    queryOptions({
      queryKey: ['horarios', 'anios'],
      queryFn: (): Promise<AnioParaHorarios[]> =>
        api.get('/horarios/anios-escolares'),
    }),
  );

// GET /horarios/anios-escolares/{anioId}/secciones: los grados del año con sus secciones. 404 si el año no existe.
export const useGradosParaHorarios = (anioId: string | undefined) =>
  useQuery(
    queryOptions({
      queryKey: ['horarios', 'grados', anioId],
      queryFn: (): Promise<GradoParaHorarios[]> =>
        api.get(`/horarios/anios-escolares/${anioId}/secciones`),
      enabled: !!anioId,
    }),
  );
