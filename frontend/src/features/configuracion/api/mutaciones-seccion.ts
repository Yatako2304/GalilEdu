import { useMutation, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  SeccionResumen,
  SolicitudCapacidad,
  SolicitudSeccion,
  SolicitudTutores,
} from '@/types/api';

import { CLAVE_ANIOS } from './anios-escolares';
import { CLAVE_SECCIONES } from './secciones';

/**
 * Tras cualquier cambio se recargan las secciones y también los datos del año: la cabecera muestra "Secciones (n)"
 * y los requisitos de CI-06 ("Secciones" y "Tutores asignados") dependen de ellas.
 */
const useRefrescarSecciones = () => {
  const queryClient = useQueryClient();
  return async () => {
    await queryClient.invalidateQueries({ queryKey: CLAVE_SECCIONES });
    await queryClient.invalidateQueries({ queryKey: CLAVE_ANIOS });
  };
};

// POST /configuracion/anios-escolares/{anioId}/secciones (modal Nueva sección). 409 si el nombre ya existe en el grado.
export const useCrearSeccion = (
  anioId: string,
  onSuccess?: (seccion: SeccionResumen) => void,
) => {
  const refrescar = useRefrescarSecciones();
  return useMutation({
    mutationFn: (data: SolicitudSeccion): Promise<SeccionResumen> =>
      api.post(`/configuracion/anios-escolares/${anioId}/secciones`, data),
    onSuccess: async (seccion) => {
      await refrescar();
      onSuccess?.(seccion);
    },
  });
};

// PUT /configuracion/secciones/{id}/tutores (modal Asignar tutor). 409 si el tutor ya lo es de otra sección activa.
export const useAsignarTutores = (
  onSuccess?: (seccion: SeccionResumen) => void,
) => {
  const refrescar = useRefrescarSecciones();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: SolicitudTutores;
    }): Promise<SeccionResumen> =>
      api.put(`/configuracion/secciones/${id}/tutores`, data),
    onSuccess: async (seccion) => {
      await refrescar();
      onSuccess?.(seccion);
    },
  });
};

// PUT /configuracion/secciones/{id} (modal Editar sección): solo cambia la capacidad. 400 si es menor que los alumnos
// que ocupan vacante.
export const useCambiarCapacidad = (
  onSuccess?: (seccion: SeccionResumen) => void,
) => {
  const refrescar = useRefrescarSecciones();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: SolicitudCapacidad;
    }): Promise<SeccionResumen> =>
      api.put(`/configuracion/secciones/${id}`, data),
    onSuccess: async (seccion) => {
      await refrescar();
      onSuccess?.(seccion);
    },
  });
};

// DELETE /configuracion/secciones/{id}: 204. 409 si la sección tuvo matrículas o cambios de sección.
export const useEliminarSeccion = (onSuccess?: () => void) => {
  const refrescar = useRefrescarSecciones();
  return useMutation({
    mutationFn: (id: string): Promise<void> =>
      api.delete(`/configuracion/secciones/${id}`),
    onSuccess: async () => {
      await refrescar();
      onSuccess?.();
    },
  });
};
