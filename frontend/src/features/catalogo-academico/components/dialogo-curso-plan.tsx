import { ListPlus, SlidersHorizontal } from 'lucide-react';
import * as React from 'react';

import { Checkbox, Select, Input } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';
import { PlanGrado, SolicitudCompetenciaPlan } from '@/types/api';
import { formatDay } from '@/utils/format';

import {
  useAgregarCursoPlan,
  useConfigurarCursoPlan,
  useCursosDisponibles,
  usePlanCurso,
} from '../api/plan-evaluacion';

import { DialogoCatalogo } from './dialogo-catalogo';

type Fila = {
  competenciaId: string;
  nombre: string;
  seleccionada: boolean;
};

const filaPorDefecto = (competenciaId: string, nombre: string): Fila => ({
  competenciaId,
  nombre,
  seleccionada: false,
});

export type ModoCursoPlan =
  { tipo: 'agregar' } | { tipo: 'configurar'; planCursoId: string };

type DialogoCursoPlanProps = {
  plan: PlanGrado;
  modo: ModoCursoPlan;
  onCerrar: () => void;
};

// Figma CI-17 > "Modal - Aritmética · 1.° de secundaria" y CI-20 > "Modal - Agregar curso a 3.° de secundaria"
export const DialogoCursoPlan = ({
  plan,
  modo,
  onCerrar,
}: DialogoCursoPlanProps) => {
  const agregando = modo.tipo === 'agregar';
  const filtro = { anioEscolarId: plan.anioEscolarId, grado: plan.gradoCodigo };
  const disponibles = useCursosDisponibles(filtro, agregando);
  const detalle = usePlanCurso(agregando ? null : modo.planCursoId);
  const cargando = agregando ? disponibles.isLoading : detalle.isLoading;

  const [cursoId, setCursoId] = React.useState('');
  const [horas, setHoras] = React.useState('');
  const [filas, setFilas] = React.useState<Fila[]>([]);
  // Errores por campo: se muestran en el estado de error del Text Field / Select de Figma.
  const [errores, setErrores] = React.useState<{
    curso?: string;
    horas?: string;
  }>({});

  // Al abrir "configurar", parte de lo guardado.
  React.useEffect(() => {
    if (detalle.data) {
      setHoras(String(detalle.data.horasSemanales));
      setFilas(detalle.data.competencias.map((c) => ({ ...c })));
    }
  }, [detalle.data]);

  const elegirCurso = (id: string) => {
    setCursoId(id);
    const curso = disponibles.data?.find((c) => c.id === id);
    setFilas(
      (curso?.competencias ?? []).map((c) => filaPorDefecto(c.id, c.nombre)),
    );
  };

  const cambiar = (id: string, seleccionada: boolean) =>
    setFilas((actuales) =>
      actuales.map((f) =>
        f.competenciaId === id ? { ...f, seleccionada } : f,
      ),
    );

  const horasPropias = agregando ? 0 : (detalle.data?.horasSemanales ?? 0);
  const disponiblesBloques =
    plan.bloquesDisponibles == null
      ? null
      : plan.bloquesDisponibles - plan.horasAsignadas + horasPropias;

  const agregar = useAgregarCursoPlan(onCerrar);
  const configurar = useConfigurarCursoPlan(onCerrar);

  const guardar = (e: React.FormEvent) => {
    e.preventDefault();
    const horasNumero = Number(horas);
    const nuevos = {
      curso: agregando && !cursoId ? 'Elige el curso.' : undefined,
      horas:
        !Number.isInteger(horasNumero) || horasNumero < 1 || horasNumero > 20
          ? 'Las horas semanales van de 1 a 20.'
          : undefined,
    };
    setErrores(nuevos);
    if (nuevos.curso || nuevos.horas) return;
    const competencias: SolicitudCompetenciaPlan[] = filas
      .filter((f) => f.seleccionada)
      .map(({ competenciaId }) => ({ competenciaId }));
    if (agregando) {
      agregar.mutate({
        anioEscolarId: plan.anioEscolarId,
        gradoCodigo: plan.gradoCodigo,
        cursoId,
        horasSemanales: horasNumero,
        competencias,
      });
    } else {
      configurar.mutate({
        id: modo.planCursoId,
        data: { horasSemanales: horasNumero, competencias },
      });
    }
  };

  const area = agregando
    ? disponibles.data?.find((c) => c.id === cursoId)?.area
    : detalle.data?.area;

  return (
    <DialogoCatalogo
      open
      onCerrar={onCerrar}
      icono={
        agregando ? (
          <ListPlus className="size-5" aria-hidden="true" />
        ) : (
          <SlidersHorizontal className="size-5" aria-hidden="true" />
        )
      }
      titulo={
        agregando
          ? `Agregar curso a ${plan.grado}`
          : detalle.data
            ? `${detalle.data.curso} · ${detalle.data.grado}`
            : 'Configurar curso'
      }
      descripcion={
        agregando || !area
          ? `Año escolar ${plan.anio}`
          : `Año escolar ${plan.anio}. Marca las competencias del área ${area} que se evaluarán en este curso.`
      }
      formId="form-curso-plan"
      textoGuardar={agregando ? 'Agregar curso' : 'Guardar configuración'}
      guardando={agregar.isPending || configurar.isPending}
      className="max-w-[920px]"
    >
      {cargando ? (
        <div className="flex h-40 items-center justify-center">
          <Spinner />
        </div>
      ) : (
        <form
          id="form-curso-plan"
          noValidate
          className="space-y-5"
          onSubmit={guardar}
        >
          <div className="flex flex-wrap items-start gap-4">
            {agregando && (
              <Select
                label="Curso *"
                wrapperClassName="min-w-64 flex-1"
                error={
                  errores.curso
                    ? { type: 'validate', message: errores.curso }
                    : undefined
                }
                placeholder="Elige un curso"
                options={(disponibles.data ?? []).map((c) => ({
                  label: `${c.nombre} · ${c.area}`,
                  value: c.id,
                }))}
                value={cursoId}
                onChange={(e) => elegirCurso(e.target.value)}
                hint="Solo aparecen cursos activos que aún no están en el plan del grado."
              />
            )}
            <Input
              type="number"
              label="Horas semanales *"
              error={
                errores.horas
                  ? { type: 'validate', message: errores.horas }
                  : undefined
              }
              wrapperClassName="w-44"
              value={horas}
              onChange={(e) => setHoras(e.target.value)}
              hint={
                disponiblesBloques == null
                  ? undefined
                  : `Disponibles: ${disponiblesBloques} bloques`
              }
            />
            {!agregando && detalle.data && (
              <p className="self-center text-body-4 text-content-tertiary">
                Última configuración: {formatDay(detalle.data.actualizadoEn)}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <p className="text-label-2 font-semibold text-content-primary">
              Competencias que evaluará
            </p>
            {filas.length === 0 ? (
              <p className="py-4 text-body-3 text-content-tertiary">
                {agregando && !cursoId
                  ? 'Elige un curso para ver las competencias de su área.'
                  : 'El área de este curso aún no tiene competencias activas.'}
              </p>
            ) : (
              <div className="divide-y divide-stroke-tertiary rounded-lg border border-stroke-tertiary px-3">
                {filas.map((f) => (
                  <div key={f.competenciaId} className="py-3">
                    <Checkbox
                      label={f.nombre}
                      checked={f.seleccionada}
                      onChange={(e) =>
                        cambiar(f.competenciaId, e.target.checked)
                      }
                    />
                  </div>
                ))}
              </div>
            )}
          </div>
        </form>
      )}
    </DialogoCatalogo>
  );
};
