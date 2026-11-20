import { ContentLayout } from '@/components/layouts';
import { EscalaCalificacion } from '@/features/catalogo-academico/components/escala-calificacion';

// Misma pantalla que la pestaña del Personal administrativo (Figma CI-23, HU-55). En Figma no está en el menú del
// Coordinador; el equipo decidió que la tengan los dos roles, como el plan de evaluación.
const CoordinadorEscalaCalificacionRoute = () => {
  return (
    <ContentLayout
      title="Escala de calificación"
      breadcrumbs={['Académico y operaciones', 'Escala de calificación']}
      subtitle="Define cómo se convierte una nota vigesimal (0–20) a un nivel de logro (AD, A, B, C)."
    >
      <EscalaCalificacion />
    </ContentLayout>
  );
};

export default CoordinadorEscalaCalificacionRoute;
