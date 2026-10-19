import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const TutorAsistenciaRoute = () => {
  return (
    <ContentLayout title="Asistencia">
      <ModulePlaceholder
        feature="asistencia"
        historias={['HU-65']}
        figmaNodeId="868:1046"
      />
    </ContentLayout>
  );
};

export default TutorAsistenciaRoute;
