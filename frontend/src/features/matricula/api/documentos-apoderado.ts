import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { DocumentosApoderado, EstadoMatriculaApoderado } from '@/types/api';

import { descargarPlantilla } from './plantillas-matricula';

// Endpoints de backend/matricula/web/MatriculaApoderadoController.java (HU-28 y HU-29).

const base = (estudianteId: string) =>
  `/matricula/apoderado/estudiantes/${estudianteId}`;

// GET /matricula/apoderado/estudiantes/{id}/documentos
export const useDocumentosApoderado = (estudianteId?: string) =>
  useQuery({
    queryKey: ['matricula', 'apoderado', 'documentos', estudianteId],
    queryFn: (): Promise<DocumentosApoderado> =>
      api.get(`${base(estudianteId ?? '')}/documentos`),
    enabled: Boolean(estudianteId),
  });

// PUT .../documentos/{documentoId} (multipart, campo "archivo"): sube o reemplaza el firmado
export const useSubirDocumentoApoderado = (estudianteId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      documentoId,
      archivo,
    }: {
      documentoId: string;
      archivo: File;
    }): Promise<DocumentosApoderado> => {
      const datos = new FormData();
      datos.append('archivo', archivo);
      return api.put(`${base(estudianteId)}/documentos/${documentoId}`, datos);
    },
    onSuccess: (documentos) =>
      queryClient.setQueryData(
        ['matricula', 'apoderado', 'documentos', estudianteId],
        documentos,
      ),
  });
};

// POST .../enviar-validacion: SECCION_RESERVADA pasa a EN_VALIDACION
export const useEnviarAValidacion = (estudianteId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (): Promise<EstadoMatriculaApoderado> =>
      api.post(`${base(estudianteId)}/enviar-validacion`),
    onSuccess: () =>
      // El estado, los documentos y la lista de hijos (su etiqueta) cambian.
      queryClient.invalidateQueries({ queryKey: ['matricula', 'apoderado'] }),
  });
};

const abrirEnPestana = (archivo: Blob) => {
  const url = URL.createObjectURL(archivo);
  window.open(url, '_blank', 'noopener');
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
};

// GET .../documentos/{documentoId}/archivo → lo abre en una pestaña nueva
export const abrirArchivoDelApoderado = async (
  estudianteId: string,
  documentoId: string,
) =>
  abrirEnPestana(
    await api.get(`${base(estudianteId)}/documentos/${documentoId}/archivo`, {
      responseType: 'blob',
    }),
  );

// GET .../documentos/{documentoId}/plantilla → la descarga con el nombre del documento
export const descargarPlantillaDelApoderado = (
  estudianteId: string,
  documentoId: string,
  nombre: string,
) =>
  descargarPlantilla(
    `${base(estudianteId)}/documentos/${documentoId}/plantilla`,
    nombre,
  );
