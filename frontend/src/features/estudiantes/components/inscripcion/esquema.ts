import { z } from 'zod';

import {
  PersonaFormValues,
  personaSchema,
  personaVacia,
} from '@/components/personas/persona-schema';
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
  persona: { ...personaVacia(), sexo },
  viveConEstudiante: true,
  tienePatriaPotestad: true,
  relacion: '',
  relacionDetalle: '',
});

const anioSiguiente = new Date().getFullYear() + 1;

export const inscripcionVacia = (): InscripcionForm => ({
  estudiante: {
    persona: personaVacia(),
    paisNacimiento: 'Perú',
    departamentoNacimiento: 'Lima',
    provinciaNacimiento: 'Lima',
    distritoNacimiento: '',
    referenciaDomicilio: '',
    tieneInternet: '',
    lenguaMaterna: 'Castellano',
    segundaLengua: '',
    autoidentificacionEtnica: '',
    tieneDiscapacidad: 'NO',
    discapacidadDetalle: '',
    colegioProcedencia: '',
    gestionProcedencia: '',
    anioAcademico: String(anioSiguiente),
    nivel: '',
    grado: '',
    formaIngreso: 'Edad normativa',
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
    const r = personaSchema.safeParse(f.persona);
    if (!r.success) {
      r.error.issues.forEach((i) =>
        ctx.addIssue({ ...i, path: ['persona', ...i.path] }),
      );
    }
  });

const estudianteSchema = z
  .object({
    persona: personaSchema,
    anioAcademico: z.string().min(1, 'Indica el año académico'),
    nivel: z.string().min(1, 'Indica el nivel'),
    grado: z.string().min(1, 'Indica el grado'),
  })
  .passthrough();

export const inscripcionSchema = z
  .object({
    estudiante: estudianteSchema,
    fichaMedica: z.object({}).passthrough(),
    padre: familiarSchema(false),
    madre: familiarSchema(false),
    incluirApoderado: z.boolean(),
    apoderado: z.custom<FamiliarForm>(),
    contactos: z
      .array(
        z.object({
          nombreCompleto: z.string().trim().min(1, 'Ingresa el nombre'),
          parentesco: z.string().trim().min(1, 'Ingresa el parentesco'),
          telefonoFijo: z.string(),
          celular: z.string(),
        }),
      )
      .min(2, 'Registra al menos 2 contactos de emergencia'),
    modoSalida: z.string(),
    autorizados: z.array(
      z.object({ nombreCompleto: z.string(), relacion: z.string() }),
    ),
    responsable: z.string(),
    correoResponsable: z.string(),
    celularResponsable: z.string(),
    verificado: z.boolean(),
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
  ['padre', 'madre', 'apoderado', 'incluirApoderado'],
  ['contactos'],
  [],
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
      pesoKg: m.pesoKg ? Number(m.pesoKg) : null,
      estaturaM: m.estaturaM ? Number(m.estaturaM) : null,
      tipoSeguro: texto(m.tipoSeguro),
      tieneAlergias: m.tieneAlergias === 'SI',
      alergiasAlimentarias: texto(m.alergiasAlimentarias),
      alergiasMedicamentosas: texto(m.alergiasMedicamentosas),
      fiebreMaxima: texto(m.fiebreMaxima),
      haConvulsionado: m.haConvulsionado === 'SI',
      tieneEnfermedad: m.tieneEnfermedad === 'SI',
      enfermedadDetalle: texto(m.enfermedadDetalle),
      tomaMedicamento: m.tomaMedicamento === 'SI',
      medicamentoDetalle: texto(m.medicamentoDetalle),
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
    autorizados: f.autorizados.filter((a) => a.nombreCompleto.trim()),
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
