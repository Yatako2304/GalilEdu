import { ArrowLeftRight } from 'lucide-react';
import * as React from 'react';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { RadioCard } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { useNotifications } from '@/components/ui/notifications';
import { AnioEscolarDetalle, Regimen } from '@/types/api';

import { useCambiarRegimen } from '../../api/mutaciones-anio';

import { nombreRegimen, periodosAlAnio } from './formato';

const REGIMENES: Regimen[] = ['BIMESTRAL', 'TRIMESTRAL'];

type DialogoCambiarRegimenProps = {
  abierto: boolean;
  onCerrar: () => void;
  anio: Pick<AnioEscolarDetalle, 'id' | 'anio' | 'regimen'>;
};

// Figma > Personal Administrativo > "Modal - Cambiar régimen de periodos" (HU-74).
export const DialogoCambiarRegimen = ({
  abierto,
  onCerrar,
  anio,
}: DialogoCambiarRegimenProps) => (
  <Dialog open={abierto} onOpenChange={(open) => !open && onCerrar()}>
    <DialogContent className="flex max-h-[92vh] max-w-[560px] flex-col gap-0 overflow-hidden rounded-[12px] border-stroke-tertiary bg-white p-0">
      <FormularioRegimen anio={anio} onCerrar={onCerrar} />
    </DialogContent>
  </Dialog>
);

const FormularioRegimen = ({
  anio,
  onCerrar,
}: Pick<DialogoCambiarRegimenProps, 'anio' | 'onCerrar'>) => {
  const { addNotification } = useNotifications();
  // Se abre con el otro régimen marcado, como en Figma: cambiarlo al mismo no tendría sentido.
  const [elegido, setElegido] = React.useState<Regimen>(
    anio.regimen === 'BIMESTRAL' ? 'TRIMESTRAL' : 'BIMESTRAL',
  );

  const cambiar = useCambiarRegimen((actualizado) => {
    addNotification({
      type: 'success',
      title: 'Régimen actualizado',
      message: `Año escolar ${actualizado.anio} · Régimen ${nombreRegimen(actualizado.regimen).toLowerCase()}`,
    });
    onCerrar();
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        cambiar.mutate({ id: anio.id, regimen: elegido });
      }}
      className="flex min-h-0 flex-1 flex-col"
    >
      <div className="flex-1 space-y-5 overflow-y-auto p-6">
        <div className="space-y-3">
          <IconBadge color="accent" size="lg" aria-hidden="true">
            <ArrowLeftRight />
          </IconBadge>
          <div className="space-y-2">
            <DialogTitle className="text-h6 font-semibold text-content-primary">
              Cambiar régimen de periodos
            </DialogTitle>
            <DialogDescription className="text-body-3 text-content-secondary">
              Año escolar {anio.anio}
            </DialogDescription>
          </div>
        </div>

        <fieldset className="grid gap-3 sm:grid-cols-2">
          <legend className="sr-only">Régimen de periodos</legend>
          {REGIMENES.map((regimen) => (
            <RadioCard
              key={regimen}
              name="regimen"
              value={regimen}
              label={nombreRegimen(regimen)}
              helperText={`${periodosAlAnio(regimen)}${regimen === anio.regimen ? ' (actual)' : ''}`}
              checked={elegido === regimen}
              onChange={() => setElegido(regimen)}
            />
          ))}
        </fieldset>

        <Alert
          type="warning"
          title="Se volverán a generar los periodos con fechas sugeridas."
        >
          Luego podrás ajustar las fechas de cada periodo.
        </Alert>
      </div>

      <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
        <Button type="button" variant="tertiary" onClick={onCerrar}>
          Cancelar
        </Button>
        <Button
          type="submit"
          isLoading={cambiar.isPending}
          disabled={cambiar.isPending || elegido === anio.regimen}
        >
          Cambiar régimen
        </Button>
      </div>
    </form>
  );
};
