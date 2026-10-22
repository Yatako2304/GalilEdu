import { useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { EstadoMatriculaApoderado, HijoApoderado } from '@/types/api';

export const getHijosApoderado = (): Promise<HijoApoderado[]> =>
  api.get('/matricula/apoderado/estudiantes');

export const useHijosApoderado = () =>
  useQuery({
    queryKey: ['matricula', 'apoderado', 'estudiantes'],
    queryFn: getHijosApoderado,
  });

export const getEstadoMatriculaApoderado = (
  estudianteId: string,
): Promise<EstadoMatriculaApoderado> =>
  api.get(`/matricula/apoderado/estudiantes/${estudianteId}/estado`);

export const useEstadoMatriculaApoderado = (estudianteId?: string) =>
  useQuery({
    queryKey: ['matricula', 'apoderado', 'estado', estudianteId],
    queryFn: () => getEstadoMatriculaApoderado(estudianteId ?? ''),
    enabled: Boolean(estudianteId),
  });
