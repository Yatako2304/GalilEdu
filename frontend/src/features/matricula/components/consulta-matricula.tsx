import {
  AlertCircle,
  Check,
  ChevronDown,
  CircleCheck,
  FileText,
  WalletCards,
} from 'lucide-react';

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
import { Spinner } from '@/components/ui/spinner';
import { Tag, TagProps } from '@/components/ui/tag';
import {
  EstadoMatricula,
  EstadoMatriculaApoderado,
  HijoApoderado,
} from '@/types/api';
import { cn } from '@/utils/cn';

const estados: Record<
  EstadoMatricula,
  { etiqueta: string; color: NonNullable<TagProps['color']> }
> = {
  SECCION_RESERVADA: { etiqueta: 'Sección reservada', color: 'info' },
  EN_VALIDACION: { etiqueta: 'En validación', color: 'info' },
  PENDIENTE_PAGO: { etiqueta: 'Pendiente de pago', color: 'warning' },
  MATRICULADO: { etiqueta: 'Matriculado', color: 'success' },
  RETIRADO: { etiqueta: 'Retirado', color: 'error' },
  ANULADA: { etiqueta: 'Anulada', color: 'neutral' },
};

export const datosEstadoMatricula = (estado?: EstadoMatricula | null) =>
  estado
    ? estados[estado]
    : { etiqueta: 'Pendiente de asignación', color: 'neutral' as const };

/** Estado general del proceso, independiente de la etapa alcanzada. */
export const datosResumenMatricula = (estado?: EstadoMatricula | null) => {
  if (estado === 'MATRICULADO') return estados.MATRICULADO;
  if (estado === 'RETIRADO') return estados.RETIRADO;
  if (estado === 'ANULADA') return estados.ANULADA;
  return { etiqueta: 'Matrícula pendiente', color: 'warning' as const };
};

export const fechaPeriodo = (fecha?: string) => {
  if (!fecha) return '—';
  return new Intl.DateTimeFormat('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    timeZone: 'America/Lima',
  })
    .format(new Date(fecha))
    .replace('.', '');
};

const iniciales = (nombre: string) =>
  nombre
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0])
    .join('')
    .toUpperCase();

const soles = (monto: number) =>
  `S/ ${new Intl.NumberFormat('es-PE', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(monto)}`;

