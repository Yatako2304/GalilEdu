import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';
import { PagoCuotas } from '@/features/pagos/components/pago-cuotas';

const ApoderadoPensionesRoute = () => {
  return (
    <ContentLayout title="Pensiones">
      <div className="space-y-6">
        {/* HU-41 · Pago en línea. Cuando exista la tabla del cronograma (HU-40), sus casillas usan
            useSeleccionCuotas y BarraPagoCuotas, y esta sección se reemplaza por esa tabla. */}
        <PagoCuotas />
        <ModulePlaceholder
          feature="pensiones"
          historias={['HU-40', 'HU-42']}
          figmaNodeId="362:6105"
        />
      </div>
    </ContentLayout>
  );
};

export default ApoderadoPensionesRoute;
