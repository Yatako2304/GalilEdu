import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  DocumentoAdjunto,
  EdicionEstudiante,
  EstadoEstudiante,
  EstudianteResumen,
  FichaEstudiante,
  InscripcionRegistrada,
  Pagina,
  PersonaEncontrada,
  SolicitudInscripcion,
  VerificacionDocumento,
} from '@/types/api';

// Endpoints de backend/estudiantes/web/EstudianteController.java (HU-7).

export type FiltrosEstudiantes = {
  q?: string;
  estado?: EstadoEstudiante | '';
  page: number;
};

// GET /estudiantes?q=&estado=&page=
export const useEstudiantes = (filtros: FiltrosEstudiantes) =>
  useQuery({
    queryKey: ['estudiantes', 'lista', filtros],
    queryFn: (): Promise<Pagina<EstudianteResumen>> => {
      const params = Object.fromEntries(
        Object.entries({ size: 8, ...filtros }).filter(
          ([, v]) => v !== '' && v != null,
        ),
      );
      return api.get('/estudiantes', { params });
    },
    placeholderData: keepPreviousData,
  });

// GET /estudiantes/{id}
export const useFichaEstudiante = (id: string) =>
  useQuery({
    queryKey: ['estudiantes', 'ficha', id],
    queryFn: (): Promise<FichaEstudiante> => api.get(`/estudiantes/${id}`),
  });

export type ResultadoInscripcion = InscripcionRegistrada & {
  /** true si la inscripción se guardó pero el documento del apoderado no se pudo subir. */
  documentoFallido: boolean;
};

// POST /estudiantes/inscripciones (o PUT /estudiantes/{id} al editar) y, si hay, POST /estudiantes/{id}/apoderado/documento
export const useInscribir = (onSuccess: (r: ResultadoInscripcion) => void) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({
      solicitud,
      documento,
      estudianteId,
    }: {
      solicitud: SolicitudInscripcion;
      documento: { tipo: string; archivo: File } | null;
      /** Si viene, es "Editar": se reemplaza la inscripción de ese estudiante. */
      estudianteId?: string;
    }): Promise<ResultadoInscripcion> => {
      const r: InscripcionRegistrada = estudianteId
        ? await api.put(`/estudiantes/${estudianteId}`, solicitud)
        : await api.post('/estudiantes/inscripciones', solicitud);
      if (!documento) return { ...r, documentoFallido: false };
      try {
        await subirDocumentoApoderado(r.id, documento.tipo, documento.archivo);
        return { ...r, documentoFallido: false };
      } catch {
        // La inscripción ya quedó guardada: no se pierde por un fallo del archivo.
        return { ...r, documentoFallido: true };
      }
    },
    onSuccess: (r) => {
      queryClient.invalidateQueries({ queryKey: ['estudiantes'] });
      queryClient.invalidateQueries({ queryKey: ['usuarios'] });
      onSuccess(r);
    },
  });
};

/**
 * GET /estudiantes/documentos?documento= → paso 1 de la inscripción: ¿se puede inscribir un estudiante con
 * este documento? Libre y eliminado sí (el eliminado se actualiza al registrar); activo, retirado u otra
 * persona, no.
 */
export const verificarDocumentoEstudiante = (
  documento: string,
): Promise<VerificacionDocumento> =>
  api.get('/estudiantes/documentos', { params: { documento } });

/** GET /estudiantes/correos?correo= → si el correo no está asignado a ninguna persona (paso 3). */
export const correoDisponible = async (correo: string): Promise<boolean> => {
  const r: { disponible: boolean } = await api.get('/estudiantes/correos', {
    params: { correo },
  });
  return r.disponible;
};

// GET /estudiantes/{id}/edicion: la inscripción para precargar "Editar".
export const useEdicionEstudiante = (id: string) =>
  useQuery({
    queryKey: ['estudiantes', 'edicion', id],
    queryFn: (): Promise<EdicionEstudiante> =>
      api.get(`/estudiantes/${id}/edicion`),
  });

export type AccionEstudiante = 'retirar' | 'reactivar' | 'eliminar';

// POST /estudiantes/{id}/retirar ("Inactivar") · POST /estudiantes/{id}/reactivar · DELETE /estudiantes/{id}
export const useCambiarEstadoEstudiante = (onSuccess?: () => void) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, accion }: { id: string; accion: AccionEstudiante }) =>
      accion === 'eliminar'
        ? api.delete(`/estudiantes/${id}`)
        : api.post(`/estudiantes/${id}/${accion}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['estudiantes'] });
      onSuccess?.();
    },
  });
};

// POST /estudiantes/{id}/apoderado/documento (multipart)
export const subirDocumentoApoderado = (
  estudianteId: string,
  tipo: string,
  archivo: File,
): Promise<DocumentoAdjunto> => {
  const datos = new FormData();
  datos.append('tipo', tipo);
  datos.append('archivo', archivo);
  return api.post(`/estudiantes/${estudianteId}/apoderado/documento`, datos);
};

// GET /estudiantes/{id}/apoderado/documento → lo abre en una pestaña nueva
export const abrirDocumentoApoderado = async (estudianteId: string) => {
  const archivo: Blob = await api.get(
    `/estudiantes/${estudianteId}/apoderado/documento`,
    { responseType: 'blob' },
  );
  const url = URL.createObjectURL(archivo);
  window.open(url, '_blank', 'noopener');
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
};

/**
 * GET /estudiantes/personas?documento= → la persona si ya existe (ej. por un hermano), o null.
 * El 404 no es un error aquí: significa "persona nueva", por eso no se muestra notificación.
 */
export const buscarPersonaPorDocumento = async (
  documento: string,
): Promise<PersonaEncontrada | null> => {
  const respuesta: PersonaEncontrada | { message: string } = await api.get(
    '/estudiantes/personas',
    { params: { documento }, validateStatus: (s) => s === 200 || s === 404 },
  );
  return 'personaId' in respuesta ? respuesta : null;
};
