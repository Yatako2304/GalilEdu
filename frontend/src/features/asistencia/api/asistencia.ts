import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  AsistenciaDelDia,
  MarcaAsistencia,
  SeccionAsistencia,
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
