import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const AdministrativoAmbientesRoute = () => {
  return (
    <ContentLayout title="Ambientes físicos">
      <ModulePlaceholder
        feature="ambientes"
        historias={['HU-43']}
        figmaNodeId="857:10215"
      />
    </ContentLayout>
  );
};

export default AdministrativoAmbientesRoute;
