import { Clock } from 'lucide-react';
import * as React from 'react';
import { Link } from 'react-router';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Select } from '@/components/ui/form';
import { IconBadge } from '@/components/ui/icon-badge';
import { Spinner } from '@/components/ui/spinner';
import { SelectableTag } from '@/components/ui/tag';
import { paths } from '@/config/paths';
import { useDebouncedValue } from '@/hooks/use-debounced-value';

import { useCruces } from '../api/cruces';
import {
  aCeldasSolicitadas,
  buscarCelda,
  horasAsignadasEnBorrador,
  sinCelda,
} from '../lib/borrador';
import { formatRango } from '../lib/formato';
import {
  BloqueDeJornada,
  CeldaDelHorario,
  DiaCoordinador,
  NOMBRE_DEL_DIA,
  OpcionesDeHorario,
} from '../types';

type DialogoAsignarBloqueProps = {
  seccionId: string;
  dia: DiaCoordinador;
  bloque: BloqueDeJornada;
  /** null mientras se piden las opciones. */
  opciones: OpcionesDeHorario | undefined;
  /** Lo guardado en el backend y el borrador actual: de ahí salen las horas ya asignadas de cada curso. */
  guardadas: CeldaDelHorario[];
  borrador: CeldaDelHorario[];
  /** Solo actualiza el borrador del navegador: no llama al backend para guardar. */
  onAsignar: (celda: CeldaDelHorario) => void;
  onQuitar: () => void;
  onCerrar: () => void;
};

const ESPERA_CRUCES_MS = 300;

