import { Select } from '@/components/ui/form';
import { AnioEscolarResumen, EstadoTarifario } from '@/types/api';

import { EtiquetaEstadoTarifario } from './etiqueta-estado-tarifario';

type SelectorAnioProps = {
  anios: AnioEscolarResumen[];
  /** Id del año elegido. */
  value: string;
  onChange: (anioEscolarId: string) => void;
  /** Estado del tarifario del año elegido; null mientras se carga. */
  estado: EstadoTarifario | null;
};

// Figma > P19 > "Año escolar 2027" con su etiqueta de estado a la derecha. Del más reciente al más antiguo.
export const SelectorAnio = ({
  anios,
  value,
  onChange,
  estado,
}: SelectorAnioProps) => (
  <div className="flex flex-wrap items-center gap-4">
    <Select
      aria-label="Año escolar"
      options={[...anios]
        .sort((a, b) => b.anio - a.anio)
        .map((anio) => ({ label: `Año escolar ${anio.anio}`, value: anio.id }))}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      wrapperClassName="w-60"
      className="h-10"
    />
    {estado && <EtiquetaEstadoTarifario estado={estado} />}
  </div>
);
