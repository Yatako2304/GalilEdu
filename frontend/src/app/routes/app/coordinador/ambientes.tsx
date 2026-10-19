import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const CoordinadorAmbientesRoute = () => {
  return (
    <ContentLayout title="Ambientes físicos">
      <ModulePlaceholder
        feature="ambientes"
        historias={['HU-48']}
        figmaNodeId="308:1874"
      />
    </ContentLayout>
  );
};

export default CoordinadorAmbientesRoute;
