import {
  keepPreviousData,
  queryOptions,
  useQuery,
} from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { QueryConfig } from '@/lib/react-query';
import {
  AnioDisponibleMatricula,
  EstadoVisiblePeriodoMatricula,
  FilaPeriodoMatricula,
  FormularioPeriodoMatricula,
  Pagina,
  PeriodoMatriculaVigente,
} from '@/types/api';

// Figma > Personal Administrativo > pantalla 13 "Periodos de matrícula" (HU-23)

const RUTA = '/configuracion/periodos-matricula';

/** Todas las consultas de periodos de matrícula cuelgan de esta clave; las mutaciones la invalidan. */
export const CLAVE_PERIODOS_MATRICULA = [
  'configuracion',
  'periodos-matricula',
] as const;
export const CLAVE_LISTA_PERIODOS = [
  ...CLAVE_PERIODOS_MATRICULA,
  'lista',
] as const;
export const CLAVE_TODOS_LOS_PERIODOS = [
  ...CLAVE_LISTA_PERIODOS,
  'todos',
] as const;
export const CLAVE_PERIODO_VIGENTE = [
  ...CLAVE_PERIODOS_MATRICULA,
  'vigente',
] as const;

export type FiltrosPeriodosMatricula = {
  anio?: number | '';
  estado?: EstadoVisiblePeriodoMatricula | '';
  page: number;
  size?: number;
};

const TAMANO_PAGINA = 8;
/** Tope de `size` del backend: alcanza para todos los periodos (hay uno por año). */
const TAMANO_TODOS = 100;

const sinVacios = (filtros: Record<string, unknown>) =>
  Object.fromEntries(
    Object.entries(filtros).filter(([, v]) => v !== '' && v != null),
  );

// GET /configuracion/periodos-matricula?anio=&estado=&page=&size= (del más reciente al más antiguo)
export const getPeriodosMatricula = (
  filtros: FiltrosPeriodosMatricula,
): Promise<Pagina<FilaPeriodoMatricula>> =>
  api.get(RUTA, { params: sinVacios({ size: TAMANO_PAGINA, ...filtros }) });

export const getPeriodosMatriculaQueryOptions = (
  filtros: FiltrosPeriodosMatricula,
) =>
  queryOptions({
    queryKey: [...CLAVE_LISTA_PERIODOS, filtros],
    queryFn: () => getPeriodosMatricula(filtros),
    placeholderData: keepPreviousData,
  });

export const usePeriodosMatricula = ({
  filtros,
  queryConfig,
}: {
  filtros: FiltrosPeriodosMatricula;
  queryConfig?: QueryConfig<typeof getPeriodosMatriculaQueryOptions>;
}) =>
  useQuery({ ...getPeriodosMatriculaQueryOptions(filtros), ...queryConfig });

/**
 * Todos los periodos, sin filtros. Sirve para dos cosas que el listado paginado no puede dar: saber si hay un
 * periodo vigente (alguna fila con `vigente = true`, para no pedir /vigente en vano) y las opciones del filtro
 * "Año escolar".
 */
export const usePeriodosMatriculaTodos = () =>
  useQuery(
    queryOptions({
      queryKey: CLAVE_TODOS_LOS_PERIODOS,
      queryFn: () => getPeriodosMatricula({ page: 1, size: TAMANO_TODOS }),
    }),
  );

/**
 * GET /configuracion/periodos-matricula/vigente: responde 404 si no hay periodo vigente, y ese error saldría como
 * notificación. Por eso solo se consulta con `habilitado` (una fila de la lista completa trae `vigente = true`).
 */
export const usePeriodoMatriculaVigente = (habilitado: boolean) =>
  useQuery(
    queryOptions({
      queryKey: CLAVE_PERIODO_VIGENTE,
      queryFn: (): Promise<PeriodoMatriculaVigente> =>
        api.get(`${RUTA}/vigente`),
      enabled: habilitado,
    }),
  );

// GET /configuracion/periodos-matricula/anios-disponibles (selector "Año académico")
export const useAniosDisponiblesMatricula = (habilitado = true) =>
  useQuery(
    queryOptions({
      enabled: habilitado,
      queryKey: [...CLAVE_PERIODOS_MATRICULA, 'anios-disponibles'],
      queryFn: (): Promise<AnioDisponibleMatricula[]> =>
        api.get(`${RUTA}/anios-disponibles`),
    }),
  );

// GET /configuracion/periodos-matricula/nuevo?anioEscolarId= (formulario vacío del año elegido)
export const useFormularioNuevoPeriodo = (anioEscolarId: string | undefined) =>
  useQuery(
    queryOptions({
      queryKey: [...CLAVE_PERIODOS_MATRICULA, 'nuevo', anioEscolarId],
      queryFn: (): Promise<FormularioPeriodoMatricula> =>
        api.get(`${RUTA}/nuevo`, { params: { anioEscolarId } }),
      enabled: !!anioEscolarId,
    }),
  );

// GET /configuracion/periodos-matricula/{id} (editar o ver; abierto o cerrado es de solo lectura)
export const usePeriodoMatricula = (id: string | undefined) =>
  useQuery(
    queryOptions({
      queryKey: [...CLAVE_PERIODOS_MATRICULA, 'detalle', id],
      queryFn: (): Promise<FormularioPeriodoMatricula> =>
        api.get(`${RUTA}/${id}`),
      enabled: !!id,
    }),
  );

// GET /configuracion/periodos-matricula/{id}/documentos/{documentoId}/plantilla (en cualquier estado del periodo)
export const getPlantilla = (
  periodoId: string,
  documentoId: string,
): Promise<Blob> =>
  api.get(`${RUTA}/${periodoId}/documentos/${documentoId}/plantilla`, {
    responseType: 'blob',
  });
