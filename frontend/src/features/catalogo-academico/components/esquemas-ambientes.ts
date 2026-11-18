import { z } from 'zod';

import { SolicitudAmbiente, TipoAmbiente } from '@/types/api';

// Mismas reglas que Ambiente.java y DtosAmbientes.java (los mensajes del backend se muestran si algo se escapa).

export const NOMBRE_MAXIMO = 80;
export const RECURSO_MAXIMO = 60;
/** Figma CI-26 > "Modal - Nuevo ambiente físico": «Personas. Un aula regular admite como máximo 45.» */
export const AFORO_MAXIMO_AULA_REGULAR = 45;
/** La columna es SMALLINT. */
export const AFORO_MAXIMO_COLUMNA = 32767;

export const MENSAJE_AFORO_AULA_REGULAR =
  'Un aula regular admite como máximo 45.';

/** Espacios repetidos a uno y sin espacios en los extremos (\s ya incluye los espacios no separables y los saltos de línea). */
const compactar = (texto: string) => texto.replace(/\s+/g, ' ').trim();

/** Cuenta caracteres como lo hace la base de datos (puntos de código), no unidades de UTF-16. */
const longitud = (texto: string) => [...texto].length;

/**
 * "Kits de robótica, Proyector, 12 laptops" → ['Kits de robótica', 'Proyector', '12 laptops'].
 * Igual que el backend: se recortan, se quitan los vacíos y los repetidos (sin distinguir mayúsculas; queda el
 * primero) y se conserva el orden.
 */
export const parsearRecursos = (texto: string): string[] => {
  const vistos = new Set<string>();
  const recursos: string[] = [];
  for (const crudo of texto.split(',')) {
    const recurso = compactar(crudo);
    const clave = recurso.toLowerCase();
    if (recurso && !vistos.has(clave)) {
      vistos.add(clave);
      recursos.push(recurso);
    }
  }
  return recursos;
};

/** La lista que viene del backend, como texto para el campo de edición. */
export const textoRecursos = (recursos: string[]) => recursos.join(', ');

/**
 * Esquema del formulario. En edición el tipo no cambia: el campo está bloqueado y su valor es el del ambiente
 * (valor por defecto del formulario), que es el que decide el tope de 45.
 */
export const ambienteSchema = z
  .object({
    tipo: z
      .string()
      .refine(
        (valor) => valor === 'AULA_REGULAR' || valor === 'ESPACIO_ESPECIAL',
        'Elige el tipo de ambiente.',
      ),
    nombre: z
      .string()
      .transform(compactar)
      .pipe(
        z
          .string()
          .min(1, 'Ingresa el nombre del ambiente.')
          .refine(
            (n) => longitud(n) <= NOMBRE_MAXIMO,
            'El nombre admite como máximo 80 caracteres.',
          ),
      ),
    aforoMaximo: z
      .string()
      .trim()
      .min(1, 'Ingresa el aforo máximo.')
      .regex(/^-?\d+$/, 'Ingresa el aforo como un número entero.')
      .transform(Number)
      .pipe(
        z
          .number()
          .min(1, 'El aforo debe ser mayor que 0.')
          .max(
            AFORO_MAXIMO_COLUMNA,
            `El aforo no puede ser mayor que ${AFORO_MAXIMO_COLUMNA}.`,
          ),
      ),
    recursos: z
      .string()
      .transform(parsearRecursos)
      .refine(
        (lista) => lista.every((r) => longitud(r) <= RECURSO_MAXIMO),
        'Cada recurso admite como máximo 60 caracteres.',
      ),
  })
  .superRefine((valores, contexto) => {
    if (
      valores.tipo === 'AULA_REGULAR' &&
      valores.aforoMaximo > AFORO_MAXIMO_AULA_REGULAR
    ) {
      contexto.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['aforoMaximo'],
        message: MENSAJE_AFORO_AULA_REGULAR,
      });
    }
  });

/** Lo que escribe el usuario: todo texto, el tipo vacío hasta que elija. */
export type AmbienteFormulario = z.input<typeof ambienteSchema>;

/** Lo que se envía al crear, ya normalizado. */
export const aSolicitudAmbiente = (
  valores: z.output<typeof ambienteSchema>,
): SolicitudAmbiente => ({
  tipo: valores.tipo as TipoAmbiente,
  nombre: valores.nombre,
  aforoMaximo: valores.aforoMaximo,
  recursos: valores.recursos,
});

/** Campo del formulario al que pertenece un error del backend; `null` si no es de ningún campo. */
export type CampoAmbiente = 'nombre' | 'aforoMaximo' | 'tipo' | 'recursos';

/**
 * Los 409 de crear y editar dicen en el mensaje de qué campo hablan: «Ya existe un ambiente llamado "X".» es del
 * nombre y «No puedes bajar el aforo a N: la sección …» es del aforo (RF-111). Cualquier otro error va en una alerta
 * del modal.
 */
export const campoDelErrorDelBackend = (
  estado: number | undefined,
  mensaje: string | undefined,
): CampoAmbiente | null => {
  if (estado !== 409 || !mensaje) return null;
  if (mensaje.startsWith('Ya existe un ambiente')) return 'nombre';
  if (mensaje.startsWith('No puedes bajar el aforo')) return 'aforoMaximo';
  return null;
};
