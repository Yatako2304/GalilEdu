import { HttpResponse, http } from 'msw';

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
  EstructuraHorariaResumen,
  SolicitudEdicionEstructuraHoraria,
} from '@/types/api';

import { EstructuraHorariaPestana } from '../estructura-horaria-pestana';

import { detalleDe, primaria, secundaria } from './datos';

// Editar y Eliminar una estructura horaria (HU-44): Figma no los dibuja; siguen el modal "Nueva estructura horaria".

const API = env.API_URL;
const ESPERA = { timeout: 4000 };
const DURACION_BLOQUE = /Duración del bloque/;
const DURACION_RECESO = /^Duración(\s*min)?$/;

type Servidor = {
  ediciones: { id: string; cuerpo: SolicitudEdicionEstructuraHoraria }[];
  eliminadas: string[];
};

const mostrar = async ({
  anioModificable = true,
  respuestaEditar,
  respuestaEliminar,
  estructuras: iniciales = [primaria, secundaria],
}: {
  anioModificable?: boolean;
  respuestaEditar?: () => Response;
  respuestaEliminar?: () => Response;
  estructuras?: EstructuraHorariaResumen[];
} = {}) => {
  const registro: Servidor = { ediciones: [], eliminadas: [] };
  let estructuras = [...iniciales];
  server.use(
    http.get(`${API}/configuracion/anios-escolares/anio-2026`, () =>
      HttpResponse.json({
        id: 'anio-2026',
        anio: 2026,
        fechaInicio: '2026-03-02',
        fechaFin: '2026-12-18',
        regimen: 'BIMESTRAL',
        estado: anioModificable ? 'PLANIFICADO' : 'ACTIVO',
        cantidadSecciones: 0,
        cantidadEstructuras: estructuras.length,
        periodos: [],
      }),
    ),
    http.get(
      `${API}/configuracion/anios-escolares/anio-2026/estructuras-horarias`,
      () => HttpResponse.json({ anioModificable, estructuras }),
    ),
    http.get(`${API}/configuracion/estructuras-horarias/:id`, ({ params }) => {
      const e = estructuras.find((x) => x.id === params.id);
      return e
        ? HttpResponse.json({ ...detalleDe(e), anioModificable })
        : HttpResponse.json({ message: 'No existe' }, { status: 404 });
    }),
    http.post(
      `${API}/configuracion/anios-escolares/anio-2026/estructuras-horarias/vista-previa`,
      () => HttpResponse.json({ valida: true, errores: [], jornada: null }),
    ),
    http.put(
      `${API}/configuracion/estructuras-horarias/:id`,
      async ({ params, request }) => {
        registro.ediciones.push({
          id: String(params.id),
          cuerpo: (await request.json()) as SolicitudEdicionEstructuraHoraria,
        });
        const e = estructuras.find((x) => x.id === params.id) ?? primaria;
        const guardada: EstructuraGuardada = {
          estructura: detalleDe(e),
          advertencias: [],
        };
        return respuestaEditar?.() ?? HttpResponse.json(guardada);
      },
    ),
    http.delete(
      `${API}/configuracion/estructuras-horarias/:id`,
      ({ params }) => {
        registro.eliminadas.push(String(params.id));
        const respuesta = respuestaEliminar?.();
        if (respuesta) return respuesta;
        estructuras = estructuras.filter((x) => x.id !== params.id);
        return new HttpResponse(null, { status: 204 });
      },
    ),
  );
  await renderApp(
    <EstructuraHorariaPestana anioId="anio-2026" estadoAnio="PLANIFICADO" />,
    { user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }) },
  );
  await screen.findByRole('heading', { name: iniciales[0].codigo });
  return registro;
};

const abrirEditar = async () => {
  await userEvent.click(screen.getByRole('button', { name: 'Editar' }));
  return screen.findByRole('dialog');
};

