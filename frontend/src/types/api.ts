// Tipos de los contratos con el backend.
// Lo ideal es que estos tipos se generen (o se mantengan sincronizados) con el backend.
// Cuando el back defina un DTO, actualízalo aquí y TypeScript te marcará qué romper.

import { ROLES } from '@/config/roles';

export type BaseEntity = {
  id: string;
  createdAt: number;
};

export type Entity<T> = {
  [K in keyof T]: T[K];
} & BaseEntity;

export type Meta = {
  page: number;
  total: number;
  totalPages: number;
};

// ---------- Auth / Usuarios ----------

export type User = Entity<{
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  roles: ROLES[];
}>;

export type AuthResponse = {
  jwt: string;
  user: User;
};

// Respuesta paginada del backend (compartido/web/Pagina.java). page empieza en 1.
export type Pagina<T> = {
  data: T[];
  meta: { page: number; size: number; total: number; totalPages: number };
};

// ---------- Personas (backend: personas/aplicacion/DatosPersona.java) ----------

export type TipoDocumento = 'DNI' | 'CARNET_EXTRANJERIA';
export type Sexo = 'MASCULINO' | 'FEMENINO';

export type DatosPersona = {
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  apellidoPaterno: string;
  apellidoMaterno?: string | null;
  nombres: string;
  fechaNacimiento?: string | null; // yyyy-mm-dd
  nacionalidad?: string | null;
  lugarNacimiento?: string | null;
  sexo?: Sexo | null;
  estadoCivil?: string | null;
  gradoInstruccion?: string | null;
  celular?: string | null;
  correo?: string | null;
  telefonoCasa?: string | null;
  profesion?: string | null;
  centroTrabajo?: string | null;
  cargo?: string | null;
  distrito?: string | null;
  direccion?: string | null;
  esExalumno?: boolean | null;
};

// ---------- Usuarios (HU-6, HU-9, HU-12) · backend: usuarios/aplicacion/Dtos.java ----------

export type EstadoUsuario = 'ACTIVO' | 'INACTIVO';

export type UsuarioResumen = {
  id: string;
  username: string;
  nombre: string;
  nombreCompleto: string;
  correo: string | null;
  roles: ROLES[];
  estado: EstadoUsuario;
  ultimoAcceso: string | null;
};

export type UsuarioDetalle = {
  id: string;
  username: string;
  estado: EstadoUsuario;
  creadoEn: string;
  ultimoAcceso: string | null;
  roles: ROLES[];
  personaId: string;
  persona: DatosPersona;
};

export type SolicitudUsuario = {
  persona: DatosPersona;
  roles: ROLES[];
};

export type UsuarioCreado = {
  id: string;
  username: string;
  correo: string;
  estado: EstadoUsuario;
};

export type RolCatalogo = { codigo: ROLES; nombre: string };

// Carga masiva (HU-8). La fila es la de Excel (el encabezado es la fila 1).
export type ErrorFila = { fila: number; mensaje: string };

export type ResultadoCargaMasiva = {
  nombreArchivo: string;
  total: number;
  validos: number;
  /** 0 en la validación (paso 2); usuarios creados en la carga (paso 3). */
  creados: number;
  errores: ErrorFila[];
};

export type ResumenUsuarios = {
  total: number;
  activos: number;
  inactivos: number;
  ultimoAcceso: { nombre: string; fecha: string } | null;
  porRol: {
    rol: ROLES;
    nombre: string;
    cantidad: number;
    usuarios: string[];
  }[];
  cuentasInactivas: UsuarioResumen[];
};

// ---------- Estudiantes (HU-7) · backend: estudiantes/aplicacion/DtosEstudiantes.java ----------

/** Situación del estudiante: inscrito, matriculado en su año académico o retirado. */
export type EstadoEstudiante = 'INSCRITO' | 'MATRICULADO' | 'RETIRADO';
export type Nivel = 'PRIMARIA' | 'SECUNDARIA';
export type ModoSalida = 'PERSONA_AUTORIZADA' | 'SOLO' | 'MOVILIDAD';
export type Parentesco = 'PADRE' | 'MADRE' | 'APODERADO';

export type EstudianteResumen = {
  id: string;
  nombreCompleto: string;
  documento: string;
  grado: string;
  anioAcademico: number;
  estado: EstadoEstudiante;
  responsable: string;
};

export type DatosEstudiante = {
  persona: DatosPersona | null;
  paisNacimiento?: string | null;
  departamentoNacimiento?: string | null;
  provinciaNacimiento?: string | null;
  distritoNacimiento?: string | null;
  referenciaDomicilio?: string | null;
  tieneInternet?: boolean | null;
  lenguaMaterna?: string | null;
  segundaLengua?: string | null;
  autoidentificacionEtnica?: string | null;
  tieneDiscapacidad?: boolean | null;
  discapacidadDetalle?: string | null;
  colegioProcedencia?: string | null;
  gestionProcedencia?: string | null;
  anioAcademico: number;
  /** Código del catálogo de grados: PRI-1…PRI-6, SEC-1…SEC-5. */
  gradoCodigo: string;
  formaIngreso?: string | null;
};

