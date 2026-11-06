import { CirclePlus } from 'lucide-react';
import * as React from 'react';
import { useParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { Button } from '@/components/ui/button';
import { paths } from '@/config/paths';
import { usePeriodoMatriculas } from '@/features/matricula/api/matriculas-periodo';
import { DialogoNuevaMatricula } from '@/features/matricula/components/dialogo-nueva-matricula';
import { MatriculasPeriodo } from '@/features/matricula/components/matriculas-periodo';

const AdministrativoMatriculasPeriodoRoute = () => {
  const { periodoId = '' } = useParams();
  const { data: periodo } = usePeriodoMatriculas(periodoId);
  const [nueva, setNueva] = React.useState(false);
  const cerrado = periodo?.estado === 'CERRADO';
  return (
    <ContentLayout
      title={periodo ? `Matrículas ${periodo.anio}` : 'Matrículas del periodo'}
      breadcrumbs={[
        {
          label: 'Periodos de matrícula',
          to: paths.app.administrativo.matricula.getHref(),
        },
        periodo ? `Matrícula ${periodo.anio}` : 'Matrículas del periodo',
      ]}
      subtitle="Revisa el estado de cada estudiante y realiza las acciones disponibles según la etapa de su matrícula."
      actions={
        // Solo con el periodo abierto se asignan secciones de matrícula regular. La extemporánea (HU-25) aún no existe.
        <Button
          disabled={periodo?.estado !== 'ABIERTO'}
          icon={<CirclePlus className="size-4" />}
          onClick={() => setNueva(true)}
        >
          {cerrado ? 'Matrícula extemporánea' : 'Nueva matrícula'}
        </Button>
      }
    >
      <MatriculasPeriodo periodoId={periodoId} />
      {nueva && (
        <DialogoNuevaMatricula
          periodoId={periodoId}
          onCerrar={() => setNueva(false)}
        />
      )}
    </ContentLayout>
  );
};

export default AdministrativoMatriculasPeriodoRoute;
