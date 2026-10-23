import { Button, ButtonProps } from '@/components/ui/button';
import { Tooltip } from '@/components/ui/tooltip';

import { MOTIVO_ANIO_CERRADO } from './formato';

type BotonSeccionProps = ButtonProps & {
  /** Con el año CERRADO el botón se ve deshabilitado y un tooltip dice por qué. */
  bloqueado: boolean;
};

// Figma > CI-09: "Nueva sección" y "Agregar sección". aria-disabled (y no disabled) deja el botón enfocable, así el
// tooltip también sale con el teclado (igual que "Activar año").
export const BotonSeccion = ({
  bloqueado,
  onClick,
  ...props
}: BotonSeccionProps) =>
  bloqueado ? (
    <Tooltip content={MOTIVO_ANIO_CERRADO} side="bottom" align="end">
      <Button {...props} aria-disabled="true" />
    </Tooltip>
  ) : (
    <Button {...props} onClick={onClick} />
  );
