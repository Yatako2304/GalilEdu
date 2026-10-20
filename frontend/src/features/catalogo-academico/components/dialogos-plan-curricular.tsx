import { zodResolver } from '@hookform/resolvers/zod';
import { BookOpen, Layers, Target } from 'lucide-react';
import { useForm } from 'react-hook-form';

import { Input, Textarea } from '@/components/ui/form';
import { GRADOS_POR_NIVEL } from '@/config/catalogos';
import { AreaResumen, CompetenciaResumen, CursoResumen } from '@/types/api';
import { cn } from '@/utils/cn';

import {
  useGuardarArea,
  useGuardarCompetencia,
  useGuardarCurso,
} from '../api/plan-curricular';

import { DialogoCatalogo } from './dialogo-catalogo';
import {
  AreaFormulario,
  CompetenciaFormulario,
  CursoFormulario,
  areaSchema,
  competenciaSchema,
  cursoSchema,
} from './esquemas';

// ---------- Área (HU-52) ----------

type DialogoAreaProps = {
  /** null = nueva área. */
  area: Pick<AreaResumen, 'id' | 'nombre' | 'descripcion'> | null;
  onCerrar: () => void;
  onGuardada?: (id: string) => void;
};

// Figma CI-14 > "Modal - Nueva area"
export const DialogoArea = ({
  area,
  onCerrar,
  onGuardada,
}: DialogoAreaProps) => {
  const form = useForm<AreaFormulario>({
    resolver: zodResolver(areaSchema),
    defaultValues: {
      nombre: area?.nombre ?? '',
      descripcion: area?.descripcion ?? '',
    },
  });
  const guardar = useGuardarArea((a) => {
    onCerrar();
    onGuardada?.(a.id);
  });

  return (
    <DialogoCatalogo
      open
      onCerrar={onCerrar}
      icono={<Layers className="size-5" aria-hidden="true" />}
      titulo={area ? 'Editar área curricular' : 'Nueva área curricular'}
      formId="form-area"
      textoGuardar={area ? 'Guardar cambios' : 'Agregar área'}
      guardando={guardar.isPending}
    >
      <form
        id="form-area"
        className="space-y-4"
        onSubmit={form.handleSubmit((valores) =>
          guardar.mutate({ id: area?.id, data: areaSchema.parse(valores) }),
        )}
      >
        <Input
          label="Nombre *"
          placeholder="Ej. Matemática"
          registration={form.register('nombre')}
          error={form.formState.errors.nombre}
        />
        <Textarea
          label="Descripción"
          placeholder="Qué desarrolla el área en los estudiantes"
          registration={form.register('descripcion')}
          error={form.formState.errors.descripcion}
        />
      </form>
    </DialogoCatalogo>
  );
};

// ---------- Competencia (HU-53) ----------

type DialogoCompetenciaProps = {
  area: Pick<AreaResumen, 'id' | 'nombre'>;
  /** null = nueva competencia. */
  competencia: CompetenciaResumen | null;
  onCerrar: () => void;
};

// Figma CI-14 > "Modal - Nueva competencia"
export const DialogoCompetencia = ({
  area,
  competencia,
  onCerrar,
}: DialogoCompetenciaProps) => {
  const form = useForm<CompetenciaFormulario>({
    resolver: zodResolver(competenciaSchema),
    defaultValues: {
      nombre: competencia?.nombre ?? '',
      descripcion: competencia?.descripcion ?? '',
    },
  });
  const guardar = useGuardarCompetencia(onCerrar);

  return (
    <DialogoCatalogo
      open
      onCerrar={onCerrar}
      icono={<Target className="size-5" aria-hidden="true" />}
      titulo={competencia ? 'Editar competencia' : 'Nueva competencia'}
      descripcion={`Área: ${area.nombre}`}
      formId="form-competencia"
      textoGuardar={competencia ? 'Guardar cambios' : 'Agregar competencia'}
      guardando={guardar.isPending}
    >
      <form
        id="form-competencia"
        className="space-y-4"
        onSubmit={form.handleSubmit((valores) =>
          guardar.mutate({
            areaId: area.id,
            id: competencia?.id,
            data: competenciaSchema.parse(valores),
          }),
        )}
      >
        <Input
          label="Nombre *"
          placeholder="Ej. Resuelve problemas de cantidad"
          registration={form.register('nombre')}
          error={form.formState.errors.nombre}
        />
        <Textarea
          label="Descripción"
          placeholder="Qué logra el estudiante con esta competencia"
          registration={form.register('descripcion')}
          error={form.formState.errors.descripcion}
        />
      </form>
    </DialogoCatalogo>
  );
};

