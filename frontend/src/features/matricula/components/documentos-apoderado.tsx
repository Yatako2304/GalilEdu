import { Eye, Paperclip } from 'lucide-react';
import * as React from 'react';

import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { FileItem } from '@/components/ui/files';
import { useNotifications } from '@/components/ui/notifications';
import { Tag } from '@/components/ui/tag';
import { ArchivoGuardado, DocumentoApoderado } from '@/types/api';

import {
  abrirArchivoDelApoderado,
  descargarPlantillaDelApoderado,
  useDocumentosApoderado,
  useEnviarAValidacion,
  useSubirDocumentoApoderado,
} from '../api/documentos-apoderado';
import { problemaDelArchivo, tamanoLegible } from '../archivos-matricula';

import { BotonPlantilla } from './boton-plantilla';

const ESTADO_ARCHIVO: Record<
  ArchivoGuardado['estado'],
  { etiqueta: string; color: 'info' | 'success' | 'error' }
> = {
  EN_REVISION: { etiqueta: 'En revisión', color: 'info' },
  APROBADO: { etiqueta: 'Aprobado', color: 'success' },
  OBSERVADO: { etiqueta: 'Observado', color: 'error' },
};

/**
 * Figma > Apoderado > Matrícula 2027 · "Documentos de matrícula" (HU-28 descarga la plantilla, HU-29 sube el firmado y
 * lo envía a validación). Solo se puede subir con la sección reservada y el periodo abierto.
 */
export const DocumentosApoderado = ({
  estudianteId,
}: {
  estudianteId: string;
}) => {
  const { addNotification } = useNotifications();
  const consulta = useDocumentosApoderado(estudianteId);
  const subir = useSubirDocumentoApoderado(estudianteId);
  const enviar = useEnviarAValidacion(estudianteId);
  const entrada = React.useRef<HTMLInputElement>(null);
  const [elegido, setElegido] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  const datos = consulta.data;
  if (!datos) return null;

  const faltan = datos.documentos.filter((d) => !d.archivo).length;
  const observados = datos.documentos.filter(
    (d) => d.archivo?.estado === 'OBSERVADO',
  );
  const porEnviar = faltan + observados.length;

  const pedir = (documentoId: string) => {
    setElegido(documentoId);
    entrada.current?.click();
  };

  const recibir = async (archivo: File | undefined) => {
    if (!archivo || !elegido) return;
    const problema = problemaDelArchivo(archivo);
    setError(problema);
    if (problema) return;
    try {
      await subir.mutateAsync({ documentoId: elegido, archivo });
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    }
  };

  const enviarAValidacion = async () => {
    try {
      await enviar.mutateAsync();
      addNotification({
        type: 'success',
        title: 'Enviado a validación',
        message: 'El colegio revisará tus documentos.',
      });
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    }
  };

  const fila = (documento: DocumentoApoderado) => {
    const { archivo } = documento;
    const estado = archivo ? ESTADO_ARCHIVO[archivo.estado] : null;
    return (
      <FileItem
        key={documento.id}
        name={archivo?.nombre ?? documento.nombre}
        mimeType={archivo?.tipoMime}
        meta={
          archivo
            ? `${documento.nombre} · ${tamanoLegible(archivo.tamanoBytes)}${archivo.observacion ? ` · ${archivo.observacion}` : ''}`
            : (documento.descripcion ?? undefined)
        }
        actions={
          <>
            <Tag size="sm" color={estado?.color ?? 'warning'}>
              {estado?.etiqueta ?? 'Pendiente'}
            </Tag>
            {documento.tienePlantilla && datos.puedeSubir && (
              <BotonPlantilla
                documento={documento.nombre}
                alDescargar={() =>
                  descargarPlantillaDelApoderado(
                    estudianteId,
                    documento.id,
                    documento.nombre,
                  )
                }
              />
            )}
            {archivo && (
              <Button
                type="button"
                size="icon"
                variant="tertiary"
                aria-label={`Ver ${documento.nombre}`}
                onClick={() =>
                  abrirArchivoDelApoderado(estudianteId, documento.id)
                }
              >
                <Eye className="size-4" aria-hidden="true" />
              </Button>
            )}
            {datos.puedeSubir && archivo?.estado !== 'APROBADO' && (
              <Button
                type="button"
                variant={archivo ? 'ghost' : 'primary-light'}
                size="sm"
                icon={
                  archivo ? undefined : (
                    <Paperclip className="size-4" aria-hidden="true" />
                  )
                }
                aria-label={`${archivo ? 'Reemplazar' : 'Subir'} ${documento.nombre}`}
                disabled={subir.isPending}
                onClick={() => pedir(documento.id)}
              >
                {archivo ? 'Reemplazar' : 'Subir'}
              </Button>
            )}
          </>
        }
      />
    );
  };

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Documentos de matrícula</CardTitle>
          <p className="mt-1 text-body-4 text-content-tertiary">
            {datos.puedeSubir
              ? 'Descarga cada plantilla, fírmala y súbela. PDF, JPG o PNG, máximo 10 MB.'
              : 'Estos son los documentos de la matrícula.'}
          </p>
        </div>
      </CardHeader>
      <CardContent className="space-y-3">
        {observados.length > 0 && (
          <Alert
            type="error"
            title={`El colegio observó ${observados.length} documento(s)`}
          >
            Reemplaza solo los observados y vuelve a enviar a validación. Los
            aprobados no se modifican.
          </Alert>
        )}
        <input
          ref={entrada}
          type="file"
          accept="application/pdf,image/jpeg,image/png"
          className="sr-only"
          aria-label="Archivo del documento"
          onChange={(e) => {
            void recibir(e.target.files?.[0]);
            e.target.value = '';
          }}
        />
        {datos.documentos.map(fila)}
        {error && (
          <p role="alert" className="text-body-4 text-content-error">
            {error}
          </p>
        )}
      </CardContent>
      {datos.puedeSubir && (
        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-stroke-tertiary px-6 py-4">
          <p className="text-body-4 text-content-tertiary">
            {faltan > 0
              ? `Te faltan ${faltan} documento(s) por subir.`
              : observados.length > 0
                ? `Corrige ${observados.length} documento(s) observado(s).`
                : 'Ya subiste todos los documentos.'}
          </p>
          <Button
            type="button"
            disabled={porEnviar > 0 || enviar.isPending}
            onClick={enviarAValidacion}
          >
            Enviar a validación
          </Button>
        </div>
      )}
    </Card>
  );
};
