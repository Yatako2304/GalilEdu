import { ContentLayout } from '@/components/layouts';
import { AniosLista } from '@/features/configuracion/components/anio-escolar/anios-lista';
import { BotonNuevoAnio } from '@/features/configuracion/components/anio-escolar/boton-nuevo-anio';

// Figma > Personal Administrativo > CI-01 · Año escolar — listado de años (HU-74)
const AdministrativoAnioEscolarRoute = () => {
  return (
    <ContentLayout
      title="Año escolar"
      breadcrumbs={['Configuración', 'Año escolar']}
      subtitle="Gestiona los años escolares con sus periodos académicos, secciones por grado y estructura horaria. Solo un año puede estar activo a la vez."
      actions={<BotonNuevoAnio />}
    >
      <AniosLista />
    </ContentLayout>
  );
};

export default AdministrativoAnioEscolarRoute;
