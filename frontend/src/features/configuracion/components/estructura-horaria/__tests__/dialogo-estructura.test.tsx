import { HttpResponse, delay, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  renderApp,
  screen,
  userEvent,
  waitFor,
  within,
} from '@/testing/test-utils';
import {
  EstructuraGuardada,
  SolicitudEstructuraHoraria,
  SolicitudVistaPrevia,
  VistaPreviaJornada,
} from '@/types/api';

import { DialogoEstructura } from '../dialogo-estructura';

import { detalleDe, secundaria } from './datos';

// Figma > "Modal - Nueva estructura horaria" (HU-44)

const API = env.API_URL;
const URL_ESTRUCTURAS = `${API}/configuracion/anios-escolares/anio-2027/estructuras-horarias`;
const ESPERA = { timeout: 4000 };

// El "min" va dentro de la etiqueta del campo y aparece cuando hay un valor, así que el texto de la etiqueta lo incluye
// a veces ("Duración del bloque *min").
const DURACION_BLOQUE = /Duración del bloque/;
const DURACION_RECESO = /^Duración(\s*min)?$/;

const anio2027 = {
  id: 'anio-2027',
  anio: 2027,
  fechaInicio: '2027-03-01',
  fechaFin: '2027-12-17',
  regimen: 'BIMESTRAL',
  estado: 'PLANIFICADO',
  cantidadSecciones: 0,
  cantidadEstructuras: 0,
  periodos: [],
};

// La estructura de ejemplo del modal de Figma: 14:45, 4 bloques de 45 min y un receso de 15 después del bloque 2.
const jornadaDeFigma: VistaPreviaJornada = {
  valida: true,
  errores: [],
  jornada: {
    filas: [
      {
        tipo: 'BLOQUE',
        numero: 1,
        inicio: '14:45',
        fin: '15:30',
        duracionMin: 45,
      },
      {
        tipo: 'BLOQUE',
        numero: 2,
        inicio: '15:30',
        fin: '16:15',
        duracionMin: 45,
      },
      {
        tipo: 'RECESO',
        numero: null,
        inicio: '16:15',
        fin: '16:30',
        duracionMin: 15,
      },
      {
        tipo: 'BLOQUE',
        numero: 3,
        inicio: '16:30',
        fin: '17:15',
        duracionMin: 45,
      },
      {
        tipo: 'BLOQUE',
        numero: 4,
        inicio: '17:15',
        fin: '18:00',
        duracionMin: 45,
      },
    ],
    horaSalida: '18:00',
    bloquesPorSemana: 12,
  },
};

type Servidor = {
  vistasPrevias: SolicitudVistaPrevia[];
  creadas: SolicitudEstructuraHoraria[];
};

const mostrar = async ({
  vistaPrevia = jornadaDeFigma,
  vistaPreviaSegun,
  respuestaCrear,
}: {
  vistaPrevia?: VistaPreviaJornada;
  /** Responde según el cuerpo recibido (y puede demorar), para probar cómo cambia la vista previa. */
  vistaPreviaSegun?: (
    cuerpo: SolicitudVistaPrevia,
  ) => Promise<VistaPreviaJornada>;
  respuestaCrear?: () => Response;
} = {}) => {
  const registro: Servidor = { vistasPrevias: [], creadas: [] };
  const onCerrar = vi.fn();
  const onGuardada = vi.fn();
  server.use(
    http.get(`${API}/configuracion/anios-escolares/anio-2027`, () =>
      HttpResponse.json(anio2027),
    ),
    http.post(`${URL_ESTRUCTURAS}/vista-previa`, async ({ request }) => {
      const cuerpo = (await request.json()) as SolicitudVistaPrevia;
      registro.vistasPrevias.push(cuerpo);
      return HttpResponse.json(
        vistaPreviaSegun ? await vistaPreviaSegun(cuerpo) : vistaPrevia,
      );
    }),
    http.post(URL_ESTRUCTURAS, async ({ request }) => {
      registro.creadas.push(
        (await request.json()) as SolicitudEstructuraHoraria,
      );
      const guardada: EstructuraGuardada = {
        estructura: {
          ...detalleDe({
            ...secundaria,
            id: 'estructura-nueva',
            codigo: 'EH-TAR-26',
          }),
          esPrincipal: false,
        },
        advertencias: [],
      };
      return respuestaCrear?.() ?? HttpResponse.json(guardada, { status: 201 });
    }),
  );
  await renderApp(
    <DialogoEstructura
      anioId="anio-2027"
      onCerrar={onCerrar}
      onGuardada={onGuardada}
    />,
    { user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }) },
  );
  const dialogo = await screen.findByRole('dialog');
  return { dialogo, registro, onCerrar, onGuardada };
};

