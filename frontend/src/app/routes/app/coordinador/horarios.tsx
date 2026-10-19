import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const CoordinadorHorariosRoute = () => {
  return (
    <ContentLayout title="Horarios">
      <ModulePlaceholder
        feature="horarios"
        historias={['HU-44', 'HU-45', 'HU-46', 'HU-47']}
        figmaNodeId="248:9344"
      />
    </ContentLayout>
  );
};

export default CoordinadorHorariosRoute;
