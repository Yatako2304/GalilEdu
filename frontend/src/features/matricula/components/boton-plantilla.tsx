import { Download } from 'lucide-react';

import { Button } from '@/components/ui/button';

/** "Plantilla" de un documento de matrícula: descarga el archivo que se firma y se vuelve a subir. */
export const BotonPlantilla = ({
  documento,
  alDescargar,
}: {
  documento: string;
  alDescargar: () => Promise<void>;
}) => (
  <Button
    type="button"
    variant="ghost"
    size="sm"
    icon={<Download className="size-4" aria-hidden="true" />}
    aria-label={`Descargar plantilla de ${documento}`}
    onClick={() => void alDescargar()}
  >
    Plantilla
  </Button>
);
