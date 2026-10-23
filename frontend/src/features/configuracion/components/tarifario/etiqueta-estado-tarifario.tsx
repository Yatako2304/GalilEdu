import { SelectableTag } from '@/components/ui/tag';
import { EstadoTarifario } from '@/types/api';

import { ESTADO_TARIFARIO } from './formato';

// Figma > P19 > etiqueta junto al selector de año ("En configuración"): pastilla informativa (Selectable Tag, solo lectura).
export const EtiquetaEstadoTarifario = ({
  estado,
}: {
  estado: EstadoTarifario;
}) => {
  const { texto, color } = ESTADO_TARIFARIO[estado];
  return (
    <SelectableTag readOnly shape="circle" size="sm" color={color}>
      {texto}
    </SelectableTag>
  );
};
