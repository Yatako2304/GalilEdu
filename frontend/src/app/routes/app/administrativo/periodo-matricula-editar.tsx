import { useParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { paths } from '@/config/paths';
import { usePeriodoMatricula } from '@/features/configuracion/api/periodos-matricula';
import { PeriodoExistente } from '@/features/configuracion/components/periodo-matricula/periodo-existente';

// Editar un periodo de matrícula PROGRAMADO (HU-23): el formulario de "Nuevo periodo" con el periodo cargado
const AdministrativoPeriodoMatriculaEditarRoute = () => {
  const { periodoId } = useParams();
  const periodo = usePeriodoMatricula(periodoId);
  const nombre = periodo.data?.titulo ?? 'Periodo de matrícula';

  return (
    <ContentLayout
      title={`Editar ${nombre}`}
      subtitle="Define el periodo y los requisitos de la matrícula del año académico. Los apoderados podrán matricular a sus hijos una vez abierta."
      breadcrumbs={[
        {
          label: 'Periodos de matrícula',
          to: paths.app.administrativo.matricula.getHref(),
        },
        {
          label: nombre,
          to: paths.app.administrativo.periodoMatricula.getHref(
            periodoId ?? '',
          ),
        },
        'Editar',
      ]}
    >
      <PeriodoExistente periodoId={periodoId} modo="editar" />
    </ContentLayout>
  );
};

export default AdministrativoPeriodoMatriculaEditarRoute;
