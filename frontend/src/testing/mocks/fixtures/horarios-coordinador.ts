import type {
  AmbienteOpcion,
  AnioParaHorarios,
  CeldaDelHorario,
  CruceDetectado,
  GradoParaHorarios,
  HorarioDeSeccion,
  JornadaDelDia,
  OpcionesDeHorario,
  SeccionParaHorarios,
} from '@/features/horarios/types';

// HU-45 (coordinador): datos de los endpoints de /horarios/secciones y de los selectores de /horarios/anios-escolares.

export const ANIO_2026 = 'anio-2026';

const anio = (
  id: string,
  numero: number,
  estado: AnioParaHorarios['estado'],
): AnioParaHorarios => ({ id, anio: numero, estado });

// Del más reciente al más antiguo, como GET /horarios/anios-escolares.
export const aniosCoordinador: AnioParaHorarios[] = [
  anio('anio-2027', 2027, 'PLANIFICADO'),
  anio(ANIO_2026, 2026, 'ACTIVO'),
];

const seccion = (id: string, nombre: string): SeccionParaHorarios => ({
  id,
  nombre,
  estado: 'ACTIVA',
});

// Secciones de la prueba: 5.° A tiene horario guardado, 5.° B no tiene horario ni tutor ni aula, 5.° C tiene un cruce,
// 5.° D es de un año cerrado y 4.° A es de un nivel sin estructura principal.
export const SECCION_5A = 'sec-5a';
export const SECCION_5B = 'sec-5b';
export const SECCION_5C = 'sec-5c';
export const SECCION_5D = 'sec-5d';
export const SECCION_4A = 'sec-4a';
export const SECCION_INEXISTENTE = 'sec-no-existe';

export const gradosCoordinador: GradoParaHorarios[] = [
  {
    codigo: 'SEC-4',
    nombre: '4.° de secundaria',
    nivel: 'SECUNDARIA',
    numero: 4,
    cantidadSeccionesActivas: 1,
    secciones: [seccion(SECCION_4A, 'A')],
  },
  {
    codigo: 'SEC-5',
    nombre: '5.° de secundaria',
    nivel: 'SECUNDARIA',
    numero: 5,
    cantidadSeccionesActivas: 4,
    secciones: [
      seccion(SECCION_5A, 'A'),
      seccion(SECCION_5B, 'B'),
      seccion(SECCION_5C, 'C'),
      seccion(SECCION_5D, 'D'),
    ],
  },
  {
    codigo: 'PRI-1',
    nombre: '1.° de primaria',
    nivel: 'PRIMARIA',
    numero: 1,
    cantidadSeccionesActivas: 0,
    secciones: [],
  },
];

export const jornadaCoordinador: JornadaDelDia = {
  dias: ['LUN', 'MAR', 'MIE', 'JUE', 'VIE'],
  bloques: [
    { numero: 1, inicio: '08:00', fin: '08:45' },
    { numero: 2, inicio: '08:45', fin: '09:30' },
    { numero: 3, inicio: '09:30', fin: '10:15' },
    { numero: 4, inicio: '10:30', fin: '11:15' },
  ],
  recesos: [{ despuesDeBloque: 3, inicio: '10:15', fin: '10:30' }],
};

export const PLAN_MATEMATICA = 'plan-matematica';
export const PLAN_INGLES = 'plan-ingles';
export const PLAN_GEOMETRIA = 'plan-geometria';
export const PLAN_ARTE = 'plan-arte';
export const PLAN_FISICA = 'plan-fisica';

export const AMBIENTE_A502 = 'amb-a502';
export const AMBIENTE_A401 = 'amb-a401';
export const AMBIENTE_LABORATORIO = 'amb-laboratorio';

export const ambientesCoordinador: AmbienteOpcion[] = [
  { id: AMBIENTE_A502, nombre: 'A-502', tipo: 'AULA_REGULAR', aforoMaximo: 30 },
  { id: AMBIENTE_A401, nombre: 'A-401', tipo: 'AULA_REGULAR', aforoMaximo: 32 },
  {
    id: AMBIENTE_LABORATORIO,
    nombre: 'Laboratorio de ciencias',
    tipo: 'ESPACIO_ESPECIAL',
    aforoMaximo: 24,
  },
];

const PEDRO = { id: 'doc-pedro', nombre: 'Pedro García Ramos' };
const ALESSANDRA = {
  id: 'doc-alessandra',
  nombre: 'Alessandra Huertas Villarreal',
};
const LUCIA = { id: 'doc-lucia', nombre: 'Lucía Ayala Prado' };

