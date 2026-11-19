import { CircleAlert } from 'lucide-react';

import { Avatar } from '@/components/ui/avatar';
import { Input } from '@/components/ui/form';
import { SegmentedControl } from '@/components/ui/segmented-control';
import {
  TableBody,
  TableCell,
  TableElement,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { EstadoAsistencia, FilaAsistencia } from '@/types/api';
import { cn } from '@/utils/cn';

import {
  Borrador,
  COLOR_ESTADO,
  ETIQUETA_ESTADO,
  horaCorta,
  nombreCompleto,
  OPCIONES_ASISTENCIA,
  tardanzaSinHora,
} from '../lib/asistencia';

type TablaAsistenciaProps = {
  filas: FilaAsistencia[];
  /** Número de la primera fila de la página (1, 13, 25…). */
  desde: number;
  /** null: solo lectura (AS-05). */
  borrador: Borrador | null;
  /** AS-02: después de intentar guardar se marcan los estudiantes sin marcar y las tardanzas sin hora. */
  mostrarErrores: boolean;
  /** AS-06: matrículas con un cambio sin guardar (fila resaltada y etiqueta "Modificado"). */
  cambiadas?: Set<string>;
  onMarcar: (matriculaId: string, estado: EstadoAsistencia) => void;
  onHora: (matriculaId: string, hora: string) => void;
};

// Figma > Tutor > AS-01 a AS-06: N.°, Estudiante, Asistencia y Hora de llegada.
export const TablaAsistencia = ({
  filas,
  desde,
  borrador,
  mostrarErrores,
  cambiadas,
  onMarcar,
  onHora,
}: TablaAsistenciaProps) => (
  <TableElement>
    <TableHeader>
      <TableRow>
        <TableHead className="w-16">N.°</TableHead>
        <TableHead>Estudiante</TableHead>
        <TableHead>Asistencia</TableHead>
        <TableHead className="w-40">Hora de llegada</TableHead>
      </TableRow>
    </TableHeader>
    <TableBody>
      {filas.map((fila, indice) => {
        const nombre = nombreCompleto(fila);
        const marca = borrador?.[fila.matriculaId];
        const sinMarcar = mostrarErrores && borrador !== null && !marca?.estado;
        const sinHora = mostrarErrores && tardanzaSinHora(marca);
        const cambiada = cambiadas?.has(fila.matriculaId) ?? false;
        return (
          <TableRow
            key={fila.matriculaId}
            className={cn(
              sinMarcar && 'bg-surface-error-light',
              cambiada && 'bg-surface-accent-light',
            )}
          >
            <TableCell className="text-content-tertiary">
              {desde + indice}
            </TableCell>
            <TableCell>
              <div className="flex items-center gap-3">
                <Avatar name={nombre} size="sm" />
                <div className="flex flex-col">
                  <span className="flex flex-wrap items-center gap-2 text-body-3 text-content-primary">
                    {nombre}
                    {(fila.modificado || cambiada) && (
                      <Tag size="sm" color="info">
                        Modificado
                      </Tag>
                    )}
                    {sinMarcar && (
                      <span className="inline-flex items-center gap-1 text-caption-1 text-content-error">
                        <CircleAlert className="size-3.5" aria-hidden="true" />
                        Sin marcar
                      </span>
                    )}
                  </span>
                  <span className="text-caption-1 text-content-quartiary">
                    Cód. {fila.documento}
                  </span>
                </div>
              </div>
            </TableCell>
            <TableCell>
              {borrador ? (
                <SegmentedControl
                  label={`Asistencia de ${nombre}`}
                  options={OPCIONES_ASISTENCIA}
                  value={marca?.estado ?? null}
                  onChange={(estado) => onMarcar(fila.matriculaId, estado)}
                  invalid={sinMarcar}
                />
              ) : fila.estado ? (
                <Tag size="sm" color={COLOR_ESTADO[fila.estado]}>
                  {ETIQUETA_ESTADO[fila.estado]}
                </Tag>
              ) : (
                <span className="text-content-quartiary">—</span>
              )}
            </TableCell>
            <TableCell>
              {borrador && marca?.estado === 'TARDANZA' ? (
                <Input
                  type="time"
                  aria-label={`Hora de llegada de ${nombre}`}
                  value={marca.hora}
                  onChange={(evento) =>
                    onHora(fila.matriculaId, evento.target.value)
                  }
                  error={
                    sinHora
                      ? { type: 'required', message: 'Indica la hora' }
                      : undefined
                  }
                  wrapperClassName="w-32"
                />
              ) : (
                <span className="text-body-3 text-content-secondary">
                  {!borrador && fila.estado === 'TARDANZA'
                    ? horaCorta(fila.horaLlegada)
                    : '—'}
                </span>
              )}
            </TableCell>
          </TableRow>
        );
      })}
    </TableBody>
  </TableElement>
);
