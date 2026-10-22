import dayjs from 'dayjs';
import * as React from 'react';

import iconoDoc from '@/assets/archivos/doc.svg';
import iconoDocx from '@/assets/archivos/docx.svg';
import iconoPdf from '@/assets/archivos/pdf.svg';
import iconoVideo from '@/assets/archivos/video.svg';
import iconoXls from '@/assets/archivos/xls.svg';
import iconoXlsx from '@/assets/archivos/xlsx.svg';
import { cn } from '@/utils/cn';

export type FileType = 'doc' | 'docx' | 'xls' | 'xlsx' | 'pdf' | 'video';

// Íconos de tipo de archivo de Figma > Componentes > Files (tamaño original del SVG dentro de un cuadro de 32 px).
const iconos: Record<FileType, { src: string; width: number; height: number }> =
  {
    doc: { src: iconoDoc, width: 21.94, height: 29.33 },
    docx: { src: iconoDocx, width: 21.94, height: 29.33 },
    xls: { src: iconoXls, width: 21.94, height: 29.33 },
    xlsx: { src: iconoXlsx, width: 21.94, height: 29.33 },
    pdf: { src: iconoPdf, width: 21.94, height: 29.33 },
    video: { src: iconoVideo, width: 21.94, height: 29.33 },
  };

/** Tipo de archivo según su nombre o tipo MIME. Las imágenes se muestran con el ícono de PDF (documento escaneado). */
export const tipoDeArchivo = (nombre: string, mime?: string): FileType => {
  const ext = nombre.split('.').pop()?.toLowerCase() ?? '';
  if (
    ext === 'docx' ||
    ext === 'doc' ||
    ext === 'xls' ||
    ext === 'xlsx' ||
    ext === 'pdf'
  )
    return ext;
  if (ext === 'csv') return 'xls';
  if (mime?.startsWith('video/') || ['mp4', 'mov', 'webm'].includes(ext))
    return 'video';
  return 'pdf';
};

export const FileIcon = ({
  type,
  className,
}: {
  type: FileType;
  className?: string;
}) => {
  const { src, width, height } = iconos[type];
  return (
    <span
      className={cn(
        'inline-flex size-8 shrink-0 items-center justify-center',
        className,
      )}
      aria-hidden="true"
    >
      <img src={src} width={width} height={height} alt="" />
    </span>
  );
};

/** Fecha como en Figma > Files - date: "1/03/2026". */
export const fechaArchivo = (fecha: string | Date) =>
  dayjs(fecha).format('D/MM/YYYY');

/** Hora como en Figma > Files - time: "8:00am" (sin minutos si son :00 y `sinMinutos`). */
export const horaArchivo = (fecha: string | Date, sinMinutos = false) => {
  const d = dayjs(fecha);
  return `${d.format(sinMinutos && d.minute() === 0 ? 'h' : 'h:mm')}${d.hour() < 12 ? 'am' : 'pm'}`;
};

export type FileItemProps = {
  /** Nombre completo del archivo, con su extensión. */
  name: string;
  mimeType?: string;
  /** Línea secundaria (Figma: tamaño "1.20 MB" o "Última modificación: 1/03/2026 - 10am"). */
  meta?: React.ReactNode;
  /** Botones a la derecha (ver, descargar, reemplazar, menú ⋮). */
  actions?: React.ReactNode;
  /** Versión compacta para el historial (Figma > Files_v2 > Subtype = Version): "Versión 1". */
  version?: React.ReactNode;
  className?: string;
};

// Figma > Componentes > Files y Files_v2 (Type = doc, docx, xls, pdf, xlsx, video · Subtype = Default, Version)
export const FileItem = ({
  name,
  mimeType,
  meta,
  actions,
  version,
  className,
}: FileItemProps) => (
  <div
    className={cn(
      'flex items-center gap-3 rounded-md bg-surface-secondary',
      version === undefined ? 'p-3' : 'p-2',
      className,
    )}
  >
    <div className="flex min-w-0 flex-1 items-center gap-1">
      <FileIcon
        type={tipoDeArchivo(name, mimeType)}
        className={
          version === undefined ? undefined : 'size-6 [&_img]:scale-75'
        }
      />
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <p className="truncate text-label-1 text-content-primary" title={name}>
          {name}
        </p>
        {meta && (
          <div className="text-caption-1 font-medium text-content-tertiary">
            {meta}
          </div>
        )}
      </div>
    </div>
    {version !== undefined && (
      <span className="shrink-0 text-label-3 text-content-quartiary">
        {version}
      </span>
    )}
    {actions && (
      <div className="flex shrink-0 items-center gap-1">{actions}</div>
    )}
  </div>
);
