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
  AnioEscolarDetalle,
  DocenteElegible,
  EstadoAnioEscolar,
  SolicitudCapacidad,
  SolicitudTutores,
} from '@/types/api';

import { SeccionesPestana } from '../secciones-pestana';

import { gradosDeEjemplo, seccion } from './datos';

// Menú ⋮ de CI-09: Asignar tutor (Figma "Modal - Asignar tutor"), Editar sección y Eliminar sección.

const API = env.API_URL;

// 1.° A (PRI-1) es de Rosa Huamán; Jorge Salas ya es tutor de 1.° B; Milagros no es tutora de ninguna.
const DOCENTES: DocenteElegible[] = [
  { id: 'tutor-PRI-1-A', nombre: 'Rosa Huamán Quispe', esTutorEsteAnio: true },
  { id: 'tutor-PRI-1-B', nombre: 'Jorge Salas Medina', esTutorEsteAnio: true },
  {
    id: 'cotutor-PRI-1-A',
    nombre: 'Silvia Herrera Luna',
    esTutorEsteAnio: false,
  },
  { id: 'doc-libre', nombre: 'Milagros Chávez Pinto', esTutorEsteAnio: false },
];

const anio = (estado: EstadoAnioEscolar): AnioEscolarDetalle => ({
  id: 'anio-2026',
  anio: 2026,
  fechaInicio: '2026-03-02',
  fechaFin: '2026-12-18',
  regimen: 'BIMESTRAL',
  estado,
  cantidadSecciones: 6,
  cantidadEstructuras: 0,
  periodos: [],
});

type Servidor = {
  tutores: SolicitudTutores[];
  capacidades: SolicitudCapacidad[];
  eliminadas: string[];
};

const mostrar = async (
  respuestas: {
    grados?: typeof gradosDeEjemplo;
    tutores?: () => Response;
    capacidad?: () => Response;
    eliminar?: () => Response;
  } = {},
): Promise<Servidor> => {
  const registro: Servidor = { tutores: [], capacidades: [], eliminadas: [] };
  const seccionA = gradosDeEjemplo[0].secciones[0];
  server.use(
    http.get(`${API}/configuracion/anios-escolares/anio-2026/secciones`, () =>
      HttpResponse.json(respuestas.grados ?? gradosDeEjemplo),
    ),
    http.get(`${API}/configuracion/anios-escolares/anio-2026`, () =>
      HttpResponse.json(anio('ACTIVO')),
    ),
    http.get(`${API}/configuracion/secciones/tutores-disponibles`, () =>
      HttpResponse.json(DOCENTES),
    ),
    http.put(
      `${API}/configuracion/secciones/${seccionA.id}/tutores`,
      async ({ request }) => {
        registro.tutores.push((await request.json()) as SolicitudTutores);
        return respuestas.tutores?.() ?? HttpResponse.json(seccionA);
      },
    ),
    http.put(
      `${API}/configuracion/secciones/${seccionA.id}`,
      async ({ request }) => {
        registro.capacidades.push((await request.json()) as SolicitudCapacidad);
        return (
          respuestas.capacidad?.() ??
          HttpResponse.json({ ...seccionA, capacidadMaxima: 40 })
        );
      },
    ),
    http.delete(`${API}/configuracion/secciones/${seccionA.id}`, () => {
      registro.eliminadas.push(seccionA.id);
      return respuestas.eliminar?.() ?? new HttpResponse(null, { status: 204 });
    }),
  );
  await renderApp(<SeccionesPestana anioId="anio-2026" estadoAnio="ACTIVO" />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });
  await screen.findByRole('table');
  return registro;
};

/** Abre una opción del menú ⋮ de la sección 1.° A. */
const abrirMenu = async (opcion: RegExp) => {
  await userEvent.click(
    screen.getByRole('button', { name: 'Acciones de 1.° A' }),
  );
  await userEvent.click(await screen.findByRole('menuitem', { name: opcion }));
  return screen.findByRole('dialog');
};

const combos = (dialogo: HTMLElement) =>
  within(dialogo).getAllByRole('combobox', { expanded: false });

const opcionesDe = async (combo: HTMLElement) => {
  await userEvent.click(combo);
  return within(screen.getByRole('listbox')).getAllByRole('option');
};

test('el menú ⋮ ofrece Asignar tutor, Editar sección y Eliminar sección', async () => {
  await mostrar();

  await userEvent.click(
    screen.getByRole('button', { name: 'Acciones de 1.° A' }),
  );

  expect(
    (await screen.findAllByRole('menuitem')).map((i) => i.textContent?.trim()),
  ).toEqual(['Asignar tutor', 'Editar sección', 'Eliminar sección']);
});