const dia = (dialogo: HTMLElement, nombre: string) =>
  within(dialogo).getByRole('button', { name: nombre });

/** Llena el formulario con la estructura de Figma. */
const llenarComoEnFigma = async (dialogo: HTMLElement) => {
  await userEvent.type(within(dialogo).getByLabelText('Código *'), 'eh-tar-26');
  await userEvent.type(
    within(dialogo).getByLabelText('Descripción'),
    'Secundaria · talleres de tarde',
  );
  await userEvent.selectOptions(
    within(dialogo).getByLabelText('Nivel *'),
    'SECUNDARIA',
  );
  for (const nombre of ['Lun', 'Mié', 'Vie']) {
    await userEvent.click(dia(dialogo, nombre));
  }
  await userEvent.type(
    within(dialogo).getByLabelText('Hora de entrada *'),
    '1445',
  );
  await userEvent.type(
    within(dialogo).getByLabelText('Bloques por día *'),
    '4',
  );
  await userEvent.type(within(dialogo).getByLabelText(DURACION_BLOQUE), '45');
  await userEvent.click(
    within(dialogo).getByRole('button', { name: 'Agregar receso' }),
  );
  await userEvent.selectOptions(
    within(dialogo).getByLabelText('Receso 1: después de qué bloque'),
    '2',
  );
  await userEvent.type(within(dialogo).getByLabelText(DURACION_RECESO), '15');
};

const cuerpoDeFigma: SolicitudEstructuraHoraria = {
  codigo: 'EH-TAR-26',
  descripcion: 'Secundaria · talleres de tarde',
  nivel: 'SECUNDARIA',
  esPrincipal: false,
  diasLectivos: ['LUN', 'MIE', 'VIE'],
  horaEntrada: '14:45',
  bloquesPorDia: 4,
  duracionBloqueMin: 45,
  recesos: [{ despuesDeBloque: 2, duracionMin: 15 }],
};

describe('al abrir', () => {
  test('muestra el título, el subtítulo con el año y los campos vacíos', async () => {
    const { dialogo } = await mostrar();

    expect(
      within(dialogo).getByRole('heading', {
        name: 'Nueva estructura horaria',
      }),
    ).toBeInTheDocument();
    expect(
      await within(dialogo).findByText(
        /Año escolar 2027\. Los bloques se calculan a partir de la hora de entrada, la duración y los recesos\./,
      ),
    ).toBeVisible();
    for (const etiqueta of [
      'Código *',
      'Descripción',
      'Hora de entrada *',
      'Bloques por día *',
      DURACION_BLOQUE,
    ]) {
      expect(within(dialogo).getByLabelText(etiqueta)).toHaveValue('');
    }
    expect(within(dialogo).getByText('Recesos')).toBeInTheDocument();
    expect(
      within(dialogo).getByRole('button', { name: 'Agregar receso' }),
    ).toBeInTheDocument();
    expect(
      within(dialogo).getByRole('button', { name: 'Crear estructura' }),
    ).toBeInTheDocument();
  });

  test('ofrece los seis días sin marcar, el nivel y la casilla de estructura principal', async () => {
    const { dialogo } = await mostrar();

    expect(
      ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'].map((n) =>
        dia(dialogo, n).getAttribute('aria-pressed'),
      ),
    ).toEqual(['false', 'false', 'false', 'false', 'false', 'false']);
    expect(within(dialogo).getByLabelText('Nivel *')).toHaveValue('');
    expect(
      within(dialogo).getByRole('checkbox', { name: /Estructura principal/ }),
    ).not.toBeChecked();
  });

  test('la vista previa pide completar los datos y repite el aviso de las 18:00, sin pedir nada al backend', async () => {
    const { dialogo, registro } = await mostrar();

    expect(
      within(dialogo).getByText(
        /Completa la hora de entrada, los bloques por día y la duración/,
      ),
    ).toBeInTheDocument();
    expect(
      within(dialogo).getByText(
        'La jornada debe terminar como máximo a las 18:00.',
      ),
    ).toBeInTheDocument();
    await new Promise((resolver) => setTimeout(resolver, 600));
    expect(registro.vistasPrevias).toEqual([]);
  });
});

