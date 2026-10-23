import {
  ValoresTarifario,
  solicitudDeTarifario,
  validarTarifario,
} from '../esquemas';

// Reglas del formulario P19 (HU-34): las mismas del backend (`Tarifario.validar`), con sus mismos mensajes.

const valido: ValoresTarifario = {
  montoCuotaIngreso: '500',
  montoMatricula: '450',
  montoPension: '620',
  diasGracia: '5',
  recargoPorcentaje: '2',
  topeRecargoPorcentaje: '10',
};

const errores = (cambios: Partial<ValoresTarifario>) =>
  validarTarifario({ ...valido, ...cambios });

test('acepta los valores de Figma', () => {
  expect(errores({})).toEqual({});
});

describe('montos', () => {
  test('la matrícula y la pensión son obligatorias; la cuota de ingreso, no', () => {
    expect(errores({ montoMatricula: '' })).toEqual({
      montoMatricula: 'Ingresa el monto de la matrícula.',
    });
    expect(errores({ montoPension: '  ' })).toEqual({
      montoPension: 'Ingresa el monto de la pensión.',
    });
    expect(errores({ montoCuotaIngreso: '' })).toEqual({});
  });

  test('no pueden ser negativos', () => {
    expect(errores({ montoCuotaIngreso: '-1' })).toEqual({
      montoCuotaIngreso:
        'El monto de la cuota de ingreso no puede ser negativo.',
    });
    expect(errores({ montoMatricula: '-0.5' })).toEqual({
      montoMatricula: 'El monto de la matrícula no puede ser negativo.',
    });
    expect(errores({ montoPension: '-620' })).toEqual({
      montoPension: 'El monto de la pensión no puede ser negativo.',
    });
  });

  test('admiten hasta 2 decimales; ceros de más no cuentan', () => {
    expect(errores({ montoMatricula: '450.123' })).toEqual({
      montoMatricula: 'El monto de la matrícula admite hasta 2 decimales.',
    });
    expect(errores({ montoMatricula: '450.50' })).toEqual({});
    expect(errores({ montoMatricula: '450.500' })).toEqual({});
    expect(errores({ montoMatricula: '450.10' })).toEqual({});
  });

  test('no pasan de S/ 99999999.99', () => {
    expect(errores({ montoPension: '99999999.99' })).toEqual({});
    expect(errores({ montoPension: '100000000' })).toEqual({
      montoPension: 'El monto de la pensión no puede superar S/ 99999999.99.',
    });
  });

  test('la pensión debe ser mayor que cero; la matrícula y el ingreso pueden ser 0', () => {
    expect(errores({ montoPension: '0' })).toEqual({
      montoPension: 'El monto de la pensión debe ser mayor que cero.',
    });
    expect(errores({ montoMatricula: '0', montoCuotaIngreso: '0' })).toEqual(
      {},
    );
  });
});

describe('política de morosidad', () => {
  test('los tres campos son obligatorios', () => {
    expect(
      errores({
        diasGracia: '',
        recargoPorcentaje: '',
        topeRecargoPorcentaje: '',
      }),
    ).toEqual({
      diasGracia: 'Ingresa los días de gracia.',
      recargoPorcentaje: 'Ingresa el recargo por cada 30 días.',
      topeRecargoPorcentaje: 'Ingresa el tope de recargo.',
    });
  });

  test('los días de gracia son enteros de 0 a 60', () => {
    const mensaje = 'Los días de gracia deben estar entre 0 y 60.';
    for (const dias of ['-1', '61', '2.5']) {
      expect(errores({ diasGracia: dias })).toEqual({ diasGracia: mensaje });
    }
    expect(errores({ diasGracia: '0' })).toEqual({});
    expect(errores({ diasGracia: '60' })).toEqual({});
  });

  test('el recargo y el tope van de 0 a 100 %', () => {
    expect(errores({ recargoPorcentaje: '-1' })).toEqual({
      recargoPorcentaje: 'El recargo debe estar entre 0 y 100 %.',
    });
    expect(
      errores({ recargoPorcentaje: '2', topeRecargoPorcentaje: '101' }),
    ).toEqual({
      topeRecargoPorcentaje: 'El tope de recargo debe estar entre 0 y 100 %.',
    });
    expect(
      errores({ recargoPorcentaje: '0', topeRecargoPorcentaje: '100' }),
    ).toEqual({});
  });

  test('el recargo y el tope admiten hasta 2 decimales', () => {
    expect(errores({ recargoPorcentaje: '1.234' })).toEqual({
      recargoPorcentaje: 'El recargo admite hasta 2 decimales.',
    });
    expect(errores({ topeRecargoPorcentaje: '10.555' })).toEqual({
      topeRecargoPorcentaje: 'El tope de recargo admite hasta 2 decimales.',
    });
    expect(
      errores({ recargoPorcentaje: '1.25', topeRecargoPorcentaje: '10.5' }),
    ).toEqual({});
  });

  test('el recargo no puede ser mayor que el tope (el aviso va en el recargo)', () => {
    expect(
      errores({ recargoPorcentaje: '11', topeRecargoPorcentaje: '10' }),
    ).toEqual({
      recargoPorcentaje:
        'El recargo no puede ser mayor que el tope de recargo.',
    });
    expect(
      errores({ recargoPorcentaje: '10', topeRecargoPorcentaje: '10' }),
    ).toEqual({});
  });
});

test('muestra un mensaje por cada campo con error, no solo el primero como el backend', () => {
  expect(
    errores({ montoMatricula: '', montoPension: '0', diasGracia: '99' }),
  ).toEqual({
    montoMatricula: 'Ingresa el monto de la matrícula.',
    montoPension: 'El monto de la pensión debe ser mayor que cero.',
    diasGracia: 'Los días de gracia deben estar entre 0 y 60.',
  });
});

test('solicitudDeTarifario pasa los textos a números y deja la cuota de ingreso vacía en null', () => {
  expect(solicitudDeTarifario(valido)).toEqual({
    montoCuotaIngreso: 500,
    montoMatricula: 450,
    montoPension: 620,
    diasGracia: 5,
    recargoPorcentaje: 2,
    topeRecargoPorcentaje: 10,
  });
  expect(
    solicitudDeTarifario({ ...valido, montoCuotaIngreso: '' })
      .montoCuotaIngreso,
  ).toBeNull();
});
