import { z } from 'zod';

import {
  PersonaFormValues,
  personaAFormulario,
  personaSchema,
  personaUsuarioSchema,
  personaVacia,
  textoSoloLetras,
} from '@/components/personas/persona-schema';
import { PAIS_CON_UBIGEO } from '@/config/ubigeo';
import {
  CuentaFamiliar,
  DatosFamiliar,
  DatosPersona,
  EdicionEstudiante,
  ModoSalida,
  Parentesco,
  SolicitudInscripcion,
} from '@/types/api';

// Valores del formulario de inscripción (los 5 pasos comparten un solo formulario).

export type FamiliarForm = {
  noRegistra: boolean;
  /**
   * Padre o madre fallecido: se registran sus datos, pero no tiene cuenta, no puede ser responsable de matrícula
   * ni recoger al estudiante (validación de la iteración 1 con el PO).
   */
  fallecido: boolean;
  motivo: string;
  observacion: string;
  /** Si la persona ya existe en GalilEdu (búsqueda por documento), solo se vincula. */
  personaId: string | null;
  tieneCuenta: boolean;
  aCargo: string[];
  persona: PersonaFormValues;
  viveConEstudiante: boolean;
  tienePatriaPotestad: boolean;
  relacion: string;
  /** Texto libre cuando la relación es "Otro" (Figma: "Especificar"). */
  relacionDetalle: string;
};

export type InscripcionForm = {
  estudiante: {
    persona: PersonaFormValues;
    paisNacimiento: string;
    departamentoNacimiento: string;
    provinciaNacimiento: string;
    distritoNacimiento: string;
    referenciaDomicilio: string;
    tieneInternet: '' | 'SI' | 'NO';
    lenguaMaterna: string;
    segundaLengua: string;
    autoidentificacionEtnica: string;
    tieneDiscapacidad: 'SI' | 'NO';
    discapacidadDetalle: string;
    colegioProcedencia: string;
    gestionProcedencia: string;
    anioAcademico: string;
    nivel: string;
    grado: string;
    formaIngreso: string;
  };
  fichaMedica: {
    tipoSangre: string;
    pesoKg: string;
    estaturaM: string;
    tipoSeguro: string;
    tieneAlergias: 'SI' | 'NO';
    alergiasAlimentarias: string;
    alergiasMedicamentosas: string;
    fiebreMaxima: string;
    haConvulsionado: 'SI' | 'NO';
    tieneEnfermedad: 'SI' | 'NO';
    enfermedadDetalle: string;
    tomaMedicamento: 'SI' | 'NO';
    medicamentoDetalle: string;
    observaciones: string;
  };
  padre: FamiliarForm;
  madre: FamiliarForm;
  incluirApoderado: boolean;
  apoderado: FamiliarForm;
  contactos: {
    nombreCompleto: string;
    parentesco: string;
    telefonoFijo: string;
    celular: string;
  }[];
  modoSalida: '' | ModoSalida;
  autorizados: { nombreCompleto: string; relacion: string }[];
  responsable: '' | Parentesco;
  correoResponsable: string;
  celularResponsable: string;
  verificado: boolean;
  /** Documento que acredita al apoderado. Se sube después de registrar la inscripción. */
  documentoApoderado: { tipo: string; archivo: File | null };
  /** "Editar": los familiares ya registrados se pueden corregir, así que también se validan. */
  editando: boolean;
};

const familiarVacio = (sexo: 'MASCULINO' | 'FEMENINO' | ''): FamiliarForm => ({
  noRegistra: false,
  fallecido: false,
  motivo: '',
  observacion: '',
  personaId: null,
  tieneCuenta: false,
  aCargo: [],
  // Como en el Paso 1, nada trae valor por defecto salvo el sexo del padre y de la madre; las casillas empiezan sin
  // marcar.
  persona: {
    ...personaVacia(),
    tipoDocumento: '' as unknown as PersonaFormValues['tipoDocumento'],
    nacionalidad: '',
    sexo,
  },
  viveConEstudiante: false,
  tienePatriaPotestad: false,
  relacion: '',
  relacionDetalle: '',
});

