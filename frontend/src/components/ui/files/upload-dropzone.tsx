import { cva } from 'class-variance-authority';
import { CloudUpload, Upload } from 'lucide-react';
import * as React from 'react';

import fondo from '@/assets/dropzone-fondo.png';
import { cn } from '@/utils/cn';

import { FileIcon, tipoDeArchivo } from './file-item';

// Figma > Componentes > Upload Dropzone (State = Default, Hover, Error, Uploaded · Size = Large, Medium, Small)
const zonaVariants = cva(
  'relative flex w-full flex-col items-center justify-center overflow-hidden rounded-xl border-2 border-dashed bg-cover bg-center text-center transition-colors',
  {
    variants: {
      size: {
        lg: 'gap-4 px-6 py-10',
        md: 'gap-3 px-5 py-8',
        sm: 'gap-2 px-4 py-6',
      },
      state: {
        default: 'border-stroke-primary bg-surface-primary',
        hover: 'border-stroke-primary bg-surface-primary-hover',
        error: 'border-stroke-error bg-surface-primary',
      },
    },
    defaultVariants: { size: 'lg', state: 'default' },
  },
);

const formatoTamano = (bytes: number) =>
  bytes >= 1024 * 1024
    ? `${(bytes / (1024 * 1024)).toFixed(1)} MB`
    : `${Math.max(1, Math.round(bytes / 1024))} KB`;

export type UploadDropzoneProps = {
  /** Se llama con el archivo elegido o soltado. La validación (formato, tamaño) la hace quien lo usa. */
  onFile: (file: File) => void;
  /** Tipos aceptados por el selector de archivos, ej. ".xlsx,.xls,.csv". */
  accept?: string;
  /** Texto de formatos (Figma: "PDF, DOC, DOCX, XLS, XLSX"). */
  formats: string;
  /** Tamaño máximo para mostrar (Figma: "10 MB"). */
  maxSize: string;
  /** Archivo ya elegido: muestra su nombre, tamaño y "Cambiar archivo" (Figma: State = Uploaded). */
  file?: { name: string; size: number; type?: string } | null;
  /** Mensaje de error debajo de la zona (Figma: State = Error). */
  error?: string | null;
  size?: 'lg' | 'md' | 'sm';
  disabled?: boolean;
  className?: string;
};

export const UploadDropzone = ({
  onFile,
  accept,
  formats,
  maxSize,
  file,
  error,
  size = 'lg',
  disabled = false,
  className,
}: UploadDropzoneProps) => {
  const entrada = React.useRef<HTMLInputElement>(null);
  const [arrastrando, setArrastrando] = React.useState(false);
  const elegir = () => entrada.current?.click();
  const estado = error ? 'error' : arrastrando ? 'hover' : 'default';

  return (
    <div className={cn('flex w-full flex-col gap-2', className)}>
      <div
        onDragOver={(e) => {
          if (disabled) return;
          e.preventDefault();
          setArrastrando(true);
        }}
        onDragLeave={() => setArrastrando(false)}
        onDrop={(e) => {
          e.preventDefault();
          setArrastrando(false);
          const soltado = e.dataTransfer.files?.[0];
          if (soltado && !disabled) onFile(soltado);
        }}
        style={{ backgroundImage: `url(${fondo})` }}
        className={zonaVariants({ size, state: estado })}
      >
        <input
          ref={entrada}
          type="file"
          accept={accept}
          disabled={disabled}
          className="sr-only"
          aria-label="Seleccionar archivo"
          onChange={(e) => {
            const elegido = e.target.files?.[0];
            if (elegido) onFile(elegido);
            e.target.value = '';
          }}
        />
        {file ? (
          <>
            <span className="inline-flex rounded-full bg-surface-accent-light p-2">
              <FileIcon type={tipoDeArchivo(file.name, file.type)} />
            </span>
            <div className="flex w-full flex-col items-center gap-2">
              <p className="w-full truncate text-label-1 font-bold text-content-primary">
                {file.name}
              </p>
              <p className="text-body-4 text-content-quartiary">
                {formatoTamano(file.size)}
              </p>
            </div>
            <button
              type="button"
              onClick={elegir}
              disabled={disabled}
              className="rounded-md p-0.5 text-label-3 font-medium text-content-accent-primary hover:underline disabled:opacity-50"
            >
              Cambiar Archivo
            </button>
          </>
        ) : (
          <>
            <span
              className={cn(
                'inline-flex rounded-full p-3',
                error
                  ? 'bg-surface-error-light text-content-error'
                  : 'bg-surface-accent-light text-content-accent-secondary',
              )}
              aria-hidden="true"
            >
              <CloudUpload className="size-6" />
            </span>
            <div className="flex w-full flex-col items-center gap-2">
              <p className="text-label-1 font-bold text-content-primary">
                Arrastra y suelta tu archivo aquí
              </p>
              <p className="text-body-4 text-content-quartiary">
                {formats} (Máx. {maxSize})
              </p>
            </div>
            <button
              type="button"
              onClick={elegir}
              disabled={disabled}
              className={cn(
                'inline-flex items-center justify-center gap-1.5 rounded-lg px-6 py-3 text-label-2 font-medium transition-colors disabled:opacity-50',
                error
                  ? 'bg-surface-error-light text-content-error'
                  : 'bg-surface-accent-light text-content-accent-secondary hover:bg-surface-accent-light-hover hover:text-vibrant-blue-600',
              )}
            >
              <Upload className="size-4" aria-hidden="true" />
              Seleccionar Archivo
            </button>
          </>
        )}
      </div>
      {error && (
        <p role="alert" className="text-label-3 text-content-error">
          {error}
        </p>
      )}
    </div>
  );
};
