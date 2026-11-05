import { ContentLayout } from '@/components/layouts';
import { CalificacionesDocente } from '@/features/calificaciones/components/calificaciones-docente';

const DocenteCalificacionesRoute = () => {
  return (
    <ContentLayout
      title="Calificaciones"
      subtitle="Registra y consulta las notas de tus estudiantes por periodo lectivo."
    >
      <CalificacionesDocente />
    </ContentLayout>
  );
};

export default DocenteCalificacionesRoute;