export const inscripcionVacia = (): InscripcionForm => ({
  estudiante: {
    // Ningún campo del Paso 1 trae valor por defecto (salvo "¿Tiene alguna discapacidad?": No).
    persona: {
      ...personaVacia(),
      tipoDocumento: '' as unknown as PersonaFormValues['tipoDocumento'],
      nacionalidad: '',
    },
    paisNacimiento: '',
    departamentoNacimiento: '',
    provinciaNacimiento: '',
    distritoNacimiento: '',
    referenciaDomicilio: '',
    tieneInternet: '',
    lenguaMaterna: '',
    segundaLengua: '',
    autoidentificacionEtnica: '',
    tieneDiscapacidad: 'NO',
    discapacidadDetalle: '',
    colegioProcedencia: '',
    gestionProcedencia: '',
    anioAcademico: '',
    nivel: '',
    grado: '',
    formaIngreso: '',
  },
  fichaMedica: {
    tipoSangre: '',
    pesoKg: '',
    estaturaM: '',
    tipoSeguro: '',
    tieneAlergias: 'NO',
    alergiasAlimentarias: '',
    alergiasMedicamentosas: '',
    fiebreMaxima: '',
    haConvulsionado: 'NO',
    tieneEnfermedad: 'NO',
    enfermedadDetalle: '',
    tomaMedicamento: 'NO',
    medicamentoDetalle: '',
    observaciones: '',
  },
  padre: familiarVacio('MASCULINO'),
  madre: familiarVacio('FEMENINO'),
  incluirApoderado: false,
  // El apoderado no trae ningún valor por defecto (tampoco el sexo).
  apoderado: familiarVacio(''),
  contactos: [
    { nombreCompleto: '', parentesco: '', telefonoFijo: '', celular: '' },
    { nombreCompleto: '', parentesco: '', telefonoFijo: '', celular: '' },
  ],
  modoSalida: 'PERSONA_AUTORIZADA',
  autorizados: [],
  responsable: '',
  correoResponsable: '',
  celularResponsable: '',
  verificado: false,
  documentoApoderado: { tipo: '', archivo: null },
  editando: false,
});

/** Relación del apoderado con el estudiante (Figma). Una relación guardada que no está aquí es "Otro". */
export const RELACIONES_APODERADO = [
  'Abuelo(a)',
  'Tío(a)',
  'Hermano(a)',
  'Amigo(a) de la familia',
  'Tutor legal',
  'Otro',
];

/** Un familiar cuenta como registrado si no está marcado "no registra" y tiene documento. */
export const registra = (f: FamiliarForm) =>
  !f.noRegistra && (!!f.personaId || !!f.persona.numeroDocumento);

/**
 * Registrado y no fallecido: puede ser responsable de matrícula, recoger al estudiante y tiene cuenta. Un padre o
 * una madre fallecidos no cuentan para la regla del apoderado.
 */
export const presente = (f: FamiliarForm) => registra(f) && !f.fallecido;

/**
 * Motivos de "No registra" (validación de la iteración 1 con el PO). "Fallecido(a)" ya no es un motivo: el fallecido
 * se registra con sus datos. Con "Otro" la observación es obligatoria.
 */
export const MOTIVOS_NO_REGISTRA = [
  'No se conoce',
  'No ha reconocido al estudiante',
  'Otro',
];

/** Validación de la iteración 1 con el PO: padre, madre y apoderado deben ser mayores de edad. */
export const MAYORIA_DE_EDAD = 18;

/** Edad cumplida hoy para una fecha AAAA-MM-DD; null si no es una fecha. */
export const edadCumplida = (fecha: string | null | undefined) => {
  if (!fecha) return null;
  const nacimiento = new Date(`${fecha}T00:00:00`);
  if (Number.isNaN(nacimiento.getTime())) return null;
  const hoy = new Date();
  let edad = hoy.getFullYear() - nacimiento.getFullYear();
  if (
    hoy.getMonth() < nacimiento.getMonth() ||
    (hoy.getMonth() === nacimiento.getMonth() &&
      hoy.getDate() < nacimiento.getDate())
  ) {
    edad--;
  }
  return edad;
};

const vacio = (v: unknown) => typeof v !== 'string' || v.trim() === '';