export type DatosFichaMedica = {
  tipoSangre?: string | null;
  pesoKg?: number | null;
  estaturaM?: number | null;
  tipoSeguro?: string | null;
  tieneAlergias?: boolean | null;
  alergiasAlimentarias?: string | null;
  alergiasMedicamentosas?: string | null;
  fiebreMaxima?: string | null;
  haConvulsionado?: boolean | null;
  tieneEnfermedad?: boolean | null;
  enfermedadDetalle?: string | null;
  tomaMedicamento?: boolean | null;
  medicamentoDetalle?: string | null;
  observaciones?: string | null;
};

export type DatosFamiliar = {
  noRegistra?: boolean;
  motivo?: string | null;
  observacion?: string | null;
  personaId?: string | null;
  persona?: DatosPersona | null;
  viveConEstudiante?: boolean;
  tienePatriaPotestad?: boolean;
  relacion?: string | null;
};

export type DatosContacto = {
  nombreCompleto: string;
  parentesco: string;
  telefonoFijo?: string | null;
  celular?: string | null;
};

export type DatosAutorizado = { nombreCompleto: string; relacion: string };

export type SolicitudInscripcion = {
  estudiante: DatosEstudiante;
  fichaMedica: DatosFichaMedica;
  padre: DatosFamiliar;
  madre: DatosFamiliar;
  apoderado: DatosFamiliar | null;
  contactos: DatosContacto[];
  modoSalida: ModoSalida | null;
  autorizados: DatosAutorizado[];
  responsable: Parentesco;
  correoResponsable?: string | null;
  celularResponsable?: string | null;
};

export type InscripcionRegistrada = {
  id: string;
  estudiante: string;
  responsable: string;
  usuarioResponsable: string;
  cuentaNueva: boolean;
};

export type DocumentoAdjunto = {
  tipo: string;
  nombreArchivo: string;
  subidoEn: string;
};

export type PersonaEncontrada = {
  personaId: string;
  persona: DatosPersona;
  tieneCuenta: boolean;
  estudiantesACargo: string[];
};

export type FichaEstudiante = {
  id: string;
  estado: EstadoEstudiante;
  grado: string;
  anioAcademico: number;
  persona: DatosPersona;
  datos: DatosEstudiante;
  fichaMedica: DatosFichaMedica | null;
  familia: {
    personaId: string | null;
    parentesco: Parentesco;
    nombreCompleto: string | null;
    documento: string | null;
    celular: string | null;
    relacion: string | null;
    viveConEstudiante: boolean;
    esResponsable: boolean;
    motivoNoRegistra: string | null;
    /** Documento que acredita al apoderado (carta poder…), si se adjuntó. */
    acreditacion: DocumentoAdjunto | null;
  }[];
  contactos: DatosContacto[];
  modoSalida: ModoSalida | null;
  autorizados: DatosAutorizado[];
  hermanos: EstudianteResumen[];
};

// ---------- Horarios (HU-44 a HU-51) ----------

export type DiaSemana = 'LUNES' | 'MARTES' | 'MIERCOLES' | 'JUEVES' | 'VIERNES';

export type BloqueHorario = {
  id: string;
  nombre: string; // "Bloque 1", "Recreo 1"
  tipo: 'CLASE' | 'RECREO';
  horaInicio: string; // "08:00"
  horaFin: string; // "08:45"
  orden: number;
};

export type CursoSesion = {
  curso: string;
  docente: string;
  aula: string;
};

export type SesionClase = {
  dia: DiaSemana;
  bloqueId: string;
  tipo: 'ASIGNADO' | 'SUBGRUPOS' | 'SIN_ASIGNAR';
  cursos: CursoSesion[];
};

export type HorarioSeccion = {
  anioAcademico: number;
  seccion: {
    id: string;
    grado: number;
    letra: string;
    tutor: string;
  };
  estudiante?: {
    id: string;
    nombreCompleto: string;
  };
  bloques: BloqueHorario[];
  sesiones: SesionClase[];
};

// ---------- Catálogo académico · Plan curricular (HU-52, HU-53, HU-54) ----------
// Iguales a backend/.../academico/aplicacion/DtosAcademico.java

export type EstadoArea = 'ACTIVA' | 'INACTIVA';
export type EstadoCurso = 'ACTIVO' | 'INACTIVO';

export type ResumenPlanCurricular = {
  areasActivas: number;
  areasRegistradas: number;
  competenciasActivas: number;
  competenciasRegistradas: number;
  cursosActivos: number;
  cursosRegistrados: number;
  anioActivo: number | null;
};

export type AreaResumen = {
  id: string;
  nombre: string;
  descripcion: string | null;
  estado: EstadoArea;
  competencias: number;
  cursos: number;
};

export type CompetenciaResumen = {
  id: string;
  areaId: string;
  nombre: string;
  descripcion: string | null;
  orden: number;
  estado: EstadoArea;
};

