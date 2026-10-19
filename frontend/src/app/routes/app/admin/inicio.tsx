import { ContentLayout } from '@/components/layouts';
import { paths } from '@/config/paths';
import { InicioAdministrador } from '@/features/usuarios/components/inicio-administrador';
import { useUser } from '@/lib/auth';

// Figma > Administrador > "00 · Home — Administrador"
const AdminInicioRoute = () => {
  const user = useUser();
  const usuarios = paths.app.admin.usuarios.getHref();

  return (
    <ContentLayout
      title={`Hola, ${user.data?.firstName.split(' ')[0] ?? ''}`}
      breadcrumbs={['Inicio']}
      subtitle="Administrador · Usuarios y accesos del sistema"
    >
      <InicioAdministrador
        hrefUsuarios={usuarios}
        hrefNuevoUsuario={`${usuarios}?nuevo=1`}
        hrefInactivos={`${usuarios}?estado=INACTIVO`}
        hrefCargaMasiva={`${usuarios}?carga=1`}
      />
    </ContentLayout>
  );
};

export default AdminInicioRoute;
