import {
  keepPreviousData,
  queryOptions,
  useQuery,
} from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  DetalleEstructuraHoraria,
  EstructurasHorariasDelAnio,
  SolicitudVistaPrevia,
  VistaPreviaJornada,
} from '@/types/api';

// Figma > Personal Administrativo > CI-12 · Estructura horaria (HU-44) y "Modal - Nueva estructura horaria"

/** Las consultas de estructuras cuelgan de esta clave; las mutaciones la invalidan. */
export const CLAVE_ESTRUCTURAS = [
  'configuracion',
  'estructuras-horarias',
] as const;

/**
 * La vista previa tiene su propia clave, fuera de CLAVE_ESTRUCTURAS: guardar una estructura no debe volver a pedir las
 * vistas previas que quedaron en la caché.
 */
export const CLAVE_VISTA_PREVIA_ESTRUCTURA = [
  'configuracion',
  'vista-previa-estructura-horaria',
] as const;

// GET /configuracion/anios-escolares/{anioId}/estructuras-horarias: un objeto `{anioModificable, estructuras}`, no un arreglo
export const useEstructurasHorarias = (anioId: string | undefined) =>
  useQuery(
    queryOptions({
      queryKey: [...CLAVE_ESTRUCTURAS, 'lista', anioId],
      queryFn: (): Promise<EstructurasHorariasDelAnio> =>
        api.get(
          `/configuracion/anios-escolares/${anioId}/estructuras-horarias`,
        ),
      enabled: !!anioId,
    }),
  );

// GET /configuracion/estructuras-horarias/{id}: la estructura con su jornada calculada (la lista no la trae)
export const useDetalleEstructura = (id: string | undefined) =>
  useQuery(
    queryOptions({
      queryKey: [...CLAVE_ESTRUCTURAS, 'detalle', id],
      queryFn: (): Promise<DetalleEstructuraHoraria> =>
        api.get(`/configuracion/estructuras-horarias/${id}`),
      enabled: !!id,
    }),
  );

/**
 * POST /configuracion/anios-escolares/{anioId}/estructuras-horarias/vista-previa: calcula la jornada sin guardar. No es
 * una mutación, aunque use POST: no cambia nada en el servidor, y como consulta la caché guarda la respuesta de cada
 * formulario distinto. Así una respuesta lenta de un formulario viejo no pisa la del actual, y repetir un formulario
 * ya visto responde al instante. `solicitud` null (o sin año) no pide nada. Mientras llega la respuesta nueva se
 * sigue mostrando la anterior, para que la vista previa no parpadee.
 */
export const useVistaPreviaJornada = (
  anioId: string | undefined,
  solicitud: SolicitudVistaPrevia | null,
) =>
  useQuery(
    queryOptions({
      queryKey: [...CLAVE_VISTA_PREVIA_ESTRUCTURA, anioId, solicitud],
      queryFn: (): Promise<VistaPreviaJornada> =>
        api.post(
          `/configuracion/anios-escolares/${anioId}/estructuras-horarias/vista-previa`,
          solicitud,
        ),
      enabled: !!anioId && !!solicitud,
      placeholderData: keepPreviousData,
      retry: false,
    }),
  );
