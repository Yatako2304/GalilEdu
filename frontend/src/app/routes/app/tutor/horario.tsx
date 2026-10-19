import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const TutorHorarioRoute = () => {
  return (
    <ContentLayout title="Horario">
      <ModulePlaceholder
        feature="horarios"
        historias={['HU-49']}
        figmaNodeId="393:18119"
      />
    </ContentLayout>
  );
};

export default TutorHorarioRoute;
