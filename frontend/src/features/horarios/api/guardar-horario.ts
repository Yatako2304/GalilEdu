import { useMutation, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';

import { HorarioDeSeccion, SolicitudHorario } from '../types';

import { claveHorarioSeccion, claveOpciones } from './horario-seccion';

// PUT /horarios/secciones/{seccionId}: guarda la matriz COMPLETA con la versión que devolvió el GET y responde con el
// estado resultante (el mismo cuerpo que el GET). 400 y 409 traen `{ message }`; la pantalla los muestra tal cual
// (por eso se silencia la notificación de error del cliente). Un 409 de versión pide recargar.
export const useGuardarHorario = (
  seccionId: string,
  onSuccess?: (horario: HorarioDeSeccion) => void,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: SolicitudHorario): Promise<HorarioDeSeccion> =>
      api.put(`/horarios/secciones/${seccionId}`, data, {
        silenciarNotificacion: true,
      }),
    onSuccess: async (horario) => {
      queryClient.setQueryData(claveHorarioSeccion(seccionId), horario);
      // Las horas asignadas de cada curso cambiaron: las opciones se vuelven a pedir.
      await queryClient.invalidateQueries({
        queryKey: claveOpciones(seccionId),
      });
      onSuccess?.(horario);
    },
  });
};
