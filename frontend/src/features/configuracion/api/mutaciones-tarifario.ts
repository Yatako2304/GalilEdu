import { useMutation, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { SolicitudTarifario, TarifarioVista } from '@/types/api';

import { CLAVE_PERIODOS_MATRICULA } from './periodos-matricula';
import { CLAVE_TARIFARIOS, RUTA_TARIFARIOS } from './tarifarios';

// Figma > Personal Administrativo > "P19 · Tarifario y calendario" > "Confirmar tarifario" (HU-34)

// POST /configuracion/tarifarios/{anioEscolarId}/confirmar: guarda los montos y la morosidad, regenera el calendario con
// las fechas del año y deja el tarifario CONFIRMADO (lo crea si no existía). No hay "Guardar".
export const useConfirmarTarifario = (
  onSuccess?: (tarifario: TarifarioVista) => void,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      anioEscolarId,
      data,
    }: {
      anioEscolarId: string;
      data: SolicitudTarifario;
    }): Promise<TarifarioVista> =>
      api.post(`${RUTA_TARIFARIOS}/${anioEscolarId}/confirmar`, data),
    onSuccess: async (tarifario, { anioEscolarId }) => {
      // La respuesta ya es el tarifario guardado: no hace falta volver a pedirlo.
      queryClient.setQueryData([...CLAVE_TARIFARIOS, anioEscolarId], tarifario);
      await Promise.all([
        // La propuesta de otro año parte de los valores de este, así que se vuelve a pedir.
        queryClient.invalidateQueries({
          queryKey: CLAVE_TARIFARIOS,
          predicate: ({ queryKey }) => queryKey[2] !== anioEscolarId,
        }),
        // "Nuevo periodo de matrícula" y el detalle de un periodo dependen de si el año tiene el tarifario confirmado.
        queryClient.invalidateQueries({
          queryKey: CLAVE_PERIODOS_MATRICULA,
          predicate: ({ queryKey }) =>
            queryKey[2] === 'nuevo' || queryKey[2] === 'detalle',
        }),
      ]);
      onSuccess?.(tarifario);
    },
  });
};
