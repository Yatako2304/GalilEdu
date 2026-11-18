import {
  ambienteSchema,
  aSolicitudAmbiente,
  campoDelErrorDelBackend,
  parsearRecursos,
  textoRecursos,
} from '../esquemas-ambientes';

// Figma > CI-26 > "Modal - Nuevo ambiente físico" (nodo 857:11500). Mismas reglas que Ambiente.java.

const valido = {
  tipo: 'ESPACIO_ESPECIAL',
  nombre: 'Laboratorio de robótica',
  aforoMaximo: '25',
  recursos: 'Kits de robótica, Proyector, 12 laptops',
};

/** Mensaje de error por campo (el primero de cada uno), como lo mostraría el formulario. */
const errores = (valores: Record<string, string>) => {
  const resultado = ambienteSchema.safeParse({ ...valido, ...valores });
  if (resultado.success) return {};
  const porCampo: Record<string, string> = {};
  // El formulario muestra el primer error de cada campo.
  resultado.error.issues.forEach((i) => {
    porCampo[String(i.path[0])] ??= i.message;
  });
  return porCampo;
};

describe('parsearRecursos', () => {
  test('separa por comas y recorta', () => {
    expect(parsearRecursos('Kits de robótica, Proyector, 12 laptops')).toEqual([
      'Kits de robótica',
      'Proyector',
      '12 laptops',
    ]);
  });

  test('quita los vacíos y los repetidos sin distinguir mayúsculas, y queda el primero', () => {
    expect(
      parsearRecursos(' Proyector , , proyector,PROYECTOR,  Pizarra  ,'),
    ).toEqual(['Proyector', 'Pizarra']);
  });

  test('reduce los espacios repetidos de cada recurso', () => {
    expect(parsearRecursos('35   computadoras, 35 computadoras')).toEqual([
      '35 computadoras',
    ]);
  });

  test('un texto vacío o solo con comas y espacios son ningún recurso', () => {
    expect(parsearRecursos('')).toEqual([]);
    expect(parsearRecursos('  , ,  ')).toEqual([]);
  });

  test('ida y vuelta con el texto del campo', () => {
    expect(textoRecursos(['Sonido', 'Escenario'])).toBe('Sonido, Escenario');
    expect(parsearRecursos(textoRecursos(['Sonido', 'Escenario']))).toEqual([
      'Sonido',
      'Escenario',
    ]);
  });
});

