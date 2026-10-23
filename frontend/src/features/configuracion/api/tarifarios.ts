import { queryOptions, useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { QueryConfig } from '@/lib/react-query';
import { TarifarioVista } from '@/types/api';

// Figma > Personal Administrativo > Pensiones > Configuración > "P19 · Tarifario y calendario" (HU-34)

export const RUTA_TARIFARIOS = '/configuracion/tarifarios';

/** Todas las consultas del tarifario cuelgan de esta clave: `[…, anioEscolarId]`. La mutación las actualiza. */
export const CLAVE_TARIFARIOS = ['configuracion', 'tarifarios'] as const;

// GET /configuracion/tarifarios?anioEscolarId=: el tarifario guardado o, si el año no tiene, una propuesta (existe = false)
export const getTarifario = (anioEscolarId: string): Promise<TarifarioVista> =>
  api.get(RUTA_TARIFARIOS, { params: { anioEscolarId } });

export const getTarifarioQueryOptions = (anioEscolarId: string | undefined) =>
  queryOptions({
    queryKey: [...CLAVE_TARIFARIOS, anioEscolarId],
    queryFn: () => getTarifario(anioEscolarId as string),
    enabled: !!anioEscolarId,
  });

export const useTarifario = ({
  anioEscolarId,
  queryConfig,
}: {
  anioEscolarId: string | undefined;
  queryConfig?: QueryConfig<typeof getTarifarioQueryOptions>;
}) => useQuery({ ...getTarifarioQueryOptions(anioEscolarId), ...queryConfig });
