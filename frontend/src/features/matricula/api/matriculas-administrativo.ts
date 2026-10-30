import { useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { MatriculaAdministrativa } from '@/types/api';

const RUTA = '/matricula/administrativo';

export const getMatriculasDelPeriodo = (
  periodoId: string,
): Promise<MatriculaAdministrativa[]> =>
  api.get(`${RUTA}/periodos/${periodoId}/matriculas`);

export const useMatriculasDelPeriodo = (periodoId: string) =>
  useQuery({
    queryKey: ['matricula', 'administrativo', 'periodo', periodoId],
    queryFn: () => getMatriculasDelPeriodo(periodoId),
    enabled: !!periodoId,
  });

export const getMatriculaAdministrativa = (
  matriculaId: string,
): Promise<MatriculaAdministrativa> =>
  api.get(`${RUTA}/matriculas/${matriculaId}`);

export const useMatriculaAdministrativa = (matriculaId?: string) =>
  useQuery({
    queryKey: ['matricula', 'administrativo', 'detalle', matriculaId],
    queryFn: () => getMatriculaAdministrativa(matriculaId as string),
    enabled: !!matriculaId,
  });
