import { CirclePlus } from 'lucide-react';
import { useParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { Button } from '@/components/ui/button';
import { paths } from '@/config/paths';
import { MatriculasPeriodo } from '@/features/matricula/components/matriculas-periodo';

const AdministrativoMatriculasPeriodoRoute = () => {
  const { periodoId = 'periodo-2027' } = useParams();
  return (
    <ContentLayout
      title="Matrículas 2027"
      breadcrumbs={[
        {
          label: 'Periodos de matrícula',
          to: paths.app.administrativo.matricula.getHref(),
        },
        'Matrícula 2027',
      ]}
      subtitle="Revisa el estado de cada estudiante y realiza las acciones disponibles según la etapa de su matrícula."
      actions={
        <Button disabled icon={<CirclePlus className="size-4" />}>
          Nueva matrícula
        </Button>
      }
    >
      <MatriculasPeriodo periodoId={periodoId} />
      <span className="sr-only">Periodo {periodoId}</span>
    </ContentLayout>
  );
};

export default AdministrativoMatriculasPeriodoRoute;
