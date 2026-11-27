import { useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { EstadoCuentaGeneral } from '@/types/api';

// GET /pagos/estado-cuenta?anio= (P2 · Estado de cuenta, HU-37)
export const getEstadoCuenta = (anio?: number): Promise<EstadoCuentaGeneral> =>
  api.get('/pagos/estado-cuenta', { params: anio ? { anio } : {} });

export const useEstadoCuenta = (anio?: number) =>
  useQuery({
    queryKey: ['pagos', 'estado-cuenta', anio ?? 'activo'],
    queryFn: () => getEstadoCuenta(anio),
  });
