import {
  ClaseDelHorario,
  HijoConHorario,
  HorarioDelEstudiante,
} from '@/features/horarios/api/horario-apoderado';

// Datos tomados de Figma > Apoderado (real) > "Horario" y Apoderado > "HU-51 Consulta de horario…".

export const hijosConHorario: HijoConHorario[] = [
  {
    estudianteId: 'est-valeria',
    nombreCompleto: 'Valeria Sánchez Ferrara',
    matriculas: [
      {
        anio: 2026,
        grado: '5.° de secundaria',
        nivel: 'SECUNDARIA',
        seccion: 'A',
      },
    ],
  },
];

const innovacion = {
  curso: 'Innovación y descubrimiento',
  docente: 'Valeria Montes Ríos',
  aula: 'A-401',
};
const logica = {
  curso: 'Lógica y estrategia',
  docente: 'Mateo Salazar Vega',
  aula: 'A-502',
};
const comunicacion = {
  curso: 'Comunicación internacional',
  docente: 'Camila Torres León',
  aula: 'A-502',
};

const DIAS = ['LUN', 'MAR', 'MIE', 'JUE', 'VIE'] as const;
const fila = (
  numeroBloque: number,
  cursos: (typeof innovacion)[],
): ClaseDelHorario[] =>
  cursos.map((c, i) => ({ dia: DIAS[i], numeroBloque, ...c }));

export const horarioValeria2026: HorarioDelEstudiante = {
  estudianteId: 'est-valeria',
  nombreCompleto: 'Valeria Sánchez Ferrara',
  anio: 2026,
  grado: '5.° de secundaria',
  nivel: 'SECUNDARIA',
  seccion: 'A',
  tutor: {
    nombre: 'Lucía Ayala Prado',
    celular: '987456123',
    correo: 'lucia.ayala@galiledu.edu.pe',
  },
  disponible: true,
  jornada: {
    dias: [...DIAS],
    bloques: [
      { numero: 1, inicio: '08:00', fin: '08:45' },
      { numero: 2, inicio: '08:45', fin: '09:30' },
      { numero: 3, inicio: '09:30', fin: '10:15' },
      { numero: 4, inicio: '10:30', fin: '11:15' },
      { numero: 5, inicio: '11:15', fin: '12:00' },
      { numero: 6, inicio: '12:45', fin: '13:30' },
      { numero: 7, inicio: '13:30', fin: '14:15' },
    ],
    recesos: [
      { despuesDeBloque: 3, inicio: '10:15', fin: '10:30' },
      { despuesDeBloque: 5, inicio: '12:00', fin: '12:45' },
    ],
  },
  clases: [
    ...fila(1, [innovacion, logica, innovacion, comunicacion, innovacion]),
    ...fila(2, [innovacion, comunicacion, innovacion, innovacion, logica]),
    ...fila(3, [innovacion, logica, innovacion, comunicacion, innovacion]),
    ...fila(4, [innovacion, innovacion, comunicacion, innovacion, logica]),
    ...fila(5, [innovacion, innovacion, comunicacion, innovacion, logica]),
    ...fila(6, [innovacion, comunicacion, innovacion, innovacion, logica]),
    ...fila(7, [innovacion, comunicacion, innovacion, innovacion, logica]),
  ],
};