const familiarSchema = (opcional: boolean, editando = false) =>
  z.custom<FamiliarForm>().superRefine((f, ctx) => {
    if (f.noRegistra) {
      if (!opcional && !f.motivo) {
        ctx.addIssue({
          code: 'custom',
          path: ['motivo'],
          message: 'Indica el motivo',
        });
      }
      if (!opcional && f.motivo === 'Otro' && vacio(f.observacion)) {
        ctx.addIssue({
          code: 'custom',
          path: ['observacion'],
          message: 'Indica la observación',
        });
      }
      return;
    }
    if (f.personaId && !editando) return;
    if (f.fallecido) {
      // Fallecido: solo sus datos de identidad, todos obligatorios salvo el apellido materno (no tendrá cuenta ni se
      // le contactará).
      const r = personaSchema.safeParse(f.persona);
      if (!r.success) {
        r.error.issues.forEach((i) =>
          ctx.addIssue({ ...i, path: ['persona', ...i.path] }),
        );
      }
      const exige = (campo: string, valor: unknown, message: string) => {
        if (vacio(valor)) {
          ctx.addIssue({ code: 'custom', path: ['persona', campo], message });
        }
      };
      exige(
        'fechaNacimiento',
        f.persona.fechaNacimiento,
        'Ingresa la fecha de nacimiento',
      );
      exige(
        'nacionalidad',
        f.persona.nacionalidad,
        'Selecciona la nacionalidad',
      );
      exige('sexo', f.persona.sexo, 'Selecciona el sexo');
      exige(
        'lugarNacimiento',
        f.persona.lugarNacimiento,
        'Selecciona el país de nacimiento',
      );
      exige('estadoCivil', f.persona.estadoCivil, 'Selecciona el estado civil');
      exige(
        'gradoInstruccion',
        f.persona.gradoInstruccion,
        'Selecciona el grado de instrucción',
      );
    } else {
      // Persona nueva (o ya registrada, al editar): mismos obligatorios que un usuario (CAHU6-2), porque cada
      // familiar presente tendrá cuenta.
      const r = personaUsuarioSchema.safeParse(f.persona);
      if (!r.success) {
        r.error.issues.forEach((i) =>
          ctx.addIssue({ ...i, path: ['persona', ...i.path] }),
        );
      }
    }
    // Una fecha futura solo dice que debe ser pasada (personaUsuarioSchema ya lo revisa; al fallecido se le revisa
    // aquí). Recién con una fecha pasada se valida que sea mayor de edad (validación de la iteración 1 con el PO;
    // también el fallecido: un padre no puede ser menor).
    const fecha = f.persona.fechaNacimiento;
    if (fecha && fecha >= new Date().toLocaleDateString('en-CA')) {
      if (f.fallecido) {
        ctx.addIssue({
          code: 'custom',
          path: ['persona', 'fechaNacimiento'],
          message: 'La fecha de nacimiento debe ser pasada',
        });
      }
      return;
    }
    const edad = edadCumplida(fecha);
    if (edad !== null && edad < MAYORIA_DE_EDAD) {
      ctx.addIssue({
        code: 'custom',
        path: ['persona', 'fechaNacimiento'],
        message: 'Debe ser mayor de edad',
      });
    }
  });

/**
 * Paso 1 (HU-7): todo es obligatorio salvo apellido materno, correo, referencia, teléfono de casa, segunda
 * lengua y autoidentificación étnica. "¿Cuál?" de discapacidad es obligatorio cuando la respuesta es Sí.
 */
const estudianteSchema = z
  .object({
    persona: z.custom<PersonaFormValues>(),
    anioAcademico: z.string().min(1, 'Selecciona el año académico'),
    nivel: z.string().min(1, 'Indica el nivel'),
    grado: z.string().min(1, 'Indica el grado'),
  })
  .passthrough()
  .superRefine((e, ctx) => {
    const datos = e as unknown as InscripcionForm['estudiante'];
    const exige = (
      path: (string | number)[],
      valor: unknown,
      message: string,
    ) => {
      if (vacio(valor)) ctx.addIssue({ code: 'custom', path, message });
    };
    const p = datos.persona;

    // Se valida aparte: si el tipo de documento está vacío, zod cortaría las demás reglas del paso.
    const personaValida = personaSchema.safeParse(p);
    if (!personaValida.success) {
      personaValida.error.issues.forEach((i) =>
        ctx.addIssue({ ...i, path: ['persona', ...i.path] }),
      );
    }

    exige(['persona', 'sexo'], p.sexo, 'Selecciona el sexo');
    exige(
      ['persona', 'nacionalidad'],
      p.nacionalidad,
      'Selecciona la nacionalidad',
    );
    exige(['persona', 'direccion'], p.direccion, 'Ingresa la dirección');
    exige(['persona', 'distrito'], p.distrito, 'Selecciona el distrito');

    if (vacio(p.fechaNacimiento)) {
      exige(
        ['persona', 'fechaNacimiento'],
        p.fechaNacimiento,
        'Ingresa la fecha de nacimiento',
      );
    } else if (
      new Date(`${p.fechaNacimiento}T00:00:00`).getTime() >=
      new Date().setHours(0, 0, 0, 0)
    ) {
      ctx.addIssue({
        code: 'custom',
        path: ['persona', 'fechaNacimiento'],
        message: 'La fecha de nacimiento debe ser pasada',
      });
    }

    if (p.telefonoCasa && !/^[0-9]+$/.test(p.telefonoCasa)) {
      ctx.addIssue({
        code: 'custom',
        path: ['persona', 'telefonoCasa'],
        message: 'El teléfono solo puede tener números',
      });
    }

    exige(['paisNacimiento'], datos.paisNacimiento, 'Selecciona el país');
    // Departamento, provincia y distrito solo aplican a los nacidos en el Perú.
    if (datos.paisNacimiento === PAIS_CON_UBIGEO) {
      exige(
        ['departamentoNacimiento'],
        datos.departamentoNacimiento,
        'Selecciona el departamento',
      );
      exige(
        ['provinciaNacimiento'],
        datos.provinciaNacimiento,
        'Selecciona la provincia',
      );
      exige(
        ['distritoNacimiento'],
        datos.distritoNacimiento,
        'Selecciona el distrito',
      );
    }
    exige(
      ['tieneInternet'],
      datos.tieneInternet,
      'Selecciona si tiene acceso a internet / dispositivo',
    );
    exige(
      ['lenguaMaterna'],
      datos.lenguaMaterna,
      'Selecciona la lengua materna',
    );
    if (datos.tieneDiscapacidad === 'SI') {
      exige(
        ['discapacidadDetalle'],
        datos.discapacidadDetalle,
        'Indica cuál es la discapacidad',
      );
    }
    exige(
      ['colegioProcedencia'],
      datos.colegioProcedencia,
      'Ingresa el colegio de procedencia',
    );
    exige(
      ['gestionProcedencia'],
      datos.gestionProcedencia,
      'Selecciona la gestión',
    );
    exige(
      ['formaIngreso'],
      datos.formaIngreso,
      'Selecciona la forma de ingreso',
    );
  });

