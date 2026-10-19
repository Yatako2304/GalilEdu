import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';

const AdminEstudiantesRoute = () => {
  return (
    <ContentLayout title="Estudiantes">
      <ModulePlaceholder
        feature="estudiantes"
        historias={['Inscripción (pasos 1-5)', 'Ficha del estudiante']}
        figmaNodeId="958:4465"
      />
    </ContentLayout>
  );
};

export default AdminEstudiantesRoute;