describe('esquema del ambiente', () => {
  test('acepta los datos del modal de Figma y los normaliza', () => {
    const resultado = ambienteSchema.parse({
      ...valido,
      nombre: '  Laboratorio   de robótica ',
      aforoMaximo: ' 25 ',
    });

    expect(aSolicitudAmbiente(resultado)).toEqual({
      tipo: 'ESPACIO_ESPECIAL',
      nombre: 'Laboratorio de robótica',
      aforoMaximo: 25,
      recursos: ['Kits de robótica', 'Proyector', '12 laptops'],
    });
  });

  test('la solicitud no lleva código', () => {
    const solicitud = aSolicitudAmbiente(ambienteSchema.parse(valido));

    expect(solicitud).not.toHaveProperty('codigo');
  });

  test('el nombre es obligatorio, también con solo espacios', () => {
    expect(errores({ nombre: '' }).nombre).toBe(
      'Ingresa el nombre del ambiente.',
    );
    expect(errores({ nombre: '     ' }).nombre).toBe(
      'Ingresa el nombre del ambiente.',
    );
  });

  test('el nombre admite hasta 80 caracteres, contados ya sin espacios sobrantes', () => {
    expect(errores({ nombre: 'a'.repeat(80) }).nombre).toBeUndefined();
    expect(errores({ nombre: 'a'.repeat(81) }).nombre).toBe(
      'El nombre admite como máximo 80 caracteres.',
    );
    expect(
      errores({ nombre: `  ${'a'.repeat(40)}    ${'b'.repeat(39)}  ` }).nombre,
    ).toBeUndefined();
  });

  test('hay que elegir el tipo', () => {
    expect(errores({ tipo: '' }).tipo).toBe('Elige el tipo de ambiente.');
    expect(errores({ tipo: 'AULA' }).tipo).toBe('Elige el tipo de ambiente.');
  });

  test('el aforo es obligatorio y entero', () => {
    expect(errores({ aforoMaximo: '' }).aforoMaximo).toBe(
      'Ingresa el aforo máximo.',
    );
    expect(errores({ aforoMaximo: 'veinte' }).aforoMaximo).toBe(
      'Ingresa el aforo como un número entero.',
    );
    expect(errores({ aforoMaximo: '30.5' }).aforoMaximo).toBe(
      'Ingresa el aforo como un número entero.',
    );
  });

  test('el aforo debe ser mayor que 0', () => {
    expect(errores({ aforoMaximo: '0' }).aforoMaximo).toBe(
      'El aforo debe ser mayor que 0.',
    );
    expect(errores({ aforoMaximo: '-5' }).aforoMaximo).toBe(
      'El aforo debe ser mayor que 0.',
    );
  });

  test('el aforo cabe en la columna (hasta 32767)', () => {
    expect(
      errores({ tipo: 'ESPACIO_ESPECIAL', aforoMaximo: '32767' }).aforoMaximo,
    ).toBeUndefined();
    expect(
      errores({ tipo: 'ESPACIO_ESPECIAL', aforoMaximo: '32768' }).aforoMaximo,
    ).toBe('El aforo no puede ser mayor que 32767.');
  });

  test('un aula regular admite como máximo 45, con el texto de Figma', () => {
    expect(
      errores({ tipo: 'AULA_REGULAR', aforoMaximo: '45' }).aforoMaximo,
    ).toBeUndefined();
    expect(
      errores({ tipo: 'AULA_REGULAR', aforoMaximo: '46' }).aforoMaximo,
    ).toBe('Un aula regular admite como máximo 45.');
  });

  test('un espacio especial puede pasar de 45', () => {
    expect(
      errores({ tipo: 'ESPACIO_ESPECIAL', aforoMaximo: '180' }).aforoMaximo,
    ).toBeUndefined();
  });

  test('cada recurso admite hasta 60 caracteres', () => {
    expect(errores({ recursos: 'r'.repeat(60) }).recursos).toBeUndefined();
    expect(errores({ recursos: `Pizarra, ${'r'.repeat(61)}` }).recursos).toBe(
      'Cada recurso admite como máximo 60 caracteres.',
    );
  });

  test('los recursos son opcionales', () => {
    const resultado = ambienteSchema.parse({ ...valido, recursos: '' });

    expect(aSolicitudAmbiente(resultado).recursos).toEqual([]);
  });
});

describe('campoDelErrorDelBackend', () => {
  test('el 409 de nombre repetido es del campo Nombre', () => {
    expect(
      campoDelErrorDelBackend(409, 'Ya existe un ambiente llamado "Aula 101".'),
    ).toBe('nombre');
  });

  test('el 409 de aforo bajo la matrícula (RF-111) es del campo Aforo máximo', () => {
    expect(
      campoDelErrorDelBackend(
        409,
        'No puedes bajar el aforo a 2: la sección A de 1.° de secundaria tiene 3 estudiantes matriculados.',
      ),
    ).toBe('aforoMaximo');
  });

  test('cualquier otro error no es de un campo', () => {
    expect(
      campoDelErrorDelBackend(
        409,
        'No pudimos registrar el ambiente. Inténtalo de nuevo.',
      ),
    ).toBeNull();
    expect(campoDelErrorDelBackend(500, 'Error interno')).toBeNull();
    expect(
      campoDelErrorDelBackend(400, 'Ya existe un ambiente llamado "X".'),
    ).toBeNull();
    expect(campoDelErrorDelBackend(409, undefined)).toBeNull();
    expect(campoDelErrorDelBackend(undefined, undefined)).toBeNull();
  });
});