/**
 * Largos y rangos de la ficha médica. Los largos son los de estudiantes.ficha_medica (V2); los mismos
 * rangos están en el backend (DatosFichaMedica).
 */
export const LIMITES_FICHA = {
  peso: { min: 5, max: 200, enteros: 3, decimales: 1 },
  estatura: { min: 0.5, max: 2.5, enteros: 1, decimales: 2 },
  fiebre: { enteros: 3, decimales: 1 },
  texto: 200,
  observaciones: 500,
} as const;

/** Número con el formato dado y punto como separador decimal; null si no lo cumple. */
const numero = (
  valor: string,
  { enteros, decimales }: { enteros: number; decimales: number },
) =>
  new RegExp(`^\\d{1,${enteros}}(\\.\\d{1,${decimales}})?$`).test(valor.trim())
    ? Number(valor.trim())
    : null;

/**
 * Paso 2 (HU-7): todo es obligatorio salvo observaciones. Si tiene alergias, al menos una de las dos;
 * si padece una enfermedad o toma un medicamento, "¿Cuál?" es obligatorio.
 */
const fichaMedicaSchema = z
  .custom<InscripcionForm['fichaMedica']>()
  .superRefine((m, ctx) => {
    const exige = (path: string, valor: unknown, message: string) => {
      if (vacio(valor)) ctx.addIssue({ code: 'custom', path: [path], message });
    };
    const rango = (
      path: 'pesoKg' | 'estaturaM',
      limites: (typeof LIMITES_FICHA)['peso' | 'estatura'],
      vacioMsg: string,
      rangoMsg: string,
    ) => {
      if (vacio(m[path])) return exige(path, m[path], vacioMsg);
      const n = numero(m[path], limites);
      if (n === null || n < limites.min || n > limites.max) {
        ctx.addIssue({ code: 'custom', path: [path], message: rangoMsg });
      }
    };

    exige('tipoSangre', m.tipoSangre, 'Selecciona el tipo de sangre');
    rango(
      'pesoKg',
      LIMITES_FICHA.peso,
      'Ingresa el peso',
      'El peso debe estar entre 5 y 200 kg',
    );
    rango(
      'estaturaM',
      LIMITES_FICHA.estatura,
      'Ingresa la estatura',
      'La estatura debe estar entre 0.50 y 2.50 m',
    );
    exige('tipoSeguro', m.tipoSeguro, 'Selecciona el tipo de seguro');
    if (
      m.tieneAlergias === 'SI' &&
      vacio(m.alergiasAlimentarias) &&
      vacio(m.alergiasMedicamentosas)
    ) {
      ctx.addIssue({
        code: 'custom',
        path: ['alergiasAlimentarias'],
        message: 'Indica al menos una alergia (alimentaria o medicamentosa)',
      });
    }
    if (vacio(m.fiebreMaxima)) {
      exige(
        'fiebreMaxima',
        m.fiebreMaxima,
        'Indica hasta cuántos grados de fiebre ha llegado',
      );
    } else if (numero(m.fiebreMaxima, LIMITES_FICHA.fiebre) === null) {
      ctx.addIssue({
        code: 'custom',
        path: ['fiebreMaxima'],
        message: 'Ingresa un número (ej. 39 o 39.5)',
      });
    }
    if (m.tieneEnfermedad === 'SI') {
      exige(
        'enfermedadDetalle',
        m.enfermedadDetalle,
        'Indica cuál es la enfermedad',
      );
    }
    if (m.tomaMedicamento === 'SI') {
      exige(
        'medicamentoDetalle',
        m.medicamentoDetalle,
        'Indica cuál es el medicamento',
      );
    }
  });