describe('validaciones en vivo', () => {
  test('el código se pasa a mayúsculas al escribir', async () => {
    const { dialogo } = await mostrar();

    await userEvent.type(
      within(dialogo).getByLabelText('Código *'),
      'eh-pri-26',
    );

    expect(within(dialogo).getByLabelText('Código *')).toHaveValue('EH-PRI-26');
  });

  test.each([
    ['abc d', 'El código solo admite letras, números y guion.'],
    ['A'.repeat(21), 'El código admite hasta 20 caracteres.'],
  ])('el código %j avisa "%s"', async (texto, mensaje) => {
    const { dialogo } = await mostrar();

    await userEvent.type(within(dialogo).getByLabelText('Código *'), texto);

    expect(await within(dialogo).findByText(mensaje)).toBeInTheDocument();
  });

  test('los bloques fuera de 1 a 12 y la duración fuera de 20 a 120 avisan con los mensajes del backend', async () => {
    const { dialogo } = await mostrar();

    await userEvent.type(
      within(dialogo).getByLabelText('Bloques por día *'),
      '13',
    );
    await userEvent.type(within(dialogo).getByLabelText(DURACION_BLOQUE), '10');

    expect(
      await within(dialogo).findByText('Los bloques por día van de 1 a 12.'),
    ).toBeInTheDocument();
    expect(
      await within(dialogo).findByText(
        'La duración del bloque va de 20 a 120 minutos.',
      ),
    ).toBeInTheDocument();
  });

  test('desmarcar el último día avisa que hace falta al menos uno', async () => {
    const { dialogo } = await mostrar();

    await userEvent.click(dia(dialogo, 'Lun'));
    expect(dia(dialogo, 'Lun')).toHaveAttribute('aria-pressed', 'true');
    await userEvent.click(dia(dialogo, 'Lun'));

    expect(dia(dialogo, 'Lun')).toHaveAttribute('aria-pressed', 'false');
    expect(
      await within(dialogo).findByText('Elige al menos un día lectivo.'),
    ).toBeInTheDocument();
  });

  test('con el formulario vacío no envía y pide cada dato', async () => {
    const { dialogo, registro } = await mostrar();

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Crear estructura' }),
    );

    for (const mensaje of [
      'Ingresa el código de la estructura.',
      'Elige el nivel: primaria o secundaria.',
      'Elige al menos un día lectivo.',
      'Ingresa la hora de entrada.',
      'Los bloques por día van de 1 a 12.',
      'La duración del bloque va de 20 a 120 minutos.',
    ]) {
      expect(await within(dialogo).findByText(mensaje)).toBeInTheDocument();
    }
    expect(registro.creadas).toEqual([]);
  });
});

