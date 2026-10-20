import { ClipboardList, Copy, Trash2, UserPlus } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Select } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';
import { PlanCursoResumen, PlanGrado } from '@/types/api';

import {
  useAsignarDocentes,
  useCopiarPlan,
  useDocentes,
  useDocentesDelCurso,
  useOrigenesPlan,
  useQuitarCursoPlan,
} from '../api/plan-evaluacion';

import { DialogoCatalogo } from './dialogo-catalogo';

// ---------- Docentes por sección ----------

type DialogoDocentesProps = {
  plan: PlanGrado;
  curso: PlanCursoResumen;
  onCerrar: () => void;
};

// Figma CI-17 > "Modal - Docentes de Álgebra"
export const DialogoDocentes = ({
  plan,
  curso,
  onCerrar,
}: DialogoDocentesProps) => {
  const secciones = useDocentesDelCurso(curso.id);
  const docentes = useDocentes();
  const [elegidos, setElegidos] = React.useState<Record<string, string>>({});
  const asignar = useAsignarDocentes(onCerrar);

  React.useEffect(() => {
    if (secciones.data) {
      setElegidos(
        Object.fromEntries(
          secciones.data.map((s) => [s.seccionId, s.docenteId ?? '']),
        ),
      );
    }
  }, [secciones.data]);

  const opciones = [
    { label: 'Sin docente', value: '' },
    ...(docentes.data ?? []).map((d) => ({ label: d.nombre, value: d.id })),
  ];
  const grado = plan.grado
    .replace(' de secundaria', '')
    .replace(' de primaria', '');

  return (
    <DialogoCatalogo
      open
      onCerrar={onCerrar}
      icono={<UserPlus className="size-5" aria-hidden="true" />}
      titulo={`Docentes de ${curso.curso}`}
      descripcion={`${plan.grado} · ${plan.anio}. Elige qué docente dicta el curso en cada sección.`}
      formId="form-docentes"
      textoGuardar="Guardar"
      guardando={asignar.isPending}
    >
      {secciones.isLoading || docentes.isLoading ? (
        <div className="flex h-24 items-center justify-center">
          <Spinner />
        </div>
      ) : (
        <form
          id="form-docentes"
          className="space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            asignar.mutate({
              id: curso.id,
              asignaciones: Object.entries(elegidos).map(
                ([seccionId, docenteId]) => ({
                  seccionId,
                  docenteId: docenteId || null,
                }),
              ),
            });
          }}
        >
          {(secciones.data ?? []).length === 0 && (
            <p className="text-body-3 text-content-tertiary">
              {plan.grado} aún no tiene secciones en {plan.anio}. Créalas en Año
              escolar › Secciones.
            </p>
          )}
          {(secciones.data ?? []).map((s) => (
            <Select
              key={s.seccionId}
              label={`Sección ${grado} ${s.seccion}${s.tutor ? ` · tutor: ${s.tutor}` : ''}`}
              options={opciones}
              value={elegidos[s.seccionId] ?? ''}
              onChange={(e) =>
                setElegidos((actual) => ({
                  ...actual,
                  [s.seccionId]: e.target.value,
                }))
              }
            />
          ))}
        </form>
      )}
    </DialogoCatalogo>
  );
};

// ---------- Copiar plan ----------

type DialogoCopiarPlanProps = { plan: PlanGrado; onCerrar: () => void };