export const inscripcionSchema = z
  .object({
    estudiante: estudianteSchema,
    fichaMedica: fichaMedicaSchema,
    // Padre y madre se validan abajo, donde se sabe si se está editando.
    padre: z.custom<FamiliarForm>(),
    madre: z.custom<FamiliarForm>(),
    editando: z.boolean(),
    incluirApoderado: z.boolean(),
    apoderado: z.custom<FamiliarForm>(),
    // Paso 4: nombre, relación y celular obligatorios; teléfono fijo opcional (solo números).
    contactos: z
      .array(
        z.object({
          nombreCompleto: textoSoloLetras(
            'El nombre solo puede contener letras',
          ).min(1, 'Ingresa el nombre'),
          parentesco: textoSoloLetras(
            'La relación solo puede contener letras',
          ).min(1, 'Ingresa la relación'),
          telefonoFijo: z
            .string()
            .trim()
            .regex(/^[0-9]*$/, 'El teléfono solo puede contener números'),
          celular: z
            .string()
            .trim()
            .min(1, 'Ingresa el celular')
            .regex(/^([0-9]{9})?$/, 'El celular debe tener 9 dígitos'),
        }),
      )
      .min(2, 'Registra al menos 2 contactos de emergencia'),
    modoSalida: z.string(),
    // Las filas aparecen solo con "Agregar otra persona autorizada"; las que hay son obligatorias.
    autorizados: z.array(
      z.object({
        nombreCompleto: textoSoloLetras(
          'El nombre solo puede contener letras',
        ).min(1, 'Ingresa el nombre'),
        relacion: textoSoloLetras('La relación solo puede contener letras').min(
          1,
          'Ingresa la relación',
        ),
      }),
    ),
    // Se valida en el superRefine (solo si hay apoderado).
    documentoApoderado: z.custom<InscripcionForm['documentoApoderado']>(),
    // Paso 5: con el correo y el celular se crea (o se completa) la cuenta del responsable. Vienen del Paso 3 y
    // aquí son solo de vista, así que los mensajes dicen dónde corregirlos.
    responsable: z.string().min(1, 'Elige al responsable de matrícula'),
    correoResponsable: z
      .string()
      .trim()
      .min(1, 'Falta el correo: complétalo en el Paso 3')
      // Vacío ya tiene su mensaje: el formato se revisa solo si hay algo escrito.
      .refine((v) => v === '' || z.string().email().safeParse(v).success, {
        message: 'El correo no es válido: corrígelo en el Paso 3',
      }),
    celularResponsable: z
      .string()
      .trim()
      .min(1, 'Falta el celular: complétalo en el Paso 3')
      .regex(
        /^([0-9]{9})?$/,
        'El celular no es válido: corrígelo en el Paso 3',
      ),
    verificado: z.boolean().refine((v) => v, {
      message:
        'Confirma que verificaste los datos con los documentos originales',
    }),
  })
  .superRefine((v, ctx) => {
    const f = v as unknown as InscripcionForm;
    (['padre', 'madre'] as const).forEach((clave) => {
      const r = familiarSchema(false, f.editando).safeParse(f[clave]);
      if (!r.success) {
        r.error.issues.forEach((i) =>
          ctx.addIssue({ ...i, path: [clave, ...i.path] }),
        );
      }
    });
    if (f.incluirApoderado) {
      const r = familiarSchema(true, f.editando).safeParse(f.apoderado);
      if (!r.success) {
        r.error.issues.forEach((i) =>
          ctx.addIssue({ ...i, path: ['apoderado', ...i.path] }),
        );
      }
      // Relación con el estudiante y tipo de documento que lo acredita: obligatorios para el apoderado.
      if (registra(f.apoderado)) {
        const falta = (path: string[], valor: unknown, message: string) => {
          if (vacio(valor)) ctx.addIssue({ code: 'custom', path, message });
        };
        falta(
          ['apoderado', 'relacion'],
          f.apoderado.relacion,
          'Selecciona la relación',
        );
        if (f.apoderado.relacion === 'Otro') {
          falta(
            ['apoderado', 'relacionDetalle'],
            f.apoderado.relacionDetalle,
            'Especifica la relación',
          );
        }
        falta(
          ['documentoApoderado', 'tipo'],
          f.documentoApoderado.tipo,
          'Selecciona el documento que lo acredita',
        );
        // El archivo no es obligatorio: se puede adjuntar más adelante desde la ficha.
      }
    } else if (!presente(f.padre) && !presente(f.madre)) {
      ctx.addIssue({
        code: 'custom',
        path: ['incluirApoderado'],
        message:
          'No hay padre ni madre presentes (registrados y no fallecidos): debes registrar un apoderado.',
      });
    }
  });

