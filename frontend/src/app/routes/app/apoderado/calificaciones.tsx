import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const ApoderadoCalificacionesRoute = () => {
  return (
    <ContentLayout title="Calificaciones">
      <ModulePlaceholder
        feature="calificaciones"
        historias={['HU-69']}
        figmaNodeId="467:7656"
      />
    </ContentLayout>
  );
};

export default ApoderadoCalificacionesRoute;
