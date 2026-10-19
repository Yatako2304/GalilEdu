import { useMutation, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { SolicitudUsuario, UsuarioCreado, UsuarioDetalle } from '@/types/api';

/** Tras cualquier cambio se recargan la lista, el detalle y el resumen del inicio. */
const useRefrescarUsuarios = () => {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ['usuarios'] });
};

// POST /usuarios (HU-6)
export const useCrearUsuario = (onSuccess?: (u: UsuarioCreado) => void) => {
  const refrescar = useRefrescarUsuarios();
  return useMutation({
    mutationFn: (data: SolicitudUsuario): Promise<UsuarioCreado> =>
      api.post('/usuarios', data),
    onSuccess: (u) => {
      refrescar();
      onSuccess?.(u);
    },
  });
};

// PUT /usuarios/{id} (HU-9)
export const useActualizarUsuario = (onSuccess?: () => void) => {
  const refrescar = useRefrescarUsuarios();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: SolicitudUsuario;
    }): Promise<UsuarioDetalle> => api.put(`/usuarios/${id}`, data),
    onSuccess: () => {
      refrescar();
      onSuccess?.();
    },
  });
};

export type AccionEstado = 'desactivar' | 'reactivar' | 'eliminar';

// POST /usuarios/{id}/desactivar (HU-12) · POST /usuarios/{id}/reactivar · DELETE /usuarios/{id}
export const useCambiarEstadoUsuario = (onSuccess?: () => void) => {
  const refrescar = useRefrescarUsuarios();
  return useMutation({
    mutationFn: ({ id, accion }: { id: string; accion: AccionEstado }) =>
      accion === 'eliminar'
        ? api.delete(`/usuarios/${id}`)
        : api.post(`/usuarios/${id}/${accion}`),
    onSuccess: () => {
      refrescar();
      onSuccess?.();
    },
  });
};
