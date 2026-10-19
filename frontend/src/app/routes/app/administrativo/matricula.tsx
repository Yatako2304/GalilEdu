import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const AdministrativoMatriculaRoute = () => {
  return (
    <ContentLayout title="Matrícula">
      <ModulePlaceholder
        feature="matricula"
        historias={[
          'HU-21',
          'HU-22',
          'HU-23',
          'HU-24',
          'HU-29',
          'HU-31',
          'HU-36',
          'HU-71',
          'HU-72',
        ]}
        figmaNodeId="823:6028"
      />
    </ContentLayout>
  );
};

export default AdministrativoMatriculaRoute;