// Figma > HU-45 > "Asignar bloque horario" (modal sin subgrupos, 863:56080). Diferencias acordadas con Figma: el botón
// se llama «Asignar» o «Actualizar» (no «Guardar cambios»: solo cambia el borrador), el docente es un campo
// deshabilitado que se llena con el curso (no se elige) y no hay interruptor de subgrupos. Se monta solo mientras está
// abierto.
export const DialogoAsignarBloque = ({
  seccionId,
  dia,
  bloque,
  opciones,
  guardadas,
  borrador,
  onAsignar,
  onQuitar,
  onCerrar,
}: DialogoAsignarBloqueProps) => {
  const existente = buscarCelda(borrador, dia, bloque.numero);
  const [planCursoId, setPlanCursoId] = React.useState(
    existente?.planCursoId ?? '',
  );
  // Por defecto va el aula de la sección; si no tiene, hay que elegir un ambiente antes de asignar.
  const [ambienteId, setAmbienteId] = React.useState(
    existente?.ambiente.id ?? opciones?.ambientePorDefectoId ?? '',
  );

  // Las opciones pueden llegar después de abrir el modal: el aula de la sección se pone entonces, sin pisar lo elegido.
  React.useEffect(() => {
    if (!existente && !ambienteId && opciones?.ambientePorDefectoId) {
      setAmbienteId(opciones.ambientePorDefectoId);
    }
  }, [existente, ambienteId, opciones?.ambientePorDefectoId]);

  const curso = opciones?.cursos.find((c) => c.planCursoId === planCursoId);
  const ambiente = opciones?.ambientes.find((a) => a.id === ambienteId);
  const cursosSinDocente = (opciones?.cursos ?? []).filter((c) => !c.docente);

  // Cruces: el borrador completo, con este bloque ya asignado, va al backend (con una pausa al elegir).
  const completo = !!curso && !!ambiente;
  const solicitado = React.useMemo(() => {
    if (!completo) return '';
    const sin = sinCelda(borrador, dia, bloque.numero);
    return JSON.stringify([
      ...aCeldasSolicitadas(sin),
      { dia, numeroBloque: bloque.numero, planCursoId, ambienteId },
    ]);
  }, [completo, borrador, dia, bloque.numero, planCursoId, ambienteId]);
  const solicitadoConPausa = useDebouncedValue(solicitado, ESPERA_CRUCES_MS);
  const celdasParaCruces = React.useMemo(
    () => (solicitadoConPausa ? JSON.parse(solicitadoConPausa) : []),
    [solicitadoConPausa],
  );
  const cruces = useCruces(
    seccionId,
    celdasParaCruces,
    completo && solicitadoConPausa === solicitado,
  );
  // Solo importan los cruces de este bloque. Si la consulta falla (404, red), no hay alerta y no se bloquea nada.
  const crucesDelBloque =
    completo && solicitadoConPausa === solicitado
      ? (cruces.data ?? []).filter(
          (c) => c.dia === dia && c.numeroBloque === bloque.numero,
        )
      : [];

  const asignar = () => {
    if (!curso || !ambiente) return;
    onAsignar({
      dia,
      numeroBloque: bloque.numero,
      planCursoId: curso.planCursoId,
      curso: curso.nombre,
      docente: curso.docente,
      ambiente: { id: ambiente.id, nombre: ambiente.nombre },
      cruces: [],
    });
  };

  return (
    <Dialog open onOpenChange={(open) => !open && onCerrar()}>
      <DialogContent className="flex max-h-[92vh] max-w-[670px] flex-col gap-0 overflow-hidden rounded-xl border-stroke-tertiary bg-white p-0">
        <div className="flex-1 space-y-6 overflow-y-auto p-8">
          <div className="flex items-start gap-3 border-b border-stroke-tertiary pb-3">
            <IconBadge color="accent" size="md" aria-hidden="true">
              <Clock />
            </IconBadge>
            <div className="min-w-0 flex-1 space-y-2">
              <DialogDescription className="text-body-3 text-content-quartiary">
                Asignar bloque horario
              </DialogDescription>
              <DialogTitle className="text-h6 font-medium text-content-primary">
                {NOMBRE_DEL_DIA[dia]} - Bloque {bloque.numero}
              </DialogTitle>
              <p className="text-body-3 text-content-quartiary">
                {formatRango(bloque.inicio, bloque.fin)}
              </p>
            </div>
          </div>

          {!opciones ? (
            <div className="flex h-32 items-center justify-center">
              <Spinner size="lg" />
            </div>
          ) : (
            <>
              <div className="space-y-2">
                <p
                  className="text-label-3 text-content-tertiary"
                  id="curso-titulo"
                >
                  Curso
                </p>
                <div
                  role="group"
                  aria-labelledby="curso-titulo"
                  className="flex flex-wrap gap-2"
                >
                  {opciones.cursos.map((c) => {
                    const asignadas = horasAsignadasEnBorrador(
                      c,
                      guardadas,
                      borrador,
                      existente,
                    );
                    const agotado = asignadas >= c.horasSemanales;
                    return (
                      <SelectableTag
                        key={c.planCursoId}
                        size="md"
                        selected={c.planCursoId === planCursoId}
                        disabled={!c.docente || agotado}
                        aria-pressed={c.planCursoId === planCursoId}
                        onClick={() => setPlanCursoId(c.planCursoId)}
                      >
                        {c.nombre} · {asignadas}/{c.horasSemanales} h
                      </SelectableTag>
                    );
                  })}
                </div>
                {cursosSinDocente.length > 0 && (
                  <Alert
                    type="warning"
                    title={`Sin docente: ${cursosSinDocente.map((c) => c.nombre).join(', ')}`}
                    action={
                      <Link
                        to={paths.app.coordinador.planEvaluacion.getHref()}
                        className="text-label-2 font-medium text-content-accent-secondary underline-offset-4 hover:underline"
                      >
                        Ir a Plan de evaluación
                      </Link>
                    }
                  >
                    Asigna un docente en Plan de evaluación para poder ubicar el
                    curso en el horario.
                  </Alert>
                )}
              </div>

              <div className="grid grid-cols-1 gap-8 sm:grid-cols-2">
                <Select
                  label="Docente"
                  placeholder="Docente"
                  disabled
                  value={curso?.docente?.id ?? ''}
                  onChange={() => undefined}
                  options={
                    curso?.docente
                      ? [
                          {
                            label: curso.docente.nombre,
                            value: curso.docente.id,
                          },
                        ]
                      : []
                  }
                />
                <Select
                  label="Ambiente"
                  placeholder="Ambiente"
                  value={ambienteId}
                  onChange={(e) => setAmbienteId(e.target.value)}
                  options={opciones.ambientes.map((a) => ({
                    label: a.nombre,
                    value: a.id,
                  }))}
                />
              </div>

              {crucesDelBloque.length > 0 && (
                <Alert type="warning" title="Cruce de horario">
                  {crucesDelBloque.map((c) => (
                    <span key={`${c.tipo}-${c.mensaje}`} className="block">
                      {c.mensaje}
                    </span>
                  ))}
                </Alert>
              )}
            </>
          )}
        </div>

        <div className="flex items-center justify-end gap-3 border-t border-stroke-tertiary px-8 py-5">
          {existente && (
            <Button variant="error" onClick={onQuitar}>
              Quitar
            </Button>
          )}
          <Button variant="primary-light" onClick={onCerrar}>
            Cancelar
          </Button>
          <Button
            variant="secondary"
            onClick={asignar}
            disabled={!completo || crucesDelBloque.length > 0}
          >
            {existente ? 'Actualizar' : 'Asignar'}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
