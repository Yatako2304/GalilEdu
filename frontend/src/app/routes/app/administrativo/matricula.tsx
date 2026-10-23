import { CirclePlus } from 'lucide-react';
import { Link, useSearchParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { Button } from '@/components/ui/button';
import { paths } from '@/config/paths';
import { CardPeriodoVigente } from '@/features/configuracion/components/periodo-matricula/card-periodo-vigente';
import { PeriodosMatriculaLista } from '@/features/configuracion/components/periodo-matricula/periodos-matricula-lista';
import { PagoMatriculaCard } from '@/features/pagos/components/pago-matricula-card';

const AdministrativoMatriculaRoute = () => {
  const [params] = useSearchParams();
  const matriculaId = params.get('matriculaId');

  if (matriculaId) {
    return (
      <ContentLayout
        title="Detalle de matrícula"
        breadcrumbs={['Matrícula', 'Detalle']}
      >
        <div className="ml-auto w-full max-w-[420px]">
          <PagoMatriculaCard matriculaId={matriculaId} />
        </div>
      </ContentLayout>
    );
  }
  // Figma > pantalla 13 "Periodos de matrícula" (HU-23).
  return (
    <ContentLayout
      title="Periodos de matrícula"
      subtitle="Consulta los periodos de matrícula, configura sus fechas y vacantes, y revisa las matrículas de los estudiantes en cada periodo."
      actions={
        <Button asChild>
          <Link to={paths.app.administrativo.periodoMatriculaNuevo.getHref()}>
            <CirclePlus className="size-4" aria-hidden="true" />
            <span className="px-1">Nuevo periodo de matrícula</span>
          </Link>
        </Button>
      }
    >
      <CardPeriodoVigente />
      <PeriodosMatriculaLista />
    </ContentLayout>
  );
};

export default AdministrativoMatriculaRoute;
