import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const AdministrativoPensionesRoute = () => {
  return (
    <ContentLayout title="Pensiones">
      <ModulePlaceholder
        feature="pensiones"
        historias={['HU-34', 'HU-35', 'HU-36', 'HU-37', 'HU-38', 'HU-39']}
        figmaNodeId="837:6069"
      />
    </ContentLayout>
  );
};

export default AdministrativoPensionesRoute;
