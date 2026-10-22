import { Meta, StoryObj } from '@storybook/react';
import { Download, Eye } from 'lucide-react';
import * as React from 'react';

import { FileItem, fechaArchivo, horaArchivo } from './file-item';
import { UploadDropzone } from './upload-dropzone';

const acciones = (
  <>
    <button type="button" aria-label="Ver" className="rounded-full p-1">
      <Eye className="size-5" />
    </button>
    <button type="button" aria-label="Descargar" className="rounded-full p-1">
      <Download className="size-5" />
    </button>
  </>
);

const meta: Meta<typeof FileItem> = {
  title: 'Components/Files',
  component: FileItem,
};

export default meta;
type Story = StoryObj<typeof FileItem>;

const modificado = '2026-03-01T10:00';

export const Archivos: Story = {
  render: () => (
    <div className="flex max-w-lg flex-col gap-2">
      <FileItem name="Plan anual.docx" meta="1.20 MB" actions={acciones} />
      <FileItem name="Notas.xlsx" meta="1.20 MB" actions={acciones} />
      <FileItem
        name="DNI apoderado.pdf"
        meta={`Última modificación: ${fechaArchivo(modificado)} - ${horaArchivo(modificado, true)}`}
        actions={acciones}
      />
      <FileItem name="Clase grabada.mp4" meta="12 MB" actions={acciones} />
    </div>
  ),
};

export const Versiones: Story = {
  render: () => (
    <div className="flex max-w-lg flex-col gap-2">
      <FileItem name="Plan anual.docx" version="Versión 2" />
      <FileItem name="Plan anual.docx" version="Versión 1" />
    </div>
  ),
};

const DemoDropzone = ({ error }: { error?: string }) => {
  const [archivo, setArchivo] = React.useState<File | null>(null);
  return (
    <UploadDropzone
      formats="XLSX, XLS o CSV"
      maxSize="10 MB"
      file={archivo}
      error={error}
      onFile={setArchivo}
    />
  );
};

export const Dropzone: Story = { render: () => <DemoDropzone /> };
export const DropzoneConError: Story = {
  render: () => <DemoDropzone error="El archivo supera los 10 MB." />,
};
