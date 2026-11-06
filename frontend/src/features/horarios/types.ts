// HU-45 · Asignación de horarios (coordinador). Contrato del backend de /horarios/secciones.
// `DiaSemana` de `@/types/api` (LUNES…VIERNES) es el del horario de lectura de la P2 (HU-50/51): aquí los días son
// los del backend de horarios (LUN…SAB).

export type DiaCoordinador = 'LUN' | 'MAR' | 'MIE' | 'JUE' | 'VIE' | 'SAB';

export const NOMBRE_DEL_DIA: Record<DiaCoordinador, string> = {
  LUN: 'Lunes',
  MAR: 'Martes',
  MIE: 'Miércoles',
  JUE: 'Jueves',
  VIE: 'Viernes',
  SAB: 'Sábado',
};

export type Referencia = { id: string; nombre: string };

export type TipoCruce = 'DOCENTE' | 'AMBIENTE';

export type CruceDetectado = {
  tipo: TipoCruce;
  dia: DiaCoordinador;
  numeroBloque: number;
  mensaje: string;
  seccion: string;
  curso: string;
};

export type CeldaDelHorario = {
  dia: DiaCoordinador;
  numeroBloque: number;
  planCursoId: string;
  curso: string;
  docente: Referencia | null;
  ambiente: Referencia;
  cruces: CruceDetectado[];
};

export type BloqueDeJornada = { numero: number; inicio: string; fin: string };

export type RecesoDeJornada = {
  despuesDeBloque: number;
  inicio: string;
  fin: string;
};

export type JornadaDelDia = {
  dias: DiaCoordinador[];
  bloques: BloqueDeJornada[];
  recesos: RecesoDeJornada[];
};

export type SeccionDelHorario = {
  id: string;
  nombre: string;
  gradoNombre: string;
  nivel: 'PRIMARIA' | 'SECUNDARIA';
  anioEscolarId: string;
  ambienteId: string | null;
  tutor: Referencia | null;
};

/** GET y PUT /horarios/secciones/{seccionId}. `version` es null si la sección aún no tiene horario guardado. */
export type HorarioDeSeccion = {
  seccion: SeccionDelHorario;
  version: number | null;
  /** false con el año cerrado: se puede ver, no editar. */
  editable: boolean;
  estructura: { id: string; codigo: string; principal: boolean };
  jornada: JornadaDelDia;
  celdas: CeldaDelHorario[];
};

export type CursoOpcion = {
  planCursoId: string;
  nombre: string;
  horasSemanales: number;
  horasAsignadas: number;
  docente: Referencia | null;
};

export type AmbienteOpcion = {
  id: string;
  nombre: string;
  tipo: 'AULA_REGULAR' | 'ESPACIO_ESPECIAL';
  aforoMaximo: number;
};

/** GET /horarios/secciones/{seccionId}/opciones. */
export type OpcionesDeHorario = {
  ambientePorDefectoId: string | null;
  cursos: CursoOpcion[];
  ambientes: AmbienteOpcion[];
};

/** Lo que viaja en el PUT y en el POST de cruces: sin nombres ni docente (el backend lo resuelve). */
export type CeldaSolicitada = {
  dia: DiaCoordinador;
  numeroBloque: number;
  planCursoId: string;
  ambienteId: string;
};

export type SolicitudHorario = {
  version: number | null;
  celdas: CeldaSolicitada[];
};

// Selectores de año, grado y sección: GET /horarios/anios-escolares y /horarios/anios-escolares/{anioId}/secciones.
// Son los de Horarios porque los de /configuracion son solo del Personal administrativo (el coordinador recibiría 403).

export type AnioParaHorarios = {
  id: string;
  anio: number;
  estado: 'PLANIFICADO' | 'ACTIVO' | 'CERRADO';
};

export type SeccionParaHorarios = {
  id: string;
  nombre: string;
  estado: 'ACTIVA' | 'INACTIVA';
};

export type GradoParaHorarios = {
  codigo: string;
  nombre: string;
  nivel: 'PRIMARIA' | 'SECUNDARIA';
  numero: number;
  cantidadSeccionesActivas: number;
  secciones: SeccionParaHorarios[];
};
