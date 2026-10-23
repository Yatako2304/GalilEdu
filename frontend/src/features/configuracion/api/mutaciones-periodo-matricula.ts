import { useMutation, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  DocumentoRequeridoVista,
  FormularioPeriodoMatricula,
  Pagina,
  FilaPeriodoMatricula,
  SolicitudPeriodoMatricula,
} from '@/types/api';

import {
  CLAVE_LISTA_PERIODOS,
  CLAVE_PERIODOS_MATRICULA,
  CLAVE_PERIODO_VIGENTE,
  CLAVE_TODOS_LOS_PERIODOS,
} from './periodos-matricula';
import { quitarPlantilla, subirPlantilla } from './plantillas';

// Figma > Personal Administrativo > pantalla 13 "Periodos de matrícula" (HU-23)

const RUTA = '/configuracion/periodos-matricula';

/**
 * Tras cualquier cambio se recarga primero la lista; recién entonces el resto. Si la lista completa ya no tiene un
 * periodo vigente, se descarta la consulta `vigente` en lugar de recargarla: el backend respondería 404 y saldría
 * como notificación de error.
 */
const useRefrescarPeriodos = () => {
  const queryClient = useQueryClient();
  return async () => {
    await queryClient.invalidateQueries({ queryKey: CLAVE_LISTA_PERIODOS });
    const todos = queryClient.getQueryData<Pagina<FilaPeriodoMatricula>>(
      CLAVE_TODOS_LOS_PERIODOS,
    );
    if (todos && !todos.data.some((fila) => fila.vigente)) {
      queryClient.removeQueries({ queryKey: CLAVE_PERIODO_VIGENTE });
    }
    await queryClient.invalidateQueries({
      queryKey: CLAVE_PERIODOS_MATRICULA,
      predicate: ({ queryKey }) => queryKey[2] !== 'lista',
    });
  };
};

// POST /configuracion/periodos-matricula ("Guardar configuración" de Nuevo periodo de matrícula)
export const useCrearPeriodoMatricula = (
  onSuccess?: (periodo: FormularioPeriodoMatricula) => void,
) => {
  const refrescar = useRefrescarPeriodos();
  return useMutation({
    mutationFn: (
      data: SolicitudPeriodoMatricula,
    ): Promise<FormularioPeriodoMatricula> => api.post(RUTA, data),
    onSuccess: async (periodo) => {
      await refrescar();
      onSuccess?.(periodo);
    },
  });
};

// PUT /configuracion/periodos-matricula/{id} (solo un periodo PROGRAMADO)
export const useEditarPeriodoMatricula = (
  onSuccess?: (periodo: FormularioPeriodoMatricula) => void,
) => {
  const refrescar = useRefrescarPeriodos();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string;
      data: SolicitudPeriodoMatricula;
    }): Promise<FormularioPeriodoMatricula> => api.put(`${RUTA}/${id}`, data),
    onSuccess: async (periodo) => {
      await refrescar();
      onSuccess?.(periodo);
    },
  });
};

// DELETE /configuracion/periodos-matricula/{id} (solo PROGRAMADO y sin matrículas ni entregas en el año)
export const useEliminarPeriodoMatricula = (onSuccess?: () => void) => {
  const refrescar = useRefrescarPeriodos();
  return useMutation({
    mutationFn: (id: string): Promise<void> => api.delete(`${RUTA}/${id}`),
    onSuccess: async () => {
      await refrescar();
      onSuccess?.();
    },
  });
};

// POST .../documentos/{documentoId}/plantilla (reemplaza la plantilla de un documento de un periodo PROGRAMADO)
export const useSubirPlantilla = (
  onSuccess?: (documento: DocumentoRequeridoVista) => void,
) => {
  const refrescar = useRefrescarPeriodos();
  return useMutation({
    mutationFn: ({
      periodoId,
      documentoId,
      archivo,
    }: {
      periodoId: string;
      documentoId: string;
      archivo: File;
    }) => subirPlantilla(periodoId, documentoId, archivo),
    onSuccess: async (documento) => {
      await refrescar();
      onSuccess?.(documento);
    },
  });
};

// DELETE .../documentos/{documentoId}/plantilla
export const useQuitarPlantilla = (
  onSuccess?: (documento: DocumentoRequeridoVista) => void,
) => {
  const refrescar = useRefrescarPeriodos();
  return useMutation({
    mutationFn: ({
      periodoId,
      documentoId,
    }: {
      periodoId: string;
      documentoId: string;
    }) => quitarPlantilla(periodoId, documentoId),
    onSuccess: async (documento) => {
      await refrescar();
      onSuccess?.(documento);
    },
  });
};

/** Para quien sube varias plantillas después de guardar (`subirPlantillas`): refresca una sola vez al terminar. */
export const useRefrescarPeriodosMatricula = useRefrescarPeriodos;
