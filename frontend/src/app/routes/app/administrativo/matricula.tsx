import { useSearchParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';
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
  return (
    <ContentLayout title="Matrícula">
      <ModulePlaceholder
        feature="matricula"
        historias={[
          'HU-21',
          'HU-22',
          'HU-23',
          'HU-24',
          'HU-29',
          'HU-31',
          'HU-36',
          'HU-71',
          'HU-72',
        ]}
        figmaNodeId="823:6028"
      />
    </ContentLayout>
  );
};

export default AdministrativoMatriculaRoute;
