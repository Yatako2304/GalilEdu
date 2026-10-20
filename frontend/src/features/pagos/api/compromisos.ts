import { useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { CompromisoAnio, CompromisoDetalle } from '@/types/api';

// GET /pagos/estudiantes/{estudianteId}/compromisos (selector "Compromiso 2026")
export const getCompromisosEstudiante = (
  estudianteId: string,
): Promise<CompromisoAnio[]> =>
  api.get(`/pagos/estudiantes/${estudianteId}/compromisos`);

export const useCompromisosEstudiante = (estudianteId: string) =>
  useQuery({
    queryKey: ['pagos', 'compromisos', 'estudiante', estudianteId],
    queryFn: () => getCompromisosEstudiante(estudianteId),
    enabled: !!estudianteId,
  });

// GET /pagos/compromisos/{id} (HU-37: resumen y cronograma de cuotas)
export const getCompromiso = (id: string): Promise<CompromisoDetalle> =>
  api.get(`/pagos/compromisos/${id}`);

export const useCompromiso = (id: string | undefined) =>
  useQuery({
    queryKey: ['pagos', 'compromisos', 'detalle', id],
    queryFn: () => getCompromiso(id as string),
    enabled: !!id,
  });
