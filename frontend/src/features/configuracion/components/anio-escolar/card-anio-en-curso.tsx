import { ArrowRight, CalendarCheck } from 'lucide-react';
import { Link } from 'react-router';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { IconBadge } from '@/components/ui/icon-badge';
import { ProgressBar } from '@/components/ui/progress-bar';
import { paths } from '@/config/paths';
import { AnioEscolarActual } from '@/types/api';
import { formatDay } from '@/utils/format';

import { DatoResumenCard } from './dato-resumen';
import { EstadoAnioTag } from './encabezado-anio';
import {
  alumnosMatriculados,
  codigosDeEstructuras,
  datosDePeriodos,
  nombreRegimen,
} from './formato';

type CardAnioEnCursoProps = { actual: AnioEscolarActual };

// Figma > CI-01 · Año escolar > "Card - Año en curso": el año ACTIVO con sus datos y su avance.
export const CardAnioEnCurso = ({ actual }: CardAnioEnCursoProps) => {
  const { periodo, limite } = datosDePeriodos(actual);

  return (
    <Card className="flex flex-col gap-[18px] p-6">
      <div className="flex flex-wrap items-center gap-4">
        <IconBadge color="accent" shape="square" size="xl" aria-hidden="true">
          <CalendarCheck />
        </IconBadge>
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <p className="text-body-4 text-content-tertiary">
            Año escolar en curso
          </p>
          <div className="flex items-center gap-2.5">
            <h2 className="text-h6 font-semibold text-content-primary">
              Año escolar {actual.anio}
            </h2>
            <EstadoAnioTag estado="ACTIVO" />
          </div>
          <p className="text-body-4 text-content-tertiary">
            Del {formatDay(actual.fechaInicio)} al {formatDay(actual.fechaFin)}{' '}
            · Régimen {nombreRegimen(actual.regimen).toLowerCase()}
          </p>
        </div>
        <Button asChild variant="secondary">
          <Link
            to={paths.app.administrativo.anioEscolarDetalle.getHref(actual.id)}
          >
            <ArrowRight className="size-4" aria-hidden="true" />
            <span className="px-1">Ver detalle</span>
          </Link>
        </Button>
      </div>

      <div className="flex flex-wrap gap-3">
        <DatoResumenCard {...periodo} />
        <DatoResumenCard {...limite} />
        <DatoResumenCard
          etiqueta="Secciones activas"
          valor={String(actual.seccionesActivas)}
          detalle={alumnosMatriculados(actual.alumnosMatriculados)}
        />
        <DatoResumenCard
          etiqueta="Estructuras horarias"
          valor={String(actual.estructurasHorarias)}
          detalle={codigosDeEstructuras(actual.codigosEstructuras)}
        />
      </div>

      <ProgressBar value={actual.avance} label="Avance del año escolar" />
    </Card>
  );
};