/** Los cursos del plan de 5.° con sus horas. 1 bloque = 1 hora semanal. */
export const opcionesCoordinador = (
  seccionId: string,
  asignadas: Record<string, number> = {},
): OpcionesDeHorario => ({
  ambientePorDefectoId: seccionId === SECCION_5B ? null : AMBIENTE_A502,
  cursos: [
    {
      planCursoId: PLAN_MATEMATICA,
      nombre: 'Matemática',
      horasSemanales: 3,
      horasAsignadas: asignadas[PLAN_MATEMATICA] ?? 0,
      docente: PEDRO,
    },
    {
      planCursoId: PLAN_INGLES,
      nombre: 'Inglés',
      horasSemanales: 2,
      horasAsignadas: asignadas[PLAN_INGLES] ?? 0,
      docente: ALESSANDRA,
    },
    {
      planCursoId: PLAN_GEOMETRIA,
      nombre: 'Geometría',
      horasSemanales: 2,
      horasAsignadas: asignadas[PLAN_GEOMETRIA] ?? 0,
      docente: null,
    },
    {
      planCursoId: PLAN_ARTE,
      nombre: 'Arte y Cultura',
      horasSemanales: 1,
      horasAsignadas: asignadas[PLAN_ARTE] ?? 1,
      docente: LUCIA,
    },
    {
      planCursoId: PLAN_FISICA,
      nombre: 'Física',
      horasSemanales: 2,
      horasAsignadas: asignadas[PLAN_FISICA] ?? 0,
      docente: PEDRO,
    },
  ],
  ambientes: ambientesCoordinador,
});

const celda = (
  dia: CeldaDelHorario['dia'],
  numeroBloque: number,
  planCursoId: string,
  curso: string,
  docente: CeldaDelHorario['docente'],
  ambienteId = AMBIENTE_A502,
  cruces: CruceDetectado[] = [],
): CeldaDelHorario => ({
  dia,
  numeroBloque,
  planCursoId,
  curso,
  docente,
  ambiente: {
    id: ambienteId,
    nombre:
      ambientesCoordinador.find((a) => a.id === ambienteId)?.nombre ??
      ambienteId,
  },
  cruces,
});

export const CRUCE_DE_DOCENTE: CruceDetectado = {
  tipo: 'DOCENTE',
  dia: 'MAR',
  numeroBloque: 2,
  mensaje:
    'Alessandra Huertas Villarreal ya dicta Inglés con 5.° B el martes de 8:45 a 9:30.',
  seccion: '5.° B',
  curso: 'Inglés',
};

export const CRUCE_DE_AMBIENTE_NUEVO =
  '5.° B ya usa Laboratorio de ciencias para Física el lunes de 8:00 a 8:45.';

const base = (
  id: string,
  nombre: string,
  extra: Partial<HorarioDeSeccion> = {},
): HorarioDeSeccion => ({
  seccion: {
    id,
    nombre,
    gradoNombre: '5.° de secundaria',
    nivel: 'SECUNDARIA',
    anioEscolarId: ANIO_2026,
    ambienteId: AMBIENTE_A502,
    tutor: null,
  },
  version: null,
  editable: true,
  estructura: { id: 'estructura-sec', codigo: 'EH-SEC-26', principal: true },
  jornada: jornadaCoordinador,
  celdas: [],
  ...extra,
});

/** El horario guardado de cada sección de la prueba. */
export const horarioDe = (seccionId: string): HorarioDeSeccion | null => {
  switch (seccionId) {
    case SECCION_5A:
      return base(SECCION_5A, 'A', {
        version: 3,
        seccion: {
          ...base(SECCION_5A, 'A').seccion,
          tutor: { id: 'tutor-sec-5a', nombre: 'Lucía Ayala Prado' },
        },
        celdas: [
          celda('LUN', 1, PLAN_MATEMATICA, 'Matemática', PEDRO),
          celda('MAR', 2, PLAN_INGLES, 'Inglés', ALESSANDRA),
          // Su docente ya no está asignado en el plan.
          celda('MIE', 3, PLAN_GEOMETRIA, 'Geometría', null),
          celda('JUE', 1, PLAN_ARTE, 'Arte y Cultura', LUCIA),
        ],
      });
    case SECCION_5B:
      // Sin horario guardado: la jornada de la estructura principal, sin celdas y sin versión. Sin aula ni tutor.
      return base(SECCION_5B, 'B', {
        seccion: { ...base(SECCION_5B, 'B').seccion, ambienteId: null },
      });
    case SECCION_5C:
      return base(SECCION_5C, 'C', {
        version: 1,
        seccion: {
          ...base(SECCION_5C, 'C').seccion,
          tutor: { id: 'tutor-sec-5c', nombre: 'Pedro García Ramos' },
        },
        celdas: [
          celda('MAR', 2, PLAN_INGLES, 'Inglés', ALESSANDRA, AMBIENTE_A502, [
            CRUCE_DE_DOCENTE,
          ]),
        ],
      });
    case SECCION_5D:
      // Año cerrado: se puede ver, no editar.
      return base(SECCION_5D, 'D', {
        version: 2,
        editable: false,
        celdas: [celda('LUN', 1, PLAN_MATEMATICA, 'Matemática', PEDRO)],
      });
    default:
      return null;
  }
};

export const MENSAJE_SIN_PRINCIPAL =
  'Configura la estructura horaria principal de secundaria antes de asignar horarios.';

export const mensajeDeVersion = (grado: string, seccion: string) =>
  `Otra persona modificó el horario de ${grado} ${seccion}. Recarga para ver los cambios.`;
