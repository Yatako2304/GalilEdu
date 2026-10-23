import { z } from 'zod';

import { Regimen } from '@/types/api';

export const ANIO_MINIMO = 2000;
export const ANIO_MAXIMO = 2100;

const MENSAJE_ANIO = `El año escolar debe estar entre ${ANIO_MINIMO} y ${ANIO_MAXIMO}.`;

// Mensajes del backend (DtosConfiguracion), para que el formulario y el servidor digan lo mismo.
const fechaInicio = z.string().min(1, 'Ingresa la fecha de inicio.');
const fechaFin = z.string().min(1, 'Ingresa la fecha de fin.');

/** El orden de las fechas se valida con strings yyyy-mm-dd, que se comparan bien como texto. */
const finPosteriorAlInicio = (
  valores: { fechaInicio: string; fechaFin: string },
  ctx: z.RefinementCtx,
) => {
  if (
    valores.fechaInicio &&
    valores.fechaFin &&
    valores.fechaFin <= valores.fechaInicio
  ) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['fechaFin'],
      message: 'La fecha de fin debe ser posterior a la fecha de inicio.',
    });
  }
};

/** Modal "Nuevo año escolar" (Figma). */
export const nuevoAnioSchema = z
  .object({
    anio: z
      .string()
      .trim()
      .min(1, 'Ingresa el año escolar.')
      // El año vacío ya tiene su mensaje: no se suma el del rango.
      .refine((v) => v === '' || anioEscrito(v) !== null, MENSAJE_ANIO),
    fechaInicio,
    fechaFin,
    regimen: z.enum(['BIMESTRAL', 'TRIMESTRAL'], {
      errorMap: () => ({ message: 'Elige el régimen de periodos.' }),
    }),
  })
  .superRefine(finPosteriorAlInicio);

/** "Editar año escolar": solo las fechas; el año y el régimen no se tocan aquí. */
export const edicionAnioSchema = z
  .object({ fechaInicio, fechaFin })
  .superRefine(finPosteriorAlInicio);

export type ValoresAnio = {
  anio: string;
  fechaInicio: string;
  fechaFin: string;
  regimen: Regimen;
};

/** Inicio y fin de clases que propone el modal para un año (Figma: 01/03 y 15/12). */
export const fechasSugeridas = (anio: number) => ({
  fechaInicio: `${anio}-03-01`,
  fechaFin: `${anio}-12-15`,
});

/** El año siguiente al último registrado; sin años registrados, el siguiente al actual. */
export const anioSugerido = (
  anios: { anio: number }[],
  anioActual = new Date().getFullYear(),
) =>
  (anios.length > 0 ? Math.max(...anios.map((a) => a.anio)) : anioActual) + 1;

/** Valores con los que se abre el modal Nuevo año: año sugerido, sus fechas y régimen Bimestral. */
export const valoresIniciales = (
  anios: { anio: number }[],
  anioActual?: number,
): ValoresAnio => {
  const anio = anioSugerido(anios, anioActual);
  return { anio: String(anio), ...fechasSugeridas(anio), regimen: 'BIMESTRAL' };
};

/** El año escrito, si ya es un año válido (así no se recalculan las fechas con "20" o "202"). */
export const anioEscrito = (texto: string): number | null =>
  /^\d{4}$/.test(texto.trim()) &&
  Number(texto) >= ANIO_MINIMO &&
  Number(texto) <= ANIO_MAXIMO
    ? Number(texto)
    : null;
