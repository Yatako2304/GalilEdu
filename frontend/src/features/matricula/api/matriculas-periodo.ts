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

export type OrdenMatriculas =
  'estudiante' | 'grado' | 'seccion' | 'estado' | 'reciente';

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
    // La sección propuesta depende de las secciones y vacantes del momento (se crean o se llenan en otra
    // pantalla): se pide de nuevo cada vez que se abre, si no seguía mostrando "no hay vacantes" por 60 s.
    gcTime: 0,
    staleTime: 0,
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
    gcTime: 0,
    staleTime: 0,
  });

export type SolicitudNuevaMatricula = {
  estudianteId: string;
  /** Sección elegida en "Cambiar sección"; sin ella se asigna la primera con cupo. */
  seccionId?: string;
  /** Archivo por id de documento requerido (solo los ya subidos). */
  archivos: Record<string, File>;
};

export type SolicitudMatriculaExtemporanea = {
  estudianteId: string;
  /** En una matrícula extemporánea la sección siempre la elige el usuario. */
  seccionId: string;
  motivo: string;
  /** Archivo por id de documento requerido (solo los ya subidos). */
  archivos: Record<string, File>;
};

const agregarArchivos = (datos: FormData, archivos: Record<string, File>) => {
  Object.entries(archivos).forEach(([documentoId, archivo]) =>
    datos.append(documentoId, archivo),
  );
};

const invalidarPeriodo = (
  queryClient: ReturnType<typeof useQueryClient>,
  periodoId: string,
) =>
  queryClient.invalidateQueries({
    queryKey: ['matricula', 'periodo', periodoId],
    predicate: ({ queryKey }) =>
      queryKey[3] !== 'preparacion' && queryKey[3] !== 'secciones',
  });

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
      agregarArchivos(datos, archivos);
      return api.post(`/matricula/periodos/${periodoId}/matriculas`, datos);
    },
    // La preparación y las secciones del alumno ya no aplican (ahora tiene matrícula): recargarlas
    // respondería 409 y mostraría un error justo después del éxito.
    onSuccess: () => invalidarPeriodo(queryClient, periodoId),
  });
};

// POST /matricula/periodos/{id}/matriculas-extemporaneas
// multipart: estudianteId, seccionId, motivo y un archivo por documento.
export const useCrearMatriculaExtemporanea = (periodoId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      estudianteId,
      seccionId,
      motivo,
      archivos,
    }: SolicitudMatriculaExtemporanea): Promise<MatriculaCreada> => {
      const datos = new FormData();
      datos.append('estudianteId', estudianteId);
      datos.append('seccionId', seccionId);
      datos.append('motivo', motivo.trim());
      agregarArchivos(datos, archivos);
      return api.post(
        `/matricula/periodos/${periodoId}/matriculas-extemporaneas`,
        datos,
      );
    },
    onSuccess: () => invalidarPeriodo(queryClient, periodoId),
  });
};
