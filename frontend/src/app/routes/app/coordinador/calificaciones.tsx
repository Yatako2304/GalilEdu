import { ContentLayout } from '@/components/layouts';
import { ReportesNotas } from '@/features/calificaciones/components/reportes-notas';

// HU-63 (por curso y grado) y HU-64 (institucional). Figma > Coordinador > «Competencias reporte» (349:11639).
const CoordinadorCalificacionesRoute = () => (
  <ContentLayout
    title="Calificaciones"
    subtitle="Reportes de notas por curso y grado, y de toda la institución."
  >
    <ReportesNotas vistas={['grado', 'institucional']} />
  </ContentLayout>
);

export default CoordinadorCalificacionesRoute;
