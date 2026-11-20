import { ContentLayout } from '@/components/layouts';
import { PlanCurricular } from '@/features/catalogo-academico/components/plan-curricular';

// Misma pantalla que la pestaña del Personal administrativo (Figma CI-14, HU-52 a HU-54). En Figma no está en el
// menú del Coordinador; el equipo decidió que la tengan los dos roles, como el plan de evaluación.
const CoordinadorPlanCurricularRoute = () => {
  return (
    <ContentLayout
      title="Plan curricular"
      breadcrumbs={['Académico y operaciones', 'Plan curricular']}
      subtitle="Organiza el catálogo curricular: cada área agrupa sus competencias y sus cursos."
    >
      <PlanCurricular />
    </ContentLayout>
  );
};

export default CoordinadorPlanCurricularRoute;
