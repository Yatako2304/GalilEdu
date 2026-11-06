import { formatHora } from '../components/matriz-horaria';

/** "08:00" y "08:45" -> "8:00 - 8:45am" (Figma): el sufijo se escribe una sola vez si es el mismo. */
export const formatRango = (inicio: string, fin: string) => {
  const desde = formatHora(inicio);
  const hasta = formatHora(fin);
  const mismoSufijo = desde.slice(-2) === hasta.slice(-2);
  return `${mismoSufijo ? desde.slice(0, -2) : desde} - ${hasta}`;
};

/** El mensaje que el backend manda en `{ message }`, o el del error si no hubo respuesta. */
export const mensajeDeError = (error: unknown): string => {
  const mensaje = (error as { response?: { data?: { message?: string } } })
    ?.response?.data?.message;
  return (
    mensaje ?? (error as Error)?.message ?? 'No se pudo completar la acción.'
  );
};

export const estadoDeError = (error: unknown): number | undefined =>
  (error as { response?: { status?: number } })?.response?.status;

/** Un 409 de versión: el horario cambió desde que se leyó («Otra persona modificó el horario de …»). */
export const esConflictoDeVersion = (error: unknown) =>
  estadoDeError(error) === 409 &&
  mensajeDeError(error).startsWith('Otra persona modificó');
