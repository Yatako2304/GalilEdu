import { Plus, Settings } from 'lucide-react';

import { ContentLayout } from '@/components/layouts';
import { Button } from '@/components/ui/button';
import { EstadoCuenta } from '@/features/pagos/components/estado-cuenta';

// Figma > Personal Administrativo > "P2 · Estado de cuenta — Todos los alumnos" (HU-37)
const AdministrativoPensionesRoute = () => {
  return (
    <ContentLayout
      title="Estado de cuenta"
      breadcrumbs={['Pensiones', 'Estado de cuenta']}
      subtitle="Consulta los compromisos de pago de los alumnos, su historial por año escolar y las cuotas vencidas."
      actions={
        <>
          <Button
            variant="tertiary"
            disabled
            icon={<Settings className="size-4" />}
          >
            Configuración
          </Button>
          <Button variant="primary" disabled icon={<Plus className="size-4" />}>
            Cuota adicional masiva
          </Button>
        </>
      }
    >
      <EstadoCuenta />
    </ContentLayout>
  );
};

export default AdministrativoPensionesRoute;
