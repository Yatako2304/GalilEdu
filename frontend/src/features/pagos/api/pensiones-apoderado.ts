import { useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  CompromisoAnio,
  CompromisoDetalle,
  HijoConDeuda,
  PagoRealizado,
} from '@/types/api';

export const getHijosConDeuda = (): Promise<HijoConDeuda[]> =>
  api.get('/pagos/apoderado/estudiantes');

export const useHijosConDeuda = () =>
  useQuery({
    queryKey: ['pagos', 'apoderado', 'estudiantes'],
    queryFn: getHijosConDeuda,
  });

export const getCompromisosApoderado = (
  estudianteId: string,
): Promise<CompromisoAnio[]> =>
  api.get(`/pagos/apoderado/estudiantes/${estudianteId}/compromisos`);

export const useCompromisosApoderado = (estudianteId?: string) =>
  useQuery({
    queryKey: [
      'pagos',
      'apoderado',
      'estudiantes',
      estudianteId,
      'compromisos',
    ],
    queryFn: () => getCompromisosApoderado(estudianteId as string),
    enabled: !!estudianteId,
  });

export const getCompromisoApoderado = (
  compromisoId: string,
): Promise<CompromisoDetalle> =>
  api.get(`/pagos/apoderado/compromisos/${compromisoId}`);

export const useCompromisoApoderado = (compromisoId?: string) =>
  useQuery({
    queryKey: ['pagos', 'apoderado', 'compromisos', compromisoId],
    queryFn: () => getCompromisoApoderado(compromisoId as string),
    enabled: !!compromisoId,
  });

export const getPagosApoderado = (
  compromisoId: string,
): Promise<PagoRealizado[]> =>
  api.get(`/pagos/apoderado/compromisos/${compromisoId}/pagos`);

export const usePagosApoderado = (compromisoId?: string) =>
  useQuery({
    queryKey: ['pagos', 'apoderado', 'compromisos', compromisoId, 'pagos'],
    queryFn: () => getPagosApoderado(compromisoId as string),
    enabled: !!compromisoId,
  });
