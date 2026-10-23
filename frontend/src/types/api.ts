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
  /** Solo si tiene el rol Apoderado. Pendiente en el backend: mientras no llegue, se muestra vacío. */
  estudiantesACargo?: EstudianteACargo[];
  /** Solo si tiene el rol Tutor: secciones del año escolar actual. Pendiente en el backend. */
  tutorDe?: SeccionTutoria[];
};

/** Estudiante del que la persona es padre, madre o apoderado, con los datos del vínculo de su inscripción. */
export type EstudianteACargo = {
  estudianteId: string;
  nombreCompleto: string;
  documento: string;
  grado: string;
  anioAcademico: number;
  estado: EstadoEstudiante;
  parentesco: Parentesco;
  /** Solo si el parentesco es APODERADO ("Tío(a)", "Otro: …"). */
  relacion: string | null;
  viveConEstudiante: boolean;
  tienePatriaPotestad: boolean;
  esResponsable: boolean;
  /** Documento que acredita al apoderado (carta poder…), si se adjuntó. */
  acreditacion: DocumentoAdjunto | null;
};

export type SeccionTutoria = {
  seccionId: string;
  /** Ej.: "3.° de primaria A". */
  seccion: string;
  cargo: 'TUTOR' | 'COTUTOR';
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
  /** Su cuenta de usuario está eliminada: en "Nuevo usuario" se puede volver a registrar. */
  cuentaEliminada: boolean;
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
    // Figma HU-50/51 los muestra; el contrato del backend aún no existe (Horarios, iteración 2).
    nivel?: 'PRIMARIA' | 'SECUNDARIA';
    tutorCelular?: string;
    tutorCorreo?: string;
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

// ---------- Matrícula · consulta del apoderado (HU-27 / HU-30) ----------
// Iguales a backend/.../matricula/aplicacion/DtosMatricula.java

export type EstadoMatricula =
  | 'SECCION_RESERVADA'
  | 'EN_VALIDACION'
  | 'PENDIENTE_PAGO'
  | 'MATRICULADO'
  | 'RETIRADO'
  | 'ANULADA';

export type HijoApoderado = {
  id: string;
  nombre: string;
  grado: string;
  estadoMatricula: EstadoMatricula | null;
};

export type PeriodoMatricula = {
  anio: number;
  inicio: string;
  cierre: string;
  estado: string;
  abierto: boolean;
};

export type MatriculaActual = {
  id: string;
  estado: EstadoMatricula;
  grado: string;
  seccion: string | null;
  codigo: string | null;
  documentosRequeridos: number;
  documentosEntregados: number;
};

export type ImpedimentoMatricula = {
  tipo: 'PERIODO' | 'SIN_SECCION' | 'DOCUMENTOS' | 'DEUDA';
  mensaje: string;
};

export type CuotaDeudaFamiliar = {
  estudianteId: string;
  estudiante: string;
  concepto: string;
  fechaVencimiento: string;
  monto: number;
};

export type EstadoMatriculaApoderado = {
  estudiante: { id: string; nombre: string; grado: string };
  periodo: PeriodoMatricula;
  matricula: MatriculaActual | null;
  pasoActual: number;
  puedeContinuar: boolean;
  impedimentos: ImpedimentoMatricula[];
  deuda: { total: number; cuotas: CuotaDeudaFamiliar[] };
};

// ---------- Pagos y pensiones (HU-37) ----------
// Iguales a backend/.../pagos/aplicacion/DtosPagos.java

export type EstadoCompromiso = 'VIGENTE' | 'ANULADO';

export type EstadoCuota =
  'PENDIENTE' | 'EN_PROCESO' | 'PAGADA_PARCIAL' | 'PAGADA' | 'ANULADA';

export type CompromisoAnio = {
  id: string;
  anio: number;
  estado: EstadoCompromiso;
};

export type CuotaDetalle = {
  id: string;
  concepto: string;
  numero: number | null;
  nombre: string;
  fechaVencimiento: string;
  montoBase: number;
  montoDescuento: number;
  porcentajeDescuento: number | null;
  montoFinal: number;
  montoPagado: number;
  estado: EstadoCuota;
  vencida: boolean;
  fechaPago: string | null;
};

export type ResumenCompromiso = {
  totalAnual: number;
  cuotasActivas: number;
  capitalPagado: number;
  pensionesPagadas: number;
  pensionesTotal: number;
  descuentoPorcentaje: number | null;
  descuentoNombre: string | null;
  pensionMensual: number;
  saldoPendiente: number;
  cuotasVencidas: number;
};

export type CompromisoDetalle = {
  id: string;
  anio: number;
  estado: EstadoCompromiso;
  estudiante: {
    id: string;
    nombre: string;
    numeroDocumento: string;
    apoderado: string | null;
    seccion: string | null;
  };
  resumen: ResumenCompromiso;
  cuotas: CuotaDetalle[];
};

// ---------- Pagos · Estado de cuenta (HU-37, P2) ----------

export type AlumnoEstadoCuenta = {
  estudianteId: string;
  compromisoId: string;
  nombre: string;
  numeroDocumento: string;
  apoderado: string | null;
  seccion: string | null;
  antiguedad: number;
  estadoCompromiso: EstadoCompromiso;
  totalAnual: number;
  capitalPagado: number;
  deudaVencida: number;
  cuotasVencidas: number;
};

export type EstadoCuentaGeneral = {
  anio: number;
  comprometido: number;
  cobrado: number;
  porcentajeCobrado: number;
  deudaVencida: number;
  cuotasVencidas: number;
  alumnosConDeuda: number;
  alumnos: AlumnoEstadoCuenta[];
};

export type RetiroRegistrado = { cuotasAnuladas: number };

export type CuotaMatriculaDetalle = {
  cuotaId: string;
  compromisoId: string;
  estudianteId: string;
  anio: number;
  concepto: string;
  montoBase: number;
  montoDescuento: number;
  monto: number;
  montoPagado: number;
  fechaVencimiento: string;
  estado: EstadoCuota;
  fechaPago: string | null;
  responsable: string;
};

// ---------- Configuración · Año escolar (HU-74) ----------
// Backend: configuracion/aplicacion/DtosConfiguracion.java. Las fechas llegan como yyyy-mm-dd.

export type Regimen = 'BIMESTRAL' | 'TRIMESTRAL';
export type EstadoPeriodo = 'PLANIFICADO' | 'EN_CURSO' | 'CERRADO';

export type SolicitudAnioEscolar = {
  anio: number;
  fechaInicio: string;
  fechaFin: string;
  regimen: Regimen;
};

export type SolicitudEdicionAnio = {
  fechaInicio: string;
  fechaFin: string;
};

export type SolicitudRegimen = { regimen: Regimen };

export type SolicitudPeriodo = {
  nombre: string;
  fechaInicio: string;
  fechaFin: string;
  fechaLimiteNotas: string;
};

/** Fila del listado CI-01. */
export type AnioEscolarResumen = {
  id: string;
  anio: number;
  fechaInicio: string;
  fechaFin: string;
  regimen: Regimen;
  cantidadPeriodos: number;
  seccionesActivas: number;
  estructurasHorarias: number;
  estado: EstadoAnioEscolar;
};

/** Fila de la tabla de periodos. `estado` se calcula en el backend con las fechas y el estado del año. */
export type PeriodoDetalle = {
  id: string;
  numero: number;
  nombre: string;
  fechaInicio: string;
  fechaFin: string;
  fechaLimiteNotas: string;
  duracionSemanas: number;
  estado: EstadoPeriodo;
  registroNotasAbierto: boolean;
};

export type AnioEscolarDetalle = {
  id: string;
  anio: number;
  fechaInicio: string;
  fechaFin: string;
  regimen: Regimen;
  estado: EstadoAnioEscolar;
  cantidadSecciones: number;
  cantidadEstructuras: number;
  periodos: PeriodoDetalle[];
};

export type PeriodoEnCurso = {
  id: string;
  numero: number;
  nombre: string;
  fechaInicio: string;
  fechaFin: string;
  fechaLimiteNotas: string;
  diasParaLimiteNotas: number;
};

export type PeriodoNotasAbiertas = {
  id: string;
  numero: number;
  nombre: string;
  fechaFin: string;
  fechaLimiteNotas: string;
  diasParaLimiteNotas: number;
};

export type ProximoPeriodo = {
  id: string;
  numero: number;
  nombre: string;
  fechaInicio: string;
};

/** Tarjeta "Año escolar en curso" de CI-01. Los tres periodos pueden ser null. */
export type AnioEscolarActual = {
  id: string;
  anio: number;
  fechaInicio: string;
  fechaFin: string;
  regimen: Regimen;
  periodoEnCurso: PeriodoEnCurso | null;
  periodoNotasAbiertas: PeriodoNotasAbiertas | null;
  proximoPeriodo: ProximoPeriodo | null;
  seccionesActivas: number;
  alumnosMatriculados: number;
  estructurasHorarias: number;
  codigosEstructuras: string[];
  avance: number;
};

export type CodigoRequisito =
  | 'PERIODOS'
  | 'SECCIONES'
  | 'ESTRUCTURA_HORARIA'
  | 'TUTORES'
  | 'PLAN_EVALUACION';

export type Requisito = {
  codigo: CodigoRequisito;
  texto: string;
  cumplido: boolean;
  detalle: string;
};

/** Tarjeta "Antes de activar el año" de CI-06. */
export type RequisitosAnio = {
  puedeActivar: boolean;
  obligatorios: Requisito[];
  recomendados: Requisito[];
};

// ---------- Configuración · Periodo de matrícula (HU-23) ----------
// Backend: configuracion/aplicacion/DtosPeriodoMatricula.java. Las fechas llegan como yyyy-mm-dd, las horas como
// HH:mm:ss y `inicio`/`cierre` del listado como instantes UTC (ISO). `PeriodoMatricula` es el de P4 (apoderado).

/** Lo calcula el backend con su reloj: PROGRAMADO, ABIERTO o CERRADO. */
export type EstadoVisiblePeriodoMatricula =
  'PROGRAMADO' | 'ABIERTO' | 'CERRADO';

/** Fila del listado de la pantalla 13. `vigente`: es el periodo que devuelve /vigente. */
export type FilaPeriodoMatricula = {
  id: string;
  nombre: string;
  anioEscolarId: string;
  anio: number;
  inicio: string;
  cierre: string;
  matriculas: number;
  estado: EstadoVisiblePeriodoMatricula;
  vigente: boolean;
};

/** Tarjeta "Vigente": el periodo ABIERTO ahora o, si no hay, el próximo PROGRAMADO. */
export type PeriodoMatriculaVigente = {
  id: string;
  anioEscolarId: string;
  anio: number;
  nombre: string;
  inicio: string;
  cierre: string;
  estado: EstadoVisiblePeriodoMatricula;
  bloquearPorMorosidad: boolean;
  permitirExtemporanea: boolean;
  vacantesTotales: number;
  matriculasRegistradas: number;
};

/** Años PLANIFICADO o ACTIVO que aún no tienen periodo de matrícula. */
export type AnioDisponibleMatricula = {
  id: string;
  anio: number;
  estado: EstadoAnioEscolar;
};

/** Fila de "Vacantes por grado y sección". `tutor` es null si la sección no lo tiene. */
export type SeccionVacante = {
  seccionId: string;
  etiqueta: string;
  gradoCodigo: string;
  gradoNombre: string;
  tutor: string | null;
  capacidad: number;
  alumnosOcupanVacante: number;
};

export type DocumentoRequeridoVista = {
  id: string;
  nombre: string;
  descripcion: string | null;
  orden: number;
  tienePlantilla: boolean;
  plantillaNombreArchivo: string | null;
  tieneEntregas: boolean;
};

/**
 * El formulario de la pantalla 13: vacío (GET /nuevo), para editar o en solo lectura (GET /{id}). `id`, `estado`,
 * las fechas, las horas y `diasDelPeriodo` son null mientras el periodo no existe. `montoMatricula` es null si
 * `tarifarioConfirmado` es false.
 */
export type FormularioPeriodoMatricula = {
  id: string | null;
  anioEscolarId: string;
  anio: number;
  titulo: string;
  estado: EstadoVisiblePeriodoMatricula | null;
  editable: boolean;
  eliminable: boolean;
  fechaInicio: string | null;
  horaInicio: string | null;
  fechaCierre: string | null;
  horaCierre: string | null;
  diasDelPeriodo: number | null;
  montoMatricula: number | null;
  tarifarioConfirmado: boolean;
  vacantesTotales: number;
  matriculasRegistradas: number;
  cantidadSecciones: number;
  resumenSecciones: string;
  bloquearPorMorosidad: boolean;
  permitirExtemporanea: boolean;
  secciones: SeccionVacante[];
  documentos: DocumentoRequeridoVista[];
};

export type VacanteSolicitud = { seccionId: string; capacidad: number };

/** `id` va solo en un documento que ya existe (al editar); uno nuevo no lo lleva. */
export type DocumentoSolicitud = {
  id?: string | null;
  nombre: string;
  descripcion?: string | null;
};

/** "Guardar configuración" (POST) y editar (PUT). Fechas y horas son de Lima: "2026-12-01" y "10:00". */
export type SolicitudPeriodoMatricula = {
  /** Solo al crear: un periodo no cambia de año. */
  anioEscolarId?: string | null;
  fechaInicio: string;
  horaInicio: string;
  fechaCierre: string;
  horaCierre: string;
  vacantes: VacanteSolicitud[];
  documentos: DocumentoSolicitud[];
};

// ---------- Configuración · Tarifario y calendario (HU-34) ----------
// Backend: configuracion/aplicacion/DtosConfiguracion.java (`TarifarioVista`, `SolicitudTarifario`). Los montos y los
// porcentajes llegan como número; las fechas, como yyyy-mm-dd.

/** EN_CONFIGURACION: el año aún no tiene tarifario confirmado (o es una propuesta); CONFIRMADO: Pagos ya lo puede usar. */
export type EstadoTarifario = 'EN_CONFIGURACION' | 'CONFIRMADO';

/** Tabla "Conceptos y montos". Los montos son null en una propuesta sin tarifario del año anterior. */
export type ConceptosTarifario = {
  montoCuotaIngreso: number | null;
  montoMatricula: number | null;
  vencimientoMatricula: string;
  montoPension: number | null;
  cantidadCuotas: number;
};

/** "Política de morosidad". */
export type MorosidadTarifario = {
  diasGracia: number | null;
  recargoPorcentaje: number | null;
  topeRecargoPorcentaje: number | null;
};

export type CuotaTarifarioVista = {
  numero: number;
  nombre: string;
  fechaVencimiento: string;
  monto: number | null;
};

/** "Calendario de cuotas"; `totalPensiones` es null mientras no hay monto de pensión. */
export type CalendarioTarifario = {
  cuotas: CuotaTarifarioVista[];
  totalPensiones: number | null;
};

/**
 * P19 de un año. Con `existe = false` es una propuesta que el backend no guardó (valores del año anterior o vacíos y
 * el calendario generado con las fechas del año). `soloLectura` (año CERRADO, o tarifario CONFIRMADO con compromisos
 * de pago) y su `motivoSoloLectura` los calcula el backend. `calendarioDesactualizado`: las fechas del año cambiaron
 * después de confirmar y el calendario guardado ya no coincide con ellas.
 */
export type TarifarioVista = {
  anioEscolarId: string;
  anio: number;
  existe: boolean;
  estado: EstadoTarifario;
  soloLectura: boolean;
  motivoSoloLectura: string | null;
  calendarioDesactualizado: boolean;
  conceptos: ConceptosTarifario;
  morosidad: MorosidadTarifario;
  calendario: CalendarioTarifario;
};

/** "Confirmar tarifario". La cuota de ingreso es opcional; el calendario y el vencimiento los genera el backend. */
export type SolicitudTarifario = {
  montoCuotaIngreso: number | null;
  montoMatricula: number;
  montoPension: number;
  diasGracia: number;
  recargoPorcentaje: number;
  topeRecargoPorcentaje: number;
};
