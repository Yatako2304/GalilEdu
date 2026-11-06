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
  usePesosCompetencias,
  usePlanCurso,
} from '../api/plan-evaluacion';

import { DialogoCatalogo } from './dialogo-catalogo';

type Fila = {
  competenciaId: string;
  nombre: string;
  seleccionada: boolean;
  /** Vacío = sin definir: la competencia queda con los pesos incompletos. */
  peso: string;
};

const filaPorDefecto = (competenciaId: string, nombre: string): Fila => ({
  competenciaId,
  nombre,
  seleccionada: false,
  peso: '',
});

const redondearPeso = (peso: number) => Math.round(peso * 100) / 100;

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
  const pesosDelGrado = usePesosCompetencias(filtro);
  const cargando =
    (agregando ? disponibles.isLoading : detalle.isLoading) ||
    pesosDelGrado.isLoading;

  const [cursoId, setCursoId] = React.useState('');
  const [horas, setHoras] = React.useState('');
  const [filas, setFilas] = React.useState<Fila[]>([]);
  // Errores por campo: se muestran en el estado de error del Text Field / Select de Figma.
  const [errores, setErrores] = React.useState<{
    curso?: string;
    horas?: string;
    pesos?: Record<string, string>;
  }>({});

  // Al abrir "configurar", parte de lo guardado.
  React.useEffect(() => {
    if (detalle.data) {
      setHoras(String(detalle.data.horasSemanales));
      setFilas(
        detalle.data.competencias.map((c) => ({
          competenciaId: c.competenciaId,
          nombre: c.nombre,
          seleccionada: c.seleccionada,
          peso: c.pesoCurso == null ? '' : String(c.pesoCurso),
        })),
      );
    }
  }, [detalle.data]);

  /** Lo que queda de 100 % para este curso: lo que no usan los demás cursos del grado. */
  const pesoDisponible = (competenciaId: string) => {
    const competencia = pesosDelGrado.data?.find(
      (c) => c.competenciaId === competenciaId,
    );
    const deOtros = (competencia?.cursos ?? [])
      .filter((c) => agregando || c.planCursoId !== modo.planCursoId)
      .reduce((suma, c) => suma + (c.pesoCurso ?? 0), 0);
    return redondearPeso(100 - deOtros);
  };

  const elegirCurso = (id: string) => {
    setCursoId(id);
    const curso = disponibles.data?.find((c) => c.id === id);
    setFilas(
      (curso?.competencias ?? []).map((c) => filaPorDefecto(c.id, c.nombre)),
    );
  };

  // Al marcar una competencia se propone lo que queda disponible, y se puede cambiar.
  const cambiar = (id: string, seleccionada: boolean) => {
    const disponible = pesoDisponible(id);
    setFilas((actuales) =>
      actuales.map((f) =>
        f.competenciaId === id
          ? {
              ...f,
              seleccionada,
              peso:
                seleccionada && !f.peso && disponible > 0
                  ? String(disponible)
                  : f.peso,
            }
          : f,
      ),
    );
  };

  const cambiarPeso = (id: string, peso: string) =>
    setFilas((actuales) =>
      actuales.map((f) => (f.competenciaId === id ? { ...f, peso } : f)),
    );

  const errorDePeso = (fila: Fila) => {
    if (!fila.peso) return undefined;
    const peso = Number(fila.peso);
    if (
      !Number.isFinite(peso) ||
      peso <= 0 ||
      peso > 100 ||
      redondearPeso(peso) !== peso
    ) {
      return 'El peso va de 0,01 a 100, con hasta 2 decimales.';
    }
    const disponible = pesoDisponible(fila.competenciaId);
    return peso > disponible ? `Disponible: ${disponible} %.` : undefined;
  };

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
    const elegidas = filas.filter((f) => f.seleccionada);
    const pesos: Record<string, string> = {};
    elegidas.forEach((f) => {
      const error = errorDePeso(f);
      if (error) pesos[f.competenciaId] = error;
    });
    const nuevos = {
      curso: agregando && !cursoId ? 'Elige el curso.' : undefined,
      horas:
        !Number.isInteger(horasNumero) || horasNumero < 1 || horasNumero > 20
          ? 'Las horas semanales van de 1 a 20.'
          : undefined,
      pesos,
    };
    setErrores(nuevos);
    if (nuevos.curso || nuevos.horas || Object.keys(pesos).length > 0) return;
    const competencias: SolicitudCompetenciaPlan[] = elegidas.map((f) => ({
      competenciaId: f.competenciaId,
      pesoCurso: f.peso ? Number(f.peso) : null,
    }));
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
            {filas.length > 0 && (
              <p className="text-body-4 text-content-tertiary">
                El peso indica cuánto aporta este curso a la nota de la
                competencia. Entre los cursos del grado suman hasta 100 %.
              </p>
            )}
            {filas.length === 0 ? (
              <p className="py-4 text-body-3 text-content-tertiary">
                {agregando && !cursoId
                  ? 'Elige un curso para ver las competencias de su área.'
                  : 'El área de este curso aún no tiene competencias activas.'}
              </p>
            ) : (
              <div className="divide-y divide-stroke-tertiary rounded-lg border border-stroke-tertiary px-3">
                {filas.map((f) => (
                  <div
                    key={f.competenciaId}
                    className="flex flex-wrap items-start justify-between gap-3 py-3"
                  >
                    <Checkbox
                      label={f.nombre}
                      checked={f.seleccionada}
                      onChange={(e) =>
                        cambiar(f.competenciaId, e.target.checked)
                      }
                    />
                    {f.seleccionada && (
                      <Input
                        type="number"
                        min="0.01"
                        max="100"
                        step="0.01"
                        label="Peso en %"
                        aria-label={`Peso del curso en ${f.nombre}`}
                        wrapperClassName="w-36"
                        value={f.peso}
                        onChange={(e) =>
                          cambiarPeso(f.competenciaId, e.target.value)
                        }
                        error={
                          errores.pesos?.[f.competenciaId]
                            ? {
                                type: 'validate',
                                message: errores.pesos[f.competenciaId],
                              }
                            : undefined
                        }
                        hint={`Disponible: ${pesoDisponible(f.competenciaId)} %`}
                      />
                    )}
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
