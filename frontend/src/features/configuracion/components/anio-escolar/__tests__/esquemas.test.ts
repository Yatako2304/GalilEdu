import {
  anioEscrito,
  anioSugerido,
  edicionAnioSchema,
  fechasSugeridas,
  nuevoAnioSchema,
  valoresIniciales,
} from '../esquemas';

const valido = {
  anio: '2028',
  fechaInicio: '2028-03-01',
  fechaFin: '2028-12-15',
  regimen: 'BIMESTRAL',
};

const mensajes = (resultado: {
  success: boolean;
  error?: { issues: { path: (string | number)[]; message: string }[] };
}) =>
  Object.fromEntries(
    resultado.error?.issues.map((i) => [i.path[0], i.message]) ?? [],
  );

describe('valores iniciales del modal Nuevo año escolar (Figma)', () => {
  test('propone el año siguiente al último registrado, con sus fechas y Bimestral', () => {
    expect(valoresIniciales([{ anio: 2026 }, { anio: 2027 }])).toEqual({
      anio: '2028',
      fechaInicio: '2028-03-01',
      fechaFin: '2028-12-15',
      regimen: 'BIMESTRAL',
    });
  });

  test('el último año es el mayor, aunque la lista venga desordenada', () => {
    expect(anioSugerido([{ anio: 2025 }, { anio: 2027 }, { anio: 2026 }])).toBe(
      2028,
    );
  });

  test('sin años registrados propone el siguiente al actual', () => {
    expect(anioSugerido([], 2026)).toBe(2027);
    expect(valoresIniciales([], 2026).anio).toBe('2027');
  });

  test('las fechas sugeridas son 01/03 y 15/12 del año', () => {
    expect(fechasSugeridas(2030)).toEqual({
      fechaInicio: '2030-03-01',
      fechaFin: '2030-12-15',
    });
  });
});

describe('anioEscrito', () => {
  test('solo reconoce un año completo y dentro del rango', () => {
    expect(anioEscrito('2029')).toBe(2029);
    expect(anioEscrito(' 2029 ')).toBe(2029);
    expect(anioEscrito('202')).toBeNull();
    expect(anioEscrito('')).toBeNull();
    expect(anioEscrito('1999')).toBeNull();
    expect(anioEscrito('2101')).toBeNull();
    expect(anioEscrito('abcd')).toBeNull();
  });
});

describe('nuevoAnioSchema (mensajes del backend)', () => {
  test('acepta los datos completos', () => {
    expect(nuevoAnioSchema.safeParse(valido).success).toBe(true);
  });

  test('el año es obligatorio y va de 2000 a 2100', () => {
    expect(
      mensajes(nuevoAnioSchema.safeParse({ ...valido, anio: '' })),
    ).toEqual({ anio: 'Ingresa el año escolar.' });
    for (const anio of ['1999', '2101', '20x8', '28']) {
      expect(
        mensajes(nuevoAnioSchema.safeParse({ ...valido, anio })).anio,
      ).toBe('El año escolar debe estar entre 2000 y 2100.');
    }
  });

  test('las fechas son obligatorias', () => {
    expect(
      mensajes(
        nuevoAnioSchema.safeParse({ ...valido, fechaInicio: '', fechaFin: '' }),
      ),
    ).toEqual({
      fechaInicio: 'Ingresa la fecha de inicio.',
      fechaFin: 'Ingresa la fecha de fin.',
    });
  });

  test('el fin debe ser posterior al inicio y el error va en la fecha de fin', () => {
    for (const fechaFin of ['2028-03-01', '2028-02-01']) {
      expect(
        mensajes(nuevoAnioSchema.safeParse({ ...valido, fechaFin })),
      ).toEqual({
        fechaFin: 'La fecha de fin debe ser posterior a la fecha de inicio.',
      });
    }
  });

  test('el régimen debe ser Bimestral o Trimestral', () => {
    expect(
      mensajes(nuevoAnioSchema.safeParse({ ...valido, regimen: undefined })),
    ).toEqual({ regimen: 'Elige el régimen de periodos.' });
  });
});

describe('edicionAnioSchema', () => {
  test('solo valida las fechas', () => {
    expect(
      edicionAnioSchema.safeParse({
        fechaInicio: '2027-03-08',
        fechaFin: '2027-12-10',
      }).success,
    ).toBe(true);
    expect(
      mensajes(
        edicionAnioSchema.safeParse({
          fechaInicio: '2027-03-08',
          fechaFin: '2027-03-08',
        }),
      ),
    ).toEqual({
      fechaFin: 'La fecha de fin debe ser posterior a la fecha de inicio.',
    });
  });
});
