import {
  MENSAJE_BLOQUES,
  MENSAJE_CODIGO_INVALIDO,
  MENSAJE_DURACION_BLOQUE,
  MENSAJE_DURACION_RECESO,
  MENSAJE_RECESO_REPETIDO,
  MENSAJE_SIN_LUGAR_PARA_RECESO,
  ValoresEstructura,
  aSolicitudEdicion,
  aSolicitudEstructura,
  aSolicitudVistaPrevia,
  estructuraSchema,
  mensajePosicionDeReceso,
  normalizarCodigo,
  puedeCalcularseLaJornada,
  valoresDeEstructura,
  valoresInicialesEstructura,
} from '../esquemas-estructura';

// Figma > "Modal - Nueva estructura horaria" (HU-44): reglas y mensajes del backend.

// La estructura de ejemplo del modal de Figma: 14:45, 4 bloques de 45 min y un receso de 15 después del bloque 2.
const valida: ValoresEstructura = {
  codigo: 'EH-TAR-26',
  descripcion: 'Secundaria · talleres de tarde',
  nivel: 'SECUNDARIA',
  esPrincipal: false,
  diasLectivos: ['LUN', 'MIE', 'VIE'],
  horaEntrada: '14:45',
  bloquesPorDia: '4',
  duracionBloqueMin: '45',
  recesos: [{ despuesDeBloque: '2', duracionMin: '15' }],
};

type Camino = (string | number)[];

/** Todos los mensajes de un campo (o de un receso), en el orden en que el esquema los reporta. */
const mensajesDe = (camino: Camino, cambios: Partial<ValoresEstructura>) => {
  const resultado = estructuraSchema.safeParse({ ...valida, ...cambios });
  return resultado.success
    ? []
    : resultado.error.issues
        .filter((problema) => camino.every((c, i) => problema.path[i] === c))
        .map((problema) => problema.message);
};

test('los datos del modal de Figma pasan sin mensajes', () => {
  expect(estructuraSchema.safeParse(valida).success).toBe(true);
});

test('una estructura sin descripción ni recesos también es válida', () => {
  expect(
    estructuraSchema.safeParse({ ...valida, descripcion: '', recesos: [] })
      .success,
  ).toBe(true);
});

describe('código', () => {
  test.each(['', '   '])(
    'vacío o de solo espacios pide el código (%j)',
    (codigo) => {
      expect(mensajesDe(['codigo'], { codigo })).toEqual([
        'Ingresa el código de la estructura.',
      ]);
    },
  );

  test.each(['EH-PRI-26', 'A', 'A1', '123', '-', ' EH-1 ', 'A'.repeat(20)])(
    'acepta mayúsculas, números y guion hasta 20 caracteres (%j)',
    (codigo) => {
      expect(mensajesDe(['codigo'], { codigo })).toEqual([]);
    },
  );

  test('admite hasta 20 caracteres', () => {
    expect(mensajesDe(['codigo'], { codigo: 'A'.repeat(21) })).toEqual([
      'El código admite hasta 20 caracteres.',
    ]);
  });

  test.each([
    'eh-pri',
    'EH PRI',
    'EH_PRI',
    'EH.PRI',
    'ÑU',
    'ÁB',
    'EH/1',
    'EH-1!',
  ])('solo admite letras sin tilde, números y guion (%j)', (codigo) => {
    expect(mensajesDe(['codigo'], { codigo })).toEqual([
      MENSAJE_CODIGO_INVALIDO,
    ]);
  });

  test('con un código largo y con signos se muestra primero el aviso del largo', () => {
    expect(mensajesDe(['codigo'], { codigo: `${'A'.repeat(21)}!` })[0]).toBe(
      'El código admite hasta 20 caracteres.',
    );
  });

  test('el código se pasa a mayúsculas al recibir lo escrito, sin quitar espacios', () => {
    expect(normalizarCodigo('eh-pri-26')).toBe('EH-PRI-26');
    expect(normalizarCodigo('a b')).toBe('A B');
    expect(normalizarCodigo('')).toBe('');
  });
});

