import * as React from 'react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/form';

const MAXIMO = 500;

// Observar un documento: el colegio explica qué debe corregir el apoderado y la matrícula vuelve a él. HU-32.
export const DialogoObservarDocumento = ({
  documento,
  guardando,
  alConfirmar,
  onCerrar,
}: {
  documento: string;
  guardando: boolean;
  alConfirmar: (observacion: string) => void;
  onCerrar: () => void;
}) => {
  const [motivo, setMotivo] = React.useState('');
  const limpio = motivo.trim();

  return (
    <Dialog open onOpenChange={(abierto) => !abierto && onCerrar()}>
      <DialogContent className="flex max-w-screen-sm flex-col gap-0 overflow-hidden rounded-xl border-stroke-tertiary bg-white p-0">
        <div className="space-y-4 p-6">
          <div>
            <DialogTitle className="text-h6 font-semibold text-content-primary">
              Observar documento
            </DialogTitle>
            <DialogDescription className="mt-1 text-body-4 text-content-tertiary">
              {`Indica qué debe corregir el apoderado en «${documento}». La matrícula vuelve a él para que reemplace solo este documento.`}
            </DialogDescription>
          </div>
          <Textarea
            label="Motivo de la observación"
            value={motivo}
            maxLength={MAXIMO}
            showCount
            onChange={(e) => setMotivo(e.target.value)}
          />
        </div>
        <div className="flex justify-end gap-3 border-t border-stroke-tertiary p-4">
          <Button type="button" variant="tertiary" onClick={onCerrar}>
            Cancelar
          </Button>
          <Button
            type="button"
            disabled={!limpio || guardando}
            onClick={() => alConfirmar(limpio)}
          >
            Observar
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