describe('botones Editar y Eliminar de la tarjeta', () => {
  test('con el año modificable están habilitados', async () => {
    await mostrar();

    expect(screen.getByRole('button', { name: 'Editar' })).not.toHaveAttribute(
      'aria-disabled',
    );
    expect(
      screen.getByRole('button', { name: 'Eliminar' }),
    ).not.toHaveAttribute('aria-disabled');
  });

  test('con el año no modificable quedan bloqueados, no abren nada y un tooltip dice por qué', async () => {
    await mostrar({ anioModificable: false });
    const editar = screen.getByRole('button', { name: 'Editar' });
    const eliminar = screen.getByRole('button', { name: 'Eliminar' });

    expect(editar).toHaveAttribute('aria-disabled', 'true');
    expect(eliminar).toHaveAttribute('aria-disabled', 'true');
    await userEvent.click(editar);
    await userEvent.click(eliminar);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    await userEvent.hover(editar);
    expect(
      await screen.findAllByText(
        'Solo se puede modificar la estructura horaria de un año planificado.',
      ),
    ).not.toHaveLength(0);
  });
});

describe('Editar estructura horaria', () => {
  test('abre con los datos de la estructura ya puestos', async () => {
    await mostrar();

    const dialogo = await abrirEditar();

    expect(
      within(dialogo).getByRole('heading', {
        name: 'Editar estructura horaria',
      }),
    ).toBeInTheDocument();
    expect(within(dialogo).getByLabelText('Código *')).toHaveValue('EH-PRI-26');
    expect(within(dialogo).getByLabelText('Descripción')).toHaveValue(
      'Primaria · jornada de mañana',
    );
    expect(within(dialogo).getByLabelText('Hora de entrada *')).toHaveValue(
      '07:45',
    );
    expect(within(dialogo).getByLabelText('Bloques por día *')).toHaveValue(
      '8',
    );
    expect(within(dialogo).getByLabelText(DURACION_BLOQUE)).toHaveValue('45');
    expect(
      ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'].map((d) =>
        within(dialogo)
          .getByRole('button', { name: d })
          .getAttribute('aria-pressed'),
      ),
    ).toEqual(['true', 'true', 'true', 'true', 'true', 'false']);
    // Dos recesos: después del bloque 3 (20 min) y del 6 (15 min).
    expect(
      within(dialogo).getByLabelText('Receso 1: después de qué bloque'),
    ).toHaveValue('3');
    expect(
      within(dialogo).getByLabelText('Receso 2: después de qué bloque'),
    ).toHaveValue('6');
    expect(
      within(dialogo)
        .getAllByLabelText(DURACION_RECESO)
        .map((campo) => (campo as HTMLInputElement).value),
    ).toEqual(['20', '15']);
    expect(
      within(dialogo).getByRole('button', { name: 'Guardar cambios' }),
    ).toBeInTheDocument();
  });

  test('el nivel no se puede cambiar', async () => {
    await mostrar();

    const dialogo = await abrirEditar();

    expect(within(dialogo).getByLabelText('Nivel *')).toBeDisabled();
    expect(within(dialogo).getByLabelText('Nivel *')).toHaveValue('PRIMARIA');
    expect(
      within(dialogo).getByText('El nivel no se puede cambiar.'),
    ).toBeInTheDocument();
  });

  test('la principal no se puede desmarcar y lo explica', async () => {
    await mostrar();

    const dialogo = await abrirEditar();

    const casilla = within(dialogo).getByRole('checkbox', {
      name: /Estructura principal/,
    });
    expect(casilla).toBeChecked();
    expect(casilla).toBeDisabled();
    expect(
      within(dialogo).getByText(
        'Para cambiar de principal, marca otra estructura del nivel.',
      ),
    ).toBeInTheDocument();
  });

  test('la vista previa arranca con la jornada que ya tiene la estructura', async () => {
    await mostrar();

    const dialogo = await abrirEditar();

    // Sin esperar al backend (que en esta prueba responde sin jornada).
    expect(within(dialogo).getByText('Bloque 1')).toBeInTheDocument();
    expect(within(dialogo).getByText('07:45 – 08:30')).toBeInTheDocument();
    expect(within(dialogo).getByText('Receso · 20 min')).toBeInTheDocument();
    expect(within(dialogo).getByText('14:20')).toBeInTheDocument();
  });

  test('guarda los cambios sin el nivel, avisa, cierra y deja elegida la estructura', async () => {
    const registro = await mostrar();
    const dialogo = await abrirEditar();
    const bloques = within(dialogo).getByLabelText('Bloques por día *');
    await userEvent.clear(bloques);
    await userEvent.type(bloques, '9');

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Guardar cambios' }),
    );

    expect(
      await screen.findByText('Estructura actualizada'),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(registro.ediciones).toHaveLength(1);
    expect(registro.ediciones[0].id).toBe('estructura-pri');
    expect(registro.ediciones[0].cuerpo).toEqual({
      codigo: 'EH-PRI-26',
      descripcion: 'Primaria · jornada de mañana',
      esPrincipal: true,
      diasLectivos: ['LUN', 'MAR', 'MIE', 'JUE', 'VIE'],
      horaEntrada: '07:45',
      bloquesPorDia: 9,
      duracionBloqueMin: 45,
      recesos: [
        { despuesDeBloque: 3, duracionMin: 20 },
        { despuesDeBloque: 6, duracionMin: 15 },
      ],
    });
    expect(registro.ediciones[0].cuerpo).not.toHaveProperty('nivel');
  });

  test('marcar como principal una que no lo es se envía en el cuerpo', async () => {
    const registro = await mostrar({
      estructuras: [primaria, { ...secundaria, esPrincipal: false }],
    });
    await userEvent.click(screen.getByRole('button', { name: /EH-SEC-26/ }));
    await screen.findByRole('heading', { name: 'EH-SEC-26' });
    const dialogo = await abrirEditar();
    const casilla = within(dialogo).getByRole('checkbox', {
      name: /Estructura principal/,
    });
    expect(casilla).not.toBeChecked();
    expect(casilla).toBeEnabled();
    expect(
      within(dialogo).getByText(
        'Si la marcas, la principal actual del nivel deja de serlo.',
      ),
    ).toBeInTheDocument();

    await userEvent.click(casilla);
    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Guardar cambios' }),
    );

    await waitFor(() => expect(registro.ediciones).toHaveLength(1));
    expect(registro.ediciones[0].id).toBe('estructura-sec');
    expect(registro.ediciones[0].cuerpo.esPrincipal).toBe(true);
  });

  test('cada advertencia del backend sale como notificación de advertencia', async () => {
    const advertencias = [
      'La jornada de primaria pasa de 40 a 45 bloques por semana. Revisa el plan de evaluación.',
    ];
    await mostrar({
      respuestaEditar: () =>
        HttpResponse.json({
          estructura: detalleDe(primaria),
          advertencias,
        } satisfies EstructuraGuardada),
    });
    const dialogo = await abrirEditar();

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Guardar cambios' }),
    );

    expect(await screen.findByText(advertencias[0])).toBeInTheDocument();
    expect(screen.getByText('Advertencia')).toBeInTheDocument();
  });

  test('un 409 (por ejemplo, los planes no caben) se muestra una sola vez y el modal sigue abierto', async () => {
    const mensaje =
      'No se puede reducir: 1.° de primaria tiene más horas planificadas que bloques.';
    await mostrar({
      respuestaEditar: () =>
        HttpResponse.json({ message: mensaje }, { status: 409 }),
    });
    const dialogo = await abrirEditar();

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Guardar cambios' }),
    );

    expect(await screen.findAllByText(mensaje)).toHaveLength(1);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(
      screen.queryByText('Estructura actualizada'),
    ).not.toBeInTheDocument();
  });

  describe('el aviso de código repetido de la vista previa', () => {
    /** La vista previa no sabe que se edita: responde "ya existe" con el código que reciba. */
    const vistaPreviaQueRepiteElCodigo = () => {
      const recibidos: string[] = [];
      server.use(
        http.post(
          `${API}/configuracion/anios-escolares/anio-2026/estructuras-horarias/vista-previa`,
          async ({ request }) => {
            const cuerpo = (await request.json()) as { codigo: string };
            recibidos.push(cuerpo.codigo);
            return HttpResponse.json({
              valida: false,
              errores: [
                `Ya existe una estructura con el código ${cuerpo.codigo} en este año.`,
              ],
              jornada: null,
            });
          },
        ),
      );
      return recibidos;
    };

    test('no se muestra mientras el código sea el de la propia estructura', async () => {
      await mostrar();
      // Después de `mostrar`: el último manejador registrado es el que responde.
      const recibidos = vistaPreviaQueRepiteElCodigo();
      await abrirEditar();

      await waitFor(() => expect(recibidos).toContain('EH-PRI-26'), ESPERA);
      await new Promise((resolver) => setTimeout(resolver, 300));

      expect(
        screen.queryByText(/Ya existe una estructura con el código/),
      ).not.toBeInTheDocument();
    });

    test('sí se muestra si se cambia por el código de otra estructura', async () => {
      await mostrar();
      vistaPreviaQueRepiteElCodigo();
      const dialogo = await abrirEditar();
      const codigo = within(dialogo).getByLabelText('Código *');

      await userEvent.clear(codigo);
      await userEvent.type(codigo, 'eh-sec-26');

      expect(
        await within(dialogo).findByText(
          'Ya existe una estructura con el código EH-SEC-26 en este año.',
          undefined,
          ESPERA,
        ),
      ).toBeInTheDocument();
    });
  });

  test('los errores en vivo siguen funcionando al editar', async () => {
    await mostrar();
    const dialogo = await abrirEditar();
    const bloques = within(dialogo).getByLabelText('Bloques por día *');

    await userEvent.clear(bloques);
    await userEvent.type(bloques, '13');

    expect(
      await within(dialogo).findByText('Los bloques por día van de 1 a 12.'),
    ).toBeInTheDocument();
  });

  test('Cancelar cierra sin enviar nada', async () => {
    const registro = await mostrar();
    const dialogo = await abrirEditar();

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Cancelar' }),
    );

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(registro.ediciones).toEqual([]);
  });
});

