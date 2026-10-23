import { useMutation, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  AnioEscolarDetalle,
  AnioEscolarResumen,
  PeriodoDetalle,
  Regimen,
  SolicitudAnioEscolar,
  SolicitudEdicionAnio,
  SolicitudPeriodo,
} from '@/types/api';

import {
  CLAVE_ACTUAL,
  CLAVE_ANIOS,
  CLAVE_LISTA,
  anioActivo,
} from './anios-escolares';

/**
 * Tras cualquier cambio se recarga primero la lista; recién entonces el resto. Si la lista ya no tiene un año
 * ACTIVO, se descarta la consulta `actual` en lugar de recargarla: el backend respondería 404 y saldría como
 * notificación de error.
 */
const useRefrescarAnios = () => {
  const queryClient = useQueryClient();
  return async () => {
    await queryClient.invalidateQueries({ queryKey: CLAVE_LISTA });
    const lista = queryClient.getQueryData<AnioEscolarResumen[]>(CLAVE_LISTA);
    if (lista && !anioActivo(lista)) {
      queryClient.removeQueries({ queryKey: CLAVE_ACTUAL });
    }
    await queryClient.invalidateQueries({
      queryKey: CLAVE_ANIOS,
      predicate: ({ queryKey }) => queryKey[2] !== 'lista',
    });
  };
};

// POST /configuracion/anios-escolares (modal Nuevo año escolar)
export const useCrearAnio = (
  onSuccess?: (anio: AnioEscolarDetalle) => void,
) => {
  const refrescar = useRefrescarAnios();
  return useMutation({
    mutationFn: (data: SolicitudAnioEscolar): Promise<AnioEscolarDetalle> =>
      api.post('/configuracion/anios-escolares', data),
    onSuccess: async (anio) => {
      await refrescar();
      onSuccess?.(anio);
    },
  });
};

// PUT /configuracion/anios-escolares/{id} (botón Editar de CI-06: solo con el año PLANIFICADO)
export const useEditarAnio = (
  onSuccess?: (anio: AnioEscolarDetalle) => void,
) => {
  const refrescar = useRefrescarAnios();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: SolicitudEdicionAnio;
    }): Promise<AnioEscolarDetalle> =>
      api.put(`/configuracion/anios-escolares/${id}`, data),
    onSuccess: async (anio) => {
      await refrescar();
      onSuccess?.(anio);
    },
  });
};

// PUT /configuracion/anios-escolares/{id}/regimen (modal Cambiar régimen de periodos)
export const useCambiarRegimen = (
  onSuccess?: (anio: AnioEscolarDetalle) => void,
) => {
  const refrescar = useRefrescarAnios();
  return useMutation({
    mutationFn: ({
      id,
      regimen,
    }: {
      id: string;
      regimen: Regimen;
    }): Promise<AnioEscolarDetalle> =>
      api.put(`/configuracion/anios-escolares/${id}/regimen`, { regimen }),
    onSuccess: async (anio) => {
      await refrescar();
      onSuccess?.(anio);
    },
  });
};

// POST /configuracion/anios-escolares/{id}/activar (409 si otro año sigue activo o faltan requisitos)
export const useActivarAnio = (
  onSuccess?: (anio: AnioEscolarDetalle) => void,
) => {
  const refrescar = useRefrescarAnios();
  return useMutation({
    mutationFn: (id: string): Promise<AnioEscolarDetalle> =>
      api.post(`/configuracion/anios-escolares/${id}/activar`),
    onSuccess: async (anio) => {
      await refrescar();
      onSuccess?.(anio);
    },
  });
};

// POST /configuracion/anios-escolares/{id}/cerrar (409 mientras los docentes aún pueden registrar notas)
export const useCerrarAnio = (
  onSuccess?: (anio: AnioEscolarDetalle) => void,
) => {
  const refrescar = useRefrescarAnios();
  return useMutation({
    mutationFn: (id: string): Promise<AnioEscolarDetalle> =>
      api.post(`/configuracion/anios-escolares/${id}/cerrar`),
    onSuccess: async (anio) => {
      await refrescar();
      onSuccess?.(anio);
    },
  });
};

// PUT /configuracion/periodos-academicos/{id} (modal Editar periodo)
export const useEditarPeriodo = (
  onSuccess?: (periodo: PeriodoDetalle) => void,
) => {
  const refrescar = useRefrescarAnios();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: SolicitudPeriodo;
    }): Promise<PeriodoDetalle> =>
      api.put(`/configuracion/periodos-academicos/${id}`, data),
    onSuccess: async (periodo) => {
      await refrescar();
      onSuccess?.(periodo);
    },
  });
};
