import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const ApoderadoMatriculaRoute = () => {
  return (
    <ContentLayout title="Matrícula">
      <ModulePlaceholder
        feature="matricula"
        historias={['HU-27', 'HU-28', 'HU-29', 'HU-30', 'HU-31', 'HU-32']}
        figmaNodeId="292:5478"
      />
    </ContentLayout>
  );
};

export default ApoderadoMatriculaRoute;
