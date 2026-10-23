import {
  InscripcionForm,
  aSolicitud,
  gradoSugerido,
  inscripcionSchema,
  inscripcionVacia,
  registra,
  validarDocumento,
} from '../esquema';

/** Inscripción válida: estudiante, padre ya registrado y madre nueva. */
const valida = (): InscripcionForm => {
  const f = inscripcionVacia();
  f.estudiante.persona = {
    ...f.estudiante.persona,
    tipoDocumento: 'DNI',
    nacionalidad: 'Peruana',
    numeroDocumento: '78945612',
    apellidoPaterno: 'Quispe',
    apellidoMaterno: 'Rojas',
    nombres: 'Mateo',
    sexo: 'MASCULINO',
    fechaNacimiento: '2015-05-10',
    direccion: 'Av. La Marina 1234',
    distrito: 'San Miguel',
  };
  f.estudiante.paisNacimiento = 'Perú';
  f.estudiante.departamentoNacimiento = 'Lima';
  f.estudiante.provinciaNacimiento = 'Lima';
  f.estudiante.distritoNacimiento = 'San Miguel';
  f.estudiante.lenguaMaterna = 'Castellano';
  f.estudiante.formaIngreso = 'Edad normativa';
  f.estudiante.anioAcademico = '2027';
  f.estudiante.tieneInternet = 'SI';
  f.estudiante.colegioProcedencia = 'I.E.I. Mis Pequeños Pasos';
  f.estudiante.gestionProcedencia = 'Privada';
  f.estudiante.nivel = 'PRIMARIA';
  f.estudiante.grado = 'PRI-6';
  f.fichaMedica = {
    ...f.fichaMedica,
    tipoSangre: 'O+',
    pesoKg: '22',
    estaturaM: '1.18',
    tipoSeguro: 'EsSalud',
    fiebreMaxima: '39',
  };
  f.padre.personaId = 'id-del-padre';
  f.padre.persona.numeroDocumento = '41234567';
  f.madre.persona = {
    ...f.madre.persona,
    numeroDocumento: '42345678',
    apellidoPaterno: 'Rojas',
    nombres: 'Carla',
    correo: 'carla@gmail.com',
  };
  f.contactos = [
    {
      nombreCompleto: 'Rosa',
      parentesco: 'Abuela',
      telefonoFijo: '',
      celular: '987 654 321',
    },
    {
      nombreCompleto: 'Pedro',
      parentesco: 'Tío',
      telefonoFijo: '',
      celular: '',
    },
  ];
  f.responsable = 'MADRE';
  f.verificado = true;
  return f;
};

const mensajes = (f: InscripcionForm) => {
  const r = inscripcionSchema.safeParse(f);
  return r.success ? [] : r.error.issues.map((i) => i.message);
};

describe('gradoSugerido (edad al 31 de marzo del año académico)', () => {
  test.each([
    ['2021-02-14', 2027, 'PRIMARIA', 'PRI-1'],
    ['2014-01-01', 2027, 'SECUNDARIA', 'SEC-2'],
  ])('nacido el %s para %i → %s %s', (fecha, anio, nivel, grado) => {
    expect(gradoSugerido(fecha, anio)).toMatchObject({ nivel, grado });
  });

  test('sin fecha, en edad de inicial o fuera de rango no sugiere nada', () => {
    expect(gradoSugerido('2021-04-01', 2027)).toBeNull();
    expect(gradoSugerido('', 2027)).toBeNull();
    expect(gradoSugerido('2025-01-01', 2027)).toBeNull();
  });
});

