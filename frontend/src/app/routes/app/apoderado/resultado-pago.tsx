import { useSearchParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { ResultadoPago } from '@/features/pagos/components/resultado-pago';

// HU-41 · Regreso desde la pasarela: /app/apoderado/pensiones/pago?orden=OP-2026-000001[&cancelado=1]
const ApoderadoResultadoPagoRoute = () => {
  const [params] = useSearchParams();
  const codigo = params.get('orden') ?? '';
  return (
    <ContentLayout title="Pago en línea" breadcrumbs={['Pensiones', 'Pago']}>
      <ResultadoPago codigo={codigo} cancelado={params.has('cancelado')} />
    </ContentLayout>
  );
};

export default ApoderadoResultadoPagoRoute;
