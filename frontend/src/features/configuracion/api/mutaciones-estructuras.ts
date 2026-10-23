import { useMutation, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  EstructuraGuardada,
  SolicitudEdicionEstructuraHoraria,
  SolicitudEstructuraHoraria,
} from '@/types/api';

import { CLAVE_ANIOS } from './anios-escolares';
import { CLAVE_ESTRUCTURAS } from './estructuras-horarias';

/**
 * Tras cualquier cambio se recargan las estructuras y también los datos del año: la cabecera muestra
 * "Estructura horaria (n)" y los requisitos de CI-06 dependen de ellas.
 */
const useRefrescarEstructuras = () => {
  const queryClient = useQueryClient();
  return async () => {
    await queryClient.invalidateQueries({ queryKey: CLAVE_ESTRUCTURAS });
    await queryClient.invalidateQueries({ queryKey: CLAVE_ANIOS });
  };
};

/**
 * POST /configuracion/anios-escolares/{anioId}/estructuras-horarias (201). Solo con el año PLANIFICADO. 409 si el
 * código ya existe en el año, el año no es modificable o los planes de evaluación no caben. La respuesta trae las
 * `advertencias` para mostrar al usuario.
 */
export const useCrearEstructura = (
  anioId: string,
  onSuccess?: (guardada: EstructuraGuardada) => void,
) => {
  const refrescar = useRefrescarEstructuras();
  return useMutation({
    mutationFn: (
      data: SolicitudEstructuraHoraria,
    ): Promise<EstructuraGuardada> =>
      api.post(
        `/configuracion/anios-escolares/${anioId}/estructuras-horarias`,
        data,
      ),
    onSuccess: async (guardada) => {
      await refrescar();
      onSuccess?.(guardada);
    },
  });
};

/**
 * PUT /configuracion/estructuras-horarias/{id}: sin `nivel`. Responde con la estructura y las `advertencias`. 409 si se
 * desmarca la principal, el año no es modificable o los planes no caben.
 */
export const useEditarEstructura = (
  onSuccess?: (guardada: EstructuraGuardada) => void,
) => {
  const refrescar = useRefrescarEstructuras();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: SolicitudEdicionEstructuraHoraria;
    }): Promise<EstructuraGuardada> =>
      api.put(`/configuracion/estructuras-horarias/${id}`, data),
    onSuccess: async (guardada) => {
      await refrescar();
      onSuccess?.(guardada);
    },
  });
};

/**
 * DELETE /configuracion/estructuras-horarias/{id} (204). 409 si es la principal y quedan otras del nivel, si es la
 * única del nivel y hay cursos planificados, o si el año no es modificable.
 */
export const useEliminarEstructura = (onSuccess?: () => void) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string): Promise<void> =>
      api.delete(`/configuracion/estructuras-horarias/${id}`),
    onSuccess: async (_, id) => {
      // Se recarga todo menos el detalle de la estructura eliminada: mientras la pantalla lo muestre, recargarlo daría
      // un 404 (y un aviso de error) por una estructura que ya no existe. Tampoco se quita de la caché: quitar una
      // consulta que la pantalla está usando la vuelve a pedir. Al elegir otra estructura, este detalle queda sin uso.
      await queryClient.invalidateQueries({
        queryKey: CLAVE_ESTRUCTURAS,
        predicate: (consulta) =>
          !(consulta.queryKey[2] === 'detalle' && consulta.queryKey[3] === id),
      });
      await queryClient.invalidateQueries({ queryKey: CLAVE_ANIOS });
      onSuccess?.();
    },
  });
};
