import { ContentLayout } from '@/components/layouts';
import { ReportesNotas } from '@/features/calificaciones/components/reportes-notas';

// HU-64 · Reporte institucional de notas para el Personal administrativo. No hay frame en Figma: usa la misma vista
// que la pestaña «Institucional» del Coordinador.
const AdministrativoCalificacionesRoute = () => (
  <ContentLayout
    title="Calificaciones"
    breadcrumbs={['Académico y operaciones', 'Calificaciones']}
    subtitle="Reporte institucional de notas por grado, sección y área."
  >
    <ReportesNotas vistas={['institucional']} />
  </ContentLayout>
);

export default AdministrativoCalificacionesRoute;