describe('descripción', () => {
  test('es opcional', () => {
    expect(mensajesDe(['descripcion'], { descripcion: '' })).toEqual([]);
  });

  test('admite hasta 100 caracteres', () => {
    expect(
      mensajesDe(['descripcion'], { descripcion: 'a'.repeat(100) }),
    ).toEqual([]);
    expect(
      mensajesDe(['descripcion'], { descripcion: 'a'.repeat(101) }),
    ).toEqual(['La descripción admite hasta 100 caracteres.']);
  });
});

describe('nivel', () => {
  test.each(['PRIMARIA', 'SECUNDARIA'] as const)('acepta %s', (nivel) => {
    expect(mensajesDe(['nivel'], { nivel })).toEqual([]);
  });

  test('sin elegir pide el nivel', () => {
    expect(mensajesDe(['nivel'], { nivel: '' })).toEqual([
      'Elige el nivel: primaria o secundaria.',
    ]);
  });
});

describe('días lectivos', () => {
  test('al menos uno', () => {
    expect(mensajesDe(['diasLectivos'], { diasLectivos: [] })).toEqual([
      'Elige al menos un día lectivo.',
    ]);
  });

  test.each([
    [['LUN']],
    [['SAB']],
    [['LUN', 'MAR', 'MIE', 'JUE', 'VIE', 'SAB']],
  ] as const)('acepta %j', (diasLectivos) => {
    expect(
      mensajesDe(['diasLectivos'], { diasLectivos: [...diasLectivos] }),
    ).toEqual([]);
  });
});

describe('hora de entrada', () => {
  test.each(['', '7:45', '25:00', '14:60', 'abc', '14:45:30'])(
    'una hora vacía o mal escrita pide la hora de entrada (%j)',
    (horaEntrada) => {
      expect(mensajesDe(['horaEntrada'], { horaEntrada })).toEqual([
        'Ingresa la hora de entrada.',
      ]);
    },
  );

  test.each(['00:00', '07:45', '14:45', '23:59'])(
    'acepta %s',
    (horaEntrada) => {
      expect(mensajesDe(['horaEntrada'], { horaEntrada })).toEqual([]);
    },
  );
});

describe('bloques por día', () => {
  test.each(['1', '8', '12', ' 4 '])(
    'acepta enteros de 1 a 12 (%j)',
    (bloquesPorDia) => {
      expect(mensajesDe(['bloquesPorDia'], { bloquesPorDia })).toEqual([]);
    },
  );

  test.each(['', '0', '13', '-1', '4.5', '4,5', 'abc', '1e1'])(
    'fuera de 1 a 12 o que no es un entero da el mensaje del backend (%j)',
    (bloquesPorDia) => {
      expect(mensajesDe(['bloquesPorDia'], { bloquesPorDia })).toEqual([
        MENSAJE_BLOQUES,
      ]);
    },
  );
});

describe('duración del bloque', () => {
  test.each(['20', '45', '120'])('acepta %s', (duracionBloqueMin) => {
    expect(mensajesDe(['duracionBloqueMin'], { duracionBloqueMin })).toEqual(
      [],
    );
  });

  test.each(['', '19', '121', '0', '45.5', 'abc'])(
    'fuera de 20 a 120 o que no es un entero da el mensaje del backend (%j)',
    (duracionBloqueMin) => {
      expect(mensajesDe(['duracionBloqueMin'], { duracionBloqueMin })).toEqual([
        MENSAJE_DURACION_BLOQUE,
      ]);
    },
  );
});

