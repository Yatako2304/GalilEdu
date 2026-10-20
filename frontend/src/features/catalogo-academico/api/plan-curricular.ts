import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  AreaDetalle,
  AreaResumen,
  CompetenciaResumen,
  CursoResumen,
  ResumenPlanCurricular,
  SolicitudArea,
  SolicitudCompetencia,
  SolicitudCurso,
} from '@/types/api';

// Figma > Personal Administrativo > CI-14 · Plan curricular (HU-52, HU-53, HU-54)

// GET /areas/resumen
export const getResumenPlanCurricular = (): Promise<ResumenPlanCurricular> =>
  api.get('/areas/resumen');

// GET /areas
export const getAreas = (): Promise<AreaResumen[]> => api.get('/areas');

// GET /areas/{id}
export const getArea = (id: string): Promise<AreaDetalle> =>
  api.get(`/areas/${id}`);

export const useResumenPlanCurricular = () =>
  useQuery(
    queryOptions({
      queryKey: ['plan-curricular', 'resumen'],
      queryFn: getResumenPlanCurricular,
    }),
  );

export const useAreas = () =>
  useQuery(
    queryOptions({
      queryKey: ['plan-curricular', 'areas'],
      queryFn: getAreas,
    }),
  );

export const useArea = (id: string | null) =>
  useQuery(
    queryOptions({
      queryKey: ['plan-curricular', 'area', id],
      queryFn: () => getArea(id as string),
      enabled: !!id,
    }),
  );

/** Tras cualquier cambio se recargan el resumen, la lista y el detalle del área. */
const useRefrescarPlanCurricular = () => {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ['plan-curricular'] });
};

const useMutacion = <V, R>(
  mutationFn: (variables: V) => Promise<R>,
  onSuccess?: (resultado: R) => void,
) => {
  const refrescar = useRefrescarPlanCurricular();
  return useMutation({
    mutationFn,
    onSuccess: (resultado: R) => {
      refrescar();
      onSuccess?.(resultado);
    },
  });
};

// POST /areas · PUT /areas/{id} (HU-52)
export const useGuardarArea = (onSuccess?: (area: AreaDetalle) => void) =>
  useMutacion(
    ({
      id,
      data,
    }: {
      id?: string;
      data: SolicitudArea;
    }): Promise<AreaDetalle> =>
      id ? api.put(`/areas/${id}`, data) : api.post('/areas', data),
    onSuccess,
  );

// POST /areas/{id}/competencias · PUT /competencias/{id} (HU-53)
export const useGuardarCompetencia = (onSuccess?: () => void) =>
  useMutacion(
    ({
      areaId,
      id,
      data,
    }: {
      areaId: string;
      id?: string;
      data: SolicitudCompetencia;
    }): Promise<CompetenciaResumen> =>
      id
        ? api.put(`/competencias/${id}`, data)
        : api.post(`/areas/${areaId}/competencias`, data),
    onSuccess,
  );

// POST /areas/{id}/cursos · PUT /cursos/{id} (HU-54)
export const useGuardarCurso = (onSuccess?: () => void) =>
  useMutacion(
    ({
      areaId,
      id,
      data,
    }: {
      areaId: string;
      id?: string;
      data: SolicitudCurso;
    }): Promise<CursoResumen> =>
      id
        ? api.put(`/cursos/${id}`, data)
        : api.post(`/areas/${areaId}/cursos`, data),
    onSuccess,
  );

export type RecursoCatalogo = 'areas' | 'competencias' | 'cursos';
export type AccionEstado = 'desactivar' | 'reactivar';

// POST /{areas|competencias|cursos}/{id}/{desactivar|reactivar}
export const useCambiarEstado = (onSuccess?: () => void) =>
  useMutacion(
    ({
      recurso,
      id,
      accion,
    }: {
      recurso: RecursoCatalogo;
      id: string;
      accion: AccionEstado;
    }): Promise<unknown> => api.post(`/${recurso}/${id}/${accion}`),
    onSuccess,
  );
