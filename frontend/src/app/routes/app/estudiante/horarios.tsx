import { QueryClient } from '@tanstack/react-query';

import { ContentLayout } from '@/components/layouts';
import { getMiHorarioQueryOptions } from '@/features/horarios/api/get-mi-horario';
import { MiHorario } from '@/features/horarios/components/mi-horario';

// Precarga el horario del año actual antes de mostrar la página (patrón clientLoader).
export const clientLoader = (queryClient: QueryClient) => async () => {
  const query = getMiHorarioQueryOptions({ anio: new Date().getFullYear() });

  return (
    queryClient.getQueryData(query.queryKey) ??
    (await queryClient.fetchQuery(query))
  );
};

// Figma: Estudiante/Horarios | HU-50
const EstudianteHorariosRoute = () => {
  return (
    <ContentLayout title="Horario">
      <MiHorario />
    </ContentLayout>
  );
};

export default EstudianteHorariosRoute;
