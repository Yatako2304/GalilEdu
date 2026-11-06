import { api } from '@/lib/api-client';

const EXTENSIONES: Record<string, string> = {
  'application/pdf': 'pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document':
    'docx',
  'image/jpeg': 'jpg',
  'image/png': 'png',
};

/** Descarga la plantilla de `ruta` con el nombre del documento y la extensión de su tipo (HU-28). */
export const descargarPlantilla = async (ruta: string, nombre: string) => {
  const archivo: Blob = await api.get(ruta, { responseType: 'blob' });
  const extension = EXTENSIONES[archivo.type];
  const url = URL.createObjectURL(archivo);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = extension ? `${nombre}.${extension}` : nombre;
  enlace.click();
  URL.revokeObjectURL(url);
};

// GET /matricula/periodos/{periodoId}/documentos/{documentoId}/plantilla · Personal administrativo
export const descargarPlantillaDelPeriodo = (
  periodoId: string,
  documentoId: string,
  nombre: string,
) =>
  descargarPlantilla(
    `/matricula/periodos/${periodoId}/documentos/${documentoId}/plantilla`,
    nombre,
  );