describe('Asignar tutor', () => {
  test('muestra el título y el texto de Figma con los tutores actuales elegidos', async () => {
    await mostrar();

    const dialogo = await abrirMenu(/Asignar tutor/);

    expect(
      await within(dialogo).findByRole('heading', {
        name: 'Asignar tutor · 1.° A',
      }),
    ).toBeInTheDocument();
    expect(
      within(dialogo).getByText(
        'Un docente solo puede ser tutor de una sección por año. El cotutor es opcional.',
      ),
    ).toBeInTheDocument();
    expect(within(dialogo).getByLabelText('Tutor *')).toHaveValue(
      'tutor-PRI-1-A',
    );
    expect(within(dialogo).getByLabelText('Cotutor (opcional)')).toHaveValue(
      'cotutor-PRI-1-A',
    );
  });

  test('no muestra "Vigente desde" ni el historial, que el MVP no guarda', async () => {
    await mostrar();

    const dialogo = await abrirMenu(/Asignar tutor/);
    await within(dialogo).findByLabelText('Tutor *');

    expect(
      within(dialogo).queryByText(/Vigente desde/),
    ).not.toBeInTheDocument();
    expect(
      within(dialogo).queryByText('Historial de tutoría'),
    ).not.toBeInTheDocument();
  });

  test('deshabilita a quien ya es tutor de otra sección, pero no al tutor de esta', async () => {
    await mostrar();
    const dialogo = await abrirMenu(/Asignar tutor/);
    await within(dialogo).findByLabelText('Tutor *');

    const opciones = await opcionesDe(combos(dialogo)[0]);

    const estado = Object.fromEntries(
      opciones.map((o) => [
        o.textContent,
        o.getAttribute('aria-disabled') === 'true',
      ]),
    );
    expect(estado).toEqual({
      'Rosa Huamán Quispe': false,
      'Jorge Salas Medina': true,
      'Silvia Herrera Luna': false,
      'Milagros Chávez Pinto': false,
    });
  });

  test('elegir una opción deshabilitada no la selecciona', async () => {
    await mostrar();
    const dialogo = await abrirMenu(/Asignar tutor/);
    await within(dialogo).findByLabelText('Tutor *');

    const opciones = await opcionesDe(combos(dialogo)[0]);
    await userEvent.click(
      opciones.find((o) => o.textContent === 'Jorge Salas Medina')!,
    );

    // La lista sigue abierta y el campo conserva a Rosa.
    expect(
      within(dialogo).getAllByRole('combobox', { expanded: true })[0],
    ).toHaveTextContent('Rosa Huamán Quispe');
  });

  test('el cotutor ofrece "Sin cotutor" y no deja elegir al tutor', async () => {
    await mostrar();
    const dialogo = await abrirMenu(/Asignar tutor/);
    await within(dialogo).findByLabelText('Tutor *');

    const opciones = await opcionesDe(combos(dialogo)[1]);

    expect(opciones.map((o) => o.textContent)).toEqual([
      'Sin cotutor',
      'Rosa Huamán Quispe',
      'Jorge Salas Medina',
      'Silvia Herrera Luna',
      'Milagros Chávez Pinto',
    ]);
    // Rosa es el tutor elegido: no puede ser también cotutora. Jorge sí puede ser cotutor (la regla es solo del tutor).
    expect(opciones[1]).toHaveAttribute('aria-disabled', 'true');
    expect(opciones[2]).not.toHaveAttribute('aria-disabled');
  });

  test('guarda el tutor y el cotutor elegidos, avisa y cierra', async () => {
    const registro = await mostrar();
    const dialogo = await abrirMenu(/Asignar tutor/);
    await within(dialogo).findByLabelText('Tutor *');

    const tutores = await opcionesDe(combos(dialogo)[0]);
    await userEvent.click(
      tutores.find((o) => o.textContent === 'Milagros Chávez Pinto')!,
    );
    const cotutores = await opcionesDe(combos(dialogo)[1]);
    await userEvent.click(
      cotutores.find((o) => o.textContent === 'Sin cotutor')!,
    );
    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Guardar' }),
    );

    expect(await screen.findByText('Tutor asignado')).toBeInTheDocument();
    expect(registro.tutores).toEqual([
      { tutorId: 'doc-libre', cotutorId: null },
    ]);
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
  });

  test('sin tutor no envía y pide elegir uno', async () => {
    // Una sección que aún no tiene tutor ni cotutor.
    const registro = await mostrar({
      grados: [
        {
          ...gradosDeEjemplo[0],
          secciones: [seccion('PRI-1', 'A', 0, null)],
        },
      ],
    });
    const dialogo = await abrirMenu(/Asignar tutor/);
    await within(dialogo).findByLabelText('Tutor *');

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Guardar' }),
    );

    expect(await within(dialogo).findByText('Elige un tutor.')).toBeVisible();
    expect(registro.tutores).toEqual([]);
  });

  test('un error del backend se muestra una sola vez y el modal sigue abierto', async () => {
    const mensaje = 'Elige otro tutor.';
    await mostrar({
      tutores: () => HttpResponse.json({ message: mensaje }, { status: 409 }),
    });
    const dialogo = await abrirMenu(/Asignar tutor/);
    await within(dialogo).findByLabelText('Tutor *');

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Guardar' }),
    );

    expect(await screen.findAllByText(mensaje)).toHaveLength(1);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  test('Cancelar cierra sin enviar', async () => {
    const registro = await mostrar();
    const dialogo = await abrirMenu(/Asignar tutor/);
    await within(dialogo).findByLabelText('Tutor *');

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Cancelar' }),
    );

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(registro.tutores).toEqual([]);
  });
});

