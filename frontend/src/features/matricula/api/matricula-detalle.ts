import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { DetalleMatricula, SeccionesDelGrado } from '@/types/api';

// Endpoints de backend/matricula/web/MatriculaDetalleController.java (HU-24 · A5, B6, B7).

// GET /matricula/matriculas/{id}
export const useDetalleMatricula = (matriculaId?: string) =>
  useQuery({
    queryKey: ['matricula', 'detalle', matriculaId],
    queryFn: (): Promise<DetalleMatricula> =>
      api.get(`/matricula/matriculas/${matriculaId}`),
    enabled: Boolean(matriculaId),
  });

// GET /matricula/matriculas/{id}/secciones · secciones del grado con su ocupación
export const useSeccionesDeMatricula = (
  matriculaId: string,
  habilitado: boolean,
) =>
  useQuery({
    queryKey: ['matricula', 'detalle', matriculaId, 'secciones'],
    queryFn: (): Promise<SeccionesDelGrado> =>
      api.get(`/matricula/matriculas/${matriculaId}/secciones`),
    enabled: habilitado,
    // Las vacantes cambian: se piden de nuevo cada vez que se abre el modal.
    gcTime: 0,
  });

const useRefrescarDetalle = (matriculaId: string) => {
  const queryClient = useQueryClient();
  return (detalle: DetalleMatricula) => {
    queryClient.setQueryData(['matricula', 'detalle', matriculaId], detalle);
    // El listado y los indicadores del periodo también cambian.
    return queryClient.invalidateQueries({
      queryKey: ['matricula', 'periodo'],
    });
  };
};

// PUT /matricula/matriculas/{id}/seccion
export const useCambiarSeccion = (matriculaId: string) => {
  const refrescar = useRefrescarDetalle(matriculaId);
  return useMutation({
    mutationFn: (seccionId: string): Promise<DetalleMatricula> =>
      api.put(`/matricula/matriculas/${matriculaId}/seccion`, { seccionId }),
    onSuccess: refrescar,
  });
};

// PUT /matricula/matriculas/{id}/documentos/{documentoId} (multipart, campo "archivo")
export const useSubirDocumento = (matriculaId: string) => {
  const refrescar = useRefrescarDetalle(matriculaId);
  return useMutation({
    mutationFn: ({
      documentoId,
      archivo,
    }: {
      documentoId: string;
      archivo: File;
    }): Promise<DetalleMatricula> => {
      const datos = new FormData();
      datos.append('archivo', archivo);
      return api.put(
        `/matricula/matriculas/${matriculaId}/documentos/${documentoId}`,
        datos,
      );
    },
    onSuccess: refrescar,
  });
};

// GET /matricula/matriculas/{id}/documentos/{documentoId}/archivo → lo abre en una pestaña nueva
export const abrirDocumentoDeMatricula = async (
  matriculaId: string,
  documentoId: string,
) => {
  const archivo: Blob = await api.get(
    `/matricula/matriculas/${matriculaId}/documentos/${documentoId}/archivo`,
    { responseType: 'blob' },
  );
  const url = URL.createObjectURL(archivo);
  window.open(url, '_blank', 'noopener');
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
};

// PUT /matricula/matriculas/{id}/documentos/{documentoId}/observacion · el colegio observa un documento
export const useObservarDocumento = (matriculaId: string) => {
  const refrescar = useRefrescarDetalle(matriculaId);
  return useMutation({
    mutationFn: ({
      documentoId,
      observacion,
    }: {
      documentoId: string;
      observacion: string;
    }): Promise<DetalleMatricula> =>
      api.put(
        `/matricula/matriculas/${matriculaId}/documentos/${documentoId}/observacion`,
        { observacion },
      ),
    onSuccess: refrescar,
  });
};

// POST /matricula/matriculas/{id}/validar · "¿Aprobar documentación?"
export const useValidarMatricula = (matriculaId: string) => {
  const refrescar = useRefrescarDetalle(matriculaId);
  return useMutation({
    mutationFn: (): Promise<DetalleMatricula> =>
      api.post(`/matricula/matriculas/${matriculaId}/validar`),
    onSuccess: refrescar,
  });
};
