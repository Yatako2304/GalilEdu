import { ContentLayout } from '@/components/layouts';
import { PensionesApoderado } from '@/features/pagos/components/pensiones-apoderado';

const ApoderadoPensionesRoute = () => {
  return (
    <ContentLayout
      title="Pensiones"
      subtitle="Consulta el cronograma de cuotas, paga las pendientes y descarga tus constancias."
      breadcrumbs={['Pensiones', 'Cronograma de cuotas']}
    >
      <PensionesApoderado />
    </ContentLayout>
  );
};

export default ApoderadoPensionesRoute;