describe('Editar sección', () => {
  test('abre con la capacidad actual y el número de alumnos', async () => {
    await mostrar();

    const dialogo = await abrirMenu(/Editar sección/);

    expect(
      within(dialogo).getByRole('heading', { name: 'Editar sección · 1.° A' }),
    ).toBeInTheDocument();
    expect(within(dialogo).getByLabelText(/Capacidad máxima/)).toHaveValue(
      '30',
    );
    expect(
      within(dialogo).getByText(/Hay 29 alumnos que ocupan vacante/),
    ).toBeInTheDocument();
  });

  test('envía la capacidad nueva como número, avisa y cierra', async () => {
    const registro = await mostrar();
    const dialogo = await abrirMenu(/Editar sección/);

    const campo = within(dialogo).getByLabelText(/Capacidad máxima/);
    await userEvent.clear(campo);
    await userEvent.type(campo, '40');
    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Guardar' }),
    );

    expect(await screen.findByText('Sección actualizada')).toBeInTheDocument();
    expect(registro.capacidades).toEqual([{ capacidadMaxima: 40 }]);
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
  });

  test.each(['0', '46', 'abc'])(
    'la capacidad %s da el aviso de Figma y no se envía',
    async (valor) => {
      const registro = await mostrar();
      const dialogo = await abrirMenu(/Editar sección/);

      const campo = within(dialogo).getByLabelText(/Capacidad máxima/);
      await userEvent.clear(campo);
      await userEvent.type(campo, valor);
      await userEvent.click(
        within(dialogo).getByRole('button', { name: 'Guardar' }),
      );

      expect(await within(dialogo).findByRole('alert')).toHaveTextContent(
        'Entre 1 y 45 alumnos.',
      );
      expect(registro.capacidades).toEqual([]);
    },
  );

  test('un error del backend (menor que los matriculados) sale una sola vez y el modal sigue abierto', async () => {
    const mensaje = 'La capacidad no puede ser menor que los alumnos.';
    await mostrar({
      capacidad: () => HttpResponse.json({ message: mensaje }, { status: 400 }),
    });
    const dialogo = await abrirMenu(/Editar sección/);

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Guardar' }),
    );

    expect(await screen.findAllByText(mensaje)).toHaveLength(1);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });
});

describe('Eliminar sección', () => {
  test('pide confirmación antes de eliminar', async () => {
    const registro = await mostrar();

    const dialogo = await abrirMenu(/Eliminar sección/);

    expect(
      within(dialogo).getByText('¿Eliminar la sección 1.° A?'),
    ).toBeInTheDocument();
    expect(registro.eliminadas).toEqual([]);
  });

  test('al confirmar elimina, avisa y cierra', async () => {
    const registro = await mostrar();
    const dialogo = await abrirMenu(/Eliminar sección/);

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Eliminar' }),
    );

    expect(await screen.findByText('Sección eliminada')).toBeInTheDocument();
    expect(registro.eliminadas).toHaveLength(1);
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
  });

  test('si la sección tiene historial, el 409 del backend se ve una vez y el diálogo sigue abierto', async () => {
    const mensaje = 'La sección tiene matrículas o cambios de sección.';
    await mostrar({
      eliminar: () => HttpResponse.json({ message: mensaje }, { status: 409 }),
    });
    const dialogo = await abrirMenu(/Eliminar sección/);

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Eliminar' }),
    );

    expect(await screen.findAllByText(mensaje)).toHaveLength(1);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.queryByText('Sección eliminada')).not.toBeInTheDocument();
  });

  test('Cancelar no elimina', async () => {
    const registro = await mostrar();
    const dialogo = await abrirMenu(/Eliminar sección/);

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Cancelar' }),
    );

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(registro.eliminadas).toEqual([]);
  });
});
