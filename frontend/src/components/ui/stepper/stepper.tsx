import { Check } from 'lucide-react';

import { cn } from '@/utils/cn';

export type StepState = 'incompleted' | 'active' | 'completed' | 'error';

export type Step = {
  label: string;
  description?: string;
  /** Si no se indica, se calcula con `current`: antes = completado, igual = activo, después = pendiente. */
  state?: StepState;
};

// Figma > Componentes > Steps (State = Incompleted, Active, Completed, Error)
const circulo: Record<StepState, string> = {
  incompleted: 'border-stroke-primary bg-surface-primary text-content-tertiary',
  active:
    'border-stroke-primary-solid bg-surface-primary-pressed text-content-primary',
  completed:
    'border-stroke-accent-secondary bg-surface-accent-secondary text-white',
  error: 'border-stroke-error bg-surface-error text-white',
};

const etiqueta: Record<StepState, string> = {
  incompleted: 'text-content-disabled',
  active: 'text-content-primary',
  completed: 'text-content-secondary',
  error: 'text-content-error',
};

type StepItemProps = Step & {
  number: number;
  state: StepState;
  first: boolean;
  last: boolean;
};

const StepItem = ({
  label,
  description,
  number,
  state,
  first,
  last,
}: StepItemProps) => (
  <li
    className="flex min-w-0 flex-1 flex-col items-center gap-1 text-center"
    aria-current={state === 'active' ? 'step' : undefined}
  >
    <div className="flex w-full items-center">
      <span
        className={cn(
          'h-px flex-1',
          first ? 'bg-transparent' : 'bg-stroke-secondary',
        )}
      />
      <span
        className={cn(
          'inline-flex size-6 shrink-0 items-center justify-center rounded-full border text-label-2',
          circulo[state],
        )}
      >
        {state === 'completed' ? (
          <Check className="size-4" aria-hidden="true" />
        ) : state === 'error' ? (
          <span aria-hidden="true">!</span>
        ) : (
          number
        )}
      </span>
      <span
        className={cn(
          'h-px flex-1',
          last ? 'bg-transparent' : 'bg-stroke-secondary',
        )}
      />
    </div>
    <p className={cn('text-label-2', etiqueta[state])}>{label}</p>
    {description && (
      <p
        className={cn(
          'text-body-4',
          state === 'active'
            ? 'text-content-secondary'
            : 'text-content-disabled',
        )}
      >
        {description}
      </p>
    )}
  </li>
);

export type StepperProps = {
  steps: Step[];
  /** Paso actual, empezando en 1. */
  current: number;
  className?: string;
};

// Figma > Componentes > Stepper (N° Steps = 2 a 6)
export const Stepper = ({ steps, current, className }: StepperProps) => (
  <ol className={cn('flex w-full items-start', className)}>
    {steps.map((step, i) => {
      const numero = i + 1;
      const estado: StepState =
        step.state ??
        (numero < current
          ? 'completed'
          : numero === current
            ? 'active'
            : 'incompleted');
      return (
        <StepItem
          key={step.label}
          {...step}
          number={numero}
          state={estado}
          first={i === 0}
          last={i === steps.length - 1}
        />
      );
    })}
  </ol>
);
