import {
  crearPeriodoMatriculaSchema,
  solicitudDePeriodo,
  totalDeVacantes,
  validarDocumento,
} from '../esquemas';

// Reglas del formulario "Nuevo periodo de matrícula" (HU-23): las mismas del backend, con sus mismos mensajes.

const secciones = [
  { seccionId: 's1', etiqueta: '1.° A', alumnosOcupanVacante: 0 },
  { seccionId: 's2', etiqueta: '1.° B', alumnosOcupanVacante: 2 },
];
const schema = crearPeriodoMatriculaSchema(secciones);

const valido = {
  anioEscolarId: 'anio-2027',
  fechaInicio: '2026-12-01',
  horaInicio: '10:00',
  fechaCierre: '2027-02-15',
  horaCierre: '23:59',
  vacantes: [
    { seccionId: 's1', capacidad: '30' },
    { seccionId: 's2', capacidad: '30' },
  ],
};

/** Un mensaje por campo, indexado por su ruta ("fechaCierre", "vacantes.1.capacidad"). */
const errores = (cambios: Record<string, unknown>) => {
  const resultado = schema.safeParse({ ...valido, ...cambios });
  return Object.fromEntries(
    (resultado.success ? [] : resultado.error.issues).map((i) => [
      i.path.join('.'),
      i.message,
    ]),
  );
};

describe('año, fechas y horas', () => {
  test('acepta un periodo completo', () => {
    expect(schema.safeParse(valido).success).toBe(true);
  });

  test('hay que elegir el año académico', () => {
    expect(errores({ anioEscolarId: '' })).toEqual({
      anioEscolarId: 'Elige el año académico.',
    });
  });

  test('sin fecha, el aviso va en la fecha; con fecha pero sin hora, en la hora', () => {
    expect(errores({ fechaInicio: '' })).toEqual({
      fechaInicio: 'Ingresa la fecha y la hora de inicio.',
    });
    expect(errores({ horaInicio: '' })).toEqual({
      horaInicio: 'Ingresa la fecha y la hora de inicio.',
    });
    expect(errores({ fechaCierre: '' })).toEqual({
      fechaCierre: 'Ingresa la fecha y la hora de cierre.',
    });
    expect(errores({ horaCierre: '' })).toEqual({
      horaCierre: 'Ingresa la fecha y la hora de cierre.',
    });
  });

  test('el cierre debe ser posterior al inicio, contando la hora', () => {
    const mensaje = 'La fecha de cierre debe ser posterior a la de inicio.';
    expect(errores({ fechaCierre: '2026-11-30' })).toEqual({
      fechaCierre: mensaje,
    });
    // El mismo día, a la misma hora o antes, tampoco.
    expect(errores({ fechaCierre: '2026-12-01', horaCierre: '10:00' })).toEqual(
      { fechaCierre: mensaje },
    );
    expect(errores({ fechaCierre: '2026-12-01', horaCierre: '09:30' })).toEqual(
      { fechaCierre: mensaje },
    );
    // El mismo día pero más tarde sí.
    expect(errores({ fechaCierre: '2026-12-01', horaCierre: '10:30' })).toEqual(
      {},
    );
  });

  test('no pide el orden de las fechas mientras falte alguna', () => {
    expect(errores({ fechaCierre: '2026-11-01', horaInicio: '' })).toEqual({
      horaInicio: 'Ingresa la fecha y la hora de inicio.',
    });
  });
});

describe('vacantes', () => {
  const conCapacidad = (primera: string, segunda = '30') => ({
    vacantes: [
      { seccionId: 's1', capacidad: primera },
      { seccionId: 's2', capacidad: segunda },
    ],
  });

  test('son obligatorias', () => {
    expect(errores(conCapacidad(''))).toEqual({
      'vacantes.0.capacidad': 'Ingresa las vacantes de la sección 1.° A.',
    });
    expect(errores(conCapacidad('  '))).toEqual({
      'vacantes.0.capacidad': 'Ingresa las vacantes de la sección 1.° A.',
    });
  });

  test('van de 1 a 45 y son un número entero', () => {
    const mensaje =
      'Las vacantes de la sección 1.° A deben estar entre 1 y 45.';
    for (const capacidad of ['0', '46', '-3', '2.5', 'abc']) {
      expect(errores(conCapacidad(capacidad))).toEqual({
        'vacantes.0.capacidad': mensaje,
      });
    }
    expect(errores(conCapacidad('1'))).toEqual({});
    expect(errores(conCapacidad('45'))).toEqual({});
  });

  test('no pueden ser menos que los alumnos que ya ocupan vacante', () => {
    expect(errores(conCapacidad('30', '1'))).toEqual({
      'vacantes.1.capacidad':
        'Las vacantes de la sección 1.° B no pueden ser menos que los alumnos que ocupan vacante (2).',
    });
    expect(errores(conCapacidad('30', '2'))).toEqual({});
  });

  test('cada sección muestra su propio error', () => {
    expect(errores(conCapacidad('', '99'))).toEqual({
      'vacantes.0.capacidad': 'Ingresa las vacantes de la sección 1.° A.',
      'vacantes.1.capacidad':
        'Las vacantes de la sección 1.° B deben estar entre 1 y 45.',
    });
  });
});