describe('inscripcionSchema (reglas de Figma)', () => {
  test('una inscripción completa es válida', () => {
    expect(mensajes(valida())).toEqual([]);
  });

  test('ningún campo del Paso 1 trae valor por defecto, salvo la discapacidad (No)', () => {
    const e = inscripcionVacia().estudiante;
    const { tieneDiscapacidad, ...resto } = e;
    expect(tieneDiscapacidad).toBe('NO');
    const conValor = Object.entries({ ...resto, ...resto.persona })
      .filter(([k, v]) => typeof v === 'string' && v !== '' && k !== 'persona')
      .map(([k]) => k);
    expect(conValor).toEqual([]);
  });

  test('si nace fuera del Perú no se piden departamento, provincia ni distrito', () => {
    const f = valida();
    Object.assign(f.estudiante, {
      paisNacimiento: 'Chile',
      departamentoNacimiento: '',
      provinciaNacimiento: '',
      distritoNacimiento: '',
    });
    expect(mensajes(f)).toEqual([]);
    expect(aSolicitud(f).estudiante).toMatchObject({
      paisNacimiento: 'Chile',
      departamentoNacimiento: null,
      provinciaNacimiento: null,
      distritoNacimiento: null,
    });
  });

  test('si nace en el Perú, departamento, provincia y distrito son obligatorios', () => {
    const f = valida();
    Object.assign(f.estudiante, {
      departamentoNacimiento: '',
      provinciaNacimiento: '',
      distritoNacimiento: '',
    });
    expect(mensajes(f)).toEqual(
      expect.arrayContaining([
        'Selecciona el departamento',
        'Selecciona la provincia',
        'Selecciona el distrito',
      ]),
    );
  });

  test('el tipo de documento vacío no oculta los demás errores del paso', () => {
    const m = mensajes(inscripcionVacia());
    expect(m).toContain('Selecciona el tipo de documento');
    expect(m).toContain('Ingresa el apellido paterno');
    expect(m).toContain('Selecciona el país');
    expect(m).toContain('Selecciona la nacionalidad');
    expect(m).toContain('Selecciona la lengua materna');
    expect(m).toContain('Selecciona el año académico');
    expect(m).toContain('Selecciona la forma de ingreso');
  });

  test('el Paso 1 exige todos los campos salvo correo, referencia, teléfono, segunda lengua y etnia', () => {
    const f = inscripcionVacia();
    const m = mensajes(f);
    [
      'Ingresa el número de documento',
      'Ingresa el apellido paterno',
      'Ingresa los nombres',
      'Selecciona el sexo',
      'Ingresa la fecha de nacimiento',
      'Ingresa la dirección',
      'Selecciona el distrito',
      'Selecciona si tiene acceso a internet / dispositivo',
      'Ingresa el colegio de procedencia',
      'Selecciona la gestión',
      'Indica el nivel',
      'Indica el grado',
    ].forEach((mensaje) => expect(m).toContain(mensaje));
  });

  test('apellido materno, referencia, teléfono de casa, correo, segunda lengua y etnia son opcionales', () => {
    const f = valida();
    f.estudiante.persona.apellidoMaterno = '';
    Object.assign(f.estudiante, {
      referenciaDomicilio: '',
      segundaLengua: '',
      autoidentificacionEtnica: '',
    });
    Object.assign(f.estudiante.persona, { telefonoCasa: '', correo: '' });
    expect(mensajes(f)).toEqual([]);
  });

  test('si tiene discapacidad, "¿Cuál?" es obligatorio', () => {
    const f = valida();
    f.estudiante.tieneDiscapacidad = 'SI';
    f.estudiante.discapacidadDetalle = '';
    expect(mensajes(f)).toContain('Indica cuál es la discapacidad');
    f.estudiante.discapacidadDetalle = 'Visual';
    expect(mensajes(f)).toEqual([]);
  });

  test('la fecha de nacimiento debe ser pasada', () => {
    const f = valida();
    f.estudiante.persona.fechaNacimiento = '2999-01-01';
    expect(mensajes(f)).toContain('La fecha de nacimiento debe ser pasada');
  });

  test('sin padre ni madre exige un apoderado', () => {
    const f = valida();
    f.padre = { ...f.padre, noRegistra: true, motivo: 'Fallecido(a)' };
    f.madre = { ...f.madre, noRegistra: true, motivo: 'No registra' };
    expect(mensajes(f)).toContain(
      'No hay padre ni madre registrados: debes registrar un apoderado.',
    );
  });

  test('si el padre no registra, pide el motivo', () => {
    const f = valida();
    f.padre = { ...f.padre, noRegistra: true, motivo: '' };
    expect(mensajes(f)).toContain('Indica el motivo');
  });

  test('exige al menos 2 contactos de emergencia', () => {
    const f = valida();
    f.contactos = f.contactos.slice(0, 1);
    expect(mensajes(f)).toContain(
      'Registra al menos 2 contactos de emergencia',
    );
  });
});

