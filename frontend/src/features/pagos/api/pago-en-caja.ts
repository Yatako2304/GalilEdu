import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  CuotaPorPagar,
  PagoCajaRegistrado,
  SolicitudPagoCaja,
} from '@/types/api';

// Pago en caja: el Personal administrativo registra lo que el apoderado pagó en el colegio (P8, «Registrar pago»).

// GET /pagos/compromisos/{id}/cuotas-por-pagar?fecha= (recargo calculado a la fecha del pago)
export const getCuotasPorPagarCompromiso = (
  compromisoId: string,
  fecha: string,
): Promise<CuotaPorPagar[]> =>
  api.get(`/pagos/compromisos/${compromisoId}/cuotas-por-pagar`, {
    params: { fecha },
  });

export const useCuotasPorPagarCompromiso = (
  compromisoId: string,
  fecha: string,
  enabled: boolean,
) =>
  useQuery({
    queryKey: ['pagos', 'compromisos', compromisoId, 'cuotas-por-pagar', fecha],
    queryFn: () => getCuotasPorPagarCompromiso(compromisoId, fecha),
    enabled: enabled && !!fecha,
  });

// POST /pagos/compromisos/{id}/pagos
export const useRegistrarPagoCaja = (
  compromisoId: string,
  onSuccess?: (pago: PagoCajaRegistrado) => void,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (solicitud: SolicitudPagoCaja): Promise<PagoCajaRegistrado> =>
      api.post(`/pagos/compromisos/${compromisoId}/pagos`, solicitud),
    onSuccess: async (pago) => {
      // Cambian el cronograma, el resumen, la morosidad, la pestaña «Pagos» y el estado de cuenta.
      await queryClient.invalidateQueries({ queryKey: ['pagos'] });
      onSuccess?.(pago);
    },
  });
};