describe('recesos', () => {
  test('"Agregar receso" suma una fila y "Quitar" la saca', async () => {
    const { dialogo } = await mostrar();

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Agregar receso' }),
    );
    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Agregar receso' }),
    );
    expect(
      within(dialogo).getByLabelText('Receso 2: después de qué bloque'),
    ).toBeInTheDocument();

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Quitar el receso 2' }),
    );

    expect(
      within(dialogo).queryByLabelText('Receso 2: después de qué bloque'),
    ).not.toBeInTheDocument();
    expect(
      within(dialogo).getByLabelText('Receso 1: después de qué bloque'),
    ).toBeInTheDocument();
  });

  test('con 4 bloques por día el receso ofrece "Después del bloque 1" a "3"', async () => {
    const { dialogo } = await mostrar();
    await userEvent.type(
      within(dialogo).getByLabelText('Bloques por día *'),
      '4',
    );

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Agregar receso' }),
    );

    const receso = within(dialogo).getByLabelText(
      'Receso 1: después de qué bloque',
    );
    expect(
      within(receso)
        .getAllByRole('option')
        .map((o) => o.textContent),
    ).toEqual([
      'Después del bloque…',
      'Después del bloque 1',
      'Después del bloque 2',
      'Después del bloque 3',
    ]);
  });

  test('al bajar los bloques por día se revisan los recesos ya puestos', async () => {
    const { dialogo } = await mostrar();
    const bloques = within(dialogo).getByLabelText('Bloques por día *');
    await userEvent.type(bloques, '4');
    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Agregar receso' }),
    );
    await userEvent.selectOptions(
      within(dialogo).getByLabelText('Receso 1: después de qué bloque'),
      '3',
    );
    await userEvent.type(within(dialogo).getByLabelText(DURACION_RECESO), '15');
    expect(
      within(dialogo).queryByText(/Un receso va después de un bloque entre/),
    ).not.toBeInTheDocument();

    await userEvent.clear(bloques);
    await userEvent.type(bloques, '3');

    expect(
      await within(dialogo).findByText(
        'Un receso va después de un bloque entre 1 y 2.',
      ),
    ).toBeInTheDocument();
  });

  test('la duración del receso fuera de 5 a 60 avisa con el mensaje del backend', async () => {
    const { dialogo } = await mostrar();
    await userEvent.type(
      within(dialogo).getByLabelText('Bloques por día *'),
      '4',
    );
    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Agregar receso' }),
    );

    await userEvent.type(within(dialogo).getByLabelText(DURACION_RECESO), '90');

    expect(
      await within(dialogo).findByText('El receso dura de 5 a 60 minutos.'),
    ).toBeInTheDocument();
  });
});

describe('vista previa de la jornada', () => {
  test('con la hora, los bloques y la duración pide la jornada al backend y la dibuja', async () => {
    const { dialogo, registro } = await mostrar();

    await llenarComoEnFigma(dialogo);

    expect(
      await within(dialogo).findByText('Bloque 1', undefined, ESPERA),
    ).toBeInTheDocument();
    expect(within(dialogo).getByText('14:45 – 15:30')).toBeInTheDocument();
    expect(within(dialogo).getByText('Receso · 15 min')).toBeInTheDocument();
    expect(within(dialogo).getByText('16:15 – 16:30')).toBeInTheDocument();
    expect(within(dialogo).getByText('Bloque 4')).toBeInTheDocument();
    expect(within(dialogo).getByText('17:15 – 18:00')).toBeInTheDocument();
    // Hora de salida y bloques por semana (los calcula el backend).
    expect(within(dialogo).getByText('Hora de salida')).toBeInTheDocument();
    expect(within(dialogo).getByText('18:00')).toBeInTheDocument();
    expect(within(dialogo).getByText('Bloques por semana')).toBeInTheDocument();
    expect(within(dialogo).getByText('12')).toBeInTheDocument();
    expect(registro.vistasPrevias.at(-1)).toMatchObject({
      horaEntrada: '14:45',
      bloquesPorDia: 4,
      duracionBloqueMin: 45,
      recesos: [{ despuesDeBloque: 2, duracionMin: 15 }],
    });
  });

  test('no se pide en cada tecla: escribir "45" seguido manda una sola petición', async () => {
    const { dialogo, registro } = await mostrar();
    await userEvent.type(
      within(dialogo).getByLabelText('Hora de entrada *'),
      '1445',
    );
    await userEvent.type(
      within(dialogo).getByLabelText('Bloques por día *'),
      '4',
    );

    await userEvent.type(within(dialogo).getByLabelText(DURACION_BLOQUE), '45');

    await within(dialogo).findByText('Bloque 1', undefined, ESPERA);
    expect(registro.vistasPrevias).toHaveLength(1);
    expect(registro.vistasPrevias[0].duracionBloqueMin).toBe(45);
  });

  test('si la jornada pasa de las 18:00 se ve igual, con la salida real y el aviso en rojo', async () => {
    const pasada: VistaPreviaJornada = {
      valida: false,
      errores: ['La jornada debe terminar como máximo a las 18:00.'],
      jornada: { ...jornadaDeFigma.jornada!, horaSalida: '18:45' },
    };
    const { dialogo } = await mostrar({ vistaPrevia: pasada });

    await llenarComoEnFigma(dialogo);

    expect(
      await within(dialogo).findByText('18:45', undefined, ESPERA),
    ).toBeInTheDocument();
    expect(within(dialogo).getByText('Bloque 4')).toBeInTheDocument();
    expect(
      within(dialogo).getByRole('alert', {
        name: '',
      }),
    ).toHaveTextContent('La jornada debe terminar como máximo a las 18:00.');
  });

  test('un error del backend que el formulario no cubre se muestra; los que ya salen en su campo no se repiten', async () => {
    const { dialogo } = await mostrar({
      vistaPrevia: {
        valida: false,
        errores: [
          'Ingresa el código de la estructura.',
          'El texto contiene caracteres no válidos.',
        ],
        jornada: jornadaDeFigma.jornada,
      },
    });

    await llenarComoEnFigma(dialogo);

    expect(
      await within(dialogo).findByText(
        'El texto contiene caracteres no válidos.',
        undefined,
        ESPERA,
      ),
    ).toBeInTheDocument();
    expect(
      within(dialogo).queryByText('Ingresa el código de la estructura.'),
    ).not.toBeInTheDocument();
  });
});

