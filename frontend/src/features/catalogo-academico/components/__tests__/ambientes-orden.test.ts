import { AmbienteResumen } from '@/types/api';

import { esCampoOrden, ordenarAmbientes } from '../ambientes-orden';

const ambiente = (cambios: Partial<AmbienteResumen>): AmbienteResumen => ({
  id: cambios.nombre ?? 'x',
  codigo: 'A-101',
  nombre: 'Aula',
  tipo: 'AULA_REGULAR',
  aforoMaximo: 30,
  recursos: [],
  estado: 'ACTIVO',
  ...cambios,
});

const nombres = (lista: AmbienteResumen[]) => lista.map((a) => a.nombre);

test('por nombre, sin distinguir mayúsculas ni tildes y con los números en orden natural', () => {
  const lista = [
    ambiente({ nombre: 'Aula 10' }),
    ambiente({ nombre: 'aula 2' }),
    ambiente({ nombre: 'Áula 1' }),
    ambiente({ nombre: 'Biblioteca' }),
  ];

  expect(
    nombres(ordenarAmbientes(lista, { campo: 'nombre', direccion: 'asc' })),
  ).toEqual(['Áula 1', 'aula 2', 'Aula 10', 'Biblioteca']);
  expect(
    nombres(ordenarAmbientes(lista, { campo: 'nombre', direccion: 'desc' })),
  ).toEqual(['Biblioteca', 'Aula 10', 'aula 2', 'Áula 1']);
});

test('por aforo, como número y no como texto', () => {
  const lista = [
    ambiente({ nombre: 'A', aforoMaximo: 180 }),
    ambiente({ nombre: 'B', aforoMaximo: 25 }),
    ambiente({ nombre: 'C', aforoMaximo: 30 }),
  ];

  expect(
    nombres(
      ordenarAmbientes(lista, { campo: 'aforoMaximo', direccion: 'asc' }),
    ),
  ).toEqual(['B', 'C', 'A']);
  expect(
    nombres(
      ordenarAmbientes(lista, { campo: 'aforoMaximo', direccion: 'desc' }),
    ),
  ).toEqual(['A', 'C', 'B']);
});

test('por tipo y por estado, con el texto que se ve en la tabla', () => {
  const lista = [
    ambiente({ nombre: 'A', tipo: 'ESPACIO_ESPECIAL', estado: 'INACTIVO' }),
    ambiente({ nombre: 'B', tipo: 'AULA_REGULAR', estado: 'ACTIVO' }),
  ];

  expect(
    nombres(ordenarAmbientes(lista, { campo: 'tipo', direccion: 'asc' })),
  ).toEqual(['B', 'A']); // "Aula regular" antes que "Espacio especial"
  expect(
    nombres(ordenarAmbientes(lista, { campo: 'estado', direccion: 'asc' })),
  ).toEqual(['B', 'A']); // "Activo" antes que "Inactivo"
});

test('por recursos, por el texto de la lista', () => {
  const lista = [
    ambiente({ nombre: 'A', recursos: ['Proyector'] }),
    ambiente({ nombre: 'B', recursos: ['Pizarra', 'Proyector'] }),
    ambiente({ nombre: 'C', recursos: [] }),
  ];

  expect(
    nombres(ordenarAmbientes(lista, { campo: 'recursos', direccion: 'asc' })),
  ).toEqual(['C', 'B', 'A']);
});

test('un empate se desempata por nombre y no se invierte al cambiar de dirección', () => {
  const lista = [
    ambiente({ nombre: 'Zeta', aforoMaximo: 30 }),
    ambiente({ nombre: 'Alfa', aforoMaximo: 30 }),
    ambiente({ nombre: 'Medio', aforoMaximo: 40 }),
  ];

  expect(
    nombres(
      ordenarAmbientes(lista, { campo: 'aforoMaximo', direccion: 'asc' }),
    ),
  ).toEqual(['Alfa', 'Zeta', 'Medio']);
  expect(
    nombres(
      ordenarAmbientes(lista, { campo: 'aforoMaximo', direccion: 'desc' }),
    ),
  ).toEqual(['Medio', 'Alfa', 'Zeta']);
});

test('no modifica la lista original', () => {
  const lista = [ambiente({ nombre: 'B' }), ambiente({ nombre: 'A' })];

  ordenarAmbientes(lista, { campo: 'nombre', direccion: 'asc' });

  expect(nombres(lista)).toEqual(['B', 'A']);
});

test('solo se ordena por las columnas visibles, no por el id ni el código', () => {
  expect(esCampoOrden('nombre')).toBe(true);
  expect(esCampoOrden('recursos')).toBe(true);
  expect(esCampoOrden('id')).toBe(false);
  expect(esCampoOrden('codigo')).toBe(false);
});
