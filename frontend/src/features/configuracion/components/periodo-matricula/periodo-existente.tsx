import { Navigate } from 'react-router';

import { Alert } from '@/components/ui/alert';
import { Spinner } from '@/components/ui/spinner';
import { paths } from '@/config/paths';

import { usePeriodoMatricula } from '../../api/periodos-matricula';

import { FormularioPeriodo } from './formulario-periodo';

type PeriodoExistenteProps = {
  periodoId: string | undefined;
  /** "ver": solo lectura. "editar": un periodo que ya no está programado se ve en solo lectura. */
  modo: 'ver' | 'editar';
};

// Figma > pantalla 13 > "Ver" (solo lectura) y edición de un periodo (HU-23). Solo un periodo PROGRAMADO se edita: el
// de un ABIERTO redirige a "Ver", donde el aviso explica por qué.
export const PeriodoExistente = ({
  periodoId,
  modo,
}: PeriodoExistenteProps) => {
  const periodo = usePeriodoMatricula(periodoId);

  if (periodo.isLoading) {
    return (
      <div className="flex h-60 items-center justify-center">
        <Spinner />
      </div>
    );
  }
  if (!periodo.data || !periodoId) {
    return (
      <Alert type="error" title="No pudimos cargar el periodo de matrícula.">
        Vuelve a los periodos de matrícula e inténtalo de nuevo.
      </Alert>
    );
  }
  if (modo === 'editar' && !periodo.data.editable) {
    return (
      <Navigate
        replace
        to={paths.app.administrativo.periodoMatricula.getHref(periodoId)}
      />
    );
  }

  return (
    <FormularioPeriodo
      // Tras guardar, la consulta se recarga y el formulario vuelve a abrir con lo que quedó guardado.
      key={periodo.dataUpdatedAt}
      periodo={periodo.data}
      soloLectura={modo === 'ver'}
    />
  );
};
