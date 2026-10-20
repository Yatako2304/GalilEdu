import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  AnioEscolarOpcion,
  CursoDisponible,
  DocenteOpcion,
  DocenteSeccion,
  OrigenPlan,
  PlanCursoDetalle,
  PlanGrado,
  SolicitudConfigurarCurso,
  SolicitudCopiaPlan,
  SolicitudPlanCurso,
} from '@/types/api';

// Figma > Personal Administrativo > CI-17 y CI-20 · Plan de evaluación (HU-56)

export type GradoAnio = { anioEscolarId: string; grado: string };

export const useAniosEscolares = () =>
  useQuery(
    queryOptions({
      queryKey: ['plan-evaluacion', 'anios'],
      queryFn: (): Promise<AnioEscolarOpcion[]> =>
        api.get('/planes-evaluacion/anios'),
    }),
  );

export const useDocentes = () =>
  useQuery(
    queryOptions({
      queryKey: ['plan-evaluacion', 'docentes'],
      queryFn: (): Promise<DocenteOpcion[]> =>
        api.get('/planes-evaluacion/docentes'),
    }),
  );

// GET /planes-evaluacion?anioEscolarId=&grado=
export const usePlanGrado = (filtro: GradoAnio | null) =>
  useQuery(
    queryOptions({
      queryKey: ['plan-evaluacion', 'plan', filtro],
      queryFn: (): Promise<PlanGrado> =>
        api.get('/planes-evaluacion', { params: filtro }),
      enabled: !!filtro,
    }),
  );

export const usePlanCurso = (id: string | null) =>
  useQuery(
    queryOptions({
      queryKey: ['plan-evaluacion', 'curso', id],
      queryFn: (): Promise<PlanCursoDetalle> =>
        api.get(`/planes-evaluacion/${id}`),
      enabled: !!id,
    }),
  );

export const useCursosDisponibles = (filtro: GradoAnio, habilitado: boolean) =>
  useQuery(
    queryOptions({
      queryKey: ['plan-evaluacion', 'disponibles', filtro],
      queryFn: (): Promise<CursoDisponible[]> =>
        api.get('/planes-evaluacion/cursos-disponibles', { params: filtro }),
      enabled: habilitado,
    }),
  );

export const useOrigenesPlan = (filtro: GradoAnio, habilitado: boolean) =>
  useQuery(
    queryOptions({
      queryKey: ['plan-evaluacion', 'origenes', filtro],
      queryFn: (): Promise<OrigenPlan[]> =>
        api.get('/planes-evaluacion/origenes', { params: filtro }),
      enabled: habilitado,
    }),
  );

export const useDocentesDelCurso = (id: string | null) =>
  useQuery(
    queryOptions({
      queryKey: ['plan-evaluacion', 'docentes-curso', id],
      queryFn: (): Promise<DocenteSeccion[]> =>
        api.get(`/planes-evaluacion/${id}/docentes`),
      enabled: !!id,
    }),
  );

/** Tras cualquier cambio se recargan el plan del grado y los modales abiertos. */
const useMutacion = <V, R>(
  mutationFn: (variables: V) => Promise<R>,
  onSuccess?: () => void,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['plan-evaluacion'] });
      onSuccess?.();
    },
  });
};

// POST /planes-evaluacion (modal "Agregar curso")
export const useAgregarCursoPlan = (onSuccess?: () => void) =>
  useMutacion(
    (data: SolicitudPlanCurso): Promise<PlanCursoDetalle> =>
      api.post('/planes-evaluacion', data),
    onSuccess,
  );

// PUT /planes-evaluacion/{id} (modal "<curso> · <grado>")
export const useConfigurarCursoPlan = (onSuccess?: () => void) =>
  useMutacion(
    ({
      id,
      data,
    }: {
      id: string;
      data: SolicitudConfigurarCurso;
    }): Promise<PlanCursoDetalle> => api.put(`/planes-evaluacion/${id}`, data),
    onSuccess,
  );

// DELETE /planes-evaluacion/{id}
export const useQuitarCursoPlan = (onSuccess?: () => void) =>
  useMutacion(
    (id: string): Promise<void> => api.delete(`/planes-evaluacion/${id}`),
    onSuccess,
  );

// PUT /planes-evaluacion/{id}/docentes (modal "Docentes de <curso>")
export const useAsignarDocentes = (onSuccess?: () => void) =>
  useMutacion(
    ({
      id,
      asignaciones,
    }: {
      id: string;
      asignaciones: { seccionId: string; docenteId: string | null }[];
    }): Promise<DocenteSeccion[]> =>
      api.put(`/planes-evaluacion/${id}/docentes`, { asignaciones }),
    onSuccess,
  );

// POST /planes-evaluacion/copias (modal "Copiar plan")
export const useCopiarPlan = (onSuccess?: () => void) =>
  useMutacion(
    (data: SolicitudCopiaPlan): Promise<PlanGrado> =>
      api.post('/planes-evaluacion/copias', data),
    onSuccess,
  );