describe('recesos', () => {
  test.each(['5', '15', '60'])('la duración acepta %s', (duracionMin) => {
    expect(
      mensajesDe(['recesos', 0, 'duracionMin'], {
        recesos: [{ despuesDeBloque: '2', duracionMin }],
      }),
    ).toEqual([]);
  });

  test.each(['', '4', '61', '0', '10.5', 'abc'])(
    'la duración fuera de 5 a 60 o que no es un entero da el mensaje del backend (%j)',
    (duracionMin) => {
      expect(
        mensajesDe(['recesos', 0, 'duracionMin'], {
          recesos: [{ despuesDeBloque: '2', duracionMin }],
        }),
      ).toEqual([MENSAJE_DURACION_RECESO]);
    },
  );

  test.each(['1', '2', '3'])(
    'con 4 bloques por día un receso puede ir después del bloque %s',
    (despuesDeBloque) => {
      expect(
        mensajesDe(['recesos', 0, 'despuesDeBloque'], {
          recesos: [{ despuesDeBloque, duracionMin: '15' }],
        }),
      ).toEqual([]);
    },
  );

  test.each(['', '0', '4', '5', '-1', '2.5', 'abc'])(
    'con 4 bloques por día un receso después del bloque %j da "entre 1 y 3"',
    (despuesDeBloque) => {
      expect(
        mensajesDe(['recesos', 0, 'despuesDeBloque'], {
          recesos: [{ despuesDeBloque, duracionMin: '15' }],
        }),
      ).toEqual([mensajePosicionDeReceso(4)]);
    },
  );

  test('el mensaje de la posición nombra el último bloque posible', () => {
    expect(mensajePosicionDeReceso(4)).toBe(
      'Un receso va después de un bloque entre 1 y 3.',
    );
    expect(mensajePosicionDeReceso(12)).toBe(
      'Un receso va después de un bloque entre 1 y 11.',
    );
  });

  test('con un solo bloque por día no hay dónde poner un receso', () => {
    expect(
      mensajesDe(['recesos', 0, 'despuesDeBloque'], {
        bloquesPorDia: '1',
        recesos: [{ despuesDeBloque: '1', duracionMin: '15' }],
      }),
    ).toEqual([MENSAJE_SIN_LUGAR_PARA_RECESO]);
  });

  test('con bloques por día inválidos no se avisa de la posición: ya avisa el campo de bloques', () => {
    expect(
      mensajesDe(['recesos', 0, 'despuesDeBloque'], {
        bloquesPorDia: '',
        recesos: [{ despuesDeBloque: '9', duracionMin: '15' }],
      }),
    ).toEqual([]);
    expect(mensajesDe(['bloquesPorDia'], { bloquesPorDia: '' })).toEqual([
      MENSAJE_BLOQUES,
    ]);
  });

  test('dos recesos después del mismo bloque se avisan en el segundo', () => {
    const recesos = [
      { despuesDeBloque: '2', duracionMin: '15' },
      { despuesDeBloque: '2', duracionMin: '10' },
    ];

    expect(mensajesDe(['recesos', 0, 'despuesDeBloque'], { recesos })).toEqual(
      [],
    );
    expect(mensajesDe(['recesos', 1, 'despuesDeBloque'], { recesos })).toEqual([
      MENSAJE_RECESO_REPETIDO,
    ]);
  });

  test('recesos en bloques distintos son válidos', () => {
    expect(
      estructuraSchema.safeParse({
        ...valida,
        recesos: [
          { despuesDeBloque: '1', duracionMin: '15' },
          { despuesDeBloque: '3', duracionMin: '10' },
        ],
      }).success,
    ).toBe(true);
  });

  test('cada receso avisa lo suyo: uno bien y otro mal', () => {
    const recesos = [
      { despuesDeBloque: '2', duracionMin: '15' },
      { despuesDeBloque: '9', duracionMin: '1' },
    ];

    expect(mensajesDe(['recesos', 0], { recesos })).toEqual([]);
    expect(mensajesDe(['recesos', 1, 'duracionMin'], { recesos })).toEqual([
      MENSAJE_DURACION_RECESO,
    ]);
    expect(mensajesDe(['recesos', 1, 'despuesDeBloque'], { recesos })).toEqual([
      mensajePosicionDeReceso(4),
    ]);
  });
});

test('un formulario vacío avisa cada campo obligatorio una sola vez', () => {
  const resultado = estructuraSchema.safeParse(valoresInicialesEstructura());

  expect(resultado.success).toBe(false);
  if (!resultado.success) {
    expect(resultado.error.issues.map((p) => p.path[0])).toEqual([
      'codigo',
      'nivel',
      'diasLectivos',
      'horaEntrada',
      'bloquesPorDia',
      'duracionBloqueMin',
    ]);
  }
});

test('al abrir el modal no hay nada precargado ni recesos', () => {
  expect(valoresInicialesEstructura()).toEqual({
    codigo: '',
    descripcion: '',
    nivel: '',
    esPrincipal: false,
    diasLectivos: [],
    horaEntrada: '',
    bloquesPorDia: '',
    duracionBloqueMin: '',
    recesos: [],
  });
});

