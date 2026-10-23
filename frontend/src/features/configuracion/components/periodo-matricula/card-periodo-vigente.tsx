import { Eye, Settings } from 'lucide-react';
import * as React from 'react';
import { Link } from 'react-router';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { SelectableTag } from '@/components/ui/tag';
import { Tooltip } from '@/components/ui/tooltip';
import { paths } from '@/config/paths';

import {
  usePeriodoMatriculaVigente,
  usePeriodosMatriculaTodos,
} from '../../api/periodos-matricula';

import { DialogoResumenConfiguracion } from './dialogo-resumen-configuracion';
import {
  MATRICULAS_DEL_PERIODO_DISPONIBLE,
  TEXTO_DISPONIBLE_PRONTO,
} from './enlaces';
import { fechaLima, lineaDeApertura, matriculasDeVacantes } from './formato';

const Dato = ({ etiqueta, valor }: { etiqueta: string; valor: string }) => (
  <div className="flex min-w-0 flex-col gap-1">
    <dt className="text-body-4 text-content-tertiary">{etiqueta}</dt>
    <dd className="text-label-1 font-semibold text-content-primary">{valor}</dd>
  </div>
);

// Figma > pantalla 13 > tarjeta "Vigente": el periodo abierto ahora o, si no hay, el próximo programado. No se
// muestra si no hay uno (y entonces ni siquiera se pide /vigente, que respondería 404).
export const CardPeriodoVigente = () => {
  const [verConfiguracion, setVerConfiguracion] = React.useState(false);
  const todos = usePeriodosMatriculaTodos();
  const hayVigente = !!todos.data?.data.some((fila) => fila.vigente);
  const vigente = usePeriodoMatriculaVigente(hayVigente);

  if (!hayVigente || !vigente.data) return null;
  const periodo = vigente.data;

  return (
    <Card className="flex flex-col gap-5 p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex min-w-0 flex-col gap-1">
          <div className="flex items-center gap-3">
            <h2 className="text-h5 font-semibold text-content-primary">
              {periodo.nombre}
            </h2>
            <SelectableTag readOnly shape="circle" size="md" color="success">
              Vigente
            </SelectableTag>
          </div>
          <p className="text-body-4 text-content-tertiary">
            {lineaDeApertura(periodo)}
          </p>
        </div>
        <div className="flex gap-3">
          <Button
            variant="tertiary"
            icon={<Settings className="size-4" aria-hidden="true" />}
            onClick={() => setVerConfiguracion(true)}
          >
            Ver configuración
          </Button>
          {MATRICULAS_DEL_PERIODO_DISPONIBLE ? (
            <Button asChild variant="secondary">
              <Link
                to={paths.app.administrativo.matriculasDelPeriodo.getHref(
                  periodo.id,
                )}
              >
                <Eye className="size-4" aria-hidden="true" />
                <span className="px-1">Ver matrículas</span>
              </Link>
            </Button>
          ) : (
            <Tooltip
              content={TEXTO_DISPONIBLE_PRONTO}
              side="bottom"
              align="end"
            >
              <Button
                variant="secondary"
                aria-disabled="true"
                icon={<Eye className="size-4" aria-hidden="true" />}
              >
                Ver matrículas
              </Button>
            </Tooltip>
          )}
        </div>
      </div>

      <dl className="grid gap-4 border-t border-stroke-tertiary pt-5 sm:grid-cols-2 lg:grid-cols-4">
        <Dato etiqueta="Año escolar" valor={String(periodo.anio)} />
        <Dato
          etiqueta="Inicio de matrícula"
          valor={fechaLima(periodo.inicio)}
        />
        <Dato etiqueta="Fin de matrícula" valor={fechaLima(periodo.cierre)} />
        <Dato
          etiqueta="Matrículas registradas"
          valor={matriculasDeVacantes(
            periodo.matriculasRegistradas,
            periodo.vacantesTotales,
          )}
        />
      </dl>

      <DialogoResumenConfiguracion
        abierto={verConfiguracion}
        onCerrar={() => setVerConfiguracion(false)}
        periodo={periodo}
      />
    </Card>
  );
};
