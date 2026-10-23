import { CircleAlert, CircleCheck } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Requisito } from '@/types/api';

type RequisitoCardProps = {
  requisito: Requisito;
  /** El destino de "Revisar" lo decide quien usa la tarjeta (otra pestaña o otra pantalla). */
  revisar: React.ReactElement;
};

// Figma > CI-06 > "Requisito": ícono (cumplido o pendiente), título, detalle y enlace "Revisar".
export const RequisitoCard = ({ requisito, revisar }: RequisitoCardProps) => (
  <li className="flex items-start gap-3 rounded-[10px] border border-stroke-tertiary p-3.5">
    {requisito.cumplido ? (
      <CircleCheck
        role="img"
        aria-label="Cumplido"
        className="size-[22px] shrink-0 text-growth-green-600"
      />
    ) : (
      <CircleAlert
        role="img"
        aria-label="Pendiente"
        className="size-[22px] shrink-0 text-content-warning"
      />
    )}
    <div className="flex min-w-0 flex-1 flex-col gap-[3px]">
      <p className="text-label-2 font-medium text-content-primary">
        {requisito.texto}
      </p>
      <p className="text-body-4 text-content-tertiary">{requisito.detalle}</p>
      <div>{revisar}</div>
    </div>
  </li>
);

/** El enlace "Revisar" de Figma: texto azul y pequeño. */
export const BotonRevisar = (
  props: Omit<React.ComponentProps<typeof Button>, 'variant' | 'size'>,
) => (
  <Button variant="link" size="sm" className="h-auto p-0" {...props}>
    Revisar
  </Button>
);
