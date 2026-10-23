import { SelectableTag } from '@/components/ui/tag';
import { EstadoVisiblePeriodoMatricula } from '@/types/api';

import { ESTADO_PERIODO_MATRICULA } from './formato';

// Figma > pantalla 13 > estado de un periodo: pastilla con borde (Selectable Tag, solo informativa).
export const EstadoPeriodoMatricula = ({
  estado,
}: {
  estado: EstadoVisiblePeriodoMatricula;
}) => {
  const { texto, color } = ESTADO_PERIODO_MATRICULA[estado];
  return (
    <SelectableTag readOnly shape="circle" size="md" color={color}>
      {texto}
    </SelectableTag>
  );
};
