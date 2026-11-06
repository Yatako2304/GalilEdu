import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  ConfiguracionDescuentos,
  DescuentoDelCompromiso,
  DescuentoVista,
  DescuentosAplicables,
  ResultadoDescuento,
  SolicitudAplicarDescuento,
  SolicitudConfiguracionDescuentos,
  SolicitudDescuento,
  SolicitudRevocarDescuento,
} from '@/types/api';

export const CLAVE_DESCUENTOS = ['pagos', 'descuentos'] as const;

export const getDescuentos = (): Promise<DescuentoVista[]> =>
  api.get('/pagos/descuentos');

export const useDescuentos = () =>
  useQuery({ queryKey: CLAVE_DESCUENTOS, queryFn: getDescuentos });

export const getDescuento = (id: string): Promise<DescuentoVista> =>
  api.get(`/pagos/descuentos/${id}`);

export const getConfiguracionDescuentos =
  (): Promise<ConfiguracionDescuentos> =>
    api.get('/pagos/descuentos/configuracion');

export const useConfiguracionDescuentos = () =>
  useQuery({
    queryKey: [...CLAVE_DESCUENTOS, 'configuracion'],
    queryFn: getConfiguracionDescuentos,
  });

const useInvalidarCatalogo = () => {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: CLAVE_DESCUENTOS });
};

export const useCrearDescuento = (
  onSuccess?: (descuento: DescuentoVista) => void,
) => {
  const invalidar = useInvalidarCatalogo();
  return useMutation({
    mutationFn: (solicitud: SolicitudDescuento): Promise<DescuentoVista> =>
      api.post('/pagos/descuentos', solicitud),
    onSuccess: async (descuento) => {
      await invalidar();
      onSuccess?.(descuento);
    },
  });
};

export const useEditarDescuento = (
  onSuccess?: (descuento: DescuentoVista) => void,
) => {
  const invalidar = useInvalidarCatalogo();
  return useMutation({
    mutationFn: ({
      id,
      solicitud,
    }: {
      id: string;
      solicitud: SolicitudDescuento;
    }): Promise<DescuentoVista> =>
      api.put(`/pagos/descuentos/${id}`, solicitud),
    onSuccess: async (descuento) => {
      await invalidar();
      onSuccess?.(descuento);
    },
  });
};

export const useCambiarEstadoDescuento = (
  onSuccess?: (descuento: DescuentoVista) => void,
) => {
  const invalidar = useInvalidarCatalogo();
  return useMutation({
    mutationFn: ({
      id,
      activo,
    }: {
      id: string;
      activo: boolean;
    }): Promise<DescuentoVista> =>
      api.post(
        `/pagos/descuentos/${id}/${activo ? 'activacion' : 'desactivacion'}`,
      ),
    onSuccess: async (descuento) => {
      await invalidar();
      onSuccess?.(descuento);
    },
  });
};

export const useCambiarConfiguracionDescuentos = (
  onSuccess?: (configuracion: ConfiguracionDescuentos) => void,
) => {
  const invalidar = useInvalidarCatalogo();
  return useMutation({
    mutationFn: (
      solicitud: SolicitudConfiguracionDescuentos,
    ): Promise<ConfiguracionDescuentos> =>
      api.put('/pagos/descuentos/configuracion', solicitud),
    onSuccess: async (configuracion) => {
      await invalidar();
      onSuccess?.(configuracion);
    },
  });
};

export const getDescuentosAplicables = (
  compromisoId: string,
): Promise<DescuentosAplicables> =>
  api.get(`/pagos/compromisos/${compromisoId}/descuentos-aplicables`);

export const useDescuentosAplicables = (
  compromisoId?: string,
  enabled = true,
) =>
  useQuery({
    queryKey: ['pagos', 'compromisos', compromisoId, 'descuentos-aplicables'],
    queryFn: () => getDescuentosAplicables(compromisoId as string),
    enabled: !!compromisoId && enabled,
  });

export const getDescuentoCompromiso = (
  compromisoId: string,
): Promise<DescuentoDelCompromiso> =>
  api.get(`/pagos/compromisos/${compromisoId}/descuento`);

export const useDescuentoCompromiso = (compromisoId?: string) =>
  useQuery({
    queryKey: ['pagos', 'compromisos', compromisoId, 'descuento'],
    queryFn: () => getDescuentoCompromiso(compromisoId as string),
    enabled: !!compromisoId,
  });

const invalidarCompromiso = async (
  queryClient: ReturnType<typeof useQueryClient>,
  compromisoId: string,
) => {
  await Promise.all([
    queryClient.invalidateQueries({
      queryKey: ['pagos', 'compromisos', 'detalle', compromisoId],
    }),
    queryClient.invalidateQueries({
      queryKey: ['pagos', 'compromisos', compromisoId],
    }),
  ]);
};

export const useAplicarDescuento = (
  compromisoId: string,
  onSuccess?: (resultado: ResultadoDescuento) => void,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (
      solicitud: SolicitudAplicarDescuento,
    ): Promise<ResultadoDescuento> =>
      api.post(`/pagos/compromisos/${compromisoId}/descuentos`, solicitud),
    onSuccess: async (resultado) => {
      await invalidarCompromiso(queryClient, compromisoId);
      onSuccess?.(resultado);
    },
  });
};

export const useRevocarDescuento = (
  compromisoId: string,
  onSuccess?: (resultado: ResultadoDescuento) => void,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      descuentoAplicadoId,
      solicitud,
    }: {
      descuentoAplicadoId: string;
      solicitud: SolicitudRevocarDescuento;
    }): Promise<ResultadoDescuento> =>
      api.post(
        `/pagos/descuentos-aplicados/${descuentoAplicadoId}/revocacion`,
        solicitud,
      ),
    onSuccess: async (resultado) => {
      await invalidarCompromiso(queryClient, compromisoId);
      onSuccess?.(resultado);
    },
  });
};