// ---------- Curso (HU-54) ----------

type DialogoCursoProps = {
  area: Pick<AreaResumen, 'id' | 'nombre'>;
  /** null = nuevo curso. */
  curso: CursoResumen | null;
  onCerrar: () => void;
};

const NIVELES = [
  { titulo: 'Grados de primaria', opciones: GRADOS_POR_NIVEL.PRIMARIA },
  { titulo: 'Grados de secundaria', opciones: GRADOS_POR_NIVEL.SECUNDARIA },
];

// Figma CI-14 > "Modal - Nueva curso"
export const DialogoCurso = ({ area, curso, onCerrar }: DialogoCursoProps) => {
  const form = useForm<CursoFormulario>({
    resolver: zodResolver(cursoSchema),
    defaultValues: { nombre: curso?.nombre ?? '', grados: curso?.grados ?? [] },
  });
  const gradosElegidos = form.watch('grados');
  const guardar = useGuardarCurso(onCerrar);

  const alternarGrado = (codigo: string) => {
    const actuales = form.getValues('grados');
    form.setValue(
      'grados',
      actuales.includes(codigo)
        ? actuales.filter((g) => g !== codigo)
        : [...actuales, codigo],
      { shouldValidate: form.formState.isSubmitted },
    );
  };

  return (
    <DialogoCatalogo
      open
      onCerrar={onCerrar}
      icono={<BookOpen className="size-5" aria-hidden="true" />}
      titulo={curso ? 'Editar curso' : 'Nuevo curso'}
      descripcion={`Área: ${area.nombre}`}
      formId="form-curso"
      textoGuardar={curso ? 'Guardar cambios' : 'Agregar curso'}
      guardando={guardar.isPending}
    >
      <form
        id="form-curso"
        className="space-y-4"
        onSubmit={form.handleSubmit((valores) =>
          guardar.mutate({
            areaId: area.id,
            id: curso?.id,
            data: cursoSchema.parse(valores),
          }),
        )}
      >
        <Input
          label="Nombre *"
          placeholder="Ej. Álgebra"
          registration={form.register('nombre')}
          error={form.formState.errors.nombre}
        />
        {NIVELES.map(({ titulo, opciones }) => (
          <fieldset key={titulo} className="space-y-2">
            <legend className="text-label-3 text-content-secondary">
              {titulo}
            </legend>
            <div className="flex flex-wrap gap-2">
              {opciones.map((grado) => {
                const elegido = gradosElegidos.includes(grado.value as string);
                return (
                  <button
                    key={grado.value}
                    type="button"
                    aria-pressed={elegido}
                    aria-label={`${grado.label} de ${titulo.split(' ').pop()}`}
                    onClick={() => alternarGrado(grado.value as string)}
                    className={cn(
                      'rounded-full border px-3 py-1 text-body-4 transition-colors',
                      elegido
                        ? 'border-transparent bg-deep-blue-800 text-white'
                        : 'border-stroke-accent-primary bg-white text-content-accent-secondary hover:bg-surface-accent-light',
                    )}
                  >
                    {grado.label}
                  </button>
                );
              })}
            </div>
          </fieldset>
        ))}
        {form.formState.errors.grados && (
          <p className="text-body-4 text-content-error">
            {form.formState.errors.grados.message}
          </p>
        )}
      </form>
    </DialogoCatalogo>
  );
};
