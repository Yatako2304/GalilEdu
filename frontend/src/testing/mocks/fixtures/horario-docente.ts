import {
  AnioDelDocente,
  ClaseDictada,
  HorarioDocente,
  HorarioPorEstructura,
} from '@/features/horarios/api/horario-docente';
import { JornadaDelDia } from '@/features/horarios/types';

// HU-49 · Figma > Docente / Tutor > Horario (393:18119). Datos con la forma de GET /horarios/docente/anios y
// /horarios/docente/horario.

export const ANIO_2026: AnioDelDocente = {
  id: 'anio-2026',
  anio: 2026,
  estado: 'ACTIVO',
};
export const ANIO_2025: AnioDelDocente = {
  id: 'anio-2025',
  anio: 2025,
  estado: 'CERRADO',
};

/** Los años con clases, del más reciente al más antiguo. */
export const aniosDocente: AnioDelDocente[] = [ANIO_2026, ANIO_2025];

const DIAS = ['LUN', 'MAR', 'MIE', 'JUE', 'VIE'] as const;

/** Secundaria: entrada a las 08:00, bloques de 45 min y un recreo después del bloque 3. */
export const jornadaSecundaria: JornadaDelDia = {
  dias: [...DIAS],
  bloques: [
    { numero: 1, inicio: '08:00', fin: '08:45' },
    { numero: 2, inicio: '08:45', fin: '09:30' },
    { numero: 3, inicio: '09:30', fin: '10:15' },
    { numero: 4, inicio: '10:30', fin: '11:15' },
  ],
  recesos: [{ despuesDeBloque: 3, inicio: '10:15', fin: '10:30' }],
};

/** Primaria: entrada a las 07:45, con otro recreo y más temprano. */
export const jornadaPrimaria: JornadaDelDia = {
  dias: [...DIAS],
  bloques: [
    { numero: 1, inicio: '07:45', fin: '08:30' },
    { numero: 2, inicio: '08:30', fin: '09:15' },
    { numero: 3, inicio: '09:15', fin: '10:00' },
  ],
  recesos: [{ despuesDeBloque: 2, inicio: '09:15', fin: '09:35' }],
};

const clase = (
  dia: ClaseDictada['dia'],
  numeroBloque: number,
  curso: string,
  seccion: string,
  ambiente: string,
): ClaseDictada => ({ dia, numeroBloque, curso, seccion, ambiente });

export const clasesSecundaria: ClaseDictada[] = [
  clase('LUN', 1, 'Álgebra', '1.° A', 'A-401'),
  clase('LUN', 2, 'Álgebra', '1.° B', 'A-402'),
  clase('MAR', 3, 'Geometría', '2.° A', 'A-502'),
  clase('VIE', 4, 'Álgebra', '1.° A', 'A-401'),
];

export const secundaria: HorarioPorEstructura = {
  nivel: 'SECUNDARIA',
  titulo: 'Secundaria',
  jornada: jornadaSecundaria,
  clases: clasesSecundaria,
};

export const primaria: HorarioPorEstructura = {
  nivel: 'PRIMARIA',
  titulo: 'Primaria',
  jornada: jornadaPrimaria,
  clases: [
    clase('LUN', 1, 'Aritmética', '3.° A', 'P-101'),
    clase('JUE', 3, 'Aritmética', '3.° B', 'P-102'),
  ],
};

/** Docente solo con secundaria. */
export const horarioSoloSecundaria: HorarioDocente = {
  anio: ANIO_2026,
  horarios: [secundaria],
};

/** Docente con primaria y secundaria (jornadas distintas): primaria primero. */
export const horarioPrimariaYSecundaria: HorarioDocente = {
  anio: ANIO_2026,
  horarios: [primaria, secundaria],
};

/** El año anterior, con un solo curso. */
export const horarioDocente2025: HorarioDocente = {
  anio: ANIO_2025,
  horarios: [
    {
      ...secundaria,
      clases: [clase('MIE', 2, 'Aritmética', '1.° A', 'A-401')],
    },
  ],
};

/** Sin clases en ningún año (también el estado de un tutor que no dicta). */
export const horarioSinClases: HorarioDocente = { anio: null, horarios: [] };
