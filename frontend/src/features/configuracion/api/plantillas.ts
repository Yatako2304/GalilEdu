import { api } from '@/lib/api-client';
import { DocumentoRequeridoVista } from '@/types/api';

import { getPlantilla } from './periodos-matricula';

// Plantillas de los documentos requeridos (HU-23). Se suben después de guardar el periodo, con los ids de
// documento que devuelve; solo con el periodo PROGRAMADO. XLSX, XLS o CSV de hasta 10 MB.

const rutaPlantilla = (periodoId: string, documentoId: string) =>
  `/configuracion/periodos-matricula/${periodoId}/documentos/${documentoId}/plantilla`;

// POST .../plantilla (multipart, parte "archivo")
export const subirPlantilla = (
  periodoId: string,
  documentoId: string,
  archivo: File,
): Promise<DocumentoRequeridoVista> => {
  const datos = new FormData();
  datos.append('archivo', archivo);
  return api.post(rutaPlantilla(periodoId, documentoId), datos);
};

// DELETE .../plantilla
export const quitarPlantilla = (
  periodoId: string,
  documentoId: string,
): Promise<DocumentoRequeridoVista> =>
  api.delete(rutaPlantilla(periodoId, documentoId));

/** El nombre es único en el periodo sin distinguir mayúsculas ni espacios de los bordes (lo valida el backend). */
const clave = (nombre: string) => nombre.trim().toLowerCase();

export type PlantillaPendiente = { nombre: string; archivo: File };

/**
 * Une cada archivo elegido en el formulario con el documento ya guardado que le corresponde. Se une por nombre y no
 * por posición: el nombre es único en el periodo. Un archivo sin documento guardado se descarta.
 */
export const emparejarPlantillas = (
  pendientes: PlantillaPendiente[],
  documentos: Pick<DocumentoRequeridoVista, 'id' | 'nombre'>[],
): { documentoId: string; nombre: string; archivo: File }[] =>
  pendientes.flatMap(({ nombre, archivo }) => {
    const documento = documentos.find((d) => clave(d.nombre) === clave(nombre));
    return documento
      ? [{ documentoId: documento.id, nombre: documento.nombre, archivo }]
      : [];
  });

/**
 * Sube las plantillas una tras otra y sigue aunque una falle (el aviso de cada error ya sale como notificación por
 * el interceptor). Devuelve los nombres de los documentos cuya plantilla no se pudo subir.
 */
export const subirPlantillas = async (
  periodoId: string,
  pendientes: { documentoId: string; nombre: string; archivo: File }[],
): Promise<{ fallidas: string[] }> => {
  const fallidas: string[] = [];
  for (const { documentoId, nombre, archivo } of pendientes) {
    try {
      await subirPlantilla(periodoId, documentoId, archivo);
    } catch {
      fallidas.push(nombre);
    }
  }
  return { fallidas };
};

/** Descarga la plantilla guardada con el nombre del archivo que se subió. */
export const descargarPlantilla = async (
  periodoId: string,
  documentoId: string,
  nombreArchivo: string,
) => {
  const contenido = await getPlantilla(periodoId, documentoId);
  const url = URL.createObjectURL(contenido);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombreArchivo;
  document.body.appendChild(enlace);
  enlace.click();
  enlace.remove();
  URL.revokeObjectURL(url);
};
