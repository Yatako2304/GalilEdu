import { ContentLayout } from '@/components/layouts';
import { ReportesNotas } from '@/features/calificaciones/components/reportes-notas';

// HU-64 · Reporte institucional de notas para el Administrador. No hay frame en Figma: usa la misma vista que el
// Coordinador (Figma > Coordinador > «Competencias reporte»).
const AdminCalificacionesRoute = () => (
  <ContentLayout
    title="Calificaciones"
    subtitle="Reporte institucional de notas por grado, sección y área."
  >
    <ReportesNotas vistas={['institucional']} />
  </ContentLayout>
);

export default AdminCalificacionesRoute;
