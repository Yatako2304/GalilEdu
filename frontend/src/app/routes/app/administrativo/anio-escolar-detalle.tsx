import { useParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { Alert } from '@/components/ui/alert';
import { Spinner } from '@/components/ui/spinner';
import { paths } from '@/config/paths';
import { useAnioEscolar } from '@/features/configuracion/api/anios-escolares';
import { AccionesAnio } from '@/features/configuracion/components/anio-escolar/acciones-anio';
import { ContenidoAnio } from '@/features/configuracion/components/anio-escolar/contenido-anio';
import { EstadoAnioTag } from '@/features/configuracion/components/anio-escolar/encabezado-anio';
import { subtituloAnio } from '@/features/configuracion/components/anio-escolar/formato';

const MIGA_ANIOS = {
  label: 'Año escolar',
  to: paths.app.administrativo.anioEscolar.getHref(),
};

// Figma > Personal Administrativo > CI-03 / CI-06 / CI-09 / CI-12: un año con sus pestañas (HU-74, HU-75, HU-44).
const AdministrativoAnioEscolarDetalleRoute = () => {
  const { anioId } = useParams();
  const anio = useAnioEscolar(anioId);

  if (anio.data) {
    return (
      <ContentLayout
        title={`Año escolar ${anio.data.anio}`}
        titleTag={<EstadoAnioTag estado={anio.data.estado} />}
        subtitle={subtituloAnio(anio.data)}
        breadcrumbs={['Configuración', MIGA_ANIOS, String(anio.data.anio)]}
        actions={<AccionesAnio anio={anio.data} />}
      >
        <ContenidoAnio key={anio.data.id} anio={anio.data} />
      </ContentLayout>
    );
  }

  return (
    <ContentLayout
      title="Año escolar"
      breadcrumbs={['Configuración', MIGA_ANIOS, '…']}
    >
      {anio.isError ? (
        <Alert type="error" title="No encontramos ese año escolar.">
          Vuelve al listado y elige otro año.
        </Alert>
      ) : (
        <div className="flex h-60 items-center justify-center">
          <Spinner />
        </div>
      )}
    </ContentLayout>
  );
};

export default AdministrativoAnioEscolarDetalleRoute;
