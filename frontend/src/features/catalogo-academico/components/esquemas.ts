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

// ---------- Escala de calificación (HU-55) ----------

export const NIVELES_DE_MENOR_A_MAYOR = ['C', 'B', 'A', 'AD'] as const;

type Rango = { nivel: string; notaDesde: number; notaHasta: number };

/**
 * Mismas reglas y mensajes que TablaConversion.validarRangos (backend): C empieza en 0, cada nivel empieza justo
 * después del anterior y AD termina en 20. Devuelve el primer error o null.
 */
export const errorDeRangos = (niveles: Rango[]): string | null => {
  let esperado = 0;
  for (const codigo of NIVELES_DE_MENOR_A_MAYOR) {
    const n = niveles.find((r) => r.nivel === codigo);
    if (!n) return 'Define un rango para cada nivel: AD, A, B y C.';
    if (n.notaDesde > n.notaHasta)
      return `En ${codigo}, la nota "desde" no puede ser mayor que "hasta".`;
    if (n.notaDesde < esperado)
      return `El rango de ${codigo} se cruza con el nivel anterior.`;
    if (n.notaDesde > esperado)
      return `Falta cubrir de ${esperado} a ${n.notaDesde - 1} antes de ${codigo}.`;
    esperado = n.notaHasta + 1;
  }
  return esperado - 1 === 20 ? null : 'El nivel AD debe terminar en 20.';
};

const nota = z.coerce
  .number({ invalid_type_error: 'Ingresa una nota.' })
  .int('Ingresa una nota entera.')
  .min(0, 'Las notas van de 0 a 20.')
  .max(20, 'Las notas van de 0 a 20.');

export const nuevaVersionSchema = z
  .object({
    vigenteDesde: z
      .string()
      .min(1, 'Elige la fecha desde la que rige la versión.'),
    niveles: z.array(
      z.object({
        nivel: z.enum(['AD', 'A', 'B', 'C']),
        notaDesde: nota,
        notaHasta: nota,
      }),
    ),
  })
  .superRefine((valores, ctx) => {
    const error = errorDeRangos(valores.niveles);
    if (error)
      ctx.addIssue({ code: 'custom', path: ['niveles'], message: error });
  });

export type NuevaVersionFormulario = z.input<typeof nuevaVersionSchema>;
