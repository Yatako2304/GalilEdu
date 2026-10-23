import { Resolver } from 'react-hook-form';

import { SolicitudTarifario, TarifarioVista } from '@/types/api';

// Valores del formulario P19 "Tarifario y calendario" (HU-34). Los campos son texto, como lo escribe quien lo llena;
// se convierten a número al enviar.

export type ValoresTarifario = {
  montoCuotaIngreso: string;
  montoMatricula: string;
  montoPension: string;
  diasGracia: string;
  recargoPorcentaje: string;
  topeRecargoPorcentaje: string;
};

const texto = (valor: number | null) => (valor === null ? '' : String(valor));

/** Lo que el formulario muestra al abrir un año: lo guardado o la propuesta del backend (campos vacíos si no hay). */
export const valoresDeTarifario = (
  tarifario: TarifarioVista,
): ValoresTarifario => ({
  montoCuotaIngreso: texto(tarifario.conceptos.montoCuotaIngreso),
  montoMatricula: texto(tarifario.conceptos.montoMatricula),
  montoPension: texto(tarifario.conceptos.montoPension),
  diasGracia: texto(tarifario.morosidad.diasGracia),
  recargoPorcentaje: texto(tarifario.morosidad.recargoPorcentaje),
  topeRecargoPorcentaje: texto(tarifario.morosidad.topeRecargoPorcentaje),
});

/** Un número escrito en un campo, o null si está vacío o todavía no es un número. */
export const aNumero = (escrito: string): number | null => {
  const limpio = escrito.trim();
  if (limpio === '') return null;
  const numero = Number(limpio);
  return Number.isFinite(numero) ? numero : null;
};

/** El monto total sin arrastrar errores de coma flotante: 620.1 × 3 = 1860.3. */
export const multiplicarMonto = (monto: number, veces: number) =>
  Math.round(monto * veces * 100) / 100;

// ---------- Reglas del formulario ----------
// Son las mismas que valida el backend (`Tarifario.validar`), con sus mismos mensajes. El backend devuelve solo el
// primer error; aquí se muestra cada uno bajo su campo. El backend sigue validando como segunda barrera.

export const MONTO_MAXIMO = 99999999.99;
export const DIAS_GRACIA_MAXIMOS = 60;

type Campo = keyof ValoresTarifario;

/** Cantidad de decimales que de verdad tiene lo escrito ("620.50" tiene 1; "620.123" tiene 3). */
const decimales = (escrito: string) => {
  const [, fraccion = ''] = escrito.trim().split('.');
  return fraccion.replace(/0+$/, '').length;
};

/** Un monto en soles: no negativo, de hasta 2 decimales y sin pasar de S/ 99999999.99. */
const mensajeDeMonto = (numero: number, escrito: string, de: string) => {
  if (numero < 0) return `El monto de ${de} no puede ser negativo.`;
  if (decimales(escrito) > 2)
    return `El monto de ${de} admite hasta 2 decimales.`;
  if (numero > MONTO_MAXIMO) {
    return `El monto de ${de} no puede superar S/ ${MONTO_MAXIMO}.`;
  }
  return null;
};

/** Un porcentaje: de 0 a 100 y de hasta 2 decimales. */
const mensajeDePorcentaje = (numero: number, escrito: string, de: string) => {
  if (numero < 0 || numero > 100) return `${de} debe estar entre 0 y 100 %.`;
  if (decimales(escrito) > 2) return `${de} admite hasta 2 decimales.`;
  return null;
};

/** Un mensaje por campo con error (vacío si todo está bien). */
export const validarTarifario = (
  valores: ValoresTarifario,
): Partial<Record<Campo, string>> => {
  const errores: Partial<Record<Campo, string>> = {};

  // La cuota de ingreso es opcional.
  const ingreso = aNumero(valores.montoCuotaIngreso);
  if (ingreso !== null) {
    const mensaje = mensajeDeMonto(
      ingreso,
      valores.montoCuotaIngreso,
      'la cuota de ingreso',
    );
    if (mensaje) errores.montoCuotaIngreso = mensaje;
  }

  const matricula = aNumero(valores.montoMatricula);
  if (matricula === null) {
    errores.montoMatricula = 'Ingresa el monto de la matrícula.';
  } else {
    const mensaje = mensajeDeMonto(
      matricula,
      valores.montoMatricula,
      'la matrícula',
    );
    if (mensaje) errores.montoMatricula = mensaje;
  }

  const pension = aNumero(valores.montoPension);
  if (pension === null) {
    errores.montoPension = 'Ingresa el monto de la pensión.';
  } else {
    const mensaje =
      mensajeDeMonto(pension, valores.montoPension, 'la pensión') ??
      (pension === 0
        ? 'El monto de la pensión debe ser mayor que cero.'
        : null);
    if (mensaje) errores.montoPension = mensaje;
  }

  const gracia = aNumero(valores.diasGracia);
  if (gracia === null) {
    errores.diasGracia = 'Ingresa los días de gracia.';
  } else if (
    !Number.isInteger(gracia) ||
    gracia < 0 ||
    gracia > DIAS_GRACIA_MAXIMOS
  ) {
    errores.diasGracia = `Los días de gracia deben estar entre 0 y ${DIAS_GRACIA_MAXIMOS}.`;
  }

  const recargo = aNumero(valores.recargoPorcentaje);
  if (recargo === null) {
    errores.recargoPorcentaje = 'Ingresa el recargo por cada 30 días.';
  } else {
    const mensaje = mensajeDePorcentaje(
      recargo,
      valores.recargoPorcentaje,
      'El recargo',
    );
    if (mensaje) errores.recargoPorcentaje = mensaje;
  }

  const tope = aNumero(valores.topeRecargoPorcentaje);
  if (tope === null) {
    errores.topeRecargoPorcentaje = 'Ingresa el tope de recargo.';
  } else {
    const mensaje = mensajeDePorcentaje(
      tope,
      valores.topeRecargoPorcentaje,
      'El tope de recargo',
    );
    if (mensaje) errores.topeRecargoPorcentaje = mensaje;
  }

  if (
    recargo !== null &&
    tope !== null &&
    !errores.recargoPorcentaje &&
    !errores.topeRecargoPorcentaje &&
    recargo > tope
  ) {
    errores.recargoPorcentaje =
      'El recargo no puede ser mayor que el tope de recargo.';
  }
  return errores;
};

/** Resolver de react-hook-form: cada mensaje va bajo su campo. */
export const resolverTarifario: Resolver<ValoresTarifario> = async (
  valores,
) => {
  const errores = validarTarifario(valores);
  const campos = Object.keys(errores) as Campo[];
  if (campos.length === 0) return { values: valores, errors: {} };
  return {
    values: {},
    errors: Object.fromEntries(
      campos.map((campo) => [
        campo,
        { type: 'validate', message: errores[campo] },
      ]),
    ),
  };
};

/** El cuerpo de `POST …/confirmar`. Solo se llama con valores que ya pasaron `validarTarifario`. */
export const solicitudDeTarifario = (
  valores: ValoresTarifario,
): SolicitudTarifario => ({
  montoCuotaIngreso: aNumero(valores.montoCuotaIngreso),
  montoMatricula: aNumero(valores.montoMatricula) as number,
  montoPension: aNumero(valores.montoPension) as number,
  diasGracia: aNumero(valores.diasGracia) as number,
  recargoPorcentaje: aNumero(valores.recargoPorcentaje) as number,
  topeRecargoPorcentaje: aNumero(valores.topeRecargoPorcentaje) as number,
});
