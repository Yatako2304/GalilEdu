import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const AdminUsuariosRoute = () => {
  return (
    <ContentLayout title="Usuarios">
      <ModulePlaceholder
        feature="usuarios"
        historias={['HU-6', 'HU-7', 'HU-8']}
        figmaNodeId="700:14588"
      />
    </ContentLayout>
  );
};

export default AdminUsuariosRoute;
