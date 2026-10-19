import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const AdministrativoAnioEscolarRoute = () => {
  return (
    <ContentLayout title="Año escolar">
      <ModulePlaceholder
        feature="anio-escolar"
        historias={['HU-44', 'HU-74', 'HU-75']}
        figmaNodeId="847:7929"
      />
    </ContentLayout>
  );
};

export default AdministrativoAnioEscolarRoute;