describe('Paso 2 · ficha médica', () => {
  test('exige tipo de sangre, peso, estatura, seguro y fiebre máxima', () => {
    const f = valida();
    f.fichaMedica = inscripcionVacia().fichaMedica;
    expect(mensajes(f)).toEqual([
      'Selecciona el tipo de sangre',
      'Ingresa el peso',
      'Ingresa la estatura',
      'Selecciona el tipo de seguro',
      'Indica hasta cuántos grados de fiebre ha llegado',
    ]);
  });

  test.each([
    ['pesoKg', '4.9', 'El peso debe estar entre 5 y 200 kg'],
    ['pesoKg', '200.5', 'El peso debe estar entre 5 y 200 kg'],
    ['pesoKg', 'veinte', 'El peso debe estar entre 5 y 200 kg'],
    ['estaturaM', '118', 'La estatura debe estar entre 0.50 y 2.50 m'],
    ['estaturaM', '0.4', 'La estatura debe estar entre 0.50 y 2.50 m'],
    ['fiebreMaxima', 'mucha', 'Ingresa un número (ej. 39 o 39.5)'],
    ['fiebreMaxima', '39.55', 'Ingresa un número (ej. 39 o 39.5)'],
  ] as const)('%s = "%s" → %s', (campo, valor, mensaje) => {
    const f = valida();
    f.fichaMedica[campo] = valor;
    expect(mensajes(f)).toEqual([mensaje]);
  });

  test('acepta los extremos de cada rango con el formato ###.# y #.##', () => {
    const f = valida();
    f.fichaMedica.pesoKg = '200.0';
    f.fichaMedica.estaturaM = '0.50';
    f.fichaMedica.fiebreMaxima = '39.5';
    expect(mensajes(f)).toEqual([]);
    const s = aSolicitud(f).fichaMedica;
    expect(s).toMatchObject({
      pesoKg: 200,
      estaturaM: 0.5,
      fiebreMaxima: '39.5',
    });
  });

  test.each([
    ['pesoKg', '22,5', 'El peso debe estar entre 5 y 200 kg'],
    ['pesoKg', '22.55', 'El peso debe estar entre 5 y 200 kg'],
    ['estaturaM', '1,18', 'La estatura debe estar entre 0.50 y 2.50 m'],
    ['estaturaM', '1.185', 'La estatura debe estar entre 0.50 y 2.50 m'],
    ['fiebreMaxima', '39,5', 'Ingresa un número (ej. 39 o 39.5)'],
  ] as const)(
    'el separador decimal es el punto: %s = "%s" no vale',
    (campo, valor, mensaje) => {
      const f = valida();
      f.fichaMedica[campo] = valor;
      expect(mensajes(f)).toEqual([mensaje]);
    },
  );

  test('con alergias, al menos una de las dos', () => {
    const f = valida();
    f.fichaMedica.tieneAlergias = 'SI';
    expect(mensajes(f)).toEqual([
      'Indica al menos una alergia (alimentaria o medicamentosa)',
    ]);
    f.fichaMedica.alergiasMedicamentosas = 'Penicilina';
    expect(mensajes(f)).toEqual([]);
  });

  test('si padece una enfermedad o toma un medicamento, "¿Cuál?" es obligatorio', () => {
    const f = valida();
    f.fichaMedica.tieneEnfermedad = 'SI';
    f.fichaMedica.tomaMedicamento = 'SI';
    expect(mensajes(f)).toEqual([
      'Indica cuál es la enfermedad',
      'Indica cuál es el medicamento',
    ]);
  });

  test('los detalles de una respuesta "No" no se envían', () => {
    const f = valida();
    f.fichaMedica.alergiasAlimentarias = 'Maní';
    f.fichaMedica.enfermedadDetalle = 'Asma';
    f.fichaMedica.medicamentoDetalle = 'Salbutamol';
    expect(aSolicitud(f).fichaMedica).toMatchObject({
      alergiasAlimentarias: null,
      enfermedadDetalle: null,
      medicamentoDetalle: null,
    });
  });
});

describe('aSolicitud (formulario → contrato del backend)', () => {
  test('una persona ya registrada solo envía su id', () => {
    const s = aSolicitud(valida());
    expect(s.padre).toMatchObject({ personaId: 'id-del-padre', persona: null });
    expect(s.madre.persona?.numeroDocumento).toBe('42345678');
    expect(s.apoderado).toBeNull();
  });

  test('normaliza celulares, números y respuestas Sí/No', () => {
    const f = valida();
    f.fichaMedica.pesoKg = '22.5';
    f.fichaMedica.tieneAlergias = 'SI';
    const s = aSolicitud(f);
    expect(s.contactos[0].celular).toBe('987654321');
    expect(s.fichaMedica.pesoKg).toBe(22.5);
    expect(s.fichaMedica.tieneAlergias).toBe(true);
    expect(s.estudiante.anioAcademico).toBe(Number(f.estudiante.anioAcademico));
    expect(s.estudiante.gradoCodigo).toBe('PRI-6');
  });

  test('"no registra" se envía con su motivo y sin persona', () => {
    const f = valida();
    f.padre = {
      ...f.padre,
      personaId: null,
      noRegistra: true,
      motivo: 'Fallecido(a)',
    };
    expect(registra(f.padre)).toBe(false);
    expect(aSolicitud(f).padre).toEqual({
      noRegistra: true,
      motivo: 'Fallecido(a)',
      observacion: null,
    });
  });
});

describe('documento del apoderado', () => {
  const archivo = (tipo: string, bytes: number) =>
    new File([new Uint8Array(bytes)], 'carta.pdf', { type: tipo });

  test('acepta PDF, JPG o PNG de hasta 5 MB', () => {
    expect(validarDocumento(archivo('application/pdf', 1024))).toBeNull();
    expect(validarDocumento(archivo('image/png', 1024))).toBeNull();
  });

  test('rechaza otros formatos y archivos de más de 5 MB', () => {
    expect(validarDocumento(archivo('application/zip', 10))).toBe(
      'El documento debe ser PDF, JPG o PNG',
    );
    expect(
      validarDocumento(archivo('application/pdf', 5 * 1024 * 1024 + 1)),
    ).toBe('El documento no puede pesar más de 5 MB');
  });

  test('si la relación es "Otro", se envía el texto de "Especificar"', () => {
    const f = valida();
    f.incluirApoderado = true;
    f.apoderado = {
      ...f.apoderado,
      personaId: 'id-apoderado',
      relacion: 'Otro',
      relacionDetalle: 'Vecina',
    };
    expect(aSolicitud(f).apoderado).toMatchObject({ relacion: 'Vecina' });
  });
});
