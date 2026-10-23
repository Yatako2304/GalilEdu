import { CursoSesion, HorarioSeccion, SesionClase } from '@/types/api';

// Datos tomados de la pantalla de Figma "Estudiante/Horarios | HU-50".

const innovacion: CursoSesion = {
  curso: 'Innovación y descubrimiento',
  docente: 'Prof. Valeria Montes Ríos',
  aula: 'A-401',
};
const logica: CursoSesion = {
  curso: 'Lógica y estrategia',
  docente: 'Prof. Mateo Salazar Vega',
  aula: 'A-502',
};
const comunicacion: CursoSesion = {
  curso: 'Comunicación internacional',
  docente: 'Prof. Camila Torres León',
  aula: 'A-502',
};

const DIAS = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'] as const;

type Celda = CursoSesion | CursoSesion[];

const fila = (bloqueId: string, celdas: Celda[]): SesionClase[] =>
  celdas.map((celda, index) => ({
    dia: DIAS[index],
    bloqueId,
    tipo: Array.isArray(celda) ? 'SUBGRUPOS' : 'ASIGNADO',
    cursos: Array.isArray(celda) ? celda : [celda],
  }));

export const horarioSeccion5A: HorarioSeccion = {
  anioAcademico: 2026,
  seccion: {
    id: 'sec-5a',
    grado: 5,
    letra: 'A',
    tutor: 'Lucía Ayala Prado',
    nivel: 'SECUNDARIA',
    tutorCelular: '987 456 123',
    tutorCorreo: 'lucia.ayala@galiledu.edu.pe',
  },
  estudiante: {
    id: 'est-1',
    nombreCompleto: 'Valeria Sánchez Ferrara',
  },
  bloques: [
    {
      id: 'b1',
      nombre: 'Bloque 1',
      tipo: 'CLASE',
      horaInicio: '08:00',
      horaFin: '08:45',
      orden: 1,
    },
    {
      id: 'b2',
      nombre: 'Bloque 2',
      tipo: 'CLASE',
      horaInicio: '08:45',
      horaFin: '09:30',
      orden: 2,
    },
    {
      id: 'b3',
      nombre: 'Bloque 3',
      tipo: 'CLASE',
      horaInicio: '09:30',
      horaFin: '10:15',
      orden: 3,
    },
    {
      id: 'r1',
      nombre: 'Recreo 1',
      tipo: 'RECREO',
      horaInicio: '10:15',
      horaFin: '10:30',
      orden: 4,
    },
    {
      id: 'b4',
      nombre: 'Bloque 4',
      tipo: 'CLASE',
      horaInicio: '10:30',
      horaFin: '11:15',
      orden: 5,
    },
    {
      id: 'b5',
      nombre: 'Bloque 5',
      tipo: 'CLASE',
      horaInicio: '11:15',
      horaFin: '12:00',
      orden: 6,
    },
    {
      id: 'r2',
      nombre: 'Recreo 2',
      tipo: 'RECREO',
      horaInicio: '12:00',
      horaFin: '12:45',
      orden: 7,
    },
    {
      id: 'b6',
      nombre: 'Bloque 6',
      tipo: 'CLASE',
      horaInicio: '12:45',
      horaFin: '13:30',
      orden: 8,
    },
    {
      id: 'b7',
      nombre: 'Bloque 7',
      tipo: 'CLASE',
      horaInicio: '13:30',
      horaFin: '14:15',
      orden: 9,
    },
  ],
  sesiones: [
    ...fila('b1', [innovacion, logica, innovacion, comunicacion, innovacion]),
    ...fila('b2', [innovacion, comunicacion, innovacion, innovacion, logica]),
    ...fila('b3', [
      innovacion,
      logica,
      innovacion,
      comunicacion,
      [
        {
          curso: 'Movimiento creativo',
          docente: 'Prof. Valeria Montes Ríos',
          aula: 'E-401',
        },
        {
          curso: 'Expresión escénica',
          docente: 'Prof. Diego Navarro Cruz',
          aula: 'Z-908',
        },
      ],
    ]),
    ...fila('b4', [innovacion, innovacion, comunicacion, innovacion, logica]),
    ...fila('b5', [innovacion, innovacion, comunicacion, innovacion, logica]),
    ...fila('b6', [innovacion, comunicacion, innovacion, innovacion, logica]),
    ...fila('b7', [
      [
        {
          curso: 'Laboratorio de arte',
          docente: 'Prof. Valeria Montes Ríos',
          aula: 'E-401',
        },
        {
          curso: 'Sonido y composición',
          docente: 'Prof. Diego Navarro Cruz',
          aula: 'Z-908',
        },
      ],
      comunicacion,
      innovacion,
      innovacion,
      logica,
    ]),
  ],
};
