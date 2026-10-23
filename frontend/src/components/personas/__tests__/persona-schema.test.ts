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

  test('nombres y apellidos solo aceptan letras, espacios, apóstrofo y guion', () => {
    expect(
      errores(personaSchema.safeParse({ ...base(), nombres: 'Jorge 2' })),
    ).toContain('Los nombres solo pueden contener letras');
    expect(
      errores(
        personaSchema.safeParse({ ...base(), apellidoPaterno: 'Pérez@' }),
      ),
    ).toContain('El apellido solo puede contener letras');
    expect(
      personaSchema.safeParse({
        ...base(),
        nombres: 'María José',
        apellidoPaterno: "D'Ángelo-Núñez",
      }).success,
    ).toBe(true);
  });

  test('normaliza el celular "987 111 222" y valida 9 dígitos', () => {
    const r = personaSchema.safeParse({ ...base(), celular: '987 111 222' });
    expect(r.success && r.data.celular).toBe('987111222');
    expect(
      personaSchema.safeParse({ ...base(), celular: '98711' }).success,
    ).toBe(false);
  });
});

describe('personaUsuarioSchema (HU-6, criterios CAHU6-*)', () => {
  const completo = () => ({
    ...base(),
    fechaNacimiento: '1990-04-12',
    nacionalidad: 'Peruana',
    lugarNacimiento: 'Perú',
    sexo: 'FEMENINO' as const,
    estadoCivil: 'Soltero(a)',
    gradoInstruccion: 'Superior universitaria',
    celular: '987111222',
    correo: 'jorge.quispe@gmail.com',
    profesion: 'Contador',
    centroTrabajo: 'Galiledu',
    cargo: 'Docente',
    distrito: 'San Miguel',
    direccion: 'Av. La Marina 123',
  });

  test('acepta los datos completos', () => {
    expect(personaUsuarioSchema.safeParse(completo()).success).toBe(true);
  });

  test('exige todos los campos salvo apellido materno, teléfono de casa y exalumno', () => {
    const r = personaUsuarioSchema.safeParse({
      ...personaVacia(),
      tipoDocumento: '',
      nacionalidad: '',
    });
    expect(errores(r)).toEqual(
      expect.arrayContaining([
        'Selecciona el tipo de documento',
        'Ingresa el número de documento',
        'Ingresa el apellido paterno',
        'Ingresa los nombres',
        'Ingresa la fecha de nacimiento',
        'Selecciona la nacionalidad',
        'Selecciona el país de nacimiento',
        'Selecciona el sexo',
        'Selecciona el estado civil',
        'Selecciona el grado de instrucción',
        'Ingresa el celular',
        'Ingresa el correo electrónico',
        'Ingresa la profesión u ocupación',
        'Ingresa el centro de trabajo',
        'Ingresa el cargo',
        'Selecciona el distrito',
        'Ingresa la dirección',
      ]),
    );
    expect(errores(r)).not.toContain('Ingresa el apellido materno');
  });

  test('muestra el error del DNI aunque falten otros campos', () => {
    const r = personaUsuarioSchema.safeParse({
      ...personaVacia(),
      numeroDocumento: '3',
    });
    expect(errores(r)).toEqual(
      expect.arrayContaining([
        'El DNI debe tener 8 dígitos',
        'Ingresa el apellido paterno',
      ]),
    );
  });

  test('apellido materno y teléfono de casa son opcionales', () => {
    const r = personaUsuarioSchema.safeParse({
      ...completo(),
      apellidoMaterno: '',
      telefonoCasa: '',
    });
    expect(r.success).toBe(true);
  });

  test('la fecha de nacimiento debe ser pasada', () => {
    const manana = new Date(Date.now() + 86_400_000).toLocaleDateString(
      'en-CA',
    );
    expect(
      errores(
        personaUsuarioSchema.safeParse({
          ...completo(),
          fechaNacimiento: manana,
        }),
      ),
    ).toContain('La fecha de nacimiento debe ser pasada');
  });

  test.each([
    'anagmail.com',
    'ana@@gmail.com',
    '@gmail.com',
    'ana@',
    'ana@gmail',
    'ana perez@gmail.com',
  ])('rechaza el correo inválido %s', (correo) => {
    expect(
      errores(personaUsuarioSchema.safeParse({ ...completo(), correo })),
    ).toContain('Ingresa un correo válido');
  });

  test('el teléfono de casa solo acepta números', () => {
    expect(
      errores(
        personaUsuarioSchema.safeParse({
          ...completo(),
          telefonoCasa: '01-452',
        }),
      ),
    ).toContain('El teléfono de casa solo puede contener números');
  });

  test('guarda el correo en minúsculas', () => {
    const r = personaUsuarioSchema.safeParse({
      ...completo(),
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
