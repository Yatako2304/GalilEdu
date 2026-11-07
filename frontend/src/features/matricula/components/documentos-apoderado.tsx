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
  abrirEnPestana,
  descargarPlantillaDelApoderado,
  useDocumentosApoderado,
  useEnviarAValidacion,
  useIniciarMatricula,
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
 * lo envía a validación). Solo con el periodo abierto.
 *
 * Si el hijo aún no tiene matrícula, el apoderado la inicia aquí: elige los archivos (quedan en la pantalla) y
 * «Enviar a validación» los manda todos juntos; recién entonces se crea la matrícula y llega al colegio. Si la
 * matrícula ya existe (la creó el colegio o devolvió un documento observado), cada archivo se sube al elegirlo.
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
  const iniciar = useIniciarMatricula(estudianteId);
  const entrada = React.useRef<HTMLInputElement>(null);
  const [elegido, setElegido] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  // Archivos elegidos antes de que exista la matrícula: se envían todos con «Enviar a validación».
  const [locales, setLocales] = React.useState<Record<string, File>>({});

  const datos = consulta.data;
  if (!datos) return null;

  const sinMatricula = datos.estado === null;
  const faltan = datos.documentos.filter((d) =>
    sinMatricula ? !locales[d.id] : !d.archivo,
  ).length;
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
    if (sinMatricula) {
      setLocales((actuales) => ({ ...actuales, [elegido]: archivo }));
      return;
    }
    try {
      await subir.mutateAsync({ documentoId: elegido, archivo });
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    }
  };

  const enviarAValidacion = async () => {
    try {
      if (sinMatricula) {
        await iniciar.mutateAsync(locales);
        setLocales({});
      } else {
        await enviar.mutateAsync();
      }
      addNotification({
        type: 'success',
        title: 'Enviado a validación',
        message: 'El colegio revisará tus documentos.',
      });
    } catch {
      // El cliente HTTP muestra el mensaje específico del backend.
    }
  };

  const filaLocal = (documento: DocumentoApoderado) => {
    const local = locales[documento.id];
    return (
      <FileItem
        key={documento.id}
        name={local?.name ?? documento.nombre}
        mimeType={local?.type}
        meta={
          local
            ? `${documento.nombre} · ${tamanoLegible(local.size)}`
            : (documento.descripcion ?? undefined)
        }
        actions={
          <>
            <Tag size="sm" color={local ? 'info' : 'warning'}>
              {local ? 'Por enviar' : 'Pendiente'}
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
            {local && (
              <Button
                type="button"
                size="icon"
                variant="tertiary"
                aria-label={`Ver ${documento.nombre}`}
                onClick={() => abrirEnPestana(local)}
              >
                <Eye className="size-4" aria-hidden="true" />
              </Button>
            )}
            {datos.puedeSubir && (
              <Button
                type="button"
                variant={local ? 'ghost' : 'primary-light'}
                size="sm"
                icon={
                  local ? undefined : (
                    <Paperclip className="size-4" aria-hidden="true" />
                  )
                }
                aria-label={`${local ? 'Reemplazar' : 'Subir'} ${documento.nombre}`}
                disabled={iniciar.isPending}
                onClick={() => pedir(documento.id)}
              >
                {local ? 'Reemplazar' : 'Subir'}
              </Button>
            )}
          </>
        }
      />
    );
  };

  const fila = (documento: DocumentoApoderado) => {
    if (sinMatricula) return filaLocal(documento);
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
            {!datos.puedeSubir
              ? 'Estos son los documentos de la matrícula.'
              : sinMatricula
                ? 'Descarga cada plantilla, fírmala y súbela. PDF, JPG o PNG, máximo 10 MB. Se guardan al enviarlos a validación.'
                : 'Descarga cada plantilla, fírmala y súbela. PDF, JPG o PNG, máximo 10 MB.'}
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
            disabled={porEnviar > 0 || enviar.isPending || iniciar.isPending}
            onClick={enviarAValidacion}
          >
            Enviar a validación
          </Button>
        </div>
      )}
    </Card>
  );
};
