import { queryOptions, useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';

import { CeldaSolicitada, CruceDetectado } from '../types';

import { CLAVE_HORARIOS } from './horario-seccion';

// POST /horarios/secciones/{seccionId}/cruces (lo implementa HU-46; aún no existe en el backend). Cuerpo
// `{ celdas }` con el borrador completo; responde la lista de cruces con la forma de `celda.cruces`.
//
// Si la llamada falla (404 mientras no exista, o la red) NO se avisa ni se bloquea nada: el PUT vuelve a validar todo.
export const consultarCruces = (
  seccionId: string,
  celdas: CeldaSolicitada[],
): Promise<CruceDetectado[]> =>
  api.post(
    `/horarios/secciones/${seccionId}/cruces`,
    { celdas },
    { silenciarNotificacion: true },
  );

export const useCruces = (
  seccionId: string,
  celdas: CeldaSolicitada[],
  habilitado: boolean,
) =>
  useQuery(
    queryOptions({
      queryKey: [...CLAVE_HORARIOS, 'cruces', seccionId, celdas],
      queryFn: () => consultarCruces(seccionId, celdas),
      enabled: habilitado,
      retry: false,
      staleTime: 0,
    }),
  );
