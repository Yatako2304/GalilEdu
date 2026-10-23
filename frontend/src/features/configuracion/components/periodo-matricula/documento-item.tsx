import { Download, FileText, Trash2, Upload } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { IconBadge } from '@/components/ui/icon-badge';

import { DocumentoBorrador } from './esquemas';
import { FORMATOS_PLANTILLA, validarPlantilla } from './plantilla';

type DocumentoItemProps = {
  documento: DocumentoBorrador;
  /** Sin esta función (periodo en solo lectura, o documento con entregas) no hay papelera. */
  onQuitar?: () => void;
  /** Baja la plantilla ya guardada del documento. */
  onDescargar?: () => void;
  /** Elige otra plantilla (se sube al guardar el periodo). */
  onElegirPlantilla?: (archivo: File) => void;
  /** Quita la plantilla (guardada o recién elegida) al guardar el periodo. */
  onQuitarPlantilla?: () => void;
};

// Figma > pantalla 13 > formulario > un documento ya agregado: ícono, nombre, descripción y papelera. Al editar o ver
// un periodo se suman las acciones de su plantilla.
export const DocumentoItem = ({
  documento,
  onQuitar,
  onDescargar,
  onElegirPlantilla,
  onQuitarPlantilla,
}: DocumentoItemProps) => {
  const entrada = React.useRef<HTMLInputElement>(null);
  const [problema, setProblema] = React.useState<string | null>(null);
  const guardada = documento.quitarPlantilla
    ? null
    : (documento.plantillaNombreArchivo ?? null);
  const plantilla = documento.archivo?.name ?? guardada;

  const elegir = (e: React.ChangeEvent<HTMLInputElement>) => {
    const archivo = e.target.files?.[0];
    e.target.value = '';
    if (!archivo) return;
    const motivo = validarPlantilla(archivo);
    setProblema(motivo);
    if (!motivo) onElegirPlantilla?.(archivo);
  };

  return (
    <li className="flex flex-col gap-2 rounded-xl bg-surface-secondary p-3">
      <div className="flex items-center gap-3">
        <IconBadge color="accent" shape="square" aria-hidden="true">
          <FileText />
        </IconBadge>
        <div className="min-w-0 flex-1">
          <p className="truncate text-label-1 text-content-primary">
            {documento.nombre}
          </p>
          {documento.descripcion && (
            <p className="truncate text-caption-1 text-content-tertiary">
              {documento.descripcion}
            </p>
          )}
          {plantilla && (
            <p className="truncate text-caption-1 text-content-accent-secondary">
              Plantilla: {plantilla}
            </p>
          )}
          {documento.quitarPlantilla && !documento.archivo && (
            <p className="text-caption-1 text-content-tertiary">
              La plantilla se quitará al guardar.
            </p>
          )}
          {documento.tieneEntregas && (
            <p className="text-caption-1 text-content-tertiary">
              Ya tiene entregas: no se puede quitar.
            </p>
          )}
        </div>
        {onQuitar && (
          <Button
            type="button"
            variant="ghost"
            size="icon"
            aria-label={`Quitar ${documento.nombre}`}
            onClick={onQuitar}
          >
            <Trash2 className="size-4" aria-hidden="true" />
          </Button>
        )}
      </div>

      {(onDescargar || onElegirPlantilla || onQuitarPlantilla) && (
        <div className="flex flex-wrap gap-2 pl-[52px]">
          {onDescargar && guardada && !documento.archivo && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              icon={<Download className="size-4" aria-hidden="true" />}
              aria-label={`Descargar plantilla de ${documento.nombre}`}
              onClick={onDescargar}
            >
              Descargar
            </Button>
          )}
          {onElegirPlantilla && (
            <>
              <input
                ref={entrada}
                type="file"
                hidden
                accept={FORMATOS_PLANTILLA}
                aria-label={`Archivo de plantilla de ${documento.nombre}`}
                onChange={elegir}
              />
              <Button
                type="button"
                variant="outline"
                size="sm"
                icon={<Upload className="size-4" aria-hidden="true" />}
                aria-label={`${plantilla ? 'Cambiar' : 'Subir'} plantilla de ${documento.nombre}`}
                onClick={() => entrada.current?.click()}
              >
                {plantilla ? 'Cambiar plantilla' : 'Subir plantilla'}
              </Button>
            </>
          )}
          {onQuitarPlantilla && plantilla && (
            <Button
              type="button"
              variant="tertiary"
              size="sm"
              aria-label={`Quitar plantilla de ${documento.nombre}`}
              onClick={() => {
                setProblema(null);
                onQuitarPlantilla();
              }}
            >
              Quitar plantilla
            </Button>
          )}
        </div>
      )}
      {problema && (
        <p role="alert" className="pl-[52px] text-caption-1 text-content-error">
          {problema}
        </p>
      )}
    </li>
  );
};
