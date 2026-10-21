import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { EstadoCuentaGeneral, RetiroRegistrado } from '@/types/api';

// GET /pagos/estado-cuenta?anio= (P2 · Estado de cuenta, HU-37)
export const getEstadoCuenta = (anio?: number): Promise<EstadoCuentaGeneral> =>
  api.get('/pagos/estado-cuenta', { params: anio ? { anio } : {} });

export const useEstadoCuenta = (anio?: number) =>
  useQuery({
    queryKey: ['pagos', 'estado-cuenta', anio ?? 'activo'],
    queryFn: () => getEstadoCuenta(anio),
  });

// POST /pagos/compromisos/{id}/retiro (RF-85): anula las pensiones futuras
export const useRegistrarRetiro = (
  onSuccess?: (r: RetiroRegistrado) => void,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      compromisoId,
      fechaRetiro,
    }: {
      compromisoId: string;
      fechaRetiro: string;
    }): Promise<RetiroRegistrado> =>
      api.post(`/pagos/compromisos/${compromisoId}/retiro`, { fechaRetiro }),
    onSuccess: (r) => {
      queryClient.invalidateQueries({ queryKey: ['pagos'] });
      onSuccess?.(r);
    },
  });
};
