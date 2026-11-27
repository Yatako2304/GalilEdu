import { useMutation, useQueryClient } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import { RetiroAlumnoRegistrado, SolicitudRetiroAlumno } from '@/types/api';

// Retiro del alumno del colegio (fila 36 del plan de pruebas). El mismo modal lo usan Estudiantes, el detalle de la
// matrícula y Pensiones.

// POST /matricula/matriculas/de-estudiante/{estudianteId}/retiro
export const useRetirarAlumno = (
  estudianteId: string,
  onSuccess?: (retiro: RetiroAlumnoRegistrado) => void,
) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (
      solicitud: SolicitudRetiroAlumno,
    ): Promise<RetiroAlumnoRegistrado> =>
      api.post(
        `/matricula/matriculas/de-estudiante/${estudianteId}/retiro`,
        solicitud,
      ),
    onSuccess: async (retiro) => {
      // Cambian la matrícula, el estudiante (listado y ficha), las pensiones y el estado de cuenta.
      await queryClient.invalidateQueries();
      onSuccess?.(retiro);
    },
  });
};
