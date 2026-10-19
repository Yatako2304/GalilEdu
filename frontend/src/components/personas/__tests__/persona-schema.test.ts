import {
  personaAFormulario,
  personaSchema,
  personaUsuarioSchema,
  personaVacia,
} from '../persona-schema';

const base = () => ({
  ...personaVacia(),
  numeroDocumento: '41234567',
  apellidoPaterno: 'Quispe',
  nombres: 'Jorge Luis',
});

const errores = (r: ReturnType<typeof personaSchema.safeParse>) =>
  r.success ? [] : r.error.issues.map((i) => i.message);

describe('personaSchema (RF-12)', () => {
  test('acepta los datos mínimos y convierte vacíos a null', () => {
    const r = personaSchema.safeParse(base());
    expect(r.success).toBe(true);
    if (r.success) {
      expect(r.data.apellidoMaterno).toBeNull();
      expect(r.data.correo).toBeNull();
      expect(r.data.sexo).toBeNull();
    }
  });

  test('el DNI debe tener 8 dígitos', () => {
    expect(
      errores(personaSchema.safeParse({ ...base(), numeroDocumento: '1234' })),
    ).toContain('El DNI debe tener 8 dígitos');
  });

  test('el carné de extranjería acepta hasta 12 caracteres alfanuméricos', () => {
    const ce = { ...base(), tipoDocumento: 'CARNET_EXTRANJERIA' as const };
    expect(
      personaSchema.safeParse({ ...ce, numeroDocumento: 'AB1234567890' })
        .success,
    ).toBe(true);
    expect(
      personaSchema.safeParse({ ...ce, numeroDocumento: 'AB12345678901' })
        .success,
    ).toBe(false);
  });

  test('nombres y apellidos no aceptan números', () => {
    expect(
      errores(personaSchema.safeParse({ ...base(), nombres: 'Jorge 2' })),
    ).toContain('Los nombres no pueden contener números');
  });

  test('normaliza el celular "987 111 222" y valida 9 dígitos', () => {
    const r = personaSchema.safeParse({ ...base(), celular: '987 111 222' });
    expect(r.success && r.data.celular).toBe('987111222');
    expect(
      personaSchema.safeParse({ ...base(), celular: '98711' }).success,
    ).toBe(false);
  });
});

describe('personaUsuarioSchema (HU-6: la cuenta necesita correo y celular)', () => {
  test('exige correo y celular', () => {
    const r = personaUsuarioSchema.safeParse(base());
    expect(errores(r)).toEqual(
      expect.arrayContaining([
        'Ingresa el celular',
        'Ingresa el correo electrónico',
      ]),
    );
  });

  test('guarda el correo en minúsculas', () => {
    const r = personaUsuarioSchema.safeParse({
      ...base(),
      celular: '987111222',
      correo: 'Jorge.Quispe@Gmail.com',
    });
    expect(r.success && r.data.correo).toBe('jorge.quispe@gmail.com');
  });
});

test('personaAFormulario convierte nulls del backend a valores de formulario', () => {
  const valores = personaAFormulario({
    tipoDocumento: 'DNI',
    numeroDocumento: '41234567',
    apellidoPaterno: 'Quispe',
    nombres: 'Jorge',
    correo: null,
    esExalumno: null,
  });
  expect(valores.correo).toBe('');
  expect(valores.esExalumno).toBe(false);
});
