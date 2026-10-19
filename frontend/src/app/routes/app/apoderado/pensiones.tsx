import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const ApoderadoPensionesRoute = () => {
  return (
    <ContentLayout title="Pensiones">
      <ModulePlaceholder
        feature="pensiones"
        historias={['HU-40', 'HU-41', 'HU-42']}
        figmaNodeId="362:6105"
      />
    </ContentLayout>
  );
};

export default ApoderadoPensionesRoute;
