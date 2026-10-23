import { crearPeriodoSchema } from '../esquema-periodo';

// Reglas del modal "Editar periodo" (Figma): las mismas que el backend, con sus mismos mensajes.

const schema = crearPeriodoSchema({
  anio: { fechaInicio: '2026-03-02', fechaFin: '2026-12-18' },
  otros: [
    {
      nombre: 'II Bimestre',
      fechaInicio: '2026-05-18',
      fechaFin: '2026-07-24',
    },
    {
      nombre: 'IV Bimestre',
      fechaInicio: '2026-10-19',
      fechaFin: '2026-12-18',
    },
  ],
});

const valido = {
  nombre: 'III Bimestre',
  fechaInicio: '2026-08-03',
  fechaFin: '2026-10-09',
  fechaLimiteNotas: '2026-10-16',
};

/** Un mensaje por campo, como los ve el usuario. */
const errores = (valores: Record<string, string>) => {
  const resultado = schema.safeParse({ ...valido, ...valores });
  return Object.fromEntries(
    (resultado.success ? [] : resultado.error.issues).map((i) => [
      i.path[0],
      i.message,
    ]),
  );
};

test('acepta un periodo válido', () => {
  expect(schema.safeParse(valido).success).toBe(true);
});

test('el límite puede ser el mismo día del fin', () => {
  expect(errores({ fechaLimiteNotas: '2026-10-09' })).toEqual({});
});

test('nombre y fechas son obligatorios', () => {
  expect(
    errores({
      nombre: '  ',
      fechaInicio: '',
      fechaFin: '',
      fechaLimiteNotas: '',
    }),
  ).toEqual({
    nombre: 'Ingresa el nombre del periodo.',
    fechaInicio: 'Ingresa la fecha de inicio.',
    fechaFin: 'Ingresa la fecha de fin.',
    fechaLimiteNotas: 'Ingresa la fecha límite de notas.',
  });
});

test('el nombre no pasa de 30 caracteres', () => {
  expect(errores({ nombre: 'x'.repeat(31) })).toEqual({
    nombre: 'El nombre del periodo no puede pasar de 30 caracteres.',
  });
  expect(errores({ nombre: 'x'.repeat(30) })).toEqual({});
});

test('el fin debe ser posterior al inicio', () => {
  expect(errores({ fechaFin: '2026-08-03' })).toEqual({
    fechaFin: 'La fecha de fin debe ser posterior a la fecha de inicio.',
  });
  expect(errores({ fechaFin: '2026-08-01' })).toEqual({
    fechaFin: 'La fecha de fin debe ser posterior a la fecha de inicio.',
  });
});

test('las fechas deben estar dentro del año escolar y el error va en el campo que se pasa', () => {
  const mensaje =
    'Las fechas del periodo deben estar dentro del año escolar (02/03/2026 al 18/12/2026).';
  expect(errores({ fechaInicio: '2026-03-01' }).fechaInicio).toBe(mensaje);
  expect(
    errores({ fechaInicio: '2026-08-03', fechaFin: '2026-12-19' }).fechaFin,
  ).toBe(mensaje);
});

test('un cruce con otro periodo se avisa bajo la fecha de fin, con el mismo texto del backend', () => {
  expect(
    errores({ fechaFin: '2026-10-25', fechaLimiteNotas: '2026-11-01' }),
  ).toEqual({
    fechaFin: 'Se cruza con el IV Bimestre, que empieza el 19/10/2026.',
  });
  expect(errores({ fechaInicio: '2026-07-20' })).toEqual({
    fechaFin: 'Se cruza con el II Bimestre, que empieza el 18/05/2026.',
  });
});

test('dos periodos que se tocan en un día también se cruzan', () => {
  expect(
    errores({ fechaFin: '2026-10-19', fechaLimiteNotas: '2026-10-20' }),
  ).toEqual({
    fechaFin: 'Se cruza con el IV Bimestre, que empieza el 19/10/2026.',
  });
});

test('la fecha límite no puede ser anterior al fin del periodo', () => {
  expect(errores({ fechaLimiteNotas: '2026-10-08' })).toEqual({
    fechaLimiteNotas:
      'La fecha límite de notas no puede ser anterior al fin del periodo.',
  });
});

test('cada campo muestra a lo sumo un error', () => {
  const resultado = schema.safeParse({
    ...valido,
    fechaInicio: '2026-03-01',
    fechaFin: '2026-03-01',
  });
  const porCampo = (resultado.success ? [] : resultado.error.issues).map(
    (i) => i.path[0],
  );
  expect(new Set(porCampo).size).toBe(porCampo.length);
});
