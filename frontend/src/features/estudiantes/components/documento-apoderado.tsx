import { useQueryClient } from '@tanstack/react-query';
import { Eye, Paperclip } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { DocumentoAdjunto } from '@/types/api';

import {
  abrirDocumentoApoderado,
  subirDocumentoApoderado,
} from '../api/estudiantes';

import {
  TIPOS_DOCUMENTO_APODERADO,
  validarDocumento,
} from './inscripcion/esquema';

type DocumentoApoderadoProps = {
  estudianteId: string;
  documento: DocumentoAdjunto | null;
};

// Ficha > Familia > fila del apoderado: ver, adjuntar o reemplazar el documento que lo acredita.
export const DocumentoApoderado = ({
  estudianteId,
  documento,
}: DocumentoApoderadoProps) => {
  const queryClient = useQueryClient();
  const entrada = React.useRef<HTMLInputElement>(null);
  const [tipo, setTipo] = React.useState(
    documento?.tipo ?? TIPOS_DOCUMENTO_APODERADO[0],
  );
  const [subiendo, setSubiendo] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  const subir = async (archivo: File | undefined) => {
    if (!archivo) return;
    const problema = validarDocumento(archivo);
    setError(problema);
    if (problema) return;
    setSubiendo(true);
    try {
      await subirDocumentoApoderado(estudianteId, tipo, archivo);
      await queryClient.invalidateQueries({
        queryKey: ['estudiantes', 'ficha', estudianteId],
      });
    } finally {
      setSubiendo(false);
    }
  };

  return (
    <div className="flex flex-col gap-1">
      {documento && (
        <Button
          type="button"
          variant="link"
          size="sm"
          className="justify-start"
          icon={<Eye className="size-4" />}
          onClick={() => abrirDocumentoApoderado(estudianteId)}
          title={documento.nombreArchivo}
        >
          {documento.tipo}
        </Button>
      )}
      <div className="flex flex-col items-start gap-1">
        <select
          aria-label="Tipo de documento del apoderado"
          value={tipo}
          onChange={(e) => setTipo(e.target.value)}
          className="h-8 w-44 rounded-lg border border-stroke-primary bg-white px-2 text-body-4"
        >
          {TIPOS_DOCUMENTO_APODERADO.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
        <Button
          type="button"
          variant="primary-light"
          size="sm"
          isLoading={subiendo}
          icon={<Paperclip className="size-4" />}
          onClick={() => entrada.current?.click()}
        >
          {documento ? 'Reemplazar' : 'Adjuntar'}
        </Button>
      </div>
      <input
        ref={entrada}
        type="file"
        accept="application/pdf,image/jpeg,image/png"
        className="sr-only"
        aria-label="Archivo del documento del apoderado"
        onChange={(e) => {
          subir(e.target.files?.[0]);
          e.target.value = '';
        }}
      />
      {error && <p className="text-body-4 text-content-error">{error}</p>}
    </div>
  );
};
