import { useMutation, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { ErrorFila, ResultadoCargaMasiva } from '@/types/api';

// Carga masiva de usuarios (HU-8) · backend: UsuarioController, "/usuarios/carga-masiva".

export const TAMANO_MAXIMO_CARGA = 10 * 1024 * 1024;
export const EXTENSIONES_CARGA = ['.xlsx', '.xls', '.csv'];

/** Mismas reglas que el backend: .xlsx, .xls o .csv de hasta 10 MB. */
export const validarArchivoCarga = (archivo: File): string | null => {
  const nombre = archivo.name.toLowerCase();
  if (!EXTENSIONES_CARGA.some((ext) => nombre.endsWith(ext))) {
    return 'El archivo debe ser .xlsx, .xls o .csv';
  }
  if (archivo.size > TAMANO_MAXIMO_CARGA) {
    return 'El archivo no puede pesar más de 10 MB';
  }
  return null;
};

const formulario = (archivo: File) => {
  const datos = new FormData();
  datos.append('archivo', archivo);
  return datos;
};

const guardarComo = (contenido: Blob, nombre: string) => {
  const url = URL.createObjectURL(contenido);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  enlace.click();
  URL.revokeObjectURL(url);
};

// GET /usuarios/carga-masiva/plantilla
export const descargarPlantilla = async () => {
  const archivo: Blob = await api.get('/usuarios/carga-masiva/plantilla', {
    responseType: 'blob',
  });
  guardarComo(archivo, 'usuarios_galiledu.xlsx');
};

// POST /usuarios/carga-masiva/validacion (paso 2: no crea nada)
export const useValidarCarga = () =>
  useMutation({
    mutationFn: (archivo: File): Promise<ResultadoCargaMasiva> =>
      api.post('/usuarios/carga-masiva/validacion', formulario(archivo)),
  });

// POST /usuarios/carga-masiva (paso 3: crea los válidos)
export const useCargarUsuarios = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (archivo: File): Promise<ResultadoCargaMasiva> =>
      api.post('/usuarios/carga-masiva', formulario(archivo)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['usuarios'] }),
  });
};

/** CSV con las filas que no se crearon y su motivo ("Descargar detalle" del resumen). */
export const detalleErroresCsv = (errores: ErrorFila[]) => {
  const celda = (v: string | number) => `"${String(v).replace(/"/g, '""')}"`;
  return [
    'Fila,Error',
    ...errores.map((e) => `${celda(e.fila)},${celda(e.mensaje)}`),
  ].join('\n');
};

export const descargarDetalle = (
  nombreArchivo: string,
  errores: ErrorFila[],
) => {
  const base = nombreArchivo.replace(/\.[^.]+$/, '');
  guardarComo(
    // BOM para que Excel reconozca las tildes.
    new Blob(['\uFEFF' + detalleErroresCsv(errores)], {
      type: 'text/csv;charset=utf-8',
    }),
    `${base}_errores.csv`,
  );
};
