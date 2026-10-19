import { SesionClase } from '@/types/api';
import { cn } from '@/utils/cn';

// Figma > Componentes > Bloque horario
// Tipo: Asignado | Subgrupos | Recreo | Sin asignar
type BloqueHorarioProps =
  { tipo: 'RECREO'; sesion?: never } | { tipo: 'CLASE'; sesion?: SesionClase };

const Punto = ({ className }: { className?: string }) => (
  <span
    className={cn('mt-1.5 size-1.5 shrink-0 rounded-[1px]', className)}
    aria-hidden="true"
  />
);

export const BloqueHorario = ({ tipo, sesion }: BloqueHorarioProps) => {
  if (tipo === 'RECREO') {
    return (
      <div className="flex h-full items-center gap-2 rounded-lg border border-stroke-warning bg-surface-warning-light px-4 py-3 text-label-2 font-medium text-content-warning">
        <Punto className="mt-0 bg-amber-500" />
        Recreo
      </div>
    );
  }

  if (!sesion || sesion.tipo === 'SIN_ASIGNAR' || sesion.cursos.length === 0) {
    return (
      <div className="flex h-full min-h-[96px] items-center justify-center rounded-lg border border-dashed border-stroke-secondary bg-surface-secondary p-4 text-caption-1 text-content-quartiary">
        Sin asignar
      </div>
    );
  }

  if (sesion.tipo === 'SUBGRUPOS') {
    return (
      <div className="flex h-full flex-col gap-2 rounded-lg border border-stroke-secondary bg-surface-accent-light p-3">
        <p className="text-caption-1 font-medium text-content-accent-primary">
          Subgrupos
        </p>
        {sesion.cursos.map((curso) => (
          <div key={curso.curso} className="flex gap-2">
            <Punto className="bg-deep-blue-800" />
            <div className="min-w-0">
              <p className="text-label-2 font-medium text-content-secondary">
                {curso.curso}
              </p>
              <p className="text-caption-1 text-content-quartiary">
                {curso.docente}
              </p>
            </div>
          </div>
        ))}
        <p className="mt-auto text-caption-1 text-content-quartiary">
          {sesion.cursos.map((curso) => curso.aula).join(' · ')}
        </p>
      </div>
    );
  }

  const [curso] = sesion.cursos;

  return (
    <div className="flex h-full min-h-[96px] gap-2 rounded-lg border border-stroke-secondary bg-surface-accent-light p-3">
      <Punto className="bg-deep-blue-800" />
      <div className="flex min-w-0 flex-col">
        <p className="text-label-2 font-medium text-content-secondary">
          {curso.curso}
        </p>
        <p className="mt-2 text-caption-1 text-content-quartiary">
          {curso.docente}
        </p>
        <p className="text-caption-1 text-content-quartiary">{curso.aula}</p>
      </div>
    </div>
  );
};