/** Campos que valida cada paso antes de avanzar (nombres de react-hook-form). */
export const CAMPOS_POR_PASO = [
  ['estudiante'],
  ['fichaMedica'],
  ['padre', 'madre', 'apoderado', 'incluirApoderado', 'documentoApoderado'],
  ['contactos', 'autorizados'],
  ['responsable', 'correoResponsable', 'celularResponsable', 'verificado'],
] as const;

const texto = (v?: string | null) => (v && v.trim() ? v.trim() : null);
const siNo = (v: string) => (v === '' ? null : v === 'SI');

const persona = (p: PersonaFormValues) =>
  personaSchema.parse(p) as DatosPersona;

/**
 * Fallecido: solo se envían sus datos de identidad (los de contacto, trabajo, domicilio y exalumno no se muestran,
 * así que no se guardan aunque se hubieran escrito antes de marcar la casilla).
 */
const identidad = (p: PersonaFormValues): DatosPersona => ({
  ...persona(p),
  celular: null,
  correo: null,
  telefonoCasa: null,
  profesion: null,
  centroTrabajo: null,
  cargo: null,
  distrito: null,
  direccion: null,
  esExalumno: false,
});

const familiar = (f: FamiliarForm, editando: boolean) =>
  !registra(f)
    ? {
        noRegistra: true,
        motivo: texto(f.motivo),
        observacion: texto(f.observacion),
      }
    : {
        noRegistra: false,
        fallecido: f.fallecido,
        personaId: f.personaId,
        // Al inscribir, un familiar ya registrado solo se vincula; al editar se envían sus datos corregidos.
        persona:
          f.personaId && !editando
            ? null
            : f.fallecido
              ? identidad(f.persona)
              : persona(f.persona),
        // Un fallecido no vive con el estudiante ni tiene la patria potestad.
        viveConEstudiante: !f.fallecido && f.viveConEstudiante,
        tienePatriaPotestad: !f.fallecido && f.tienePatriaPotestad,
        relacion:
          f.relacion === 'Otro' ? texto(f.relacionDetalle) : texto(f.relacion),
      };

/** Convierte el formulario al contrato del backend (SolicitudInscripcion). */
export const aSolicitud = (f: InscripcionForm): SolicitudInscripcion => {
  const e = f.estudiante;
  const m = f.fichaMedica;
  return {
    estudiante: {
      persona: persona(e.persona),
      paisNacimiento: texto(e.paisNacimiento),
      departamentoNacimiento: texto(e.departamentoNacimiento),
      provinciaNacimiento: texto(e.provinciaNacimiento),
      distritoNacimiento: texto(e.distritoNacimiento),
      referenciaDomicilio: texto(e.referenciaDomicilio),
      tieneInternet: siNo(e.tieneInternet),
      lenguaMaterna: texto(e.lenguaMaterna),
      segundaLengua: texto(e.segundaLengua),
      autoidentificacionEtnica: texto(e.autoidentificacionEtnica),
      tieneDiscapacidad: e.tieneDiscapacidad === 'SI',
      discapacidadDetalle: texto(e.discapacidadDetalle),
      colegioProcedencia: texto(e.colegioProcedencia),
      gestionProcedencia: texto(e.gestionProcedencia),
      anioAcademico: Number(e.anioAcademico),
      gradoCodigo: e.grado,
      formaIngreso: texto(e.formaIngreso),
    },
    fichaMedica: {
      tipoSangre: texto(m.tipoSangre),
      pesoKg: numero(m.pesoKg, LIMITES_FICHA.peso),
      estaturaM: numero(m.estaturaM, LIMITES_FICHA.estatura),
      tipoSeguro: texto(m.tipoSeguro),
      // Los detalles de una respuesta "No" no se envían aunque se hayan escrito antes.
      tieneAlergias: m.tieneAlergias === 'SI',
      alergiasAlimentarias:
        m.tieneAlergias === 'SI' ? texto(m.alergiasAlimentarias) : null,
      alergiasMedicamentosas:
        m.tieneAlergias === 'SI' ? texto(m.alergiasMedicamentosas) : null,
      fiebreMaxima:
        numero(m.fiebreMaxima, LIMITES_FICHA.fiebre)?.toString() ?? null,
      haConvulsionado: m.haConvulsionado === 'SI',
      tieneEnfermedad: m.tieneEnfermedad === 'SI',
      enfermedadDetalle:
        m.tieneEnfermedad === 'SI' ? texto(m.enfermedadDetalle) : null,
      tomaMedicamento: m.tomaMedicamento === 'SI',
      medicamentoDetalle:
        m.tomaMedicamento === 'SI' ? texto(m.medicamentoDetalle) : null,
      observaciones: texto(m.observaciones),
    },
    padre: familiar(f.padre, f.editando),
    madre: familiar(f.madre, f.editando),
    apoderado: f.incluirApoderado ? familiar(f.apoderado, f.editando) : null,
    contactos: f.contactos.map((c) => ({
      nombreCompleto: c.nombreCompleto.trim(),
      parentesco: c.parentesco.trim(),
      telefonoFijo: texto(c.telefonoFijo),
      celular: c.celular ? c.celular.replace(/\s/g, '') : null,
    })),
    modoSalida: f.modoSalida || null,
    // Solo se envían si el estudiante sale con persona autorizada.
    autorizados:
      f.modoSalida === 'PERSONA_AUTORIZADA'
        ? f.autorizados.map((a) => ({
            nombreCompleto: a.nombreCompleto.trim(),
            relacion: a.relacion.trim(),
          }))
        : [],
    responsable: f.responsable as Parentesco,
    correoResponsable: texto(f.correoResponsable),
    celularResponsable: f.celularResponsable
      ? f.celularResponsable.replace(/\s/g, '')
      : null,
  };
};

