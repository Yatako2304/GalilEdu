import { ContentLayout } from '@/components/layouts';
import { paths } from '@/config/paths';
import { FormularioPeriodo } from '@/features/configuracion/components/periodo-matricula/formulario-periodo';

// Figma > Personal Administrativo > pantalla 13 > "Nuevo periodo de matrícula" (HU-23)
const AdministrativoPeriodoMatriculaNuevoRoute = () => {
  return (
    <ContentLayout
      title="Nuevo periodo de matrícula"
      subtitle="Define el periodo y los requisitos de la matrícula del año académico. Los apoderados podrán matricular a sus hijos una vez abierta."
      breadcrumbs={[
        {
          label: 'Periodos de matrícula',
          to: paths.app.administrativo.matricula.getHref(),
        },
        'Nuevo periodo de matrícula',
      ]}
    >
      <FormularioPeriodo />
    </ContentLayout>
  );
};

export default AdministrativoPeriodoMatriculaNuevoRoute;
