import * as React from 'react';

/**
 * Devuelve `valor` solo después de que deje de cambiar durante `retardoMs`. Sirve para no pedir algo al servidor en
 * cada tecla: mientras se escribe el valor retardado se queda como estaba, y cuando el usuario hace una pausa se
 * actualiza una sola vez.
 *
 * Con objetos o listas, pasa un valor que cambie solo cuando cambie su contenido (por ejemplo, un `useMemo`):
 * un objeto nuevo en cada render reiniciaría la espera todo el tiempo.
 */
export const useDebouncedValue = <T>(valor: T, retardoMs = 400): T => {
  const [retardado, setRetardado] = React.useState(valor);

  React.useEffect(() => {
    const temporizador = setTimeout(() => setRetardado(valor), retardoMs);
    return () => clearTimeout(temporizador);
  }, [valor, retardoMs]);

  return retardado;
};