/**
 * Grado sugerido según la edad cumplida al 31 de marzo del año académico (Minedu). Devuelve el código del
 * catálogo de grados (PRI-1, SEC-2…); el colegio no tiene inicial.
 */
export const gradoSugerido = (
  fechaNacimiento: string | null | undefined,
  anio: number,
): { nivel: string; grado: string; edad: number } | null => {
  if (!fechaNacimiento) return null;
  const nacimiento = new Date(`${fechaNacimiento}T00:00:00`);
  if (Number.isNaN(nacimiento.getTime())) return null;
  const corte = new Date(anio, 2, 31);
  let edad = corte.getFullYear() - nacimiento.getFullYear();
  if (
    corte.getMonth() < nacimiento.getMonth() ||
    (corte.getMonth() === nacimiento.getMonth() &&
      corte.getDate() < nacimiento.getDate())
  ) {
    edad--;
  }
  if (edad >= 6 && edad <= 11)
    return { nivel: 'PRIMARIA', grado: `PRI-${edad - 5}`, edad };
  if (edad >= 12 && edad <= 16)
    return { nivel: 'SECUNDARIA', grado: `SEC-${edad - 11}`, edad };
  return null;
};

export const TIPOS_DOCUMENTO_APODERADO = [
  'Carta poder notarial',
  'Resolución judicial de tutela',
  'Partida de nacimiento (familiar)',
  'Otro',
];

const MIME_PERMITIDOS = ['application/pdf', 'image/jpeg', 'image/png'];
export const TAMANO_MAXIMO_DOCUMENTO = 5 * 1024 * 1024;

/** Mismas reglas que el backend (ServicioDocumentosApoderado): PDF, JPG o PNG de hasta 5 MB. */
export const validarDocumento = (archivo: File): string | null => {
  if (!MIME_PERMITIDOS.includes(archivo.type)) {
    return 'El documento debe ser PDF, JPG o PNG';
  }
  if (archivo.size > TAMANO_MAXIMO_DOCUMENTO) {
    return 'El documento no puede pesar más de 5 MB';
  }
  return null;
};

const siNoForm = (v?: boolean | null): 'SI' | 'NO' => (v ? 'SI' : 'NO');
const textoForm = (v?: string | null) => v ?? '';
/** Número guardado → texto con el formato del campo (peso ###.#, estatura #.##). */
const decimalForm = (v: number | null | undefined, decimales: number) =>
  v === null || v === undefined ? '' : v.toFixed(decimales);

const familiarDesde = (
  d: DatosFamiliar | null,
  tieneCuenta: boolean,
  vacio: FamiliarForm,
): FamiliarForm => {
  if (!d || d.noRegistra) {
    return {
      ...vacio,
      noRegistra: !!d?.noRegistra,
      motivo: textoForm(d?.motivo),
      observacion: textoForm(d?.observacion),
    };
  }
  const relacion = textoForm(d.relacion);
  const conocida = relacion === '' || RELACIONES_APODERADO.includes(relacion);
  return {
    ...vacio,
    personaId: d.personaId ?? null,
    fallecido: !!d.fallecido,
    tieneCuenta,
    persona: d.persona ? personaAFormulario(d.persona) : vacio.persona,
    viveConEstudiante: !!d.viveConEstudiante,
    tienePatriaPotestad: !!d.tienePatriaPotestad,
    relacion: conocida ? relacion : 'Otro',
    relacionDetalle: conocida ? '' : relacion,
  };
};

/**
 * "Editar": convierte la inscripción guardada (GET /estudiantes/{id}/edicion) al formulario del asistente; es la
 * inversa de `aSolicitud`. El archivo del apoderado no se descarga: solo se sube uno nuevo si se adjunta.
 */
