const TIPOS_ARCHIVO = ['application/pdf', 'image/jpeg', 'image/png'];
const TAMANO_MAXIMO = 10 * 1024 * 1024;

/** Mismas reglas del servidor (RF-62): PDF, JPG o PNG de hasta 10 MB. */
export const problemaDelArchivo = (archivo: File) => {
  if (!TIPOS_ARCHIVO.includes(archivo.type)) {
    return 'El archivo debe ser PDF, JPG o PNG.';
  }
  if (archivo.size > TAMANO_MAXIMO) {
    return 'El archivo no puede pesar más de 10 MB.';
  }
  return null;
};

export const tamanoLegible = (bytes: number) =>
  bytes >= 1024 * 1024
    ? `${(bytes / (1024 * 1024)).toFixed(2)} MB`
    : `${Math.max(1, Math.round(bytes / 1024))} KB`;