describe('crear', () => {
  test('manda el cuerpo correcto, avisa, deja elegida la nueva y cierra el modal', async () => {
    const { dialogo, registro, onCerrar, onGuardada } = await mostrar();
    await llenarComoEnFigma(dialogo);

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Crear estructura' }),
    );

    expect(await screen.findByText('Estructura creada')).toBeInTheDocument();
    expect(screen.getByText('EH-TAR-26 · Secundaria')).toBeInTheDocument();
    expect(registro.creadas).toEqual([cuerpoDeFigma]);
    expect(onGuardada).toHaveBeenCalledWith('estructura-nueva');
    expect(onCerrar).toHaveBeenCalledTimes(1);
  });

  test('marcar "Estructura principal" se envía en el cuerpo', async () => {
    const { dialogo, registro } = await mostrar();
    await llenarComoEnFigma(dialogo);
    await userEvent.click(
      within(dialogo).getByRole('checkbox', { name: /Estructura principal/ }),
    );

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Crear estructura' }),
    );

    await waitFor(() => expect(registro.creadas).toHaveLength(1));
    expect(registro.creadas[0].esPrincipal).toBe(true);
  });

  test('cada advertencia del backend sale como una notificación de advertencia con su texto', async () => {
    const advertencias = [
      'EH-TAR-26 pasa a ser la estructura principal de secundaria; EH-SEC-26 deja de serlo.',
      'La jornada de secundaria pasa de 30 a 35 bloques por semana. Revisa el plan de evaluación.',
    ];
    const { dialogo, onCerrar } = await mostrar({
      respuestaCrear: () =>
        HttpResponse.json(
          {
            estructura: detalleDe(secundaria),
            advertencias,
          } satisfies EstructuraGuardada,
          { status: 201 },
        ),
    });
    await llenarComoEnFigma(dialogo);

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Crear estructura' }),
    );

    for (const advertencia of advertencias) {
      expect(await screen.findByText(advertencia)).toBeInTheDocument();
    }
    expect(screen.getAllByText('Advertencia')).toHaveLength(2);
    expect(onCerrar).toHaveBeenCalledTimes(1);
  });

  test('un 409 (código repetido) se muestra una sola vez y el modal sigue abierto con lo escrito', async () => {
    const mensaje =
      'Ya existe una estructura con el código EH-TAR-26 en este año.';
    const { dialogo, onCerrar, onGuardada } = await mostrar({
      respuestaCrear: () =>
        HttpResponse.json({ message: mensaje }, { status: 409 }),
    });
    await llenarComoEnFigma(dialogo);

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Crear estructura' }),
    );

    expect(await screen.findAllByText(mensaje)).toHaveLength(1);
    expect(onCerrar).not.toHaveBeenCalled();
    expect(onGuardada).not.toHaveBeenCalled();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(within(dialogo).getByLabelText('Código *')).toHaveValue('EH-TAR-26');
    expect(screen.queryByText('Estructura creada')).not.toBeInTheDocument();
  });

  test('Cancelar cierra el modal sin enviar nada', async () => {
    const { dialogo, registro, onCerrar } = await mostrar();

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Cancelar' }),
    );

    expect(onCerrar).toHaveBeenCalledTimes(1);
    expect(registro.creadas).toEqual([]);
  });
});

