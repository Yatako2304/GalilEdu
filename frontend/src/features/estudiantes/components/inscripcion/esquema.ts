import { z } from 'zod';

import {
  PersonaFormValues,
  personaSchema,
  personaUsuarioSchema,
  personaVacia,
  textoSoloLetras,
} from '@/components/personas/persona-schema';
import { PAIS_CON_UBIGEO } from '@/config/ubigeo';
import {
  DatosPersona,
  ModoSalida,
  Parentesco,
  SolicitudInscripcion,
} from '@/types/api';

// Valores del formulario de inscripción (los 5 pasos comparten un solo formulario).

export type FamiliarForm = {
  noRegistra: boolean;
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
};

const familiarVacio = (sexo: 'MASCULINO' | 'FEMENINO' | ''): FamiliarForm => ({
  noRegistra: false,
  motivo: '',
  observacion: '',
  personaId: null,
  tieneCuenta: false,
  aCargo: [],
  // Como en el Paso 1, el tipo de documento no trae valor por defecto.
  persona: {
    ...personaVacia(),
    tipoDocumento: '' as unknown as PersonaFormValues['tipoDocumento'],
    sexo,
  },
  viveConEstudiante: true,
  tienePatriaPotestad: true,
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
  apoderado: {
    ...familiarVacio(''),
    viveConEstudiante: false,
    tienePatriaPotestad: false,
  },
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
});

/** Un familiar cuenta como registrado si no está marcado "no registra" y tiene documento. */
export const registra = (f: FamiliarForm) =>
  !f.noRegistra && (!!f.personaId || !!f.persona.numeroDocumento);

const familiarSchema = (opcional: boolean) =>
  z.custom<FamiliarForm>().superRefine((f, ctx) => {
    if (f.noRegistra) {
      if (!opcional && !f.motivo) {
        ctx.addIssue({
          code: 'custom',
          path: ['motivo'],
          message: 'Indica el motivo',
        });
      }
      return;
    }
    if (f.personaId) return;
    // Persona nueva: mismos obligatorios que un usuario (CAHU6-2), porque el responsable tendrá cuenta.
    const r = personaUsuarioSchema.safeParse(f.persona);
    if (!r.success) {
      r.error.issues.forEach((i) =>
        ctx.addIssue({ ...i, path: ['persona', ...i.path] }),
      );
    }
  });

const vacio = (v: unknown) => typeof v !== 'string' || v.trim() === '';

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
    padre: familiarSchema(false),
    madre: familiarSchema(false),
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
    if (f.incluirApoderado) {
      const r = familiarSchema(true).safeParse(f.apoderado);
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
    } else if (!registra(f.padre) && !registra(f.madre)) {
      ctx.addIssue({
        code: 'custom',
        path: ['incluirApoderado'],
        message:
          'No hay padre ni madre registrados: debes registrar un apoderado.',
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

const familiar = (f: FamiliarForm) =>
  !registra(f)
    ? {
        noRegistra: true,
        motivo: texto(f.motivo),
        observacion: texto(f.observacion),
      }
    : {
        noRegistra: false,
        personaId: f.personaId,
        persona: f.personaId ? null : persona(f.persona),
        viveConEstudiante: f.viveConEstudiante,
        tienePatriaPotestad: f.tienePatriaPotestad,
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
    padre: familiar(f.padre),
    madre: familiar(f.madre),
    apoderado: f.incluirApoderado ? familiar(f.apoderado) : null,
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
