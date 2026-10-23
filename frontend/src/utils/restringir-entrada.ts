import { UseFormRegisterReturn } from 'react-hook-form';

// Filtros para campos donde solo se permite cierto tipo de caracteres (CAHU6-6, 10, 12, 21, 24).
// El valor se limpia antes de que react-hook-form lo lea, así que también cubre pegar texto.

type Filtro = (valor: string) => string;

const recortar = (valor: string, max?: number) =>
  max === undefined ? valor : valor.slice(0, max);

/** Solo números (teléfonos, DNI). */
export const soloDigitos =
  (max?: number): Filtro =>
  (valor) =>
    recortar(valor.replace(/\D/g, ''), max);

/** Letras y números, sin espacios ni símbolos (carné de extranjería). */
export const soloAlfanumerico =
  (max?: number): Filtro =>
  (valor) =>
    recortar(valor.replace(/[^\p{L}\p{N}]/gu, ''), max);

/** Letras (con tildes y ñ), espacios, apóstrofo y guion; sin números (apellidos y nombres). */
export const soloLetras =
  (max?: number): Filtro =>
  (valor) =>
    recortar(valor.replace(/[^\p{L}\s'’-]/gu, ''), max);

/** Aplica el filtro al valor escrito antes de entregárselo a react-hook-form. */
export const restringir = (
  registration: UseFormRegisterReturn,
  filtro: Filtro,
): UseFormRegisterReturn => ({
  ...registration,
  onChange: (evento) => {
    const filtrado = filtro(evento.target.value);
    // Solo se reescribe si cambió, para no mover el cursor cuando el texto ya era válido.
    if (filtrado !== evento.target.value) evento.target.value = filtrado;
    return registration.onChange(evento);
  },
});
