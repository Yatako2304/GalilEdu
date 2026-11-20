import { ContentLayout } from '@/components/layouts';
import { MisDatos } from '@/features/auth/components/mis-datos';

// «Mis datos»: cualquier usuario ve y edita sus propios datos personales (ampliación de HU-9; no está en Figma). El
// apoderado también ve su familia.
const MisDatosRoute = () => (
  <ContentLayout
    title="Mis datos"
    subtitle="Revisa tus datos personales y mantenlos al día."
  >
    <MisDatos />
  </ContentLayout>
);

export default MisDatosRoute;
