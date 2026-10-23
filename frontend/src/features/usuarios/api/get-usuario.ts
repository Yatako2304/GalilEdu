import { queryOptions, useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { RolCatalogo, ResumenUsuarios, UsuarioDetalle } from '@/types/api';

// GET /usuarios/{id}
export const getUsuario = (id: string): Promise<UsuarioDetalle> =>
  api.get(`/usuarios/${id}`);

export const useUsuario = (id: string | null) =>
  useQuery({
    queryKey: ['usuarios', 'detalle', id],
    queryFn: () => getUsuario(id as string),
    enabled: !!id,
  });

// GET /estudiantes/{id}/apoderado/documento: abre en otra pestaña el documento que acredita al apoderado.
export const abrirDocumentoAcreditacion = async (estudianteId: string) => {
  const archivo: Blob = await api.get(
    `/estudiantes/${estudianteId}/apoderado/documento`,
    { responseType: 'blob' },
  );
  const url = URL.createObjectURL(archivo);
  window.open(url, '_blank', 'noopener');
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
};

// GET /usuarios/resumen (Inicio del administrador)
export const getResumenUsuariosQueryOptions = () =>
  queryOptions({
    queryKey: ['usuarios', 'resumen'],
    queryFn: (): Promise<ResumenUsuarios> => api.get('/usuarios/resumen'),
  });

export const useResumenUsuarios = () =>
  useQuery(getResumenUsuariosQueryOptions());

// GET /usuarios/roles (roles asignables en el formulario)
export const useRolesAsignables = () =>
  useQuery({
    queryKey: ['usuarios', 'roles'],
    queryFn: (): Promise<RolCatalogo[]> => api.get('/usuarios/roles'),
    staleTime: Infinity,
  });
