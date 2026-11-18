import {
  keepPreviousData,
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  AmbienteResumen,
  EstadoAmbiente,
  ResumenAmbientes,
  SolicitudAmbiente,
  SolicitudEdicionAmbiente,
  TipoAmbiente,
} from '@/types/api';

// Figma > Personal Administrativo > CI-26 · Ambientes físicos (HU-43)
// y modal "Nuevo ambiente físico"

/** Todos opcionales: sin filtros se listan todos los ambientes. */
export type FiltrosAmbientes = {
  tipo?: TipoAmbiente;
  estado?: EstadoAmbiente;
  /** Busca solo en el nombre, sin distinguir mayúsculas. */
  q?: string;
};

/** Quita los filtros vacíos: un `q` en blanco no se envía. */
const normalizarFiltros = (filtros: FiltrosAmbientes): FiltrosAmbientes => {
  const q = filtros.q?.trim();
  return {
    ...(filtros.tipo ? { tipo: filtros.tipo } : {}),
    ...(filtros.estado ? { estado: filtros.estado } : {}),
    ...(q ? { q } : {}),
  };
};

// GET /ambientes/resumen (las 4 tarjetas; no dependen de los filtros)
export const getResumenAmbientes = (): Promise<ResumenAmbientes> =>
  api.get('/ambientes/resumen');

// GET /ambientes?tipo=&estado=&q= (lista simple, sin paginar, por nombre)
export const getAmbientes = (
  filtros: FiltrosAmbientes = {},
): Promise<AmbienteResumen[]> =>
  api.get('/ambientes', { params: normalizarFiltros(filtros) });

export const useResumenAmbientes = () =>
  useQuery(
    queryOptions({
      queryKey: ['ambientes', 'resumen'],
      queryFn: getResumenAmbientes,
    }),
  );

/**
 * Al cambiar un filtro o escribir en el buscador se mantiene la lista anterior mientras llega la nueva
 * (`isPlaceholderData`), para que la tabla no parpadee a "cargando" en cada cambio.
 */
export const useAmbientes = (filtros: FiltrosAmbientes = {}) => {
  const normalizados = normalizarFiltros(filtros);
  return useQuery(
    queryOptions({
      queryKey: ['ambientes', 'lista', normalizados],
      queryFn: () => getAmbientes(normalizados),
      placeholderData: keepPreviousData,
    }),
  );
};

/** Tras cualquier cambio se recargan las tarjetas y la lista, con cualquier filtro. */
const useRefrescarAmbientes = () => {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ['ambientes'] });
};

const useMutacion = <V, R>(
  mutationFn: (variables: V) => Promise<R>,
  onSuccess?: (resultado: R) => void,
) => {
  const refrescar = useRefrescarAmbientes();
  return useMutation({
    mutationFn,
    onSuccess: (resultado: R) => {
      refrescar();
      onSuccess?.(resultado);
    },
  });
};

// POST /ambientes (201). El código sale generado: no se envía.
export const useCrearAmbiente = (
  onSuccess?: (ambiente: AmbienteResumen) => void,
) =>
  useMutacion(
    (data: SolicitudAmbiente): Promise<AmbienteResumen> =>
      api.post('/ambientes', data),
    onSuccess,
  );

// PUT /ambientes/{id}: nombre, aforo y recursos (el código y el tipo no cambian)
export const useEditarAmbiente = (
  onSuccess?: (ambiente: AmbienteResumen) => void,
) =>
  useMutacion(
    ({
      id,
      data,
    }: {
      id: string;
      data: SolicitudEdicionAmbiente;
    }): Promise<AmbienteResumen> => api.put(`/ambientes/${id}`, data),
    onSuccess,
  );

// POST /ambientes/{id}/desactivar (se permite aunque esté en uso; 200 si ya estaba inactivo)
export const useDesactivarAmbiente = (
  onSuccess?: (ambiente: AmbienteResumen) => void,
) =>
  useMutacion(
    (id: string): Promise<AmbienteResumen> =>
      api.post(`/ambientes/${id}/desactivar`),
    onSuccess,
  );

// POST /ambientes/{id}/reactivar (200 si ya estaba activo)
export const useReactivarAmbiente = (
  onSuccess?: (ambiente: AmbienteResumen) => void,
) =>
  useMutacion(
    (id: string): Promise<AmbienteResumen> =>
      api.post(`/ambientes/${id}/reactivar`),
    onSuccess,
  );
