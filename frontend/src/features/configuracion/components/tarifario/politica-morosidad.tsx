import { UseFormReturn } from 'react-hook-form';

import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/form';

import { ValoresTarifario } from './esquemas';

type PoliticaMorosidadProps = {
  form: UseFormReturn<ValoresTarifario>;
  deshabilitado: boolean;
};

// Figma > P19 > tarjeta "Política de morosidad": días de gracia, recargo cada 30 días y tope de recargo.
export const PoliticaMorosidad = ({
  form,
  deshabilitado,
}: PoliticaMorosidadProps) => {
  const { register, formState } = form;
  const errores = formState.errors;
  return (
    <Card className="flex flex-col gap-4 p-6">
      <h2 className="text-label-1 font-semibold text-content-primary">
        Política de morosidad
      </h2>
      <div className="grid gap-4 md:grid-cols-3">
        <Input
          label="Días de gracia *"
          type="number"
          inputMode="numeric"
          min={0}
          disabled={deshabilitado}
          registration={register('diasGracia')}
          error={errores.diasGracia}
        />
        <Input
          label="Recargo por cada 30 días (%) *"
          type="number"
          inputMode="decimal"
          step="0.01"
          min={0}
          disabled={deshabilitado}
          registration={register('recargoPorcentaje')}
          error={errores.recargoPorcentaje}
        />
        <Input
          label="Tope de recargo (%) *"
          type="number"
          inputMode="decimal"
          step="0.01"
          min={0}
          disabled={deshabilitado}
          registration={register('topeRecargoPorcentaje')}
          error={errores.topeRecargoPorcentaje}
        />
      </div>
      <p className="text-body-4 text-content-tertiary">
        El recargo se calcula sobre el capital pendiente de la cuota a partir
        del día siguiente al periodo de gracia y no supera el tope.
      </p>
    </Card>
  );
};
