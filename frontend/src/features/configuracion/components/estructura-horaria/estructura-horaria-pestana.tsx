import { ModulePlaceholder } from '@/components/module-placeholder';
import { EstadoAnioEscolar } from '@/types/api';

export type EstructuraHorariaPestanaProps = {
  anioId: string;
  estadoAnio: EstadoAnioEscolar;
};

// Figma > Personal Administrativo > CI-12 · Estructura horaria (HU-44).
// Provisional: quien implemente HU-44 reemplaza el contenido de este archivo. La cabecera, las pestañas y la ruta
// del año ya están hechas; esta pestaña solo recibe el año.
export const EstructuraHorariaPestana = ({
  anioId,
  estadoAnio,
}: EstructuraHorariaPestanaProps) => (
  <div data-anio-id={anioId} data-estado-anio={estadoAnio}>
    <ModulePlaceholder
      feature="configuracion"
      historias={['HU-44']}
      figmaNodeId="850:10152"
    />
  </div>
);