describe('la vista previa no desaparece al editar', () => {
  const sinReceso: VistaPreviaJornada = {
    valida: true,
    errores: [],
    jornada: {
      filas: jornadaDeFigma.jornada!.filas.filter((f) => f.tipo === 'BLOQUE'),
      horaSalida: '17:45',
      bloquesPorSemana: 12,
    },
  };

  /** Responde sin receso, o con el receso de Figma si el cuerpo ya trae uno. */
  const segunRecesos = async (cuerpo: SolicitudVistaPrevia) =>
    cuerpo.recesos.length > 0 ? jornadaDeFigma : sinReceso;

  const llenarJornada = async (dialogo: HTMLElement) => {
    await userEvent.type(
      within(dialogo).getByLabelText('Hora de entrada *'),
      '1445',
    );
    await userEvent.type(
      within(dialogo).getByLabelText('Bloques por día *'),
      '4',
    );
    await userEvent.type(within(dialogo).getByLabelText(DURACION_BLOQUE), '45');
    await within(dialogo).findByText('Bloque 1', undefined, ESPERA);
  };

  const completarReceso = async (dialogo: HTMLElement) => {
    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Agregar receso' }),
    );
    await userEvent.selectOptions(
      within(dialogo).getByLabelText('Receso 1: después de qué bloque'),
      '2',
    );
    await userEvent.type(within(dialogo).getByLabelText(DURACION_RECESO), '15');
  };

  test('agregar una fila de receso vacía no pide nada nuevo ni oculta la jornada', async () => {
    const { dialogo, registro } = await mostrar({
      vistaPreviaSegun: segunRecesos,
    });
    await llenarJornada(dialogo);
    const pedidosAntes = registro.vistasPrevias.length;

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Agregar receso' }),
    );
    // Más que la espera de la vista previa (400 ms): si fuera a pedir algo, ya lo habría hecho.
    await new Promise((resolver) => setTimeout(resolver, 700));

    expect(registro.vistasPrevias).toHaveLength(pedidosAntes);
    expect(within(dialogo).getByText('Bloque 1')).toBeInTheDocument();
    expect(within(dialogo).getByText('Bloque 4')).toBeInTheDocument();
    expect(
      within(dialogo).queryByText(/Completa la hora de entrada/),
    ).not.toBeInTheDocument();
  });

  test('al completar el receso aparece entre los bloques ya dibujados, que se actualizan en su lugar', async () => {
    const { dialogo, registro } = await mostrar({
      vistaPreviaSegun: segunRecesos,
    });
    await llenarJornada(dialogo);
    const bloque1 = within(dialogo).getByText('Bloque 1');
    const bloque3 = within(dialogo).getByText('Bloque 3');
    expect(within(dialogo).queryByText(/^Receso · /)).not.toBeInTheDocument();

    await completarReceso(dialogo);

    expect(
      await within(dialogo).findByText('Receso · 15 min', undefined, ESPERA),
    ).toBeInTheDocument();
    // En orden: Bloque 2, el receso, Bloque 3.
    const filas = within(
      within(dialogo).getByRole('region', {
        name: 'Vista previa de la jornada',
      }),
    )
      .getAllByRole('listitem')
      .map((fila) => fila.textContent);
    expect(filas[1]).toContain('Bloque 2');
    expect(filas[2]).toContain('Receso · 15 min');
    expect(filas[3]).toContain('Bloque 3');
    // Las filas que ya estaban son las mismas: se actualizan, no se vuelven a crear.
    expect(within(dialogo).getByText('Bloque 1')).toBe(bloque1);
    expect(within(dialogo).getByText('Bloque 3')).toBe(bloque3);
    expect(registro.vistasPrevias.at(-1)?.recesos).toEqual([
      { despuesDeBloque: 2, duracionMin: 15 },
    ]);
  });

  test('mientras llega la respuesta nueva se sigue viendo la anterior, marcada como actualizando', async () => {
    const { dialogo } = await mostrar({
      vistaPreviaSegun: async (cuerpo) => {
        if (cuerpo.recesos.length > 0) await delay(400);
        return segunRecesos(cuerpo);
      },
    });
    await llenarJornada(dialogo);
    const vista = within(dialogo).getByRole('region', {
      name: 'Vista previa de la jornada',
    });
    expect(vista).toHaveAttribute('aria-busy', 'false');

    await completarReceso(dialogo);

    await waitFor(
      () => expect(vista).toHaveAttribute('aria-busy', 'true'),
      ESPERA,
    );
    expect(within(dialogo).getByText('Bloque 1')).toBeInTheDocument();
    expect(within(dialogo).getByText('Bloque 4')).toBeInTheDocument();
    expect(
      await within(dialogo).findByText('Receso · 15 min', undefined, ESPERA),
    ).toBeInTheDocument();
    expect(vista).toHaveAttribute('aria-busy', 'false');
  });

  test('quitar el receso vuelve a pedir la jornada sin él', async () => {
    const { dialogo } = await mostrar({ vistaPreviaSegun: segunRecesos });
    await userEvent.type(
      within(dialogo).getByLabelText('Hora de entrada *'),
      '1445',
    );
    await userEvent.type(
      within(dialogo).getByLabelText('Bloques por día *'),
      '4',
    );
    await userEvent.type(within(dialogo).getByLabelText(DURACION_BLOQUE), '45');
    await completarReceso(dialogo);
    await within(dialogo).findByText('Receso · 15 min', undefined, ESPERA);

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Quitar el receso 1' }),
    );

    await waitFor(
      () =>
        expect(
          within(dialogo).queryByText(/^Receso · /),
        ).not.toBeInTheDocument(),
      ESPERA,
    );
    expect(within(dialogo).getByText('Bloque 4')).toBeInTheDocument();
  });

  test('con datos que no alcanzan para calcular se conserva la última vista previa y se avisa', async () => {
    const { dialogo, registro } = await mostrar({
      vistaPreviaSegun: segunRecesos,
    });
    await llenarJornada(dialogo);
    const bloques = within(dialogo).getByLabelText('Bloques por día *');

    await userEvent.clear(bloques);
    await userEvent.type(bloques, '13');

    expect(
      await within(dialogo).findByText(
        'Corrige los datos marcados para actualizar la vista previa.',
        undefined,
        ESPERA,
      ),
    ).toBeInTheDocument();
    expect(within(dialogo).getByText('Bloque 1')).toBeInTheDocument();
    expect(
      within(dialogo).queryByText(/Completa la hora de entrada/),
    ).not.toBeInTheDocument();
    // Con 13 bloques no se pide nada: el backend respondería sin jornada.
    expect(registro.vistasPrevias.every((c) => c.bloquesPorDia !== 13)).toBe(
      true,
    );
  });

  test('al corregir los datos la vista previa vuelve a actualizarse y desaparece el aviso', async () => {
    const { dialogo } = await mostrar({ vistaPreviaSegun: segunRecesos });
    await llenarJornada(dialogo);
    const bloques = within(dialogo).getByLabelText('Bloques por día *');
    await userEvent.clear(bloques);
    await userEvent.type(bloques, '13');
    await within(dialogo).findByText(
      'Corrige los datos marcados para actualizar la vista previa.',
      undefined,
      ESPERA,
    );

    await userEvent.clear(bloques);
    await userEvent.type(bloques, '4');

    await waitFor(
      () =>
        expect(
          within(dialogo).queryByText(
            'Corrige los datos marcados para actualizar la vista previa.',
          ),
        ).not.toBeInTheDocument(),
      ESPERA,
    );
    expect(within(dialogo).getByText('Bloque 4')).toBeInTheDocument();
  });
});

