import { ModulePlaceholder } from '@/components/module-placeholder';
import { EstadoAnioEscolar } from '@/types/api';

export type SeccionesPestanaProps = {
  anioId: string;
  estadoAnio: EstadoAnioEscolar;
};

// Figma > Personal Administrativo > CI-09 · Secciones por grado y tutores (HU-75).
// Provisional: quien implemente HU-75 reemplaza el contenido de este archivo. La cabecera, las pestañas y la ruta
// del año ya están hechas; esta pestaña solo recibe el año.
export const SeccionesPestana = ({
  anioId,
  estadoAnio,
}: SeccionesPestanaProps) => (
  <div data-anio-id={anioId} data-estado-anio={estadoAnio}>
    <ModulePlaceholder
      feature="configuracion"
      historias={['HU-75']}
      figmaNodeId="849:9225"
    />
  </div>
);