const fechaDeuda = (valor: string) =>
  new Intl.DateTimeFormat('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
    .format(new Date(`${valor}T12:00:00`))
    .replace('.', '');

const nombresDeudores = (
  deuda: EstadoMatriculaApoderado['deuda'],
  estudianteId?: string,
) =>
  Array.from(
    new Set(
      deuda.cuotas
        .filter((cuota) => cuota.estudianteId !== estudianteId)
        .map((cuota) => cuota.estudiante),
    ),
  );

const mensajeDeuda = (
  deuda: EstadoMatriculaApoderado['deuda'],
  elegido?: HijoApoderado,
) => {
  if (!elegido) return 'Hay una deuda familiar pendiente por regularizar.';

  const tieneDeudaPropia = deuda.cuotas.some(
    (cuota) => cuota.estudianteId === elegido.id,
  );
  const otrosDeudores = nombresDeudores(deuda, elegido.id);
  const nombresOtros = new Intl.ListFormat('es-PE', {
    style: 'long',
    type: 'conjunction',
  }).format(otrosDeudores);

  if (tieneDeudaPropia && otrosDeudores.length) {
    return `${elegido.nombre} tiene pagos pendientes y también hay una deuda de ${nombresOtros}. Total familiar: ${soles(deuda.total)}.`;
  }

  if (tieneDeudaPropia) {
    return `${elegido.nombre} tiene una deuda pendiente de ${soles(deuda.total)}. Regularízala para continuar con su matrícula.`;
  }

  return `${elegido.nombre} no tiene deuda propia. Su matrícula está bloqueada por una deuda pendiente de ${nombresOtros} por ${soles(deuda.total)}.`;
};

type SelectorProps = {
  hijos: HijoApoderado[];
  elegido?: HijoApoderado;
  abierto?: boolean;
  onElegir: (id: string) => void;
};

const SelectorEstudiante = ({
  hijos,
  elegido,
  abierto,
  onElegir,
}: SelectorProps) => (
  <Card className="flex min-h-[76px] items-center justify-between gap-4 p-4">
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          className="flex min-w-0 items-center gap-3 rounded-lg text-left outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus"
          aria-label="Seleccionar estudiante"
        >
          <span className="inline-flex size-11 shrink-0 items-center justify-center rounded-full bg-surface-accent-light text-label-2 font-semibold text-content-accent-primary">
            {elegido ? iniciales(elegido.nombre) : '—'}
          </span>
          <span className="min-w-0">
            <span className="block truncate text-label-2 font-medium text-content-primary">
              {elegido?.nombre ?? 'Selecciona un estudiante'}
            </span>
            <span className="block truncate text-body-4 text-content-quartiary">
              {elegido?.grado ?? 'Sin grado asignado'}
            </span>
          </span>
          <ChevronDown
            className="size-4 shrink-0 text-content-quartiary"
            aria-hidden="true"
          />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="start"
        className="w-[min(360px,calc(100vw-32px))] rounded-[12px] border-stroke-tertiary bg-white p-2"
      >
        <DropdownMenuLabel className="px-3 py-2 text-caption-1 font-medium uppercase tracking-wide text-content-quartiary">
          Estudiantes a tu cargo ({hijos.length})
        </DropdownMenuLabel>
        <DropdownMenuSeparator className="bg-stroke-tertiary" />
        {hijos.map((hijo) => {
          const seleccionado = hijo.id === elegido?.id;
          return (
            <DropdownMenuItem
              key={hijo.id}
              onSelect={() => onElegir(hijo.id)}
              className="gap-3 rounded-lg p-3 focus:bg-surface-secondary"
            >
              <span className="inline-flex size-9 shrink-0 items-center justify-center rounded-full bg-surface-accent-light text-caption-1 font-semibold text-content-accent-primary">
                {iniciales(hijo.nombre)}
              </span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-body-3 font-medium text-content-primary">
                  {hijo.nombre}
                </span>
                <span className="block truncate text-body-4 text-content-quartiary">
                  {hijo.grado} ·{' '}
                  {datosEstadoMatricula(hijo.estadoMatricula).etiqueta}
                </span>
              </span>
              {seleccionado && (
                <Check
                  className="size-4 text-content-accent-primary"
                  aria-label="Seleccionado"
                />
              )}
            </DropdownMenuItem>
          );
        })}
      </DropdownMenuContent>
    </DropdownMenu>
    <Tag color={abierto ? 'success' : 'neutral'} size="md">
      {abierto ? 'Periodo abierto' : 'Periodo cerrado'}
    </Tag>
  </Card>
);

const pasos = [
  { titulo: 'Datos y documentos', icono: FileText },
  { titulo: 'Validación', icono: CircleCheck },
  { titulo: 'Pago de matrícula', icono: WalletCards },
  { titulo: 'Matriculado', icono: Check },
];

const ProgresoMatricula = ({ pasoActual }: { pasoActual: number }) => (
  <Card className="p-5">
    <p className="mb-5 text-label-2 font-medium text-content-primary">
      Progreso de la matrícula
    </p>
    <ol className="grid gap-4 md:grid-cols-4">
      {pasos.map(({ titulo, icono: Icono }, index) => {
        const numero = index + 1;
        const completado = pasoActual > numero;
        const activo = pasoActual === numero;
        return (
          <li
            key={titulo}
            className="relative flex items-center gap-3 md:block"
          >
            <div className="flex items-center md:mb-3">
              <span
                className={cn(
                  'inline-flex size-9 shrink-0 items-center justify-center rounded-full border',
                  completado &&
                    'border-growth-green-500 bg-growth-green-500 text-white',
                  activo &&
                    'border-stroke-focus bg-surface-accent-light text-content-accent-primary',
                  !completado &&
                    !activo &&
                    'border-stroke-tertiary bg-white text-content-placeholder',
                )}
              >
                {completado ? (
                  <Check className="size-4" />
                ) : (
                  <Icono className="size-4" />
                )}
              </span>
              {index < pasos.length - 1 && (
                <span
                  className={cn(
                    'mx-2 hidden h-px flex-1 md:block',
                    completado ? 'bg-growth-green-500' : 'bg-stroke-tertiary',
                  )}
                />
              )}
            </div>
            <div>
              <p
                className={cn(
                  'text-body-3 font-medium',
                  activo || completado
                    ? 'text-content-primary'
                    : 'text-content-quartiary',
                )}
              >
                {titulo}
              </p>
              <p className="text-caption-1 text-content-quartiary">
                Paso {numero} de 4
              </p>
            </div>
          </li>
        );
      })}
    </ol>
  </Card>
);

const DeudaPendiente = ({
  deuda,
}: {
  deuda: EstadoMatriculaApoderado['deuda'];
}) => (
  <Card>
    <CardHeader>
      <div>
        <CardTitle>Deuda pendiente</CardTitle>
        <p className="mt-1 text-body-3 text-content-quartiary">
          Regulariza estos pagos para poder iniciar la matrícula.
        </p>
      </div>
    </CardHeader>
    <CardContent className="space-y-4">
      <ul className="divide-y divide-stroke-tertiary">
        {deuda.cuotas.map((cuota) => (
          <li
            key={`${cuota.estudianteId}-${cuota.concepto}-${cuota.fechaVencimiento}`}
            className="flex items-start justify-between gap-4 py-3 first:pt-0"
          >
            <div>
              <p className="text-body-3 font-medium text-content-primary">
                {cuota.concepto}
              </p>
              <p className="text-body-4 text-content-quartiary">
                {cuota.estudiante} · Venció el{' '}
                {fechaDeuda(cuota.fechaVencimiento)}
              </p>
            </div>
            <span className="whitespace-nowrap text-body-3 font-medium text-content-primary">
              {soles(cuota.monto)}
            </span>
          </li>
        ))}
      </ul>
      <div className="flex items-center justify-between border-t border-stroke-tertiary pt-4">
        <span className="text-label-2 font-medium text-content-primary">
          Total a pagar
        </span>
        <strong className="text-h6 text-content-primary">
          {soles(deuda.total)}
        </strong>
      </div>
    </CardContent>
  </Card>
);

type ConsultaMatriculaProps = {
  hijos: HijoApoderado[];
  estudianteId?: string;
  estado?: EstadoMatriculaApoderado;
  isLoading: boolean;
  onElegir: (id: string) => void;
};

export const ConsultaMatricula = ({
  hijos,
  estudianteId,
  estado,
  isLoading,
  onElegir,
}: ConsultaMatriculaProps) => {
  const elegido = hijos.find((hijo) => hijo.id === estudianteId);
  const impedimentos = estado?.impedimentos ?? [];
  const otrosImpedimentos = impedimentos.filter(
    (impedimento) => impedimento.tipo !== 'DEUDA',
  );
  const estadoActual = datosEstadoMatricula(
    estado?.matricula?.estado ?? elegido?.estadoMatricula,
  );

  if (isLoading) {
    return (
      <div
        className="flex min-h-64 items-center justify-center"
        data-testid="matricula-loading"
      >
        <Spinner size="lg" />
      </div>
    );
  }

  if (!hijos.length) {
    return (
      <Card className="p-8 text-center text-body-3 text-content-quartiary">
        No hay estudiantes asociados a tu cuenta.
      </Card>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      <SelectorEstudiante
        hijos={hijos}
        elegido={elegido}
        abierto={estado?.periodo.abierto}
        onElegir={onElegir}
      />

      {otrosImpedimentos.map((impedimento) => (
        <div
          key={`${impedimento.tipo}-${impedimento.mensaje}`}
          className="flex gap-3 rounded-[12px] border border-stroke-error-secondary bg-surface-error-light px-4 py-3 text-content-error"
          role="alert"
        >
          <AlertCircle className="mt-0.5 size-5 shrink-0" aria-hidden="true" />
          <p className="text-body-3">{impedimento.mensaje}</p>
        </div>
      ))}

      {estado && estado.deuda.total > 0 && (
        <div
          className="flex gap-3 rounded-[12px] border border-stroke-error-secondary bg-surface-error-light px-4 py-3 text-content-error"
          role="alert"
        >
          <AlertCircle className="mt-0.5 size-5 shrink-0" aria-hidden="true" />
          <p className="text-body-3">{mensajeDeuda(estado.deuda, elegido)}</p>
        </div>
      )}

      <ProgresoMatricula pasoActual={estado?.pasoActual ?? 0} />

      {estado && estado.deuda.total > 0 && (
        <DeudaPendiente deuda={estado.deuda} />
      )}

      <Card>
        <CardHeader>
          <div>
            <CardTitle>Estado de matrícula</CardTitle>
            <p className="mt-1 text-body-3 text-content-quartiary">
              Información asignada para el periodo {estado?.periodo.anio}
            </p>
          </div>
        </CardHeader>
        <CardContent>
          <dl className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <dt className="text-caption-1 text-content-quartiary">Grado</dt>
              <dd className="mt-1 text-body-3 font-medium text-content-primary">
                {estado?.matricula?.grado ?? estado?.estudiante.grado ?? '—'}
              </dd>
            </div>
            <div>
              <dt className="text-caption-1 text-content-quartiary">Sección</dt>
              <dd className="mt-1 text-body-3 font-medium text-content-primary">
                {estado?.matricula?.seccion ?? 'Se asigna al confirmar'}
              </dd>
            </div>
            <div>
              <dt className="text-caption-1 text-content-quartiary">Estado</dt>
              <dd className="mt-1">
                <Tag color={estadoActual.color} size="md">
                  {estadoActual.etiqueta}
                </Tag>
              </dd>
            </div>
            <div>
              <dt className="text-caption-1 text-content-quartiary">
                Cierre del periodo
              </dt>
              <dd className="mt-1 text-body-3 font-medium text-content-primary">
                {fechaPeriodo(estado?.periodo.cierre)}
              </dd>
            </div>
          </dl>
          {estado?.matricula?.codigo && (
            <p className="mt-5 border-t border-stroke-tertiary pt-4 text-body-3 text-content-secondary">
              Código de matrícula:{' '}
              <strong className="text-content-primary">
                {estado.matricula.codigo}
              </strong>
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
};
