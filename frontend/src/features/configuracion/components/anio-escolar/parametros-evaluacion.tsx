import { SlidersHorizontal } from 'lucide-react';
import * as React from 'react';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Select } from '@/components/ui/form';
import { useNotifications } from '@/components/ui/notifications';
import {
  AnioEscolarDetalle,
  Consolidacion,
  Escala,
  ReglaNota,
} from '@/types/api';

import { useConfigurarEvaluacion } from '../../api/mutaciones-anio';

import {
  CONSOLIDACIONES_PERIODOS,
  ESCALAS_EVALUACION,
  REGLAS_NOTA,
} from './opciones-evaluacion';

export const ParametrosEvaluacionAnio = ({
  anio,
}: {
  anio: AnioEscolarDetalle;
}) => {
  const actuales = anio.parametrosEvaluacion;
  const [escala, setEscala] = React.useState<Escala>(actuales.escalaEvaluacion);
  const [regla, setRegla] = React.useState<ReglaNota>(actuales.reglaNota);
  const [consolidacion, setConsolidacion] = React.useState<Consolidacion>(
    actuales.consolidacionPeriodos,
  );
  const { addNotification } = useNotifications();
  const guardar = useConfigurarEvaluacion(() =>
    addNotification({
      type: 'success',
      title: 'Parámetros de evaluación actualizados',
      message: `Se aplicarán a todos los cursos del año escolar ${anio.anio}.`,
    }),
  );

  React.useEffect(() => {
    setEscala(actuales.escalaEvaluacion);
    setRegla(actuales.reglaNota);
    setConsolidacion(actuales.consolidacionPeriodos);
  }, [actuales]);

  const cambiarEscala = (nueva: Escala) => {
    setEscala(nueva);
    setRegla(REGLAS_NOTA[nueva][0].value);
  };
  const hayCambios =
    escala !== actuales.escalaEvaluacion ||
    regla !== actuales.reglaNota ||
    consolidacion !== actuales.consolidacionPeriodos;

  return (
    <Card className="space-y-5 p-6">
      <div className="flex items-start gap-3">
        <div className="rounded-lg bg-surface-accent-light p-2 text-content-accent-primary">
          <SlidersHorizontal className="size-5" aria-hidden="true" />
        </div>
        <div>
          <h2 className="text-h6 font-semibold text-content-primary">
            Parámetros de evaluación
          </h2>
          <p className="text-body-3 text-content-tertiary">
            Esta configuración se aplica a todos los cursos y competencias del
            año escolar {anio.anio}.
          </p>
        </div>
      </div>

      {!actuales.editable && (
        <Alert type="info" title="Configuración cerrada">
          {actuales.motivoNoEditable}
        </Alert>
      )}

      <div className="grid gap-4 md:grid-cols-3">
        <Select
          label="Escala de evaluación"
          options={ESCALAS_EVALUACION}
          value={escala}
          disabled={!actuales.editable}
          onChange={(e) => cambiarEscala(e.target.value as Escala)}
        />
        <Select
          label={
            escala === 'VIGESIMAL'
              ? 'Cálculo del promedio'
              : 'Cálculo del nivel'
          }
          options={REGLAS_NOTA[escala]}
          value={regla}
          disabled={!actuales.editable}
          onChange={(e) => setRegla(e.target.value as ReglaNota)}
        />
        <Select
          label="Consolidación de periodos"
          options={CONSOLIDACIONES_PERIODOS}
          value={consolidacion}
          disabled={!actuales.editable}
          onChange={(e) => setConsolidacion(e.target.value as Consolidacion)}
        />
      </div>

      <p className="text-body-4 text-content-quartiary">
        La nota oficial es de cada competencia: una por periodo y una final. El
        cálculo se aplica a los ítems de cada curso y luego a los cursos que
        evalúan la misma competencia, según el peso de cada curso en el plan de
        evaluación: con promedio ponderado pondera las notas y en la escala de
        letras cada curso vota por su nivel con su peso (con promedio simple los
        cursos aportan por igual). La consolidación une los periodos en la nota
        final.
      </p>

      {actuales.editable && (
        <div className="flex justify-end">
          <Button
            disabled={!hayCambios || guardar.isPending}
            isLoading={guardar.isPending}
            onClick={() =>
              guardar.mutate({
                id: anio.id,
                data: {
                  escalaEvaluacion: escala,
                  reglaNota: regla,
                  consolidacionPeriodos: consolidacion,
                },
              })
            }
          >
            Guardar parámetros
          </Button>
        </div>
      )}
    </Card>
  );
};
