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

const obligatorio = (mensaje: string) => z.string().trim().min(1, mensaje);

/** Fecha de hoy en hora local (YYYY-MM-DD), para comparar con el input type="date". */
const hoy = () => new Date().toLocaleDateString('en-CA');

/**
 * Persona que tendrá cuenta de usuario (HU-6, criterios CAHU6-*): todo es obligatorio salvo
 * apellido materno, teléfono de casa y exalumno. Se usa en "Nuevo usuario" y "Editar usuario".
 */
const camposUsuario = base.extend({
  fechaNacimiento: obligatorio('Ingresa la fecha de nacimiento').refine(
    (v) => v < hoy(),
    { message: 'La fecha de nacimiento debe ser pasada' },
  ),
  nacionalidad: obligatorio('Selecciona la nacionalidad'),
  // En el formulario de usuario se muestra como "País de nacimiento".
  lugarNacimiento: obligatorio('Selecciona el país de nacimiento'),
  sexo: z.enum(['MASCULINO', 'FEMENINO'], { message: 'Selecciona el sexo' }),
  estadoCivil: obligatorio('Selecciona el estado civil'),
  gradoInstruccion: obligatorio('Selecciona el grado de instrucción'),
  celular: celular.refine((v) => v !== null, {
    message: 'Ingresa el celular',
  }),
  correo: z
    .string()
    .trim()
    .min(1, 'Ingresa el correo electrónico')
    .email('Ingresa un correo válido')
    .transform((v) => v.toLowerCase()),
  telefonoCasa: opcional.refine((v) => v === null || /^[0-9]+$/.test(v), {
    message: 'El teléfono de casa solo puede contener números',
  }),
  profesion: obligatorio('Ingresa la profesión u ocupación'),
  centroTrabajo: obligatorio('Ingresa el centro de trabajo'),
  cargo: obligatorio('Ingresa el cargo'),
  distrito: obligatorio('Selecciona el distrito'),
  direccion: obligatorio('Ingresa la dirección'),
});

/**
 * Se valida todo dentro de un mismo superRefine para que el error del documento (8 dígitos del DNI)
 * aparezca junto con los demás; si no, zod lo omite mientras falte algún otro campo.
 */
export const personaUsuarioSchema = z
  .custom<PersonaFormValues>()
  .superRefine((p, ctx) => {
    const campos = camposUsuario.safeParse(p);
    if (!campos.success) {
      campos.error.issues.forEach((issue) => ctx.addIssue(issue));
    }
    const numero = (p.numeroDocumento ?? '').trim();
    if (numero) {
      const valido =
        p.tipoDocumento === 'DNI'
          ? /^[0-9]{8}$/.test(numero)
          : /^[A-Za-z0-9]{1,12}$/.test(numero);
      if (!valido) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['numeroDocumento'],
          message:
            p.tipoDocumento === 'DNI'
              ? 'El DNI debe tener 8 dígitos'
              : 'Hasta 12 caracteres alfanuméricos',
        });
      }
    }
  })
  .transform((p) => camposUsuario.parse(p));

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
