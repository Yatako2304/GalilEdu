import {
  keepPreviousData,
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { Pagina } from '@/types/api';

import {
  FiltroDeReservas,
  ReservaDeAmbiente,
  SolicitudEdicionReserva,
  SolicitudReserva,
} from '../types-reservas';

import { CLAVE_HORARIOS } from './horario-seccion';

// HU-48 · Reserva de espacios especiales. Figma > Coordinador > «Reserva de espacios especiales» (308:1874).

/** Todo lo de reservas cuelga de esta clave: crear, editar o eliminar la invalida entera (lista y detalle). */
export const CLAVE_RESERVAS = [...CLAVE_HORARIOS, 'reservas'] as const;

export const TAMANO_PAGINA_RESERVAS = 10;

export const claveListaReservas = (filtro: FiltroDeReservas, page: number) =>
  [...CLAVE_RESERVAS, filtro, page] as const;

export const claveReserva = (id: string | undefined) =>
  [...CLAVE_RESERVAS, 'detalle', id] as const;

// GET /horarios/reservas?filtro=&page=&size=: las reservas activas del rango, de la más reciente a la más antigua.
// 400 si el filtro no existe.
export const getReservas = (
  filtro: FiltroDeReservas,
  page: number,
): Promise<Pagina<ReservaDeAmbiente>> =>
  api.get('/horarios/reservas', {
    params: { filtro, page, size: TAMANO_PAGINA_RESERVAS },
  });

export const getReservasQueryOptions = (
  filtro: FiltroDeReservas,
  page: number,
) =>
  queryOptions({
    queryKey: claveListaReservas(filtro, page),
    queryFn: () => getReservas(filtro, page),
    // Al cambiar de página se sigue viendo la anterior hasta que llegue la nueva.
    placeholderData: keepPreviousData,
  });

export const useReservas = (filtro: FiltroDeReservas, page: number) =>
  useQuery(getReservasQueryOptions(filtro, page));

// GET /horarios/reservas/{id}: 404 «La reserva no existe.» si no existe o ya fue cancelada. Sirve para volver a leer
// una reserva (por ejemplo, tras un 409 de versión al editarla).
export const getReserva = (id: string): Promise<ReservaDeAmbiente> =>
  api.get(`/horarios/reservas/${id}`, { silenciarNotificacion: true });

export const useReserva = (id: string | undefined, habilitado = true) =>
  useQuery(
    queryOptions({
      queryKey: claveReserva(id),
      queryFn: () => getReserva(id as string),
      enabled: !!id && habilitado,
      retry: false,
    }),
  );

const useInvalidarReservas = () => {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: CLAVE_RESERVAS });
};

// POST /horarios/reservas: 201 con la reserva. Los 400 (`message` y `errores` por campo) y los 409 (cruces, año
// cerrado) se muestran dentro del modal, por eso se silencia la notificación de error del cliente.
export const useCrearReserva = (
  onSuccess?: (reserva: ReservaDeAmbiente) => void,
) => {
  const invalidar = useInvalidarReservas();
  return useMutation({
    mutationFn: (data: SolicitudReserva): Promise<ReservaDeAmbiente> =>
      api.post('/horarios/reservas', data, { silenciarNotificacion: true }),
    onSuccess: async (reserva) => {
      await invalidar();
      onSuccess?.(reserva);
    },
  });
};

// PUT /horarios/reservas/{id}: reemplaza los datos; `version` es la que devolvió la lectura. 409 si la versión cambió
// («La reserva cambió mientras la editabas…»), si ya finalizó o fue cancelada, o por cruces.
export const useEditarReserva = (
  id: string,
  onSuccess?: (reserva: ReservaDeAmbiente) => void,
) => {
  const invalidar = useInvalidarReservas();
  return useMutation({
    mutationFn: (data: SolicitudEdicionReserva): Promise<ReservaDeAmbiente> =>
      api.put(`/horarios/reservas/${id}`, data, {
        silenciarNotificacion: true,
      }),
    onSuccess: async (reserva) => {
      await invalidar();
      onSuccess?.(reserva);
    },
  });
};

// DELETE /horarios/reservas/{id}?version=: «Eliminar» cancela la reserva (204). Aquí el error SÍ sale como notificación:
// la confirmación no tiene dónde mostrarlo (409 si ya finalizó, ya está cancelada o la versión cambió).
export const useEliminarReserva = (onSuccess?: () => void) => {
  const invalidar = useInvalidarReservas();
  return useMutation({
    mutationFn: ({
      id,
      version,
    }: {
      id: string;
      version?: number;
    }): Promise<void> =>
      api.delete(`/horarios/reservas/${id}`, {
        params: version === undefined ? undefined : { version },
      }),
    onSuccess: async () => {
      await invalidar();
      onSuccess?.();
    },
  });
};
