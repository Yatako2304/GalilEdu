import {
  DetalleEstructuraHoraria,
  EstructuraHorariaResumen,
} from '@/types/api';

// Datos de ejemplo de las pruebas de CI-12 (con los valores de Figma).

export const primaria: EstructuraHorariaResumen = {
  id: 'estructura-pri',
  codigo: 'EH-PRI-26',
  descripcion: 'Primaria · jornada de mañana',
  nivel: 'PRIMARIA',
  esPrincipal: true,
  diasLectivos: ['LUN', 'MAR', 'MIE', 'JUE', 'VIE'],
  horaEntrada: '07:45',
  horaSalida: '14:20',
  bloquesPorDia: 8,
  duracionBloqueMin: 45,
  recesos: [
    { despuesDeBloque: 3, duracionMin: 20 },
    { despuesDeBloque: 6, duracionMin: 15 },
  ],
  bloquesPorSemana: 40,
};

export const secundaria: EstructuraHorariaResumen = {
  id: 'estructura-sec',
  codigo: 'EH-SEC-26',
  descripcion: 'Secundaria · jornada de mañana',
  nivel: 'SECUNDARIA',
  esPrincipal: true,
  diasLectivos: ['LUN', 'MIE', 'VIE'],
  horaEntrada: '07:30',
  horaSalida: '14:50',
  bloquesPorDia: 9,
  duracionBloqueMin: 45,
  recesos: [{ despuesDeBloque: 4, duracionMin: 20 }],
  bloquesPorSemana: 27,
};

/** Tres bloques y un receso son suficientes para ver cómo se arma la grilla. */
export const detalleDe = (
  e: EstructuraHorariaResumen,
): DetalleEstructuraHoraria => ({
  ...e,
  anioModificable: true,
  jornada: [
    {
      tipo: 'BLOQUE',
      numero: 1,
      inicio: '07:45',
      fin: '08:30',
      duracionMin: 45,
    },
    {
      tipo: 'BLOQUE',
      numero: 2,
      inicio: '08:30',
      fin: '09:15',
      duracionMin: 45,
    },
    {
      tipo: 'RECESO',
      numero: null,
      inicio: '09:15',
      fin: '09:35',
      duracionMin: 20,
    },
    {
      tipo: 'BLOQUE',
      numero: 3,
      inicio: '09:35',
      fin: '10:20',
      duracionMin: 45,
    },
  ],
});
