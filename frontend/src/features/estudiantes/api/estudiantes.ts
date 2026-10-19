import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  EstadoEstudiante,
  EstudianteResumen,
  FichaEstudiante,
  InscripcionRegistrada,
  Pagina,
  PersonaEncontrada,
  SolicitudInscripcion,
} from '@/types/api';

// Endpoints de backend/estudiantes/web/EstudianteController.java (HU-7).

export type FiltrosEstudiantes = {
  q?: string;
  estado?: EstadoEstudiante | '';
  page: number;
};

// GET /estudiantes?q=&estado=&page=
export const useEstudiantes = (filtros: FiltrosEstudiantes) =>
  useQuery({
    queryKey: ['estudiantes', 'lista', filtros],
    queryFn: (): Promise<Pagina<EstudianteResumen>> => {
      const params = Object.fromEntries(
        Object.entries({ size: 8, ...filtros }).filter(
          ([, v]) => v !== '' && v != null,
        ),
      );
      return api.get('/estudiantes', { params });
    },
    placeholderData: keepPreviousData,
  });

// GET /estudiantes/{id}
export const useFichaEstudiante = (id: string) =>
  useQuery({
    queryKey: ['estudiantes', 'ficha', id],
    queryFn: (): Promise<FichaEstudiante> => api.get(`/estudiantes/${id}`),
  });

// POST /estudiantes/inscripciones
export const useInscribir = (onSuccess: (r: InscripcionRegistrada) => void) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: SolicitudInscripcion): Promise<InscripcionRegistrada> =>
      api.post('/estudiantes/inscripciones', data),
    onSuccess: (r) => {
      queryClient.invalidateQueries({ queryKey: ['estudiantes'] });
      queryClient.invalidateQueries({ queryKey: ['usuarios'] });
      onSuccess(r);
    },
  });
};

/**
 * GET /estudiantes/personas?documento= → la persona si ya existe (ej. por un hermano), o null.
 * El 404 no es un error aquí: significa "persona nueva", por eso no se muestra notificación.
 */
export const buscarPersonaPorDocumento = async (
  documento: string,
): Promise<PersonaEncontrada | null> => {
  const respuesta: PersonaEncontrada | { message: string } = await api.get(
    '/estudiantes/personas',
    { params: { documento }, validateStatus: (s) => s === 200 || s === 404 },
  );
  return 'personaId' in respuesta ? respuesta : null;
};
