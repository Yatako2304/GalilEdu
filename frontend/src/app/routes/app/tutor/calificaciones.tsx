import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const TutorCalificacionesRoute = () => {
  return (
    <ContentLayout title="Calificaciones">
      <ModulePlaceholder
        feature="calificaciones"
        historias={['HU-70']}
        figmaNodeId="476:12624"
      />
    </ContentLayout>
  );
};

export default TutorCalificacionesRoute;
