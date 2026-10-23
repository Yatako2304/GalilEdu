import { z } from 'zod';

import {
  SolicitudCapacidad,
  SolicitudSeccion,
  SolicitudTutores,
} from '@/types/api';

// Figma > "Modal - Nueva sección" (HU-75). Las reglas y los mensajes son los del backend (Seccion y SolicitudSeccion),
// para que el formulario y el servidor digan lo mismo.

export const LONGITUD_MAXIMA_NOMBRE = 5;
export const CAPACIDAD_MINIMA = 1;
export const CAPACIDAD_MAXIMA = 45;

const MENSAJE_CAPACIDAD = `Entre ${CAPACIDAD_MINIMA} y ${CAPACIDAD_MAXIMA} alumnos.`;

/** Letras (con tilde o ñ) y números; ni espacios ni signos. Es la misma regla del backend. */
const SOLO_LETRAS_Y_NUMEROS = /^[\p{L}\p{N}]+$/u;

/**
 * El nombre de la sección se escribe en mayúsculas ("a" y "A" son la misma sección). Se aplica al recibir lo que
 * se escribe. No quita espacios: uno en medio se avisa en vivo con el mensaje del esquema.
 */
export const normalizarNombreSeccion = (texto: string) => texto.toUpperCase();

/** Entero de 1 a 45 escrito con dígitos. */
const esCapacidadValida = (texto: string) =>
  /^\d+$/.test(texto) &&
  Number(texto) >= CAPACIDAD_MINIMA &&
  Number(texto) <= CAPACIDAD_MAXIMA;

const capacidadSchema = z
  .string()
  .trim()
  .min(1, 'Ingresa la capacidad máxima.')
  .refine(
    (valor) => valor === '' || esCapacidadValida(valor),
    MENSAJE_CAPACIDAD,
  );

/**
 * Modal "Nueva sección". Todo se escribe como texto y se convierte al enviar (como `nuevoAnioSchema`). Los espacios
 * de los bordes se ignoran (el backend también los recorta). Cada regla va una sola vez y en orden, así que el
 * formulario muestra un solo mensaje por campo.
 */
export const nuevaSeccionSchema = z.object({
  gradoCodigo: z.string().min(1, 'Elige un grado.'),
  nombre: z
    .string()
    .trim()
    .min(1, 'Ingresa el nombre de la sección.')
    .max(
      LONGITUD_MAXIMA_NOMBRE,
      `El nombre de la sección admite hasta ${LONGITUD_MAXIMA_NOMBRE} caracteres.`,
    )
    // El nombre vacío ya tiene su mensaje: no se suma el de los caracteres.
    .refine(
      (valor) => valor === '' || SOLO_LETRAS_Y_NUMEROS.test(valor),
      'El nombre de la sección solo admite letras y números.',
    ),
  capacidadMaxima: capacidadSchema,
});

/** Lo que escribe el usuario en el modal; nada viene precargado, salvo el grado si se abre desde un grado. */
export type ValoresSeccion = {
  gradoCodigo: string;
  nombre: string;
  capacidadMaxima: string;
};

export const valoresInicialesSeccion = (gradoCodigo = ''): ValoresSeccion => ({
  gradoCodigo,
  nombre: '',
  capacidadMaxima: '',
});

/** El cuerpo de `POST /configuracion/anios-escolares/{id}/secciones` a partir de lo escrito (ya validado). */
export const aSolicitudSeccion = (
  valores: ValoresSeccion,
): SolicitudSeccion => ({
  gradoCodigo: valores.gradoCodigo,
  nombre: valores.nombre.trim(),
  capacidadMaxima: Number(valores.capacidadMaxima),
});

/** Modal "Editar sección": solo la capacidad (el nombre y el grado no se editan). */
export const editarSeccionSchema = z.object({
  capacidadMaxima: capacidadSchema,
});

export type ValoresCapacidad = { capacidadMaxima: string };

export const aSolicitudCapacidad = (
  valores: ValoresCapacidad,
): SolicitudCapacidad => ({ capacidadMaxima: Number(valores.capacidadMaxima) });

/** Valor del cotutor cuando no hay: la opción "Sin cotutor" (un <select> no admite null). */
export const SIN_COTUTOR = '';

/** Modal "Asignar tutor" (mensajes del backend: Seccion.validarTutores). */
export const tutoresSchema = z
  .object({
    tutorId: z.string().min(1, 'Elige un tutor.'),
    cotutorId: z.string(),
  })
  .refine((v) => v.cotutorId === SIN_COTUTOR || v.cotutorId !== v.tutorId, {
    path: ['cotutorId'],
    message: 'El cotutor debe ser distinto del tutor.',
  });

export type ValoresTutores = { tutorId: string; cotutorId: string };

export const valoresInicialesTutores = (seccion: {
  tutorId: string | null;
  cotutorId: string | null;
}): ValoresTutores => ({
  tutorId: seccion.tutorId ?? '',
  cotutorId: seccion.cotutorId ?? SIN_COTUTOR,
});

export const aSolicitudTutores = (
  valores: ValoresTutores,
): SolicitudTutores => ({
  tutorId: valores.tutorId,
  cotutorId: valores.cotutorId === SIN_COTUTOR ? null : valores.cotutorId,
});
