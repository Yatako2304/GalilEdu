import { useParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { paths } from '@/config/paths';
import { usePeriodoMatricula } from '@/features/configuracion/api/periodos-matricula';
import { PeriodoExistente } from '@/features/configuracion/components/periodo-matricula/periodo-existente';

// Figma > Personal Administrativo > pantalla 13 > "Ver" un periodo de matrícula, en solo lectura (HU-23)
const AdministrativoPeriodoMatriculaRoute = () => {
  const { periodoId } = useParams();
  const periodo = usePeriodoMatricula(periodoId);
  const nombre = periodo.data?.titulo ?? 'Periodo de matrícula';

  return (
    <ContentLayout
      title={nombre}
      subtitle="El periodo y los requisitos de la matrícula del año académico. Los apoderados pueden matricular a sus hijos."
      breadcrumbs={[
        {
          label: 'Periodos de matrícula',
          to: paths.app.administrativo.matricula.getHref(),
        },
        nombre,
      ]}
    >
      <PeriodoExistente periodoId={periodoId} modo="ver" />
    </ContentLayout>
  );
};

export default AdministrativoPeriodoMatriculaRoute;
