import { useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { MorosidadCompromiso } from '@/types/api';

export const getMorosidadCompromiso = (
  compromisoId: string,
): Promise<MorosidadCompromiso> =>
  api.get(`/pagos/compromisos/${compromisoId}/morosidad`);

export const useMorosidadCompromiso = (compromisoId?: string) =>
  useQuery({
    queryKey: ['pagos', 'compromisos', 'morosidad', compromisoId],
    queryFn: () => getMorosidadCompromiso(compromisoId as string),
    enabled: !!compromisoId,
  });
