import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const CoordinadorCalificacionesRoute = () => {
  return (
    <ContentLayout title="Calificaciones">
      <ModulePlaceholder
        feature="calificaciones"
        historias={['HU-57']}
        figmaNodeId="327:4560"
      />
    </ContentLayout>
  );
};

export default CoordinadorCalificacionesRoute;
