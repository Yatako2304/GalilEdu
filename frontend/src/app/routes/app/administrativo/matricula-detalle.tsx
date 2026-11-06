import { useParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { Card } from '@/components/ui/card';
import { paths } from '@/config/paths';
import { useDetalleMatricula } from '@/features/matricula/api/matricula-detalle';
import {
  DetalleMatricula,
  etiquetaDetalleMatricula,
} from '@/features/matricula/components/detalle-matricula';
import { PagoMatriculaCard } from '@/features/pagos/components/pago-matricula-card';

const AdministrativoMatriculaDetalleRoute = () => {
  const { matriculaId } = useParams();
  const { data: matricula, isLoading } = useDetalleMatricula(matriculaId);

  if (!matricula) {
    return (
      <ContentLayout title="Detalle de matrícula">
        <Card className="p-8 text-center text-body-3 text-content-tertiary">
          {isLoading
            ? 'Cargando la matrícula…'
            : 'No encontramos la matrícula seleccionada.'}
        </Card>
      </ContentLayout>
    );
  }

  return (
    <ContentLayout
      title={matricula.nombre}
      titleTag={etiquetaDetalleMatricula(matricula.estado, matricula.codigo)}
      breadcrumbs={[
        {
          label: 'Periodos de matrícula',
          to: paths.app.administrativo.matricula.getHref(),
        },
        {
          label: `Matrícula ${matricula.anio}`,
          to: paths.app.administrativo.matriculasDelPeriodo.getHref(
            matricula.periodoId,
          ),
        },
        matricula.nombre,
      ]}
      subtitle={`DNI ${matricula.documento} · ${matricula.tipo === 'NUEVO' ? 'Nuevo' : 'Continuador'} · ${matricula.grado} · Periodo ${matricula.anio}${matricula.codigo ? ` · ${matricula.codigo}` : ''}`}
    >
      <DetalleMatricula
        matricula={matricula}
        pagoMatricula={<PagoMatriculaCard matriculaId={matricula.id} />}
      />
    </ContentLayout>
  );
};

export default AdministrativoMatriculaDetalleRoute;
