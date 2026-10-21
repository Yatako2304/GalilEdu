import { ContentLayout } from '@/components/layouts';
import { PlanEvaluacion } from '@/features/catalogo-academico/components/plan-evaluacion';

// Misma pantalla que la pestaña del Personal administrativo (Figma CI-17 y CI-20, HU-56).
// En Figma no está en el menú del Coordinador, pero los RF-130 a RF-132 se lo asignan.
const CoordinadorPlanEvaluacionRoute = () => {
  return (
    <ContentLayout
      title="Plan de evaluación"
      breadcrumbs={['Académico y operaciones', 'Plan de evaluación']}
      subtitle="Define qué cursos se dictan en cada grado, cuántas horas tienen, qué competencias evalúan y qué docente los dicta en cada sección."
    >
      <PlanEvaluacion />
    </ContentLayout>
  );
};

export default CoordinadorPlanEvaluacionRoute;
