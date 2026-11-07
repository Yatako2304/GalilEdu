import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  CompetenciaOpcion,
  CursoDocente,
  EvidenciaCalificacion,
  FilaCalificacion,
  ItemEvaluacion,
  ReporteCalificaciones,
  SolicitudItemEvaluacion,
  TipoReporteCalificaciones,
} from '@/types/api';

export type SeleccionCurso = {
  planCursoId: string;
  seccionId: string;
};

export type SeleccionPeriodo = SeleccionCurso & { periodoId: string };

const base = '/calificaciones/docente';

export const useCursosDocente = () =>
  useQuery(
    queryOptions({
      queryKey: ['calificaciones-docente', 'cursos'],
      queryFn: (): Promise<CursoDocente[]> => api.get(`${base}/cursos`),
    }),
  );

export const useCompetenciasCurso = (seleccion: SeleccionCurso | null) =>
  useQuery(
    queryOptions({
      queryKey: ['calificaciones-docente', 'competencias', seleccion],
      queryFn: (): Promise<CompetenciaOpcion[]> =>
        api.get(
          `${base}/cursos/${seleccion?.planCursoId}/secciones/${seleccion?.seccionId}/competencias`,
        ),
      enabled: !!seleccion,
    }),
  );

export const useItemsEvaluacion = (seleccion: SeleccionPeriodo | null) =>
  useQuery(
    queryOptions({
      queryKey: ['calificaciones-docente', 'items', seleccion],
      queryFn: (): Promise<ItemEvaluacion[]> =>
        api.get(
          `${base}/cursos/${seleccion?.planCursoId}/secciones/${seleccion?.seccionId}/periodos/${seleccion?.periodoId}/items`,
        ),
      enabled: !!seleccion,
    }),
  );

const useMutacion = <V, R>(mutationFn: (variables: V) => Promise<R>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['calificaciones-docente'] });
    },
  });
};

export const useCrearItem = () =>
  useMutacion(
    ({
      seleccion,
      datos,
    }: {
      seleccion: SeleccionPeriodo;
      datos: SolicitudItemEvaluacion;
    }): Promise<ItemEvaluacion> =>
      api.post(
        `${base}/cursos/${seleccion.planCursoId}/secciones/${seleccion.seccionId}/periodos/${seleccion.periodoId}/items`,
        datos,
      ),
  );

export const useEditarItem = () =>
  useMutacion(
    ({
      id,
      datos,
    }: {
      id: string;
      datos: SolicitudItemEvaluacion;
    }): Promise<ItemEvaluacion> => api.put(`${base}/items/${id}`, datos),
  );

export const useEliminarItem = () =>
  useMutacion((id: string): Promise<void> => api.delete(`${base}/items/${id}`));

export const useCalificacionesItem = (itemId: string | null) =>
  useQuery(
    queryOptions({
      queryKey: ['calificaciones-docente', 'calificaciones', itemId],
      queryFn: (): Promise<FilaCalificacion[]> =>
        api.get(`${base}/items/${itemId}/calificaciones`),
      enabled: !!itemId,
    }),
  );

export const useGuardarCalificaciones = () =>
  useMutacion(
    ({
      itemId,
      calificaciones,
    }: {
      itemId: string;
      calificaciones: {
        matriculaId: string;
        notaNumerica: number | null;
        nivelLogro: string | null;
      }[];
    }): Promise<FilaCalificacion[]> =>
      api.put(`${base}/items/${itemId}/calificaciones`, { calificaciones }),
  );

export const useGuardarEvidencia = () =>
  useMutacion(
    ({
      itemId,
      matriculaId,
      archivo,
    }: {
      itemId: string;
      matriculaId: string;
      archivo: File;
    }): Promise<EvidenciaCalificacion> => {
      const datos = new FormData();
      datos.append('archivo', archivo);
      return api.post(
        `${base}/items/${itemId}/calificaciones/${matriculaId}/evidencia`,
        datos,
      );
    },
  );

export const useEliminarEvidencia = () =>
  useMutacion(
    ({
      itemId,
      matriculaId,
    }: {
      itemId: string;
      matriculaId: string;
    }): Promise<void> =>
      api.delete(
        `${base}/items/${itemId}/calificaciones/${matriculaId}/evidencia`,
      ),
  );

export type FiltrosReporte = SeleccionPeriodo & {
  tipo: TipoReporteCalificaciones;
  objetivoId?: string;
  competenciaIds?: string;
  itemIds?: string;
  incluirPromedio?: boolean;
};

export const useReporteCalificaciones = (filtros: FiltrosReporte | null) =>
  useQuery(
    queryOptions({
      queryKey: ['calificaciones-docente', 'reporte', filtros],
      queryFn: (): Promise<ReporteCalificaciones> =>
        api.get(`${base}/reportes`, { params: filtros }),
      enabled:
        !!filtros &&
        (filtros.tipo === 'PERIODO' ||
          Boolean(filtros.objetivoId || filtros.competenciaIds)),
    }),
  );

export const descargarEvidencia = async (
  itemId: string,
  matriculaId: string,
  nombre: string,
) => {
  const contenido = (await api.get(
    `${base}/items/${itemId}/calificaciones/${matriculaId}/evidencia`,
    { responseType: 'blob' },
  )) as Blob;
  descargarBlob(contenido, nombre);
};

export const exportarReporte = async (filtros: FiltrosReporte) => {
  const contenido = (await api.get(`${base}/reportes/exportar-pdf`, {
    params: filtros,
    responseType: 'blob',
  })) as Blob;
  descargarBlob(contenido, 'consolidado-notas.pdf');
};

const descargarBlob = (contenido: Blob, nombre: string) => {
  const url = URL.createObjectURL(contenido);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  enlace.click();
  URL.revokeObjectURL(url);
};
