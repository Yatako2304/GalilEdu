import { useParams, useSearchParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { Card } from '@/components/ui/card';
import { paths } from '@/config/paths';
import { getMatriculaFlujo } from '@/features/matricula/components/datos-flujo-matricula';
import {
  DetalleMatricula,
  etiquetaDetalleMatricula,
} from '@/features/matricula/components/detalle-matricula';
import { PagoMatriculaCard } from '@/features/pagos/components/pago-matricula-card';

const AdministrativoMatriculaDetalleRoute = () => {
  const { matriculaId } = useParams();
  const [params] = useSearchParams();
  const periodoId = params.get('periodoId') ?? 'periodo-2027';
  const matricula = getMatriculaFlujo(matriculaId);

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
          label: 'Matrícula 2027',
          to: paths.app.administrativo.matriculasDelPeriodo.getHref(periodoId),
        },
        matricula.nombre,
      ]}
      subtitle={`DNI ${matricula.dni} · ${matricula.tipo} · ${matricula.grado} · Periodo 2027${matricula.codigo ? ` · ${matricula.codigo}` : ''}`}
    >
      <DetalleMatricula
        matricula={matricula}
        periodoId={periodoId}
        pagoMatricula={
          matricula.id.startsWith('50000000-') ? (
            <PagoMatriculaCard matriculaId={matricula.id} />
          ) : undefined
        }
      />
    </ContentLayout>
  );
};

export default AdministrativoMatriculaDetalleRoute;