describe('Eliminar estructura horaria', () => {
  const abrirEliminar = async () => {
    await userEvent.click(screen.getByRole('button', { name: 'Eliminar' }));
    return screen.findByRole('dialog');
  };

  test('pide confirmación con el código de la estructura antes de eliminar', async () => {
    const registro = await mostrar();

    const dialogo = await abrirEliminar();

    expect(
      within(dialogo).getByText('¿Eliminar la estructura EH-PRI-26?'),
    ).toBeInTheDocument();
    expect(registro.eliminadas).toEqual([]);
  });

  test('al confirmar elimina, avisa, cierra y la lista pasa a la primera que queda', async () => {
    const registro = await mostrar();
    const dialogo = await abrirEliminar();

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Eliminar' }),
    );

    expect(await screen.findByText('Estructura eliminada')).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(registro.eliminadas).toEqual(['estructura-pri']);
    expect(
      await screen.findByText('1 registrada', undefined, ESPERA),
    ).toBeInTheDocument();
    expect(
      await screen.findByRole('heading', { name: 'EH-SEC-26' }),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /EH-PRI-26/ }),
    ).not.toBeInTheDocument();
  });

  test('la principal con otras del nivel da 409: se avisa una sola vez y no se elimina', async () => {
    const mensaje = 'Marca otra como principal primero.';
    await mostrar({
      respuestaEliminar: () =>
        HttpResponse.json({ message: mensaje }, { status: 409 }),
    });
    const dialogo = await abrirEliminar();

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Eliminar' }),
    );

    expect(await screen.findAllByText(mensaje)).toHaveLength(1);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.queryByText('Estructura eliminada')).not.toBeInTheDocument();
    expect(screen.getByText('2 registradas')).toBeInTheDocument();
  });

  test('Cancelar no elimina', async () => {
    const registro = await mostrar();
    const dialogo = await abrirEliminar();

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Cancelar' }),
    );

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(registro.eliminadas).toEqual([]);
  });
});
