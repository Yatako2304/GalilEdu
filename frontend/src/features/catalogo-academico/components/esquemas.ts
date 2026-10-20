import { z } from 'zod';

// Mismas reglas que DtosAcademico.java (los mensajes del backend se muestran si algo se escapa).

const descripcion = z
  .string()
  .trim()
  .max(500, 'La descripción admite hasta 500 caracteres.')
  .transform((v) => (v === '' ? null : v));

export const areaSchema = z.object({
  nombre: z
    .string()
    .trim()
    .min(1, 'Ingresa el nombre del área.')
    .max(80, 'El nombre admite hasta 80 caracteres.'),
  descripcion,
});

export const competenciaSchema = z.object({
  nombre: z
    .string()
    .trim()
    .min(1, 'Ingresa el nombre de la competencia.')
    .max(150, 'El nombre admite hasta 150 caracteres.'),
  descripcion,
});

export const cursoSchema = z.object({
  nombre: z
    .string()
    .trim()
    .min(1, 'Ingresa el nombre del curso.')
    .max(80, 'El nombre admite hasta 80 caracteres.'),
  grados: z.array(z.string()).min(1, 'Elige al menos un grado.'),
});

export type AreaFormulario = z.input<typeof areaSchema>;
export type CompetenciaFormulario = z.input<typeof competenciaSchema>;
export type CursoFormulario = z.input<typeof cursoSchema>;

const ORDINAL = (codigo: string) => `${codigo.split('-')[1]}.°`;

/**
 * Columna "Se dicta en" de Figma CI-14: ["PRI-1","PRI-2","SEC-1"] → "Primaria 1.°, 2.° · Secundaria 1.°".
 * Los grados llegan ordenados desde el backend.
 */
export const textoGrados = (grados: string[]) =>
  [
    ['Primaria', grados.filter((g) => g.startsWith('PRI-'))],
    ['Secundaria', grados.filter((g) => g.startsWith('SEC-'))],
  ]
    .filter(([, lista]) => lista.length > 0)
    .map(
      ([nivel, lista]) =>
        `${nivel} ${(lista as string[]).map(ORDINAL).join(', ')}`,
    )
    .join(' · ');
