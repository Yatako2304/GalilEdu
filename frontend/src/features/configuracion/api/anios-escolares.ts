import { queryOptions, useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  AnioEscolarActual,
  AnioEscolarDetalle,
  AnioEscolarResumen,
  RequisitosAnio,
} from '@/types/api';

// Figma > Personal Administrativo > CI-01, CI-03 y CI-06 · Año escolar (HU-74)

/** Todas las consultas del año cuelgan de esta clave; las mutaciones la invalidan. */
export const CLAVE_ANIOS = ['configuracion', 'anios'] as const;
export const CLAVE_LISTA = [...CLAVE_ANIOS, 'lista'] as const;
export const CLAVE_ACTUAL = [...CLAVE_ANIOS, 'actual'] as const;

/** El año que está ACTIVO en la lista, si lo hay. Solo puede haber uno. */
export const anioActivo = (anios: AnioEscolarResumen[]) =>
  anios.find((a) => a.estado === 'ACTIVO');

// GET /configuracion/anios-escolares (arreglo simple, sin paginar)
export const getAniosEscolaresQueryOptions = () =>
  queryOptions({
    queryKey: CLAVE_LISTA,
    queryFn: (): Promise<AnioEscolarResumen[]> =>
      api.get('/configuracion/anios-escolares'),
  });

/** `habilitado` evita pedir la lista cuando la pantalla no la usa (por ejemplo, un año ya activo o cerrado). */
export const useAniosEscolares = (habilitado = true) =>
  useQuery({ ...getAniosEscolaresQueryOptions(), enabled: habilitado });

// GET /configuracion/anios-escolares/{id}: el año con sus periodos
export const useAnioEscolar = (id: string | undefined) =>
  useQuery(
    queryOptions({
      queryKey: [...CLAVE_ANIOS, 'detalle', id],
      queryFn: (): Promise<AnioEscolarDetalle> =>
        api.get(`/configuracion/anios-escolares/${id}`),
      enabled: !!id,
    }),
  );

/**
 * GET /configuracion/anios-escolares/actual: responde 404 si no hay un año ACTIVO, y ese error saldría como
 * notificación. Por eso solo se consulta con `habilitado` (la lista ya tiene un año ACTIVO).
 */
export const useAnioEscolarActual = (habilitado: boolean) =>
  useQuery(
    queryOptions({
      queryKey: CLAVE_ACTUAL,
      queryFn: (): Promise<AnioEscolarActual> =>
        api.get('/configuracion/anios-escolares/actual'),
      enabled: habilitado,
    }),
  );

// GET /configuracion/anios-escolares/{id}/requisitos: solo tiene sentido con el año PLANIFICADO
export const useRequisitosAnio = (
  id: string | undefined,
  habilitado: boolean,
) =>
  useQuery(
    queryOptions({
      queryKey: [...CLAVE_ANIOS, 'requisitos', id],
      queryFn: (): Promise<RequisitosAnio> =>
        api.get(`/configuracion/anios-escolares/${id}/requisitos`),
      enabled: !!id && habilitado,
    }),
  );
