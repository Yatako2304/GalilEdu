import { Plus } from 'lucide-react';
import * as React from 'react';

import { cn } from '@/utils/cn';

export type ScheduleCourse = {
  /** Curso o actividad, ej. "Ciencia y tecnología". */
  name: string;
  /** Docente, ej. "Prof. Lucía Ayala Prado". */
  teacher?: string;
};

export type ScheduleBlockProps = {
  /**
   * assigned: un curso (o varios si `courses` tiene más de uno: Figma "Subgrupos").
   * break: recreo. empty: hora libre; con `onAssign` muestra "Asignar".
   */
  type: 'assigned' | 'break' | 'empty';
  courses?: ScheduleCourse[];
  /** Aula o aulas, ej. "A-401" o "E-401 · Z-908". */
  room?: string;
  /** Sección a la derecha del aula (Figma > Vista = Profesor), ej. "3A". */
  section?: string;
  /** Texto del recreo. */
  label?: string;
  /** En la vista del coordinador, la hora libre se puede asignar. */
  onAssign?: () => void;
  className?: string;
};

const Titulo = ({
  children,
  warning,
}: {
  children: React.ReactNode;
  warning?: boolean;
}) => (
  <p className="flex items-center gap-2">
    <span
      className={cn(
        'size-2 shrink-0 rounded-[2px]',
        warning ? 'bg-amber-600' : 'bg-deep-blue-700',
      )}
      aria-hidden="true"
    />
    <span
      className={cn(
        'min-w-0 flex-1 text-label-2 font-medium',
        warning ? 'text-content-warning' : 'text-content-secondary',
      )}
    >
      {children}
    </span>
  </p>
);

// Figma > Componentes > Bloque horario (Tipo = Asignado, Sin asignar, Subgrupos, Recreo · Vista = Coordinador, Profesor)
export const ScheduleBlock = ({
  type,
  courses = [],
  room,
  section,
  label = 'Recreo',
  onAssign,
  className,
}: ScheduleBlockProps) => {
  if (type === 'break') {
    return (
      <div
        className={cn(
          'rounded-lg border border-stroke-warning bg-surface-warning-light p-4',
          className,
        )}
      >
        <Titulo warning>{label}</Titulo>
      </div>
    );
  }

  if (type === 'empty') {
    return onAssign ? (
      <button
        type="button"
        onClick={onAssign}
        className={cn(
          'flex min-h-[102px] w-full flex-col items-center justify-center gap-1 rounded-lg border border-dashed border-stroke-secondary bg-surface-secondary p-4 text-content-quartiary outline-none hover:bg-surface-tertiary focus-visible:ring-2 focus-visible:ring-stroke-focus',
          className,
        )}
      >
        <Plus className="size-8" aria-hidden="true" />
        <span className="text-caption-1">Asignar</span>
      </button>
    ) : (
      <div
        className={cn(
          'min-h-[102px] rounded-lg border border-stroke-secondary bg-surface-secondary',
          className,
        )}
      />
    );
  }

  const subgrupos = courses.length > 1;
  return (
    <div
      className={cn(
        'flex min-h-[102px] flex-col rounded-lg border border-stroke-secondary bg-surface-accent-light p-4',
        className,
      )}
    >
      <div className="flex flex-col gap-1">
        {subgrupos && (
          <p className="text-caption-1 font-medium text-content-accent-primary">
            Subgrupos
          </p>
        )}
        {courses.map((c) => (
          <React.Fragment key={c.name}>
            <Titulo>{c.name}</Titulo>
            {c.teacher && (
              <p className="text-caption-1 text-content-tertiary">
                {c.teacher}
              </p>
            )}
          </React.Fragment>
        ))}
      </div>
      {(room || section) && (
        <p className="mt-auto flex items-end pt-3 text-caption-1">
          <span className="flex-1 text-content-quartiary">{room}</span>
          {section && (
            <span className="font-medium text-content-accent-primary">
              {section}
            </span>
          )}
        </p>
      )}
    </div>
  );
};