// Figma CI-20 > "Modal - Copiar plan a 3.° de secundaria 2027"
export const DialogoCopiarPlan = ({
  plan,
  onCerrar,
}: DialogoCopiarPlanProps) => {
  const origenes = useOrigenesPlan(
    { anioEscolarId: plan.anioEscolarId, grado: plan.gradoCodigo },
    true,
  );
  const [claveElegida, setClave] = React.useState('');
  const copiar = useCopiarPlan(onCerrar);

  const lista = origenes.data ?? [];
  const claveDe = (o: { anioEscolarId: string; gradoCodigo: string }) =>
    `${o.anioEscolarId}|${o.gradoCodigo}`;
  // Por defecto, el mismo grado de otro año si existe; si no, el primero de la lista.
  const porDefecto =
    lista.find((o) => o.gradoCodigo === plan.gradoCodigo) ?? lista[0];
  const clave = claveElegida || (porDefecto ? claveDe(porDefecto) : '');
  const origen = lista.find((o) => claveDe(o) === clave);

  return (
    <DialogoCatalogo
      open
      onCerrar={onCerrar}
      icono={<Copy className="size-5" aria-hidden="true" />}
      titulo={`Copiar plan a ${plan.grado} ${plan.anio}`}
      descripcion="Se copiarán los cursos activos, sus horas y la configuración de competencias. Los docentes se asignan después."
      formId="form-copiar-plan"
      textoGuardar="Copiar plan"
      guardando={copiar.isPending}
      className="max-w-[560px]"
    >
      {origenes.isLoading ? (
        <div className="flex h-24 items-center justify-center">
          <Spinner />
        </div>
      ) : lista.length === 0 ? (
        <p className="text-body-3 text-content-tertiary">
          Aún no hay otros planes con cursos para copiar.
        </p>
      ) : (
        <form
          id="form-copiar-plan"
          className="space-y-4"
          onSubmit={(e) => {
            e.preventDefault();
            if (!origen) return;
            copiar.mutate({
              anioEscolarId: plan.anioEscolarId,
              gradoCodigo: plan.gradoCodigo,
              origenAnioEscolarId: origen.anioEscolarId,
              origenGradoCodigo: origen.gradoCodigo,
            });
          }}
        >
          <Select
            label="Copiar desde *"
            options={lista.map((o) => ({
              label: `${o.grado} · ${o.anio} (${o.cursos} cursos)`,
              value: `${o.anioEscolarId}|${o.gradoCodigo}`,
            }))}
            value={clave}
            onChange={(e) => setClave(e.target.value)}
          />
          {origen && (
            <dl className="space-y-2 rounded-lg bg-surface-secondary px-4 py-3 text-body-3">
              <div className="flex justify-between">
                <dt className="text-content-secondary">Cursos</dt>
                <dd className="font-medium text-content-primary">
                  {origen.cursos}
                </dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-content-secondary">Horas semanales</dt>
                <dd className="font-medium text-content-primary">
                  {plan.bloquesDisponibles == null
                    ? origen.horas
                    : `${origen.horas} de ${plan.bloquesDisponibles} disponibles`}
                </dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-content-secondary">
                  Competencias configuradas
                </dt>
                <dd className="font-medium text-content-primary">
                  {origen.competencias}
                </dd>
              </div>
            </dl>
          )}
          <p className="flex items-center gap-3 rounded-lg border border-stroke-accent-primary bg-surface-accent-light px-4 py-3 text-body-3 text-content-primary">
            <ClipboardList
              className="size-5 shrink-0 text-content-accent-primary"
              aria-hidden="true"
            />
            Solo se copian cursos y competencias que siguen activos en el plan
            curricular.
          </p>
        </form>
      )}
    </DialogoCatalogo>
  );
};

// ---------- Quitar curso ----------

type DialogoQuitarCursoProps = {
  plan: PlanGrado;
  curso: PlanCursoResumen;
  onCerrar: () => void;
};

export const DialogoQuitarCurso = ({
  plan,
  curso,
  onCerrar,
}: DialogoQuitarCursoProps) => {
  const quitar = useQuitarCursoPlan(onCerrar);
  return (
    <Dialog open onOpenChange={(abierto) => !abierto && onCerrar()}>
      <DialogContent className="max-w-[448px] gap-0 rounded-[12px] border-stroke-tertiary bg-white p-0">
        <div className="flex flex-col gap-3 px-6 pt-6">
          <span className="inline-flex size-10 items-center justify-center rounded-full bg-surface-error-light text-content-error">
            <Trash2 className="size-5" aria-hidden="true" />
          </span>
          <DialogTitle className="text-h6 font-semibold text-content-primary">
            ¿Quitar {curso.curso} del plan?
          </DialogTitle>
          <DialogDescription className="pb-5 text-body-3 text-content-tertiary">
            Se quitarán sus competencias y docentes de {plan.grado} {plan.anio}.
            El curso sigue en el plan curricular.
          </DialogDescription>
        </div>
        <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
          <Button
            variant="primary-light"
            onClick={onCerrar}
            disabled={quitar.isPending}
          >
            Cancelar
          </Button>
          <Button
            variant="error"
            isLoading={quitar.isPending}
            onClick={() => quitar.mutate(curso.id)}
          >
            Quitar
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};