describe('un receso nuevo no sale en error hasta su primera entrada', () => {
  const MENSAJE_DURACION = 'El receso dura de 5 a 60 minutos.';

  const conCuatroBloques = async () => {
    const contexto = await mostrar();
    await userEvent.type(
      within(contexto.dialogo).getByLabelText('Bloques por día *'),
      '4',
    );
    await userEvent.click(
      within(contexto.dialogo).getByRole('button', { name: 'Agregar receso' }),
    );
    return contexto;
  };

  test('al agregar la fila no hay ningún error', async () => {
    const { dialogo } = await conCuatroBloques();

    expect(
      within(dialogo).queryByText(MENSAJE_DURACION),
    ).not.toBeInTheDocument();
    expect(
      within(dialogo).queryByText(/Un receso va después de un bloque entre/),
    ).not.toBeInTheDocument();
    expect(within(dialogo).getByLabelText(DURACION_RECESO)).toHaveAttribute(
      'aria-invalid',
      'false',
    );
  });

  test('al elegir "Después del bloque…" la duración, todavía vacía, no sale en error', async () => {
    const { dialogo } = await conCuatroBloques();

    await userEvent.selectOptions(
      within(dialogo).getByLabelText('Receso 1: después de qué bloque'),
      '2',
    );
    // Más que lo que tarda en validar: si fuera a salir el error, ya habría salido.
    await new Promise((resolver) => setTimeout(resolver, 300));

    expect(
      within(dialogo).queryByText(MENSAJE_DURACION),
    ).not.toBeInTheDocument();
    expect(within(dialogo).getByLabelText(DURACION_RECESO)).toHaveAttribute(
      'aria-invalid',
      'false',
    );
  });

  test('con la fila nueva vacía, cambiar los bloques por día tampoco la pone en error', async () => {
    const { dialogo } = await conCuatroBloques();
    const bloques = within(dialogo).getByLabelText('Bloques por día *');

    await userEvent.clear(bloques);
    await userEvent.type(bloques, '5');
    await new Promise((resolver) => setTimeout(resolver, 300));

    expect(
      within(dialogo).queryByText(MENSAJE_DURACION),
    ).not.toBeInTheDocument();
    expect(
      within(dialogo).queryByText(/Un receso va después de un bloque entre/),
    ).not.toBeInTheDocument();
  });

  test('el error aparece con la primera entrada inválida, y se va al corregirla', async () => {
    const { dialogo } = await conCuatroBloques();
    await userEvent.selectOptions(
      within(dialogo).getByLabelText('Receso 1: después de qué bloque'),
      '2',
    );
    const duracion = within(dialogo).getByLabelText(DURACION_RECESO);

    await userEvent.type(duracion, '1');
    expect(
      await within(dialogo).findByText(MENSAJE_DURACION),
    ).toBeInTheDocument();

    await userEvent.type(duracion, '5');
    await waitFor(() =>
      expect(
        within(dialogo).queryByText(MENSAJE_DURACION),
      ).not.toBeInTheDocument(),
    );
  });

  test('al enviar con el receso vacío sí se avisa de lo que falta', async () => {
    const { dialogo, registro } = await conCuatroBloques();

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Crear estructura' }),
    );

    expect(
      await within(dialogo).findByText(MENSAJE_DURACION),
    ).toBeInTheDocument();
    expect(
      within(dialogo).getByText(
        'Un receso va después de un bloque entre 1 y 3.',
      ),
    ).toBeInTheDocument();
    expect(registro.creadas).toEqual([]);
  });

  test('un receso ya escrito sigue revisándose al cambiar los bloques, aunque haya otra fila vacía', async () => {
    const { dialogo } = await conCuatroBloques();
    await userEvent.selectOptions(
      within(dialogo).getByLabelText('Receso 1: después de qué bloque'),
      '3',
    );
    await userEvent.type(within(dialogo).getByLabelText(DURACION_RECESO), '15');
    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Agregar receso' }),
    );
    const bloques = within(dialogo).getByLabelText('Bloques por día *');

    await userEvent.clear(bloques);
    await userEvent.type(bloques, '3');

    // El receso 1 (después del bloque 3) ya no cabe con 3 bloques; la fila vacía 2 sigue sin avisar nada.
    expect(
      await within(dialogo).findAllByText(
        'Un receso va después de un bloque entre 1 y 2.',
      ),
    ).toHaveLength(1);

    await userEvent.clear(bloques);
    await userEvent.type(bloques, '4');
    await waitFor(() =>
      expect(
        within(dialogo).queryByText(/Un receso va después de un bloque entre/),
      ).not.toBeInTheDocument(),
    );
  });
});
