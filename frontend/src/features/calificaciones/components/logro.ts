/**
 * Color del nivel de logro, como se informa en la evaluación por competencias: AD (logro destacado) y A (logro
 * esperado) en verde, B (en proceso) en ámbar y C (en inicio) en rojo. Sin nota («Pendiente»), gris. No hay
 * «aprobado» ni «desaprobado» por competencia.
 */
export const colorLogro = (logro: string) => {
  if (logro.includes('Logro destacado') || logro.includes('Logro esperado'))
    return 'text-content-success';
  if (logro.includes('En proceso')) return 'text-content-warning';
  if (logro.includes('En inicio')) return 'text-content-error';
  return 'text-content-quartiary';
};
