import {
  keepPreviousData,
  queryOptions,
  useQuery,
} from '@tanstack/react-query';

import { ROLES } from '@/config/roles';
import { api } from '@/lib/api-client';
import { QueryConfig } from '@/lib/react-query';
import { EstadoUsuario, Pagina, UsuarioResumen } from '@/types/api';

export type FiltrosUsuarios = {
  q?: string;
  rol?: ROLES | '';
  estado?: EstadoUsuario | '';
  page: number;
  size?: number;
};

// GET /usuarios?q=&rol=&estado=&page=&size= (HU-6, HU-9, HU-12)
export const getUsuarios = (
  filtros: FiltrosUsuarios,
): Promise<Pagina<UsuarioResumen>> => {
  const params = Object.fromEntries(
    Object.entries({ size: 8, ...filtros }).filter(
      ([, v]) => v !== '' && v != null,
    ),
  );
  return api.get('/usuarios', { params });
};

export const getUsuariosQueryOptions = (filtros: FiltrosUsuarios) =>
  queryOptions({
    queryKey: ['usuarios', 'lista', filtros],
    queryFn: () => getUsuarios(filtros),
    placeholderData: keepPreviousData,
  });

export const useUsuarios = ({
  filtros,
  queryConfig,
}: {
  filtros: FiltrosUsuarios;
  queryConfig?: QueryConfig<typeof getUsuariosQueryOptions>;
}) => useQuery({ ...getUsuariosQueryOptions(filtros), ...queryConfig });
