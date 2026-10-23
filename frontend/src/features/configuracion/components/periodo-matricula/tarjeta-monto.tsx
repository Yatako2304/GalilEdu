import { Link } from 'react-router';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Tooltip } from '@/components/ui/tooltip';
import { paths } from '@/config/paths';

import { TARIFARIO_DISPONIBLE, TEXTO_DISPONIBLE_PRONTO } from './enlaces';
import { soles } from './formato';

/** Enlace a P19 (tarifario). Mientras esa pantalla no exista queda deshabilitado, con el aviso "Disponible pronto". */
const EnlaceTarifario = ({
  texto,
  variant,
}: {
  texto: string;
  variant: 'outline' | 'secondary';
}) =>
  TARIFARIO_DISPONIBLE ? (
    <Button asChild variant={variant} size="sm">
      <Link to={paths.app.administrativo.tarifario.getHref()}>{texto}</Link>
    </Button>
  ) : (
    <Tooltip content={TEXTO_DISPONIBLE_PRONTO} side="bottom" align="end">
      <Button variant={variant} size="sm" aria-disabled="true">
        {texto}
      </Button>
    </Tooltip>
  );

type TarjetaMontoProps = {
  /** Año elegido; null mientras no se elige. */
  anio: number | null;
  /** null si el año no tiene tarifario confirmado o aún no se elige. */
  monto: number | null;
  tarifarioConfirmado: boolean;
};

// Figma > pantalla 13 > formulario > tarjeta "Monto de matrícula". Sale del tarifario confirmado del año: aquí no se
// edita. Sin tarifario confirmado no se puede habilitar la matrícula (el backend responde 409).
export const TarjetaMonto = ({
  anio,
  monto,
  tarifarioConfirmado,
}: TarjetaMontoProps) => (
  <Card className="flex flex-col gap-4 p-6">
    <div className="border-b border-stroke-tertiary pb-4">
      <h2 className="text-label-1 font-semibold text-content-primary">
        Monto de matrícula
      </h2>
      <p className="text-body-4 text-content-tertiary">
        Se toma del tarifario vigente{anio === null ? '' : ` ${anio}`}. No se
        edita aquí.
      </p>
    </div>

    {anio !== null && !tarifarioConfirmado ? (
      <Alert
        type="warning"
        title={`Confirma el tarifario ${anio} antes de habilitar la matrícula.`}
        action={<EnlaceTarifario texto="Ir al tarifario" variant="secondary" />}
      />
    ) : (
      <>
        <p className="flex items-baseline gap-2 text-content-primary">
          <span className="text-h6 font-semibold">S/</span>
          <span className="text-[48px] font-bold leading-none">
            {monto === null ? '-' : soles(monto).replace('S/ ', '')}
          </span>
        </p>
        <div>
          <EnlaceTarifario texto="Ver tarifario completo" variant="outline" />
        </div>
      </>
    )}
  </Card>
);
