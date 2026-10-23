import {
  ordenarDias,
  textoBloquesPorDia,
  textoCantidadDias,
  textoDias,
  textoJornada,
  textoRecesos,
  textoRegistradas,
} from '../formato';

import { primaria } from './datos';

// Figma > CI-12 · Estructura horaria (HU-44): los textos de las tarjetas y los datos.

test('los días seguidos van como rango y los demás separados por comas', () => {
  expect(textoDias(['LUN', 'MAR', 'MIE', 'JUE', 'VIE'])).toBe('Lun – Vie');
  expect(textoDias(['LUN', 'MAR', 'MIE', 'JUE', 'VIE', 'SAB'])).toBe(
    'Lun – Sáb',
  );
  expect(textoDias(['MAR', 'MIE', 'JUE'])).toBe('Mar – Jue');
  expect(textoDias(['LUN', 'MIE', 'VIE'])).toBe('Lun, Mié, Vie');
  expect(textoDias(['LUN', 'MAR'])).toBe('Lun, Mar');
  expect(textoDias(['SAB'])).toBe('Sáb');
  expect(textoDias([])).toBe('—');
});

test('los días se ordenan por la semana aunque lleguen en otro orden', () => {
  expect(ordenarDias(['VIE', 'LUN', 'MIE'])).toEqual(['LUN', 'MIE', 'VIE']);
  expect(textoDias(['VIE', 'MIE', 'JUE', 'LUN', 'MAR'])).toBe('Lun – Vie');
});

test('la cantidad de días y de estructuras va en singular cuando es una', () => {
  expect(textoCantidadDias(5)).toBe('5 días');
  expect(textoCantidadDias(1)).toBe('1 día');
  expect(textoRegistradas(2)).toBe('2 registradas');
  expect(textoRegistradas(1)).toBe('1 registrada');
  expect(textoRegistradas(0)).toBe('0 registradas');
});

test('la jornada de la tarjeta lleva entrada, salida, bloques y duración', () => {
  expect(textoJornada(primaria)).toBe('07:45 – 14:20 · 8 bloques de 45 min');
  expect(textoJornada({ ...primaria, bloquesPorDia: 1 })).toBe(
    '07:45 – 14:20 · 1 bloque de 45 min',
  );
});

test('los bloques por día y los recesos se escriben como en Figma', () => {
  expect(textoBloquesPorDia(primaria)).toBe('8 × 45 min');
  expect(textoRecesos(primaria.recesos)).toBe('20 + 15 min');
  // En el orden en que ocurren en la jornada, no en el que llegan.
  expect(
    textoRecesos([
      { despuesDeBloque: 6, duracionMin: 15 },
      { despuesDeBloque: 3, duracionMin: 20 },
    ]),
  ).toBe('20 + 15 min');
  expect(textoRecesos([{ despuesDeBloque: 2, duracionMin: 15 }])).toBe(
    '15 min',
  );
  expect(textoRecesos([])).toBe('Sin recesos');
});
