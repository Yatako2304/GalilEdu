import * as React from 'react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { DialogoConfirmacion } from '@/components/ui/dialog/dialogo-confirmacion';
import { ProgressBar } from '@/components/ui/progress-bar';
import { Tag } from '@/components/ui/tag';
import { SeccionOpcion, SeccionesDelGrado } from '@/types/api';
import { cn } from '@/utils/cn';

const vacantes = (s: SeccionOpcion) => Math.max(0, s.capacidad - s.ocupadas);
const llena = (s: SeccionOpcion) => vacantes(s) === 0;

const plural = (n: number) =>
  n === 1 ? '1 vacante libre' : `${n} vacantes libres`;

// Figma > Personal Administrativo > A5 · modal "Cambiar sección" y su confirmación "¿Cambiar sección?". HU-24.
export const DialogoCambiarSeccion = ({
  secciones,
  cargando,
  guardando,
  alConfirmar,
  onCerrar,
}: {
  /** Secciones del grado; {@code seleccionadaId} es la actual (o la propuesta en Nueva matrícula). */
  secciones?: SeccionesDelGrado;
  cargando: boolean;
  guardando: boolean;
  alConfirmar: (seccionId: string) => void;
  onCerrar: () => void;
}) => {
  const [elegidaId, setElegidaId] = React.useState<string>();
  const [confirmando, setConfirmando] = React.useState(false);

  const actual = secciones?.secciones.find(
    (s) => s.id === secciones.seleccionadaId,
  );
  const destino = secciones?.secciones.find((s) => s.id === elegidaId);
  const hayCambio = !!destino && destino.id !== actual?.id;

  return (
    <>
      <Dialog open onOpenChange={(abierto) => !abierto && onCerrar()}>
        <DialogContent className="flex max-h-[92vh] max-w-screen-sm flex-col gap-0 overflow-hidden rounded-xl border-stroke-tertiary bg-white p-0">
          <div className="flex-1 space-y-5 overflow-y-auto p-6">
            <div>
              <DialogTitle className="text-h6 font-semibold text-content-primary">
                Cambiar sección
              </DialogTitle>
              <DialogDescription className="mt-1 text-body-4 text-content-tertiary">
                {secciones
                  ? `Elige otra sección de ${secciones.grado}. Las secciones sin vacantes no se pueden elegir.`
                  : 'Cargando secciones…'}
              </DialogDescription>
            </div>

            {cargando && (
              <p className="text-body-3 text-content-tertiary">
                Cargando secciones…
              </p>
            )}

            <div
              role="radiogroup"
              aria-label="Secciones disponibles"
              className="space-y-3"
            >
              {secciones?.secciones.map((s) => {
                const esActual = s.id === actual?.id;
                const marcada = s.id === elegidaId;
                const bloqueada = llena(s) && !esActual;
                return (
                  <label
                    key={s.id}
                    className={cn(
                      'flex cursor-pointer flex-col gap-2 rounded-xl border p-4',
                      marcada
                        ? 'border-stroke-accent-primary bg-surface-primary-hover'
                        : 'border-stroke-tertiary',
                      bloqueada && 'cursor-not-allowed opacity-60',
                    )}
                  >
                    <span className="flex items-center gap-3">
                      <input
                        type="radio"
                        name="seccion"
                        value={s.id}
                        checked={marcada}
                        disabled={bloqueada || esActual}
                        onChange={() => setElegidaId(s.id)}
                        aria-label={`Sección ${s.etiqueta}`}
                      />
                      <span className="flex-1">
                        <span className="block text-label-1 text-content-primary">
                          {s.etiqueta}
                        </span>
                        <span className="block text-body-4 text-content-tertiary">
                          Tutor: {s.tutor ?? 'Sin asignar'}
                        </span>
                      </span>
                      {esActual && <Tag color="info">Sección actual</Tag>}
                      {bloqueada && <Tag color="error">Completa</Tag>}
                      {!esActual && !bloqueada && (
                        <span className="text-body-4 text-content-tertiary">
                          {plural(vacantes(s))}
                        </span>
                      )}
                    </span>
                    <ProgressBar
                      value={(s.ocupadas / s.capacidad) * 100}
                      label={`Ocupación ${s.etiqueta}: ${s.ocupadas}/${s.capacidad}`}
                      showLabel={false}
                    />
                    <span className="text-caption-1 text-content-quartiary">
                      {s.ocupadas}/{s.capacidad} estudiantes
                    </span>
                  </label>
                );
              })}
            </div>

            {hayCambio && actual && destino && (
              <div className="rounded-xl bg-surface-secondary p-4">
                <p className="text-label-1 text-content-primary">
                  Efecto en las vacantes
                </p>
                <ul className="mt-2 space-y-1 text-body-4 text-content-tertiary">
                  <li>
                    {actual.etiqueta}: {actual.ocupadas} → {actual.ocupadas - 1}{' '}
                    estudiantes (se libera 1 vacante)
                  </li>
                  <li>
                    {destino.etiqueta}: {destino.ocupadas} →{' '}
                    {destino.ocupadas + 1} estudiantes (se ocupa 1 vacante)
                  </li>
                </ul>
              </div>
            )}
          </div>

          <div className="flex justify-end gap-3 border-t border-stroke-tertiary p-4">
            <Button type="button" variant="tertiary" onClick={onCerrar}>
              Cancelar
            </Button>
            <Button
              type="button"
              disabled={!hayCambio || guardando}
              onClick={() => setConfirmando(true)}
            >
              Cambiar sección
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {destino && (
        <DialogoConfirmacion
          open={confirmando}
          onOpenChange={setConfirmando}
          tipo="guardar"
          titulo="¿Cambiar sección?"
          descripcion={`${
            actual ? `Se liberará una vacante en ${actual.etiqueta} y ` : 'Se '
          }ocupará una en ${destino.etiqueta}. El horario y el tutor del estudiante se actualizarán a los de la nueva sección.`}
          textoConfirmar="Cambiar sección"
          cargando={guardando}
          onConfirmar={() => {
            setConfirmando(false);
            alConfirmar(destino.id);
          }}
        />
      )}
    </>
  );
};
