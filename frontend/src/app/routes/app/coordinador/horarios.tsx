import { ContentLayout } from '@/components/layouts';
import { AsignacionHorarios } from '@/features/horarios/components/asignacion-horarios';

// Figma > Coordinador > "HU-45: Asignación de docente y curso a un bloque de horario" (619:2757, 286:14702, 248:9344).
const CoordinadorHorariosRoute = () => {
  return (
    <ContentLayout
      title="Asignación de horarios"
      breadcrumbs={['Horarios', 'Asignación de horarios']}
    >
      <AsignacionHorarios />
    </ContentLayout>
  );
};

export default CoordinadorHorariosRoute;
