import { X } from 'lucide-react';
import * as React from 'react';

import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { FileItem, UploadDropzone } from '@/components/ui/files';
import { Select, Textarea } from '@/components/ui/form';
import { Tag } from '@/components/ui/tag';
import { FilaAsistencia, MotivoJustificacion } from '@/types/api';

import { SolicitudJustificacion } from '../api/asistencia';
import {
  fechaLarga,
  MOTIVOS_JUSTIFICACION,
  nombreCompleto,
} from '../lib/asistencia';

const TIPOS = ['application/pdf', 'image/jpeg', 'image/png'];
const MAXIMO_BYTES = 5 * 1024 * 1024;
const MAXIMO_DETALLE = 500;

type DialogoJustificarProps = {
  fila: FilaAsistencia | null;
  /** "1.° A" */
  seccion: string;
  fecha: string;
  onOpenChange: (open: boolean) => void;
  onGuardar: (solicitud: SolicitudJustificacion) => void;
  cargando: boolean;
};

// Figma > Tutor > "Frame 57": modales "Justificar inasistencia" y "Justificar Tardanza" (HU-67).
export const DialogoJustificar = ({
  fila,
  ...props
}: DialogoJustificarProps) => (
  <Dialog open={fila !== null} onOpenChange={props.onOpenChange}>
    <DialogContent className="max-w-screen-sm gap-0 rounded-xl border-stroke-tertiary bg-white p-0">
      {/* Se monta de nuevo en cada apertura: el formulario empieza vacío. */}
      {fila && <Formulario fila={fila} {...props} />}
    </DialogContent>
  </Dialog>
);

const Formulario = ({
  fila,
  seccion,
  fecha,
  onOpenChange,
  onGuardar,
  cargando,
}: DialogoJustificarProps & { fila: FilaAsistencia }) => {
  const [motivo, setMotivo] = React.useState<MotivoJustificacion | ''>('');
  const [detalle, setDetalle] = React.useState('');
  const [archivo, setArchivo] = React.useState<File | null>(null);
  const [errorArchivo, setErrorArchivo] = React.useState<string | null>(null);
  const [enviado, setEnviado] = React.useState(false);
  const esFalta = fila.estado === 'FALTA';
  const errorMotivo = enviado && !motivo ? 'Elige el motivo.' : undefined;
  const errorDetalle =
    enviado && motivo === 'OTRO' && !detalle.trim()
      ? 'Describe el motivo en el detalle.'
      : undefined;

  const elegirArchivo = (nuevo: File) => {
    if (!TIPOS.includes(nuevo.type)) {
      setErrorArchivo('El sustento debe ser PDF, JPG o PNG.');
      return;
    }
    if (nuevo.size > MAXIMO_BYTES) {
      setErrorArchivo('El sustento no puede pesar más de 5 MB.');
      return;
    }
    setErrorArchivo(null);
    setArchivo(nuevo);
  };

  const guardar = () => {
    setEnviado(true);
    if (!motivo || (motivo === 'OTRO' && !detalle.trim())) return;
    onGuardar({ matriculaId: fila.matriculaId, motivo, detalle, archivo });
  };

  return (
    <>
      <div className="flex items-start justify-between gap-4 border-b border-stroke-tertiary px-8 pb-4 pt-8">
        <div className="flex flex-col gap-2">
          <DialogTitle className="text-h6 font-semibold text-content-primary">
            {esFalta ? 'Justificar inasistencia' : 'Justificar tardanza'}
          </DialogTitle>
          <DialogDescription className="text-body-3 text-content-secondary">
            {esFalta
              ? 'Se justificará esta falta y se eximirá de penalización académica'
              : 'Se justificará esta tardanza y se eximirá de penalización académica'}
          </DialogDescription>
        </div>
      </div>
      <div className="flex flex-col gap-5 px-8 py-6">
        <div className="flex flex-wrap items-center justify-between gap-4 rounded-lg border border-stroke-tertiary px-5 py-4">
          <div className="flex items-center gap-4">
            <Avatar name={nombreCompleto(fila)} />
            <div className="flex flex-col">
              <span className="text-body-1 font-medium text-content-primary">
                {nombreCompleto(fila)}
              </span>
              <span className="text-body-4 text-content-tertiary">
                {seccion}
              </span>
            </div>
          </div>
          <div className="flex flex-col items-end gap-2">
            <span className="text-body-4 font-semibold text-content-primary">
              {fechaLarga(fecha)}
            </span>
            <Tag color={esFalta ? 'error' : 'warning'} appearance="outline">
              {esFalta ? 'Falta registrada' : 'Tardanza registrada'}
            </Tag>
          </div>
        </div>
        <Select
          label="Motivo"
          placeholder="Selecciona"
          options={MOTIVOS_JUSTIFICACION}
          value={motivo}
          onChange={(evento) =>
            setMotivo(evento.target.value as MotivoJustificacion)
          }
          error={
            errorMotivo ? { type: 'required', message: errorMotivo } : undefined
          }
        />
        <Textarea
          label="Detalle"
          value={detalle}
          maxLength={MAXIMO_DETALLE}
          showCount
          onChange={(evento) => setDetalle(evento.target.value)}
          error={
            errorDetalle
              ? { type: 'required', message: errorDetalle }
              : undefined
          }
        />
        {archivo ? (
          <FileItem
            name={archivo.name}
            mimeType={archivo.type}
            meta={`${(archivo.size / 1024 / 1024).toFixed(2)} MB`}
            actions={
              <Button
                variant="ghost"
                size="icon"
                aria-label={`Quitar ${archivo.name}`}
                icon={<X className="size-4" aria-hidden="true" />}
                onClick={() => setArchivo(null)}
              />
            }
          />
        ) : (
          <UploadDropzone
            onFile={elegirArchivo}
            accept=".pdf,.jpg,.jpeg,.png"
            formats="PDF, JPG o PNG (opcional)"
            maxSize="5 MB"
            error={errorArchivo}
            size="sm"
          />
        )}
      </div>
      <div className="flex justify-between gap-3 border-t border-stroke-tertiary px-8 py-4">
        <Button
          variant="tertiary"
          onClick={() => onOpenChange(false)}
          disabled={cargando}
        >
          Cancelar
        </Button>
        <Button variant="primary" onClick={guardar} isLoading={cargando}>
          Guardar justificación
        </Button>
      </div>
    </>
  );
};