describe('cuerpos que se envían', () => {
  test('crear recorta el código, convierte los números y ordena los días por la semana', () => {
    expect(
      aSolicitudEstructura({
        ...valida,
        codigo: ' EH-TAR-26 ',
        diasLectivos: ['VIE', 'LUN', 'MIE'],
      }),
    ).toEqual({
      codigo: 'EH-TAR-26',
      descripcion: 'Secundaria · talleres de tarde',
      nivel: 'SECUNDARIA',
      esPrincipal: false,
      diasLectivos: ['LUN', 'MIE', 'VIE'],
      horaEntrada: '14:45',
      bloquesPorDia: 4,
      duracionBloqueMin: 45,
      recesos: [{ despuesDeBloque: 2, duracionMin: 15 }],
    });
  });

  test('una descripción vacía se envía como null', () => {
    expect(
      aSolicitudEstructura({ ...valida, descripcion: '   ' }).descripcion,
    ).toBeNull();
  });

  test('editar no lleva el nivel y conserva la marca de principal', () => {
    const cuerpo = aSolicitudEdicion({ ...valida, esPrincipal: true });

    expect(cuerpo).not.toHaveProperty('nivel');
    expect(cuerpo).toMatchObject({
      codigo: 'EH-TAR-26',
      esPrincipal: true,
      bloquesPorDia: 4,
    });
  });

  test('la vista previa acepta el formulario completo con los mismos datos que crear', () => {
    expect(aSolicitudVistaPrevia(valida)).toEqual({
      codigo: 'EH-TAR-26',
      descripcion: 'Secundaria · talleres de tarde',
      nivel: 'SECUNDARIA',
      esPrincipal: false,
      diasLectivos: ['LUN', 'MIE', 'VIE'],
      horaEntrada: '14:45',
      bloquesPorDia: 4,
      duracionBloqueMin: 45,
      recesos: [{ despuesDeBloque: 2, duracionMin: 15 }],
    });
  });

  test('la vista previa con el formulario a medio llenar manda null en lo que falta', () => {
    expect(
      aSolicitudVistaPrevia({
        ...valoresInicialesEstructura(),
        horaEntrada: '14:45',
        bloquesPorDia: 'abc',
        recesos: [
          { despuesDeBloque: '', duracionMin: '' },
          { despuesDeBloque: '2', duracionMin: '' },
        ],
      }),
    ).toEqual({
      codigo: null,
      descripcion: null,
      nivel: null,
      esPrincipal: false,
      diasLectivos: [],
      horaEntrada: '14:45',
      bloquesPorDia: null,
      duracionBloqueMin: null,
      // Los recesos incompletos no se mandan: el backend respondería sin jornada y la vista previa desaparecería.
      recesos: [],
    });
  });

  describe('recesos en la vista previa: solo los completos y válidos', () => {
    const recesosEnviados = (
      bloquesPorDia: string,
      recesos: { despuesDeBloque: string; duracionMin: string }[],
    ) => aSolicitudVistaPrevia({ ...valida, bloquesPorDia, recesos }).recesos;

    test('un receso completo y válido se manda', () => {
      expect(
        recesosEnviados('4', [{ despuesDeBloque: '2', duracionMin: '15' }]),
      ).toEqual([{ despuesDeBloque: 2, duracionMin: 15 }]);
    });

    test.each([
      ['una fila vacía', { despuesDeBloque: '', duracionMin: '' }],
      ['sin duración', { despuesDeBloque: '2', duracionMin: '' }],
      ['sin posición', { despuesDeBloque: '', duracionMin: '15' }],
      [
        'con una duración menor a 5',
        { despuesDeBloque: '2', duracionMin: '4' },
      ],
      [
        'con una duración mayor a 60',
        { despuesDeBloque: '2', duracionMin: '61' },
      ],
      [
        'después del último bloque',
        { despuesDeBloque: '4', duracionMin: '15' },
      ],
      ['después del bloque 0', { despuesDeBloque: '0', duracionMin: '15' }],
      [
        'con texto en vez de número',
        { despuesDeBloque: 'dos', duracionMin: '15' },
      ],
    ])('un receso %s no se manda', (_, receso) => {
      expect(recesosEnviados('4', [receso])).toEqual([]);
    });

    test('sin bloques por día válidos no hay dónde poner recesos', () => {
      expect(
        recesosEnviados('', [{ despuesDeBloque: '2', duracionMin: '15' }]),
      ).toEqual([]);
    });

    test('agregar una fila vacía no cambia lo que se manda', () => {
      const completo = { despuesDeBloque: '2', duracionMin: '15' };
      const vacio = { despuesDeBloque: '', duracionMin: '' };

      expect(recesosEnviados('4', [completo, vacio])).toEqual(
        recesosEnviados('4', [completo]),
      );
    });

    test('con dos recesos después del mismo bloque solo se manda el primero', () => {
      expect(
        recesosEnviados('4', [
          { despuesDeBloque: '2', duracionMin: '15' },
          { despuesDeBloque: '2', duracionMin: '10' },
        ]),
      ).toEqual([{ despuesDeBloque: 2, duracionMin: 15 }]);
    });
  });

  describe('puedeCalcularseLaJornada', () => {
    const solicitud = aSolicitudVistaPrevia(valida);

    test('con hora, bloques y duración válidos', () => {
      expect(puedeCalcularseLaJornada(solicitud)).toBe(true);
    });

    test.each([
      ['sin hora', { horaEntrada: null }],
      ['sin bloques', { bloquesPorDia: null }],
      ['con 0 bloques', { bloquesPorDia: 0 }],
      ['con 13 bloques', { bloquesPorDia: 13 }],
      ['sin duración', { duracionBloqueMin: null }],
      ['con una duración menor a 20', { duracionBloqueMin: 19 }],
      ['con una duración mayor a 120', { duracionBloqueMin: 121 }],
    ])('no, %s', (_, cambios) => {
      expect(puedeCalcularseLaJornada({ ...solicitud, ...cambios })).toBe(
        false,
      );
    });

    test('el código, el nivel y los días no hacen falta para calcular', () => {
      expect(
        puedeCalcularseLaJornada({
          ...solicitud,
          codigo: null,
          nivel: null,
          diasLectivos: [],
        }),
      ).toBe(true);
    });
  });

  test('una hora mal escrita no se manda en la vista previa', () => {
    expect(
      aSolicitudVistaPrevia({ ...valida, horaEntrada: '7:4' }).horaEntrada,
    ).toBeNull();
  });
});

