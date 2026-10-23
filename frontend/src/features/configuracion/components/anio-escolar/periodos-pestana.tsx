import { ArrowLeftRight } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { AnioEscolarDetalle, PeriodoDetalle } from '@/types/api';

import { DialogoCambiarRegimen } from './dialogo-cambiar-regimen';
import { DialogoEditarPeriodo } from './dialogo-editar-periodo';
import { nombreRegimen } from './formato';
import { LineaDeTiempo } from './linea-de-tiempo';
import { PeriodosTabla } from './periodos-tabla';

type PeriodosPestanaProps = { anio: AnioEscolarDetalle };

// Figma > CI-03 y CI-06 > "Card - Periodos": línea de tiempo y tabla de los periodos académicos del año.
export const PeriodosPestana = ({ anio }: PeriodosPestanaProps) => {
  const [cambiandoRegimen, setCambiandoRegimen] = React.useState(false);
  const [periodoEnEdicion, setPeriodoEnEdicion] =
    React.useState<PeriodoDetalle | null>(null);

  const planificado = anio.estado === 'PLANIFICADO';
  const cerrado = anio.estado === 'CERRADO';

  return (
    <Card className="flex flex-col gap-4 p-6">
      <div className="flex items-start gap-4">
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <h2 className="text-label-1 font-semibold text-content-primary">
            Periodos académicos
          </h2>
          <p className="text-body-4 text-content-tertiary">
            Régimen {nombreRegimen(anio.regimen).toLowerCase()}: el año se
            divide en {anio.periodos.length} periodos.{' '}
            {planificado
              ? 'Mientras el año esté planificado puedes cambiar el régimen.'
              : 'La fecha límite de notas es el último día en que los docentes pueden registrar calificaciones del periodo.'}
          </p>
        </div>
        {planificado && (
          <Button
            size="sm"
            variant="secondary"
            icon={<ArrowLeftRight className="size-3.5" aria-hidden="true" />}
            onClick={() => setCambiandoRegimen(true)}
          >
            Cambiar régimen
          </Button>
        )}
      </div>

      <LineaDeTiempo anio={anio} />

      <PeriodosTabla
        periodos={anio.periodos}
        editable={!cerrado}
        onEditar={setPeriodoEnEdicion}
      />

      <DialogoCambiarRegimen
        abierto={cambiandoRegimen}
        onCerrar={() => setCambiandoRegimen(false)}
        anio={anio}
      />
      {periodoEnEdicion && (
        <DialogoEditarPeriodo
          key={periodoEnEdicion.id}
          periodo={periodoEnEdicion}
          anio={anio}
          onCerrar={() => setPeriodoEnEdicion(null)}
        />
      )}
    </Card>
  );
};
