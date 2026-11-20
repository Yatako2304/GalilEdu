import { ContentLayout } from '@/components/layouts';
import { ConfiguracionAccesoPanel } from '@/features/usuarios/components/configuracion-acceso';

// HU-2 · Administrador › Seguridad › Configuración de acceso (no está en Figma; ver la decisión del 2026-11-19).
const AdminConfiguracionAccesoRoute = () => (
  <ContentLayout
    title="Configuración de acceso"
    subtitle="Define cómo pueden ingresar los usuarios a GalilEdu."
  >
    <ConfiguracionAccesoPanel />
  </ContentLayout>
);

export default AdminConfiguracionAccesoRoute;
