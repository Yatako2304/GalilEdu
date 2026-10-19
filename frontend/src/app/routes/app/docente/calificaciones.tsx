import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const DocenteCalificacionesRoute = () => {
  return (
    <ContentLayout title="Calificaciones">
      <ModulePlaceholder
        feature="calificaciones"
        historias={['HU-62']}
        figmaNodeId="629:11540"
      />
    </ContentLayout>
  );
};

export default DocenteCalificacionesRoute;
