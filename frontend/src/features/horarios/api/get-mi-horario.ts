import { queryOptions, useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { QueryConfig } from '@/lib/react-query';
import { HorarioSeccion } from '@/types/api';

// HU-50 / HU-51: horario de la sección del estudiante (o del hijo/a, si es apoderado).
export const getMiHorario = ({
  anio,
}: {
  anio: number;
}): Promise<{ data: HorarioSeccion }> => {
  return api.get('/horarios/mi-seccion', { params: { anio } });
};

export const getMiHorarioQueryOptions = ({ anio }: { anio: number }) => {
  return queryOptions({
    queryKey: ['horarios', 'mi-seccion', { anio }],
    queryFn: () => getMiHorario({ anio }),
  });
};

type UseMiHorarioOptions = {
  anio: number;
  queryConfig?: QueryConfig<typeof getMiHorarioQueryOptions>;
};

export const useMiHorario = ({ anio, queryConfig }: UseMiHorarioOptions) => {
  return useQuery({
    ...getMiHorarioQueryOptions({ anio }),
    ...queryConfig,
  });
};
