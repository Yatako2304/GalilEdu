import { ContentLayout } from '@/components/layouts';
import { FamiliaDelApoderado } from '@/features/estudiantes/components/familia-apoderado';

// Figma > Apoderado > «Datos de la Familia» (ítem «Familia» del menú lateral). De solo vista.
const ApoderadoFamiliaRoute = () => (
  <ContentLayout title="Datos de la Familia">
    <FamiliaDelApoderado />
  </ContentLayout>
);

export default ApoderadoFamiliaRoute;
