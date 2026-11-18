import { queryOptions, useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';

import { OpcionesDeReserva } from '../types-reservas';

import { CLAVE_HORARIOS } from './horario-seccion';

// GET /horarios/reservas/opciones: los espacios especiales ACTIVOS por código (`AmbienteOpcion`, el mismo de la HU-45) y
// quienes pueden ser responsables (usuarios activos con rol Docente o Tutor). No depende de la sección. Año, grado y
// sección salen de `api/selectores.ts`.
//
// La clave NO cuelga de `['horarios', 'reservas']`: crear, editar o eliminar una reserva no cambia estas opciones y no
// debe volver a pedirlas.
export const claveOpcionesReserva = [
  ...CLAVE_HORARIOS,
  'opciones-reserva',
] as const;

export const useOpcionesReserva = (habilitado = true) =>
  useQuery(
    queryOptions({
      queryKey: claveOpcionesReserva,
      queryFn: (): Promise<OpcionesDeReserva> =>
        api.get('/horarios/reservas/opciones'),
      enabled: habilitado,
    }),
  );
