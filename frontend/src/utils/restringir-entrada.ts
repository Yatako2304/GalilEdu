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

/** Formato de un número decimal: cuántos dígitos enteros y cuántos decimales (ej. peso ###.# → 3 y 1). */
export type FormatoDecimal = { enteros: number; decimales: number };

/**
 * Solo números y un punto como separador decimal, con el formato dado. Si se escriben más enteros de los
 * permitidos sin punto, el punto se pone solo: con #.## "118" queda "1.18".
 */
export const soloDecimal =
  ({ enteros, decimales }: FormatoDecimal): Filtro =>
  (valor) => {
    const [entera, ...resto] = valor.replace(/[^\d.]/g, '').split('.');
    const sobrante = resto.length ? resto.join('') : entera.slice(enteros);
    const conPunto = resto.length > 0 || sobrante.length > 0;
    return conPunto
      ? `${entera.slice(0, enteros)}.${sobrante.slice(0, decimales)}`
      : entera;
  };

/** Completa los decimales que falten ("22" → "22.0" con un decimal). Un valor vacío queda vacío. */
export const completarDecimales = (
  valor: string,
  { decimales }: FormatoDecimal,
): string => {
  // Necesita al menos un dígito: "" y "." quedan como están.
  if (!/^(\d+(\.\d*)?|\.\d+)$/.test(valor)) return valor;
  const [entera, decimal = ''] = valor.split('.');
  // Sin parte entera (".23") se antepone el 0.
  return `${entera || '0'}.${decimal.padEnd(decimales, '0')}`;
};

/**
 * Campo decimal: filtra lo que se escribe (`soloDecimal`) y, al salir del campo, completa los decimales
 * (`completarDecimales`).
 */
export const decimal = (
  registration: UseFormRegisterReturn,
  formato: FormatoDecimal,
): UseFormRegisterReturn => {
  const filtrado = restringir(registration, soloDecimal(formato));
  return {
    ...filtrado,
    onBlur: (evento) => {
      const completo = completarDecimales(evento.target.value, formato);
      if (completo !== evento.target.value) {
        evento.target.value = completo;
        registration.onChange(evento);
      }
      return registration.onBlur(evento);
    },
  };
};

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
