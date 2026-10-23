// Plantilla de un documento requerido (HU-23): XLSX, XLS o CSV de hasta 10 MB. Es la misma regla del backend, con sus
// mismos mensajes; el backend vuelve a comprobarla (también el contenido del archivo) al recibirla.

export const EXTENSIONES_PLANTILLA = ['xlsx', 'xls', 'csv'];
export const FORMATOS_PLANTILLA = EXTENSIONES_PLANTILLA.map(
  (extension) => `.${extension}`,
).join(',');
export const TAMANO_MAXIMO_PLANTILLA = 10 * 1024 * 1024;

/** El mensaje de por qué no sirve el archivo, o null si sirve. */
export const validarPlantilla = (archivo: {
  name: string;
  size: number;
}): string | null => {
  if (archivo.size === 0) return 'Selecciona un archivo para la plantilla.';
  const punto = archivo.name.lastIndexOf('.');
  const extension =
    punto >= 0 ? archivo.name.slice(punto + 1).toLowerCase() : '';
  if (!EXTENSIONES_PLANTILLA.includes(extension)) {
    return 'La plantilla debe ser un archivo XLSX, XLS o CSV.';
  }
  if (archivo.size > TAMANO_MAXIMO_PLANTILLA) {
    return 'La plantilla no puede pesar más de 10 MB.';
  }
  return null;
};
