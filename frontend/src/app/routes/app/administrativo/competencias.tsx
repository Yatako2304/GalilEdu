import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const AdministrativoCompetenciasRoute = () => {
  return (
    <ContentLayout title="Competencias">
      <ModulePlaceholder
        feature="competencias"
        historias={['HU-52', 'HU-53', 'HU-54', 'HU-55', 'HU-56']}
        figmaNodeId="851:10487"
      />
    </ContentLayout>
  );
};

export default AdministrativoCompetenciasRoute;
