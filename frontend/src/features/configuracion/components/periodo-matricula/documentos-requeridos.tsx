import { Plus } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { UploadDropzone } from '@/components/ui/files';
import { Input, Textarea } from '@/components/ui/form';

import { descargarPlantilla } from '../../api/plantillas';

import { DocumentoItem } from './documento-item';
import {
  DESCRIPCION_DOCUMENTO_MAXIMA,
  DocumentoBorrador,
  NOMBRE_DOCUMENTO_MAXIMO,
  validarDocumento,
} from './esquemas';
import { FORMATOS_PLANTILLA, validarPlantilla } from './plantilla';

type DocumentosRequeridosProps = {
  documentos: DocumentoBorrador[];
  onChange: (documentos: DocumentoBorrador[]) => void;
  deshabilitado?: boolean;
  /** Un periodo que ya existe (editar o ver) permite bajar las plantillas guardadas. */
  periodoId?: string;
  /** En solo lectura solo se ve la lista y se bajan las plantillas. */
  soloLectura?: boolean;
};

type ErroresDeAlta = {
  nombre?: string;
  descripcion?: string;
  plantilla?: string;
};

// Figma > pantalla 13 > formulario > tarjeta "Documentos requeridos": los documentos ya agregados y el alta de uno
// nuevo (nombre, descripción y plantilla opcional). La plantilla se queda en memoria hasta guardar el periodo: el
// backend la recibe después, con el id que le da al documento.
export const DocumentosRequeridos = ({
  documentos,
  onChange,
  deshabilitado = false,
  periodoId,
  soloLectura = false,
}: DocumentosRequeridosProps) => {
  const [nombre, setNombre] = React.useState('');
  const [descripcion, setDescripcion] = React.useState('');
  const [archivo, setArchivo] = React.useState<File | null>(null);
  const [errores, setErrores] = React.useState<ErroresDeAlta>({});
  const contador = React.useRef(0);

  const elegirArchivo = (elegido: File) => {
    const problema = validarPlantilla(elegido);
    if (problema) {
      setErrores((e) => ({ ...e, plantilla: problema }));
      return;
    }
    setArchivo(elegido);
    setErrores((e) => ({ ...e, plantilla: undefined }));
  };

  const agregar = () => {
    const encontrados = validarDocumento(
      { nombre, descripcion },
      documentos.map((d) => d.nombre),
    );
    if (encontrados.nombre || encontrados.descripcion) {
      setErrores(encontrados);
      return;
    }
    contador.current += 1;
    onChange([
      ...documentos,
      {
        clave: `nuevo-${contador.current}`,
        nombre: nombre.trim(),
        descripcion: descripcion.trim(),
        archivo,
      },
    ]);
    setNombre('');
    setDescripcion('');
    setArchivo(null);
    setErrores({});
  };

  const cambiar = (
    documento: DocumentoBorrador,
    cambios: Partial<DocumentoBorrador>,
  ) =>
    onChange(
      documentos.map((d) => (d === documento ? { ...d, ...cambios } : d)),
    );

  const descargar = (documento: DocumentoBorrador) => {
    if (!periodoId || !documento.id) return;
    // El motivo de un fallo ya sale como notificación.
    descargarPlantilla(
      periodoId,
      documento.id,
      documento.plantillaNombreArchivo ?? 'plantilla',
    ).catch(() => undefined);
  };

  return (
    <Card className="flex flex-col gap-4 p-6">
      <div className="border-b border-stroke-tertiary pb-4">
        <h2 className="text-label-1 font-semibold text-content-primary">
          Documentos requeridos
        </h2>
        <p className="text-body-4 text-content-tertiary">
          Lo que se deberá presentar al matricular.
        </p>
      </div>

      {documentos.length > 0 && (
        <ul className="flex flex-col gap-2" aria-label="Documentos agregados">
          {documentos.map((documento) => {
            const guardado = !!documento.id && !!periodoId;
            return (
              <DocumentoItem
                key={documento.clave}
                documento={documento}
                onQuitar={
                  deshabilitado || soloLectura || documento.tieneEntregas
                    ? undefined
                    : () => onChange(documentos.filter((d) => d !== documento))
                }
                onDescargar={
                  guardado && documento.plantillaNombreArchivo
                    ? () => descargar(documento)
                    : undefined
                }
                onElegirPlantilla={
                  guardado && !soloLectura && !deshabilitado
                    ? (archivo) =>
                        cambiar(documento, { archivo, quitarPlantilla: false })
                    : undefined
                }
                onQuitarPlantilla={
                  guardado && !soloLectura && !deshabilitado
                    ? () =>
                        cambiar(documento, {
                          archivo: null,
                          quitarPlantilla:
                            !documento.archivo &&
                            !!documento.plantillaNombreArchivo,
                        })
                    : undefined
                }
              />
            );
          })}
        </ul>
      )}

      {soloLectura && documentos.length === 0 && (
        <p className="text-body-4 text-content-tertiary">
          Este periodo no tiene documentos requeridos.
        </p>
      )}

      {!soloLectura && (
        <>
          <Input
            label="Nombre del documento"
            maxLength={NOMBRE_DOCUMENTO_MAXIMO}
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            disabled={deshabilitado}
            error={
              errores.nombre
                ? { type: 'validate', message: errores.nombre }
                : undefined
            }
          />
          <Textarea
            label="Descripción del documento (opcional)"
            maxLength={DESCRIPCION_DOCUMENTO_MAXIMA}
            showCount
            value={descripcion}
            onChange={(e) => setDescripcion(e.target.value)}
            disabled={deshabilitado}
            error={
              errores.descripcion
                ? { type: 'validate', message: errores.descripcion }
                : undefined
            }
          />

          <div className="flex flex-col gap-2">
            <p className="text-label-2 text-content-secondary">
              Plantilla (opcional)
            </p>
            <UploadDropzone
              size="md"
              onFile={elegirArchivo}
              accept={FORMATOS_PLANTILLA}
              formats="XLSX, XLS o CSV"
              maxSize="10 MB"
              file={archivo}
              error={errores.plantilla}
              disabled={deshabilitado}
            />
          </div>

          <Button
            type="button"
            variant="secondary"
            icon={<Plus className="size-4" aria-hidden="true" />}
            disabled={deshabilitado}
            onClick={agregar}
          >
            Agregar documento
          </Button>
        </>
      )}
    </Card>
  );
};