export const desdeEdicion = (e: EdicionEstudiante): InscripcionForm => {
  const vacia = inscripcionVacia();
  const d = e.datos;
  const est = d.estudiante;
  const m = d.fichaMedica ?? {};
  const conCuenta = (p: Parentesco) => e.conCuenta.includes(p);
  return {
    ...vacia,
    editando: true,
    estudiante: {
      ...vacia.estudiante,
      persona: est.persona
        ? personaAFormulario(est.persona)
        : vacia.estudiante.persona,
      paisNacimiento: textoForm(est.paisNacimiento),
      departamentoNacimiento: textoForm(est.departamentoNacimiento),
      provinciaNacimiento: textoForm(est.provinciaNacimiento),
      distritoNacimiento: textoForm(est.distritoNacimiento),
      referenciaDomicilio: textoForm(est.referenciaDomicilio),
      tieneInternet:
        est.tieneInternet === null || est.tieneInternet === undefined
          ? ''
          : siNoForm(est.tieneInternet),
      lenguaMaterna: textoForm(est.lenguaMaterna),
      segundaLengua: textoForm(est.segundaLengua),
      autoidentificacionEtnica: textoForm(est.autoidentificacionEtnica),
      tieneDiscapacidad: siNoForm(est.tieneDiscapacidad),
      discapacidadDetalle: textoForm(est.discapacidadDetalle),
      colegioProcedencia: textoForm(est.colegioProcedencia),
      gestionProcedencia: textoForm(est.gestionProcedencia),
      anioAcademico: String(est.anioAcademico),
      nivel: est.gradoCodigo.startsWith('PRI') ? 'PRIMARIA' : 'SECUNDARIA',
      grado: est.gradoCodigo,
      formaIngreso: textoForm(est.formaIngreso),
    },
    fichaMedica: {
      ...vacia.fichaMedica,
      tipoSangre: textoForm(m.tipoSangre),
      pesoKg: decimalForm(m.pesoKg, LIMITES_FICHA.peso.decimales),
      estaturaM: decimalForm(m.estaturaM, LIMITES_FICHA.estatura.decimales),
      tipoSeguro: textoForm(m.tipoSeguro),
      tieneAlergias: siNoForm(m.tieneAlergias),
      alergiasAlimentarias: textoForm(m.alergiasAlimentarias),
      alergiasMedicamentosas: textoForm(m.alergiasMedicamentosas),
      fiebreMaxima: textoForm(m.fiebreMaxima),
      haConvulsionado: siNoForm(m.haConvulsionado),
      tieneEnfermedad: siNoForm(m.tieneEnfermedad),
      enfermedadDetalle: textoForm(m.enfermedadDetalle),
      tomaMedicamento: siNoForm(m.tomaMedicamento),
      medicamentoDetalle: textoForm(m.medicamentoDetalle),
      observaciones: textoForm(m.observaciones),
    },
    padre: familiarDesde(d.padre, conCuenta('PADRE'), vacia.padre),
    madre: familiarDesde(d.madre, conCuenta('MADRE'), vacia.madre),
    incluirApoderado: !!d.apoderado && !d.apoderado.noRegistra,
    apoderado: familiarDesde(
      d.apoderado,
      conCuenta('APODERADO'),
      vacia.apoderado,
    ),
    contactos: d.contactos.map((c) => ({
      nombreCompleto: c.nombreCompleto,
      parentesco: c.parentesco,
      telefonoFijo: textoForm(c.telefonoFijo),
      celular: textoForm(c.celular),
    })),
    modoSalida: d.modoSalida ?? '',
    autorizados: d.autorizados.map((a) => ({ ...a })),
    responsable: d.responsable ?? '',
    correoResponsable: textoForm(d.correoResponsable),
    celularResponsable: textoForm(d.celularResponsable),
    documentoApoderado: {
      tipo: textoForm(e.acreditacion?.tipo),
      archivo: null,
    },
  };
};

/**
 * Aviso de los usuarios creados al registrar o editar (cada familiar presente tiene usuario): "Se creó el usuario
 * de Carla Rojas (U20260010) y se enviaron las credenciales a su correo." Vacío si no se creó ninguno.
 */
export const avisoCuentasNuevas = (cuentas: CuentaFamiliar[] = []) => {
  const nuevas = cuentas
    .filter((c) => c.nueva)
    .map((c) => `${c.nombre} (${c.usuario})`);
  if (nuevas.length === 0) return '';
  const lista =
    nuevas.length === 1
      ? nuevas[0]
      : `${nuevas.slice(0, -1).join(', ')} y ${nuevas[nuevas.length - 1]}`;
  return nuevas.length === 1
    ? `Se creó el usuario de ${lista} y se enviaron las credenciales a su correo.`
    : `Se crearon los usuarios de ${lista}. Las credenciales se enviaron a sus correos.`;
};
