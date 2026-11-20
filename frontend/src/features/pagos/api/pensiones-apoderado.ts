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

/** HU-42 · RF-108: descarga la constancia del pago en PDF (`constancia-C-2026-00007.pdf`). */
export const descargarConstancia = async (pago: PagoRealizado) => {
  const contenido = (await api.get(
    `/pagos/apoderado/pagos/${pago.id}/constancia`,
    { responseType: 'blob' },
  )) as Blob;
  const url = URL.createObjectURL(contenido);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = `constancia-${pago.numeroConstancia}.pdf`;
  enlace.click();
  URL.revokeObjectURL(url);
};

export const usePagosApoderado = (compromisoId?: string) =>
  useQuery({
    queryKey: ['pagos', 'apoderado', 'compromisos', compromisoId, 'pagos'],
    queryFn: () => getPagosApoderado(compromisoId as string),
    enabled: !!compromisoId,
  });