test('totalDeVacantes suma lo escrito y no cuenta lo que aún no es un número', () => {
  expect(
    totalDeVacantes([
      { seccionId: 'a', capacidad: '30' },
      { seccionId: 'b', capacidad: '25' },
    ]),
  ).toBe(55);
  expect(
    totalDeVacantes([
      { seccionId: 'a', capacidad: '30' },
      { seccionId: 'b', capacidad: '' },
      { seccionId: 'c', capacidad: 'x' },
      { seccionId: 'd', capacidad: '4.5' },
    ]),
  ).toBe(30);
  expect(totalDeVacantes([])).toBe(0);
});

describe('validarDocumento', () => {
  test('el nombre es obligatorio y de hasta 100 caracteres', () => {
    expect(validarDocumento({ nombre: '  ', descripcion: '' }, [])).toEqual({
      nombre: 'Ingresa el nombre del documento.',
    });
    expect(
      validarDocumento({ nombre: 'x'.repeat(101), descripcion: '' }, []),
    ).toEqual({
      nombre: 'El nombre del documento no puede pasar de 100 caracteres.',
    });
    expect(
      validarDocumento({ nombre: 'x'.repeat(100), descripcion: '' }, []),
    ).toEqual({});
  });

  test('el nombre no se repite, sin distinguir mayúsculas ni espacios de los bordes', () => {
    expect(
      validarDocumento({ nombre: ' dni del estudiante ', descripcion: '' }, [
        'DNI del estudiante',
      ]),
    ).toEqual({
      nombre:
        'Ya hay un documento llamado "dni del estudiante" en este periodo.',
    });
  });

  test('la descripción es opcional y de hasta 100 caracteres', () => {
    expect(validarDocumento({ nombre: 'DNI', descripcion: '' }, [])).toEqual(
      {},
    );
    expect(
      validarDocumento({ nombre: 'DNI', descripcion: 'x'.repeat(100) }, []),
    ).toEqual({});
    expect(
      validarDocumento({ nombre: 'DNI', descripcion: 'x'.repeat(101) }, []),
    ).toEqual({
      descripcion:
        'La descripción del documento no puede pasar de 100 caracteres.',
    });
  });
});

test('solicitudDePeriodo arma el cuerpo del POST: vacantes en número y documentos sin la plantilla', () => {
  const archivo = new File(['a'], 'ficha.csv');

  const solicitud = solicitudDePeriodo(valido, [
    {
      clave: 'nuevo-1',
      nombre: ' Ficha de matrícula ',
      descripcion: ' Con firma ',
      archivo,
    },
    { clave: 'nuevo-2', nombre: 'DNI', descripcion: '  ', archivo: null },
    {
      clave: 'doc-3',
      id: 'doc-3',
      nombre: 'Foto',
      descripcion: '',
      archivo: null,
    },
  ]);

  expect(solicitud).toEqual({
    anioEscolarId: 'anio-2027',
    fechaInicio: '2026-12-01',
    horaInicio: '10:00',
    fechaCierre: '2027-02-15',
    horaCierre: '23:59',
    vacantes: [
      { seccionId: 's1', capacidad: 30 },
      { seccionId: 's2', capacidad: 30 },
    ],
    documentos: [
      { id: null, nombre: 'Ficha de matrícula', descripcion: 'Con firma' },
      { id: null, nombre: 'DNI', descripcion: null },
      { id: 'doc-3', nombre: 'Foto', descripcion: null },
    ],
  });
});
