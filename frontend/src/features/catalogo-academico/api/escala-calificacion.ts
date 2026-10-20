import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { SolicitudTablaConversion, TablaConversionDetalle } from '@/types/api';

// Figma > Personal Administrativo > CI-23 · Escala de calificación (HU-55)

// GET /tablas-conversion (de la más nueva a la más antigua)
export const getTablasConversion = (): Promise<TablaConversionDetalle[]> =>
  api.get('/tablas-conversion');

export const useTablasConversion = () =>
  useQuery(
    queryOptions({
      queryKey: ['escala-calificacion'],
      queryFn: getTablasConversion,
    }),
  );

// POST /tablas-conversion
export const useCrearTablaConversion = (onSuccess?: () => void) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (
      data: SolicitudTablaConversion,
    ): Promise<TablaConversionDetalle> => api.post('/tablas-conversion', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['escala-calificacion'] });
      onSuccess?.();
    },
  });
};
