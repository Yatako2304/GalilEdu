import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  AsistenciaDelDia,
  MarcaAsistencia,
  MotivoJustificacion,
  ReporteAsistencia as ReporteAsistenciaDto,
  SeccionAsistencia,
  TipoReporteAsistencia,
} from '@/types/api';

const RUTA = '/asistencia/secciones';

/** HU-65: secciones de las que el usuario es tutor o cotutor (año activo). */
export const useSeccionesAsistencia = () =>
  useQuery({
    queryKey: ['asistencia', 'secciones'],
    queryFn: (): Promise<SeccionAsistencia[]> => api.get(RUTA),
  });

const claveDia = (seccionId: string, fecha: string) => [
  'asistencia',
  'secciones',
  seccionId,
  'dias',
  fecha,
];

/** AS-01, AS-05: la asistencia del día (`fecha` en YYYY-MM-DD). */
export const useAsistenciaDelDia = (seccionId?: string, fecha?: string) =>
  useQuery({
    queryKey: claveDia(seccionId ?? '', fecha ?? ''),
    queryFn: (): Promise<AsistenciaDelDia> =>
      api.get(`${RUTA}/${seccionId}/dias/${fecha}`),
    enabled: Boolean(seccionId && fecha),
  });

export type SolicitudAsistencia = {
  /** null al registrar; la del registro al corregir. */
  version: number | null;
  asistencias: MarcaAsistencia[];
};

/** AS-04 (guardar) y AS-06 (corregir): deja en la consulta la asistencia que devuelve el backend. */
export const useGuardarAsistencia = (seccionId: string, fecha: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (solicitud: SolicitudAsistencia): Promise<AsistenciaDelDia> =>
      api.put(`${RUTA}/${seccionId}/dias/${fecha}`, solicitud),
    onSuccess: (dia) => {
      queryClient.setQueryData(claveDia(seccionId, fecha), dia);
    },
  });
};

export type SolicitudJustificacion = {
  matriculaId: string;
  motivo: MotivoJustificacion;
  detalle: string;
  archivo: File | null;
};

/** HU-67: justifica una falta o tardanza (multipart, con sustento opcional). */
export const useJustificar = (seccionId: string, fecha: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      matriculaId,
      motivo,
      detalle,
      archivo,
    }: SolicitudJustificacion): Promise<AsistenciaDelDia> => {
      const datos = new FormData();
      datos.append('matriculaId', matriculaId);
      datos.append('motivo', motivo);
      if (detalle.trim()) datos.append('detalle', detalle.trim());
      if (archivo) datos.append('archivo', archivo);
      return api.post(
        `${RUTA}/${seccionId}/dias/${fecha}/justificaciones`,
        datos,
      );
    },
    onSuccess: (dia) => {
      queryClient.setQueryData(claveDia(seccionId, fecha), dia);
    },
  });
};

/** HU-67: descarga el sustento adjunto de una justificación. */
export const descargarSustento = async (
  seccionId: string,
  fecha: string,
  matriculaId: string,
  nombre: string,
) => {
  const contenido = (await api.get(
    `${RUTA}/${seccionId}/dias/${fecha}/justificaciones/${matriculaId}/sustento`,
    { responseType: 'blob' },
  )) as Blob;
  const url = URL.createObjectURL(contenido);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  enlace.click();
  URL.revokeObjectURL(url);
};

export type FiltroReporte = { tipo?: TipoReporteAsistencia; valor?: string };

/** HU-68: reporte por mes o periodo. Sin filtro, el mes actual. */
export const useReporteAsistencia = (
  seccionId?: string,
  filtro?: FiltroReporte,
) =>
  useQuery({
    queryKey: ['asistencia', 'secciones', seccionId, 'reporte', filtro ?? {}],
    queryFn: (): Promise<ReporteAsistenciaDto> =>
      api.get(`${RUTA}/${seccionId}/reporte`, { params: filtro }),
    enabled: Boolean(seccionId),
    placeholderData: (anterior) => anterior,
  });

/** HU-68: descarga el reporte en PDF. */
export const exportarReporteAsistencia = async (
  seccionId: string,
  filtro: FiltroReporte,
  nombre: string,
) => {
  const contenido = (await api.get(`${RUTA}/${seccionId}/reporte/pdf`, {
    params: filtro,
    responseType: 'blob',
  })) as Blob;
  const url = URL.createObjectURL(contenido);
  const enlace = document.createElement('a');
  enlace.href = url;
  enlace.download = nombre;
  enlace.click();
  URL.revokeObjectURL(url);
};
