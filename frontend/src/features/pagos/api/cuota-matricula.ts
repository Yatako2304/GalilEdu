import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { CuotaMatriculaDetalle } from '@/types/api';

const ruta = (matriculaId: string) =>
  `/pagos/matriculas/${matriculaId}/cuota-matricula`;

export const getCuotaMatricula = async (matriculaId: string) => {
  const respuesta = (await api.get(ruta(matriculaId), {
    validateStatus: (status) => status === 200 || status === 404,
  })) as unknown as CuotaMatriculaDetalle | { message?: string };
  return 'cuotaId' in respuesta ? respuesta : null;
};

export const useCuotaMatricula = (matriculaId?: string) =>
  useQuery({
    queryKey: ['pagos', 'cuota-matricula', matriculaId],
    queryFn: () => getCuotaMatricula(matriculaId ?? ''),
    enabled: Boolean(matriculaId),
    retry: false,
  });

export const useGenerarCuotaMatricula = (matriculaId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (): Promise<CuotaMatriculaDetalle> =>
      api.post(ruta(matriculaId)),
    onSuccess: (cuota) =>
      queryClient.setQueryData(
        ['pagos', 'cuota-matricula', matriculaId],
        cuota,
      ),
  });
};
