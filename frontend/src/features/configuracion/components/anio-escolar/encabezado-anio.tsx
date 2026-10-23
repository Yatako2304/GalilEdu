import { Tag } from '@/components/ui/tag';
import { EstadoAnioEscolar } from '@/types/api';

import { ESTADO_ANIO } from './formato';

// Figma > CI-03, CI-06, CI-09 y CI-12: etiqueta de estado junto al título "Año escolar 2026".
export const EstadoAnioTag = ({ estado }: { estado: EstadoAnioEscolar }) => {
  const { texto, color } = ESTADO_ANIO[estado];
  return (
    <Tag color={color} size="md">
      {texto}
    </Tag>
  );
};
