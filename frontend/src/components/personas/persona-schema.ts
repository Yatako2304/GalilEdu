import { z } from 'zod';

import { DatosPersona } from '@/types/api';

// Validaciones de datos personales (RF-12). Mismas reglas que backend/personas/aplicacion/DatosPersona.java.

const sinNumeros = (mensaje: string) =>
  z
    .string()
    .trim()
    .regex(/^[^0-9]*$/, mensaje);

const opcional = z
  .string()
  .trim()
  .optional()
  .nullable()
  .transform((v) => (v ? v : null));

/** Celular de 9 dígitos; acepta "987 111 222" y guarda "987111222". */
const celular = z
  .string()
  .optional()
  .nullable()
  .transform((v) => (v ? v.replace(/\s/g, '') : null))
  .refine((v) => v === null || /^[0-9]{9}$/.test(v), {
    message: 'El celular debe tener 9 dígitos',
  });

const base = z.object({
  tipoDocumento: z.enum(['DNI', 'CARNET_EXTRANJERIA'], {
    message: 'Selecciona el tipo de documento',
  }),
  numeroDocumento: z.string().trim().min(1, 'Ingresa el número de documento'),
  apellidoPaterno: sinNumeros('El apellido no puede contener números').min(
    1,
    'Ingresa el apellido paterno',
  ),
  apellidoMaterno: sinNumeros('El apellido no puede contener números')
    .optional()
    .nullable()
    .transform((v) => (v ? v : null)),
  nombres: sinNumeros('Los nombres no pueden contener números').min(
    1,
    'Ingresa los nombres',
  ),
  fechaNacimiento: opcional,
  nacionalidad: opcional,
  lugarNacimiento: opcional,
  sexo: z
    .enum(['MASCULINO', 'FEMENINO'])
    .optional()
    .nullable()
    .or(z.literal('').transform(() => null)),
  estadoCivil: opcional,
  gradoInstruccion: opcional,
  celular,
  correo: z
    .string()
    .trim()
    .optional()
    .nullable()
    .transform((v) => (v ? v.toLowerCase() : null))
    .refine((v) => v === null || z.string().email().safeParse(v).success, {
      message: 'Ingresa un correo válido',
    }),
  telefonoCasa: opcional,
  profesion: opcional,
  centroTrabajo: opcional,
  cargo: opcional,
  distrito: opcional,
  direccion: opcional,
  esExalumno: z.boolean().optional().nullable(),
});

const documentoValido = <T extends z.infer<typeof base>>(
  p: T,
  ctx: z.RefinementCtx,
) => {
  const ok =
    p.tipoDocumento === 'DNI'
      ? /^[0-9]{8}$/.test(p.numeroDocumento)
      : /^[A-Za-z0-9]{1,12}$/.test(p.numeroDocumento);
  if (!ok) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['numeroDocumento'],
      message:
        p.tipoDocumento === 'DNI'
          ? 'El DNI debe tener 8 dígitos'
          : 'Hasta 12 caracteres alfanuméricos',
    });
  }
};

/** Persona con solo lo mínimo obligatorio (padres, estudiante). */
export const personaSchema = base.superRefine(documentoValido);

/** Persona que tendrá cuenta de usuario: correo y celular son obligatorios. */
export const personaUsuarioSchema = base
  .extend({
    celular: celular.refine((v) => v !== null, {
      message: 'Ingresa el celular',
    }),
    correo: z
      .string()
      .trim()
      .min(1, 'Ingresa el correo electrónico')
      .email('Ingresa un correo válido')
      .transform((v) => v.toLowerCase()),
  })
  .superRefine(documentoValido);

export type PersonaFormValues = z.input<typeof base>;

export const personaVacia = (): PersonaFormValues => ({
  tipoDocumento: 'DNI',
  numeroDocumento: '',
  apellidoPaterno: '',
  apellidoMaterno: '',
  nombres: '',
  fechaNacimiento: '',
  nacionalidad: 'Peruana',
  lugarNacimiento: '',
  sexo: '',
  estadoCivil: '',
  gradoInstruccion: '',
  celular: '',
  correo: '',
  telefonoCasa: '',
  profesion: '',
  centroTrabajo: '',
  cargo: '',
  distrito: '',
  direccion: '',
  esExalumno: false,
});

/** Convierte lo que llega del backend (nulls) a valores de formulario (strings). */
export const personaAFormulario = (p: DatosPersona): PersonaFormValues => {
  const vacia = personaVacia();
  const valores = Object.fromEntries(
    Object.entries(p).map(([k, v]) => [
      k,
      v ?? (k === 'esExalumno' ? false : ''),
    ]),
  );
  return { ...vacia, ...valores } as PersonaFormValues;
};
