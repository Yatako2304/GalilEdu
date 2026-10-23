import {
  ESTADO_SECCION,
  etiquetaSeccion,
  porcentajeMatriculados,
  textoSeccionesActivas,
} from '../formato';

// Figma > CI-09 · Secciones por grado y tutores (HU-75)

test('la etiqueta de la sección lleva su grado: "1.° A"', () => {
  expect(etiquetaSeccion({ numero: 1 }, { nombre: 'A' })).toBe('1.° A');
  expect(etiquetaSeccion({ numero: 5 }, { nombre: 'C' })).toBe('5.° C');
});

test('las secciones activas van en plural, salvo una', () => {
  expect(textoSeccionesActivas(3)).toBe('3 secciones activas');
  expect(textoSeccionesActivas(2)).toBe('2 secciones activas');
  expect(textoSeccionesActivas(1)).toBe('1 sección activa');
  expect(textoSeccionesActivas(0)).toBe('0 secciones activas');
});

test('el porcentaje de matriculados es redondeado y nunca divide por cero', () => {
  expect(
    porcentajeMatriculados({ alumnosOcupanVacante: 29, capacidadMaxima: 30 }),
  ).toBe(97);
  expect(
    porcentajeMatriculados({ alumnosOcupanVacante: 0, capacidadMaxima: 30 }),
  ).toBe(0);
  expect(
    porcentajeMatriculados({ alumnosOcupanVacante: 30, capacidadMaxima: 30 }),
  ).toBe(100);
  expect(
    porcentajeMatriculados({ alumnosOcupanVacante: 5, capacidadMaxima: 0 }),
  ).toBe(0);
});

test('el estado de la sección se ve como Activa o Inactiva', () => {
  expect(ESTADO_SECCION.ACTIVA).toEqual({ texto: 'Activa', color: 'success' });
  expect(ESTADO_SECCION.INACTIVA).toEqual({
    texto: 'Inactiva',
    color: 'neutral',
  });
});