describe('valoresDeEstructura: abrir el modal en modo edición', () => {
  const guardada = {
    codigo: 'EH-PRI-26',
    descripcion: null,
    nivel: 'PRIMARIA' as const,
    esPrincipal: true,
    diasLectivos: ['VIE', 'LUN', 'MAR'] as ('LUN' | 'MAR' | 'VIE')[],
    horaEntrada: '07:45',
    bloquesPorDia: 8,
    duracionBloqueMin: 45,
    recesos: [
      { despuesDeBloque: 6, duracionMin: 15 },
      { despuesDeBloque: 3, duracionMin: 20 },
    ],
  };

  test('escribe todo como lo escribiría el usuario: textos, días en orden y recesos en orden', () => {
    expect(valoresDeEstructura(guardada)).toEqual({
      codigo: 'EH-PRI-26',
      descripcion: '',
      nivel: 'PRIMARIA',
      esPrincipal: true,
      diasLectivos: ['LUN', 'MAR', 'VIE'],
      horaEntrada: '07:45',
      bloquesPorDia: '8',
      duracionBloqueMin: '45',
      recesos: [
        { despuesDeBloque: '3', duracionMin: '20' },
        { despuesDeBloque: '6', duracionMin: '15' },
      ],
    });
  });

  test('los valores que abre el modal son válidos y vuelven al mismo cuerpo que se guardó', () => {
    const valores = valoresDeEstructura(guardada);

    expect(estructuraSchema.safeParse(valores).success).toBe(true);
    expect(aSolicitudEdicion(valores)).toEqual({
      codigo: 'EH-PRI-26',
      descripcion: null,
      esPrincipal: true,
      diasLectivos: ['LUN', 'MAR', 'VIE'],
      horaEntrada: '07:45',
      bloquesPorDia: 8,
      duracionBloqueMin: 45,
      recesos: [
        { despuesDeBloque: 3, duracionMin: 20 },
        { despuesDeBloque: 6, duracionMin: 15 },
      ],
    });
  });
});