export type CursoResumen = {
  id: string;
  areaId: string;
  nombre: string;
  estado: EstadoCurso;
  /** Códigos del catálogo de grados en orden: PRI-1…PRI-6, SEC-1…SEC-5. */
  grados: string[];
};

export type AreaDetalle = {
  id: string;
  nombre: string;
  descripcion: string | null;
  estado: EstadoArea;
  competencias: CompetenciaResumen[];
  cursos: CursoResumen[];
};

export type SolicitudArea = { nombre: string; descripcion: string | null };
export type SolicitudCompetencia = {
  nombre: string;
  descripcion: string | null;
};
export type SolicitudCurso = { nombre: string; grados: string[] };

// ---------- Catálogo académico · Escala de calificación (HU-55) ----------

export type NivelLogro = 'AD' | 'A' | 'B' | 'C';
/** Se calcula con la fecha de vigencia: una versión con fecha futura queda programada. */
export type EstadoVersion = 'PROGRAMADA' | 'VIGENTE' | 'ANTERIOR';

export type RangoNivelDetalle = {
  nivel: NivelLogro;
  significado: string;
  notaDesde: number;
  notaHasta: number;
};

export type TablaConversionDetalle = {
  id: string;
  /** "2025-03-01" */
  vigenteDesde: string;
  estado: EstadoVersion;
  /** De mayor a menor: AD, A, B, C. */
  niveles: RangoNivelDetalle[];
};

export type SolicitudTablaConversion = {
  vigenteDesde: string;
  niveles: { nivel: NivelLogro; notaDesde: number; notaHasta: number }[];
};

// ---------- Catálogo académico · Plan de evaluación (HU-56) ----------

export type Escala = 'VIGESIMAL' | 'CUALITATIVA';
export type ReglaNota =
  | 'PROMEDIO_SIMPLE'
  | 'PROMEDIO_PONDERADO'
  | 'NIVEL_MAS_FRECUENTE'
  | 'ULTIMO_NIVEL';
export type Consolidacion = 'PROMEDIO_PERIODOS' | 'ULTIMO_PERIODO';
export type EstadoAnioEscolar = 'PLANIFICADO' | 'ACTIVO' | 'CERRADO';

export type AnioEscolarOpcion = {
  id: string;
  anio: number;
  estado: EstadoAnioEscolar;
};

export type DocenteOpcion = { id: string; nombre: string };

export type SeccionPlan = { id: string; nombre: string; tutor: string | null };

export type PlanCursoResumen = {
  id: string;
  cursoId: string;
  curso: string;
  area: string;
  horasSemanales: number;
  competencias: number;
  /** null = sin configurar; MIXTA si sus competencias usan las dos escalas. */
  escala: Escala | 'MIXTA' | null;
  docentesAsignados: number;
};

export type PlanGrado = {
  anioEscolarId: string;
  anio: number;
  estadoAnio: EstadoAnioEscolar;
  gradoCodigo: string;
  grado: string;
  /** Bloques semanales de la estructura horaria; null si el año aún no la tiene. */
  bloquesDisponibles: number | null;
  horasAsignadas: number;
  competenciasEvaluadas: number;
  cursosConfigurados: number;
  docentesAsignados: number;
  docentesRequeridos: number;
  secciones: SeccionPlan[];
  cursos: PlanCursoResumen[];
};

export type CompetenciaPlan = {
  competenciaId: string;
  nombre: string;
  estado: EstadoArea;
  seleccionada: boolean;
  escala: Escala;
  reglaNota: ReglaNota;
  consolidacion: Consolidacion;
};

export type PlanCursoDetalle = {
  id: string;
  cursoId: string;
  curso: string;
  area: string;
  anio: number;
  grado: string;
  horasSemanales: number;
  actualizadoEn: string;
  competencias: CompetenciaPlan[];
};

export type CursoDisponible = {
  id: string;
  nombre: string;
  area: string;
  competencias: CompetenciaResumen[];
};

export type OrigenPlan = {
  anioEscolarId: string;
  anio: number;
  gradoCodigo: string;
  grado: string;
  cursos: number;
  horas: number;
  competencias: number;
};

export type SolicitudCompetenciaPlan = {
  competenciaId: string;
  escala: Escala;
  reglaNota: ReglaNota;
  consolidacion: Consolidacion;
};

export type SolicitudPlanCurso = {
  anioEscolarId: string;
  gradoCodigo: string;
  cursoId: string;
  horasSemanales: number;
  competencias: SolicitudCompetenciaPlan[];
};

export type SolicitudConfigurarCurso = {
  horasSemanales: number;
  competencias: SolicitudCompetenciaPlan[];
};

export type DocenteSeccion = {
  seccionId: string;
  seccion: string;
  tutor: string | null;
  docenteId: string | null;
};

export type SolicitudCopiaPlan = {
  anioEscolarId: string;
  gradoCodigo: string;
  origenAnioEscolarId: string;
  origenGradoCodigo: string;
};
