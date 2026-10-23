import { ContentLayout } from '@/components/layouts';
import { paths } from '@/config/paths';
import { ConfiguracionPensiones } from '@/features/configuracion/components/tarifario/configuracion-pensiones';

// Figma > Personal Administrativo > "P19 · Configuración › Tarifario y calendario" (HU-34)
const AdministrativoPensionesConfiguracionRoute = () => {
  return (
    <ContentLayout
      title="Configuración de pensiones"
      subtitle="Define los montos, la política de morosidad y el calendario de cuotas de cada año escolar. Una vez confirmado, el tarifario se usa para generar los compromisos de pago."
      breadcrumbs={[
        {
          label: 'Pensiones',
          to: paths.app.administrativo.pensiones.getHref(),
        },
        'Configuración',
        'Tarifario y calendario',
      ]}
    >
      <ConfiguracionPensiones />
    </ContentLayout>
  );
};

export default AdministrativoPensionesConfiguracionRoute;
