import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  EstudianteSinMatricula,
  FilaMatriculaPeriodo,
  MatriculaCreada,
  Pagina,
  PeriodoMatriculas,
  PreparacionMatricula,
  SeccionesDelGrado,
} from '@/types/api';

// Endpoints de backend/matricula/web/MatriculaAdministrativoController.java (HU-24).

export type OrdenMatriculas = 'estudiante' | 'grado' | 'seccion' | 'estado';

export type FiltrosMatriculasPeriodo = {
  texto?: string;
  grado?: string;
  estado?: string;
  orden?: OrdenMatriculas;
  direccion?: 'asc' | 'desc';
  page: number;
  size?: number;
};

// GET /matricula/periodos/{id} · aviso del periodo e indicadores
export const usePeriodoMatriculas = (periodoId: string) =>
  useQuery({
    queryKey: ['matricula', 'periodo', periodoId],
    queryFn: (): Promise<PeriodoMatriculas> =>
      api.get(`/matricula/periodos/${periodoId}`),
  });

// GET /matricula/periodos/{id}/matriculas?texto=&grado=&estado=&orden=&direccion=&page=&size=
export const useMatriculasDelPeriodo = (
  periodoId: string,
  filtros: FiltrosMatriculasPeriodo,
) =>
  useQuery({
    queryKey: ['matricula', 'periodo', periodoId, 'matriculas', filtros],
    queryFn: (): Promise<Pagina<FilaMatriculaPeriodo>> => {
      const params = Object.fromEntries(
        Object.entries({ size: 10, ...filtros }).filter(
          ([, v]) => v !== '' && v != null,
        ),
      );
      return api.get(`/matricula/periodos/${periodoId}/matriculas`, {
        params,
      });
    },
    placeholderData: keepPreviousData,
  });

// GET /matricula/periodos/{id}/estudiantes?texto= · buscador de "Nueva matrícula"
export const useBuscarEstudiantes = (periodoId: string, texto: string) =>
  useQuery({
    queryKey: ['matricula', 'periodo', periodoId, 'estudiantes', texto],
    queryFn: (): Promise<EstudianteSinMatricula[]> =>
      api.get(`/matricula/periodos/${periodoId}/estudiantes`, {
        params: texto ? { texto } : {},
      }),
    placeholderData: keepPreviousData,
  });

// GET /matricula/periodos/{id}/estudiantes/{estudianteId}/preparacion
export const usePreparacionMatricula = (
  periodoId: string,
  estudianteId?: string,
) =>
  useQuery({
    queryKey: ['matricula', 'periodo', periodoId, 'preparacion', estudianteId],
    queryFn: (): Promise<PreparacionMatricula> =>
      api.get(
        `/matricula/periodos/${periodoId}/estudiantes/${estudianteId}/preparacion`,
      ),
    enabled: Boolean(estudianteId),
  });

// GET /matricula/periodos/{id}/estudiantes/{estudianteId}/secciones · para "Cambiar sección"
export const useSeccionesDelAlumno = (
  periodoId: string,
  estudianteId: string | undefined,
  habilitado: boolean,
) =>
  useQuery({
    queryKey: ['matricula', 'periodo', periodoId, 'secciones', estudianteId],
    queryFn: (): Promise<SeccionesDelGrado> =>
      api.get(
        `/matricula/periodos/${periodoId}/estudiantes/${estudianteId}/secciones`,
      ),
    enabled: Boolean(estudianteId) && habilitado,
  });

export type SolicitudNuevaMatricula = {
  estudianteId: string;
  /** Sección elegida en "Cambiar sección"; sin ella se asigna la primera con cupo. */
  seccionId?: string;
  /** Archivo por id de documento requerido (solo los ya subidos). */
  archivos: Record<string, File>;
};

// POST /matricula/periodos/{id}/matriculas (multipart: estudianteId + un archivo por documento)
export const useCrearMatricula = (periodoId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      estudianteId,
      seccionId,
      archivos,
    }: SolicitudNuevaMatricula): Promise<MatriculaCreada> => {
      const datos = new FormData();
      datos.append('estudianteId', estudianteId);
      if (seccionId) datos.append('seccionId', seccionId);
      Object.entries(archivos).forEach(([documentoId, archivo]) =>
        datos.append(documentoId, archivo),
      );
      return api.post(`/matricula/periodos/${periodoId}/matriculas`, datos);
    },
    // La preparación y las secciones del alumno ya no aplican (ahora tiene matrícula): recargarlas
    // respondería 409 y mostraría un error justo después del éxito.
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ['matricula', 'periodo', periodoId],
        predicate: ({ queryKey }) =>
          queryKey[3] !== 'preparacion' && queryKey[3] !== 'secciones',
      }),
  });
};
