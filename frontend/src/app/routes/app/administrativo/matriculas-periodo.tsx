import { CirclePlus } from 'lucide-react';
import { useParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { Button } from '@/components/ui/button';
import { paths } from '@/config/paths';
import { usePeriodoMatricula } from '@/features/configuracion/api/periodos-matricula';
import { MatriculasPeriodo } from '@/features/matricula/components/matriculas-periodo';

const fecha = (valor?: string | null) =>
  valor
    ? new Intl.DateTimeFormat('es-PE', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      })
        .format(new Date(`${valor}T12:00:00`))
        .replace('.', '')
    : undefined;

const AdministrativoMatriculasPeriodoRoute = () => {
  const { periodoId = 'periodo-2027' } = useParams();
  const periodo = usePeriodoMatricula(periodoId);
  const titulo = periodo.data?.anio
    ? `Matrículas ${periodo.data.anio}`
    : 'Matrículas del periodo';
  return (
    <ContentLayout
      title={titulo}
      breadcrumbs={[
        {
          label: 'Periodos de matrícula',
          to: paths.app.administrativo.matricula.getHref(),
        },
        titulo,
      ]}
      subtitle="Revisa el estado de cada estudiante y realiza las acciones disponibles según la etapa de su matrícula."
      actions={
        <Button disabled icon={<CirclePlus className="size-4" />}>
          Nueva matrícula
        </Button>
      }
    >
      <MatriculasPeriodo
        periodoId={periodoId}
        cierre={fecha(periodo.data?.fechaCierre)}
      />
      <span className="sr-only">Periodo {periodoId}</span>
    </ContentLayout>
  );
};

export default AdministrativoMatriculasPeriodoRoute;
