import { Plus, Settings } from 'lucide-react';
import { Link } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { Button } from '@/components/ui/button';
import { paths } from '@/config/paths';
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
          <Button asChild variant="tertiary">
            <Link to={paths.app.administrativo.tarifario.getHref()}>
              <Settings className="size-4" aria-hidden="true" />
              Configuración
            </Link>
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
