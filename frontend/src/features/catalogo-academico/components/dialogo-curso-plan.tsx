import { ListPlus, SlidersHorizontal } from 'lucide-react';
import * as React from 'react';

import { Checkbox, Select, Input } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';
import {
  Consolidacion,
  Escala,
  PlanGrado,
  ReglaNota,
  SolicitudCompetenciaPlan,
} from '@/types/api';
import { cn } from '@/utils/cn';
import { formatDay } from '@/utils/format';

import {
  useAgregarCursoPlan,
  useConfigurarCursoPlan,
  useCursosDisponibles,
  usePlanCurso,
} from '../api/plan-evaluacion';

import { DialogoCatalogo } from './dialogo-catalogo';

// Opciones de las columnas "Escala", "Agregación de ítems" y "Consolidación de periodos" (Figma CI-17).
export const ESCALAS: { label: string; value: Escala }[] = [
  { label: 'Vigesimal (0–20)', value: 'VIGESIMAL' },
  { label: 'Cualitativa (AD–C)', value: 'CUALITATIVA' },
];

export const REGLAS: Record<Escala, { label: string; value: ReglaNota }[]> = {
  VIGESIMAL: [
    { label: 'Promedio simple', value: 'PROMEDIO_SIMPLE' },
    { label: 'Promedio ponderado', value: 'PROMEDIO_PONDERADO' },
  ],
  CUALITATIVA: [
    { label: 'Nivel más frecuente', value: 'NIVEL_MAS_FRECUENTE' },
    { label: 'Último nivel', value: 'ULTIMO_NIVEL' },
  ],
};

export const CONSOLIDACIONES: { label: string; value: Consolidacion }[] = [
  { label: 'Promedio de periodos', value: 'PROMEDIO_PERIODOS' },
  { label: 'Último periodo', value: 'ULTIMO_PERIODO' },
];

type Fila = {
  competenciaId: string;
  nombre: string;
  seleccionada: boolean;
  escala: Escala;
  reglaNota: ReglaNota;
  consolidacion: Consolidacion;
};

const filaPorDefecto = (competenciaId: string, nombre: string): Fila => ({
  competenciaId,
  nombre,
  seleccionada: false,
  escala: 'VIGESIMAL',
  reglaNota: 'PROMEDIO_SIMPLE',
  consolidacion: 'PROMEDIO_PERIODOS',
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
  const [error, setError] = React.useState<string | null>(null);

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

  const cambiar = (id: string, cambio: Partial<Fila>) =>
    setFilas((actuales) =>
      actuales.map((f) => {
        if (f.competenciaId !== id) return f;
        const nueva = { ...f, ...cambio };
        // Al cambiar la escala, la agregación vuelve a la primera opción de esa escala.
        if (cambio.escala && cambio.escala !== f.escala) {
          nueva.reglaNota = REGLAS[cambio.escala][0].value;
        }
        return nueva;
      }),
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
    if (agregando && !cursoId) return setError('Elige el curso.');
    if (!Number.isInteger(horasNumero) || horasNumero < 1 || horasNumero > 20)
      return setError('Las horas semanales van de 1 a 20.');
    setError(null);
    const competencias: SolicitudCompetenciaPlan[] = filas
      .filter((f) => f.seleccionada)
      .map(({ competenciaId, escala, reglaNota, consolidacion }) => ({
        competenciaId,
        escala,
        reglaNota,
        consolidacion,
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
          : `Año escolar ${plan.anio}. Marca las competencias del área ${area} que se evaluarán en este curso y cómo se calculará su nota.`
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
        <form id="form-curso-plan" className="space-y-5" onSubmit={guardar}>
          <div className="flex flex-wrap items-start gap-4">
            {agregando && (
              <Select
                label="Curso *"
                wrapperClassName="min-w-64 flex-1"
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
              min={1}
              max={20}
              label="Horas semanales *"
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
              <table className="w-full text-left">
                <thead className="text-label-3 text-content-tertiary">
                  <tr className="border-b border-stroke-tertiary">
                    <th className="py-2 pr-3 font-medium">Competencia</th>
                    <th className="p-2 font-medium">Escala</th>
                    <th className="p-2 font-medium">Agregación de ítems</th>
                    <th className="p-2 font-medium">
                      Consolidación de periodos
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {filas.map((f) => (
                    <tr
                      key={f.competenciaId}
                      className="border-b border-stroke-tertiary"
                    >
                      <td className="py-2 pr-3">
                        <Checkbox
                          label={f.nombre}
                          checked={f.seleccionada}
                          onChange={(e) =>
                            cambiar(f.competenciaId, {
                              seleccionada: e.target.checked,
                            })
                          }
                        />
                      </td>
                      <CeldaSelect
                        etiqueta={`Escala de ${f.nombre}`}
                        deshabilitado={!f.seleccionada}
                        opciones={ESCALAS}
                        valor={f.escala}
                        onCambiar={(v) =>
                          cambiar(f.competenciaId, { escala: v as Escala })
                        }
                      />
                      <CeldaSelect
                        etiqueta={`Agregación de ítems de ${f.nombre}`}
                        deshabilitado={!f.seleccionada}
                        opciones={REGLAS[f.escala]}
                        valor={f.reglaNota}
                        onCambiar={(v) =>
                          cambiar(f.competenciaId, {
                            reglaNota: v as ReglaNota,
                          })
                        }
                      />
                      <CeldaSelect
                        etiqueta={`Consolidación de periodos de ${f.nombre}`}
                        deshabilitado={!f.seleccionada}
                        opciones={CONSOLIDACIONES}
                        valor={f.consolidacion}
                        onCambiar={(v) =>
                          cambiar(f.competenciaId, {
                            consolidacion: v as Consolidacion,
                          })
                        }
                      />
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
          {error && (
            <p role="alert" className="text-body-4 text-content-error">
              {error}
            </p>
          )}
        </form>
      )}
    </DialogoCatalogo>
  );
};

const CeldaSelect = ({
  etiqueta,
  deshabilitado,
  opciones,
  valor,
  onCambiar,
}: {
  etiqueta: string;
  deshabilitado: boolean;
  opciones: { label: string; value: string }[];
  valor: string;
  onCambiar: (valor: string) => void;
}) => (
  <td className="p-2">
    <Select
      aria-label={etiqueta}
      disabled={deshabilitado}
      options={opciones}
      value={valor}
      onChange={(e) => onCambiar(e.target.value)}
      className={cn('h-9 text-body-4', deshabilitado && 'opacity-60')}
    />
  </td>
);
