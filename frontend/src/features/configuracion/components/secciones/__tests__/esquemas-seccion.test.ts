import {
  aSolicitudSeccion,
  normalizarNombreSeccion,
  nuevaSeccionSchema,
  valoresInicialesSeccion,
} from '../esquemas-seccion';
import type { ValoresSeccion } from '../esquemas-seccion';

// Figma > "Modal - Nueva sección" (HU-75): reglas y mensajes del backend.

const valida: ValoresSeccion = {
  gradoCodigo: 'PRI-2',
  nombre: 'C',
  capacidadMaxima: '30',
};

/** Todos los mensajes de un campo, en el orden en que el esquema los reporta (el formulario muestra el primero). */
const mensajesDe = (
  campo: keyof ValoresSeccion,
  cambios: Partial<ValoresSeccion>,
) => {
  const resultado = nuevaSeccionSchema.safeParse({ ...valida, ...cambios });
  return resultado.success
    ? []
    : resultado.error.issues
        .filter((problema) => problema.path[0] === campo)
        .map((problema) => problema.message);
};

test('unos datos válidos pasan sin mensajes', () => {
  expect(nuevaSeccionSchema.safeParse(valida).success).toBe(true);
});

test('el grado es obligatorio', () => {
  expect(mensajesDe('gradoCodigo', { gradoCodigo: '' })).toEqual([
    'Elige un grado.',
  ]);
});

test.each(['', '   '])(
  'el nombre vacío o de solo espacios pide el nombre (%j)',
  (nombre) => {
    expect(mensajesDe('nombre', { nombre })).toEqual([
      'Ingresa el nombre de la sección.',
    ]);
  },
);

test.each(['A', 'B', 'C', 'Ñ', 'A1', '12345', 'ABCDE', 'á', ' A ', 'a'])(
  'el nombre acepta letras y números hasta 5 caracteres (%j)',
  (nombre) => {
    expect(mensajesDe('nombre', { nombre })).toEqual([]);
  },
);

test('el nombre admite hasta 5 caracteres', () => {
  expect(mensajesDe('nombre', { nombre: 'ABCDEF' })).toEqual([
    'El nombre de la sección admite hasta 5 caracteres.',
  ]);
});

test.each(['A B', 'A-', 'A.', '@', 'A_B', 'A/'])(
  'el nombre solo admite letras y números (%j)',
  (nombre) => {
    expect(mensajesDe('nombre', { nombre })).toEqual([
      'El nombre de la sección solo admite letras y números.',
    ]);
  },
);

test('con un nombre largo y con signos se muestra primero el aviso del largo', () => {
  expect(mensajesDe('nombre', { nombre: 'AB CDEF' })[0]).toBe(
    'El nombre de la sección admite hasta 5 caracteres.',
  );
});

test.each(['1', '30', '45', ' 25 '])(
  'la capacidad acepta enteros de 1 a 45 (%j)',
  (capacidadMaxima) => {
    expect(mensajesDe('capacidadMaxima', { capacidadMaxima })).toEqual([]);
  },
);

test.each(['', '   '])('la capacidad vacía la pide (%j)', (capacidadMaxima) => {
  expect(mensajesDe('capacidadMaxima', { capacidadMaxima })).toEqual([
    'Ingresa la capacidad máxima.',
  ]);
});

test.each(['0', '46', '100', '-1', '30.5', '3,5', 'abc', '4 5', '1e1'])(
  'la capacidad fuera de 1 a 45 o que no es un entero da el mensaje de Figma (%j)',
  (capacidadMaxima) => {
    expect(mensajesDe('capacidadMaxima', { capacidadMaxima })).toEqual([
      'Entre 1 y 45 alumnos.',
    ]);
  },
);

test('cada campo da un solo mensaje aunque varias reglas fallen a la vez', () => {
  const resultado = nuevaSeccionSchema.safeParse({
    gradoCodigo: '',
    nombre: '',
    capacidadMaxima: '',
  });

  expect(resultado.success).toBe(false);
  if (!resultado.success) {
    const campos = resultado.error.issues.map((p) => p.path[0]);
    expect(campos).toEqual(['gradoCodigo', 'nombre', 'capacidadMaxima']);
  }
});

test('el nombre se pasa a mayúsculas al recibir lo escrito, sin quitar espacios', () => {
  expect(normalizarNombreSeccion('a')).toBe('A');
  expect(normalizarNombreSeccion('ñ1')).toBe('Ñ1');
  expect(normalizarNombreSeccion('a b')).toBe('A B');
  expect(normalizarNombreSeccion('')).toBe('');
});

test('al abrir el modal no hay nada precargado, salvo el grado si viene de un grado', () => {
  expect(valoresInicialesSeccion()).toEqual({
    gradoCodigo: '',
    nombre: '',
    capacidadMaxima: '',
  });
  expect(valoresInicialesSeccion('PRI-2')).toEqual({
    gradoCodigo: 'PRI-2',
    nombre: '',
    capacidadMaxima: '',
  });
});

test('el cuerpo que se envía recorta el nombre y convierte la capacidad a número', () => {
  expect(
    aSolicitudSeccion({
      gradoCodigo: 'SEC-1',
      nombre: ' C ',
      capacidadMaxima: '30',
    }),
  ).toEqual({ gradoCodigo: 'SEC-1', nombre: 'C', capacidadMaxima: 30 });
});
