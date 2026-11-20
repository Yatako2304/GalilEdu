import { ArrowRight, ListChecks, Pencil } from 'lucide-react';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { IconBadge } from '@/components/ui/icon-badge';
import { Tag } from '@/components/ui/tag';
import { EstadoAsistencia } from '@/types/api';

import {
  Cambio,
  COLOR_ESTADO,
  ETIQUETA_ESTADO,
  nombreCompleto,
} from '../lib/asistencia';

type MarcoProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  icono: React.ReactNode;
  titulo: string;
  descripcion: string;
  accion: string;
  onConfirmar: () => void;
  cargando: boolean;
  children: React.ReactNode;
};

const Marco = ({
  open,
  onOpenChange,
  icono,
  titulo,
  descripcion,
  accion,
  onConfirmar,
  cargando,
  children,
}: MarcoProps) => (
  <Dialog open={open} onOpenChange={onOpenChange}>
    <DialogContent className="max-w-[520px] gap-0 rounded-xl border-stroke-tertiary bg-white p-0">
      <div className="flex flex-col gap-4 px-8 pb-6 pt-8">
        <IconBadge color="accent" size="lg">
          {icono}
        </IconBadge>
        <div className="flex flex-col gap-2">
          <DialogTitle className="text-h6 font-semibold text-content-primary">
            {titulo}
          </DialogTitle>
          <DialogDescription className="text-body-3 text-content-tertiary">
            {descripcion}
          </DialogDescription>
        </div>
        {children}
      </div>
      <div className="flex justify-end gap-3 border-t border-stroke-tertiary px-6 py-4">
        <Button
          variant="tertiary"
          onClick={() => onOpenChange(false)}
          disabled={cargando}
        >
          Cancelar
        </Button>
        <Button variant="primary" onClick={onConfirmar} isLoading={cargando}>
          {accion}
        </Button>
      </div>
    </DialogContent>
  </Dialog>
);

type DialogoGuardarProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  contexto: string;
  resumen: {
    presentes: number;
    tardanzas: number;
    faltas: number;
    total: number;
  };
  onConfirmar: () => void;
  cargando: boolean;
};

// Figma > Tutor > "Modal - ¿Guardar la asistencia de hoy?" (AS-04).
export const DialogoGuardarAsistencia = ({
  resumen,
  contexto,
  ...props
}: DialogoGuardarProps) => (
  <Marco
    {...props}
    icono={<ListChecks className="size-5" aria-hidden="true" />}
    titulo="¿Guardar la asistencia de hoy?"
    descripcion={contexto}
    accion="Guardar asistencia"
  >
    <dl className="flex flex-col gap-3 rounded-lg bg-surface-secondary px-4 py-3 text-body-3">
      {[
        ['Presentes', resumen.presentes],
        ['Tardanzas', resumen.tardanzas],
        ['Faltas', resumen.faltas],
      ].map(([etiqueta, valor]) => (
        <div key={etiqueta} className="flex justify-between">
          <dt className="text-content-tertiary">{etiqueta}</dt>
          <dd className="font-medium text-content-primary">{valor}</dd>
        </div>
      ))}
      <div className="flex justify-between">
        <dt className="text-content-tertiary">Total</dt>
        <dd className="font-medium text-content-primary">
          {resumen.total} estudiantes
        </dd>
      </div>
    </dl>
    <Alert
      type="info"
      title="Podrás corregirla solo hasta el final del día de hoy."
    >
      Quedará registrado que tú la guardaste y a qué hora.
    </Alert>
  </Marco>
);

const EtiquetaEstado = ({
  estado,
  hora,
}: {
  estado: EstadoAsistencia | null;
  hora: string;
}) =>
  estado ? (
    <Tag size="sm" color={COLOR_ESTADO[estado]}>
      {ETIQUETA_ESTADO[estado]}
      {estado === 'TARDANZA' && hora ? ` · ${hora}` : ''}
    </Tag>
  ) : (
    <Tag size="sm" color="neutral">
      Sin marcar
    </Tag>
  );

type DialogoCorreccionProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  contexto: string;
  cambios: Cambio[];
  onConfirmar: () => void;
  cargando: boolean;
};

// Figma > Tutor > "Modal - Confirmar corrección de asistencia" (AS-06).
export const DialogoConfirmarCorreccion = ({
  cambios,
  contexto,
  ...props
}: DialogoCorreccionProps) => (
  <Marco
    {...props}
    icono={<Pencil className="size-5" aria-hidden="true" />}
    titulo="Confirmar corrección de asistencia"
    descripcion={contexto}
    accion="Confirmar cambios"
  >
    <div className="flex flex-col gap-3 rounded-lg bg-surface-secondary px-4 py-3">
      <p className="text-body-3 font-semibold text-content-primary">
        {cambios.length === 1 ? '1 cambio' : `${cambios.length} cambios`}
      </p>
      <ul className="flex max-h-60 flex-col gap-2 overflow-y-auto">
        {cambios.map(({ fila, antes, despues }) => (
          <li
            key={fila.matriculaId}
            className="flex items-center justify-between gap-3 text-body-3 text-content-secondary"
          >
            <span className="truncate">{nombreCompleto(fila)}</span>
            <span className="flex shrink-0 items-center gap-2">
              <EtiquetaEstado estado={antes.estado} hora={antes.hora} />
              <ArrowRight
                className="size-4 text-icon-tertiary"
                aria-label="cambia a"
              />
              <EtiquetaEstado estado={despues.estado} hora={despues.hora} />
            </span>
          </li>
        ))}
      </ul>
    </div>
  </Marco>
);
