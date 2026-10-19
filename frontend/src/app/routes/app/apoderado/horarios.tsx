import { ContentLayout } from '@/components/layouts';
import { MiHorario } from '@/features/horarios/components/mi-horario';

// Figma: HU-51 Consulta de horario de clases de sección para el Apoderado
const ApoderadoHorariosRoute = () => {
  return (
    <ContentLayout title="Horario">
      <MiHorario />
    </ContentLayout>
  );
};

export default ApoderadoHorariosRoute;
