import { useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { PagoRealizado } from '@/types/api';

export const getPagosCompromiso = (
  compromisoId: string,
): Promise<PagoRealizado[]> =>
  api.get(`/pagos/compromisos/${compromisoId}/pagos`);

export const usePagosCompromiso = (compromisoId?: string) =>
  useQuery({
    queryKey: ['pagos', 'compromisos', 'pagos', compromisoId],
    queryFn: () => getPagosCompromiso(compromisoId as string),
    enabled: !!compromisoId,
  });
