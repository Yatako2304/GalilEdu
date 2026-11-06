import { Plus } from 'lucide-react';
import * as React from 'react';

import { ContentLayout } from '@/components/layouts';
import { Button } from '@/components/ui/button';
import { paths } from '@/config/paths';
import {
  ConfiguracionPensiones,
  PestanaConfiguracionPensiones,
} from '@/features/configuracion/components/tarifario/configuracion-pensiones';
import { CatalogoDescuentos } from '@/features/pagos/components/catalogo-descuentos';

// Figma > Personal Administrativo > "P19 · Configuración › Tarifario y calendario" (HU-34)
const AdministrativoPensionesConfiguracionRoute = () => {
  const [pestana, setPestana] =
    React.useState<PestanaConfiguracionPensiones>('tarifario');
  const [nuevoAbierto, setNuevoAbierto] = React.useState(false);

  const esCatalogo = pestana === 'descuentos';

  return (
    <ContentLayout
      title="Configuración de pensiones"
      subtitle={
        esCatalogo
          ? 'Administra los tipos de descuento que el personal puede aplicar a los compromisos de pago.'
          : 'Define los montos, la política de morosidad y el calendario de cuotas de cada año escolar. Una vez confirmado, el tarifario se usa para generar los compromisos de pago.'
      }
      breadcrumbs={[
        {
          label: 'Pensiones',
          to: paths.app.administrativo.pensiones.getHref(),
        },
        'Configuración',
        esCatalogo ? 'Catálogo de descuentos' : 'Tarifario y calendario',
      ]}
      actions={
        esCatalogo ? (
          <Button
            icon={<Plus className="size-4" />}
            onClick={() => setNuevoAbierto(true)}
          >
            Nuevo descuento
          </Button>
        ) : undefined
      }
    >
      <ConfiguracionPensiones
        pestana={pestana}
        onPestanaChange={setPestana}
        catalogoDescuentos={
          <CatalogoDescuentos
            nuevoAbierto={nuevoAbierto}
            onNuevoAbiertoChange={setNuevoAbierto}
          />
        }
      />
    </ContentLayout>
  );
};

export default AdministrativoPensionesConfiguracionRoute;
