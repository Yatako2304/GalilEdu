import { useSearchParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { PasarelaSimulada } from '@/features/pagos/components/pasarela-simulada';

// Página de pago de la pasarela simulada (solo sin Stripe): /app/apoderado/pensiones/pasarela-simulada?orden=…
const ApoderadoPasarelaSimuladaRoute = () => {
  const [params] = useSearchParams();
  return (
    <ContentLayout title="Pago en línea" breadcrumbs={['Pensiones', 'Pago']}>
      <PasarelaSimulada codigo={params.get('orden') ?? ''} />
    </ContentLayout>
  );
};

export default ApoderadoPasarelaSimuladaRoute;
