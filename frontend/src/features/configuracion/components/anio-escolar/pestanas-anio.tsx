import { Tabs } from '@/components/ui/tabs';
import { AnioEscolarDetalle } from '@/types/api';

export type PestanaAnio = 'periodos' | 'secciones' | 'estructura-horaria';

type PestanasAnioProps = {
  anio: Pick<
    AnioEscolarDetalle,
    'periodos' | 'cantidadSecciones' | 'cantidadEstructuras'
  >;
  value: PestanaAnio;
  onValueChange: (pestana: PestanaAnio) => void;
};

// Figma > CI-03, CI-06, CI-09 y CI-12: Tabs v2 "Periodos (4)", "Secciones (23)" y "Estructura horaria (2)".
export const PestanasAnio = ({
  anio,
  value,
  onValueChange,
}: PestanasAnioProps) => (
  <Tabs
    variant="pill"
    size="md"
    aria-label="Secciones del año escolar"
    value={value}
    onValueChange={onValueChange}
    items={[
      { value: 'periodos', label: `Periodos (${anio.periodos.length})` },
      { value: 'secciones', label: `Secciones (${anio.cantidadSecciones})` },
      {
        value: 'estructura-horaria',
        label: `Estructura horaria (${anio.cantidadEstructuras})`,
      },
    ]}
  />
);
