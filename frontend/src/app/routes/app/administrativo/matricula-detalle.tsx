import { useParams, useSearchParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { Card } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { paths } from '@/config/paths';
import { useMatriculaAdministrativa } from '@/features/matricula/api/matriculas-administrativo';
import {
  DetalleMatricula,
  etiquetaDetalleMatricula,
} from '@/features/matricula/components/detalle-matricula';
import { PagoMatriculaCard } from '@/features/pagos/components/pago-matricula-card';

const AdministrativoMatriculaDetalleRoute = () => {
  const { matriculaId } = useParams();
  const [params] = useSearchParams();
  const periodoId = params.get('periodoId') ?? 'periodo-2027';
  const consulta = useMatriculaAdministrativa(matriculaId);
  const matricula = consulta.data;

  if (consulta.isLoading) {
    return (
      <ContentLayout title="Detalle de matrícula">
        <div className="flex min-h-64 items-center justify-center">
          <Spinner size="lg" />
        </div>
      </ContentLayout>
    );
  }

  if (!matricula) {
    return (
      <ContentLayout title="Detalle de matrícula">
        <Card className="p-8 text-center text-body-3 text-content-tertiary">
          No encontramos la matrícula seleccionada.
        </Card>
      </ContentLayout>
    );
  }

  return (
    <ContentLayout
      title={matricula.nombre}
      titleTag={etiquetaDetalleMatricula(matricula)}
      breadcrumbs={[
        {
          label: 'Periodos de matrícula',
          to: paths.app.administrativo.matricula.getHref(),
        },
        {
          label: `Matrícula ${matricula.anio}`,
          to: paths.app.administrativo.matriculasDelPeriodo.getHref(periodoId),
        },
        matricula.nombre,
      ]}
      subtitle={`DNI ${matricula.numeroDocumento} · ${matricula.tipo === 'NUEVO' ? 'Nuevo' : 'Continuador'} · ${matricula.grado} · Periodo ${matricula.anio}${matricula.codigo ? ` · ${matricula.codigo}` : ''}`}
    >
      <DetalleMatricula
        matricula={matricula}
        periodoId={periodoId}
        pagoMatricula={<PagoMatriculaCard matriculaId={matricula.id} />}
      />
    </ContentLayout>
  );
};

export default AdministrativoMatriculaDetalleRoute;
