import { HttpResponse, http } from 'msw';

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

import { PeriodosMatriculaLista } from '../periodos-matricula-lista';

import {
  API,
  cerrado2025,
  cerrado2026,
  fila,
  programado2027,
  servirPeriodos,
} from './datos';

// Figma > Personal Administrativo > pantalla 13 "Periodos de matrícula" > listado (HU-23)

// Navegar de verdad en jsdom rompe el router de la prueba: se sustituye `useNavigate` y se comprueba el destino.
const { navegar, enlaces } = vi.hoisted(() => ({
  navegar: vi.fn(),
  enlaces: {
    MATRICULAS_DEL_PERIODO_DISPONIBLE: false,
    TARIFARIO_DISPONIBLE: false,
    TEXTO_DISPONIBLE_PRONTO: 'Disponible pronto',
  },
}));
vi.mock('react-router', async () => ({
  ...(await vi.importActual<typeof import('react-router')>('react-router')),
  useNavigate: () => navegar,
}));
vi.mock('../enlaces', () => enlaces);

beforeEach(() => {
  navegar.mockClear();
  enlaces.MATRICULAS_DEL_PERIODO_DISPONIBLE = false;
});

const mostrar = async () =>
  renderApp(<PeriodosMatriculaLista />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

const filaDe = (nombre: string) =>
  screen.findByRole('row', { name: new RegExp(nombre) });

test('la tabla muestra cada periodo con sus fechas en hora de Lima, matrículas y estado', async () => {
  servirPeriodos([programado2027, cerrado2026, cerrado2025]);
  await mostrar();

  const tabla = await screen.findByRole('table');
  expect(
    within(tabla)
      .getAllByRole('columnheader')
      .map((c) => c.textContent),
  ).toEqual([
    'Periodo',
    'Año escolar',
    'Inicio',
    'Fin',
    'Matrículas',
    'Estado',
    '',
  ]);
  const filas = within(tabla).getAllByRole('row').slice(1);
  expect(filas).toHaveLength(3);
  const celdas = (f: HTMLElement) =>
    within(f)
      .getAllByRole('cell')
      .slice(0, 6)
      .map((c) => c.textContent);
  expect(celdas(filas[0])).toEqual([
    'Matrícula 2027',
    '2027',
    '01/12/2026',
    '15/02/2027',
    '-',
    'Programado',
  ]);
  expect(celdas(filas[1])).toEqual([
    'Matrícula 2026',
    '2026',
    '01/12/2025',
    '15/02/2026',
    '331',
    'Cerrado',
  ]);
});

test('el estado es una pastilla informativa, no un botón', async () => {
  servirPeriodos([programado2027, cerrado2026]);
  await mostrar();

  const fila2027 = await filaDe('Matrícula 2027');

  expect(within(fila2027).getByText('Programado')).toHaveClass(
    'rounded-full',
    'border-stroke-warning',
  );
  // El único botón de la fila es el menú ⋮.
  expect(within(fila2027).getAllByRole('button')).toHaveLength(1);
});

test('el filtro "Año escolar" ofrece los años de todos los periodos y se envía al backend', async () => {
  const consultas: URLSearchParams[] = [];
  servirPeriodos([programado2027, cerrado2026, cerrado2025], { consultas });
  await mostrar();
  await screen.findByText('Matrícula 2027');

  const filtro = screen.getByLabelText('Filtrar por año escolar');
  expect(
    within(filtro)
      .getAllByRole('option', { hidden: true })
      .map((o) => o.textContent),
  ).toEqual(['Año escolar: Todos', '2027', '2026', '2025']);

  await userEvent.selectOptions(filtro, '2026');

  await waitFor(() => expect(consultas.at(-1)?.get('anio')).toBe('2026'));
  expect(await screen.findByText('Matrícula 2026')).toBeInTheDocument();
  expect(screen.queryByText('Matrícula 2027')).not.toBeInTheDocument();
});

test('el filtro "Estado" ofrece los tres estados y se envía al backend', async () => {
  const consultas: URLSearchParams[] = [];
  servirPeriodos([programado2027, cerrado2026, cerrado2025], { consultas });
  await mostrar();
  await screen.findByText('Matrícula 2027');

  const filtro = screen.getByLabelText('Filtrar por estado');
  expect(
    within(filtro)
      .getAllByRole('option', { hidden: true })
      .map((o) => o.textContent),
  ).toEqual(['Estado: Todos', 'Programado', 'Abierto', 'Cerrado']);

  await userEvent.selectOptions(filtro, 'CERRADO');

  await waitFor(() => expect(consultas.at(-1)?.get('estado')).toBe('CERRADO'));
  await waitFor(() =>
    expect(screen.queryByText('Matrícula 2027')).not.toBeInTheDocument(),
  );
  expect(screen.getByText('Matrícula 2025')).toBeInTheDocument();
});

test('con muchos periodos se pagina y se vuelve a la primera página al filtrar', async () => {
  const consultas: URLSearchParams[] = [];
  const muchos = Array.from({ length: 10 }, (_, i) =>
    fila(2026 - i, 'CERRADO'),
  );
  servirPeriodos(muchos, { consultas });
  await mostrar();
  await screen.findByText('Matrícula 2026');
  expect(screen.getByText(/Mostrando/)).toHaveTextContent(
    'Mostrando 1-8 de 10',
  );

  await userEvent.click(screen.getByRole('button', { name: '2' }));

  await waitFor(() => expect(consultas.at(-1)?.get('page')).toBe('2'));
  expect(await screen.findByText('Matrícula 2018')).toBeInTheDocument();

  await userEvent.selectOptions(
    screen.getByLabelText('Filtrar por estado'),
    'CERRADO',
  );

  await waitFor(() => expect(consultas.at(-1)?.get('page')).toBe('1'));
});

test('sin periodos muestra el estado vacío y no hay paginación', async () => {
  servirPeriodos([]);
  await mostrar();

  expect(
    await screen.findByText('Aún no hay periodos de matrícula.'),
  ).toBeInTheDocument();
  expect(screen.queryByText(/Mostrando/)).not.toBeInTheDocument();
});

test('sin resultados para los filtros lo dice', async () => {
  servirPeriodos([cerrado2026]);
  await mostrar();
  await screen.findByText('Matrícula 2026');

  await userEvent.selectOptions(
    screen.getByLabelText('Filtrar por estado'),
    'ABIERTO',
  );

  expect(
    await screen.findByText(
      'No se encontraron periodos de matrícula con esos filtros.',
    ),
  ).toBeInTheDocument();
});

test('si el listado falla muestra un aviso en la pantalla', async () => {
  server.use(
    http.get(API, () =>
      HttpResponse.json({ message: 'Error inesperado.' }, { status: 500 }),
    ),
  );
  await mostrar();

  expect(
    await screen.findByText('No pudimos cargar los periodos de matrícula.'),
  ).toBeInTheDocument();
  expect(screen.queryByRole('table')).not.toBeInTheDocument();
});

describe('menú ⋮ de cada fila', () => {
  const abrirMenu = async (nombre: string) => {
    await userEvent.click(
      await screen.findByRole('button', { name: `Acciones de ${nombre}` }),
    );
    return screen.findAllByRole('menuitem');
  };
  const nombres = (opciones: HTMLElement[]) =>
    opciones.map((o) => o.textContent?.trim());

  beforeEach(() => {
    servirPeriodos([programado2027, fila(2028, 'ABIERTO'), cerrado2026]);
  });

  test('programado: Ver, Ver matrículas (deshabilitada, "Disponible pronto"), Editar y Eliminar', async () => {
    await mostrar();

    const opciones = await abrirMenu('Matrícula 2027');

    expect(nombres(opciones)).toEqual([
      'Ver',
      'Ver matrículasDisponible pronto',
      'Editar',
      'Eliminar',
    ]);
    expect(opciones[1]).toHaveAttribute('aria-disabled', 'true');
  });

  test('abierto: Ver, Ver matrículas y Editar (abre la vista de solo lectura), sin Eliminar', async () => {
    await mostrar();

    const opciones = await abrirMenu('Matrícula 2028');

    expect(nombres(opciones)).toEqual([
      'Ver',
      'Ver matrículasDisponible pronto',
      'Editar',
    ]);
  });

  test('cerrado: solo Ver y Ver matrículas, sin Editar', async () => {
    await mostrar();

    const opciones = await abrirMenu('Matrícula 2026');

    expect(nombres(opciones)).toEqual([
      'Ver',
      'Ver matrículasDisponible pronto',
    ]);
  });

  test('Ver y Editar llevan a su ruta con el id del periodo', async () => {
    await mostrar();

    await userEvent.click((await abrirMenu('Matrícula 2027'))[0]);
    expect(navegar).toHaveBeenLastCalledWith(
      '/app/administrativo/matricula/periodos/periodo-2027',
    );

    await userEvent.click((await abrirMenu('Matrícula 2027'))[2]);
    expect(navegar).toHaveBeenLastCalledWith(
      '/app/administrativo/matricula/periodos/periodo-2027/editar',
    );
  });

  test('"Ver matrículas" deshabilitada no navega', async () => {
    await mostrar();

    await userEvent.click((await abrirMenu('Matrícula 2027'))[1]);

    expect(navegar).not.toHaveBeenCalled();
  });

  test('con la pantalla de Matrícula disponible, "Ver matrículas" se habilita y lleva a ella', async () => {
    enlaces.MATRICULAS_DEL_PERIODO_DISPONIBLE = true;
    await mostrar();

    const opciones = await abrirMenu('Matrícula 2027');
    expect(opciones[1]).not.toHaveAttribute('aria-disabled', 'true');
    expect(nombres(opciones)[1]).toBe('Ver matrículas');
    await userEvent.click(opciones[1]);

    expect(navegar).toHaveBeenLastCalledWith(
      '/app/administrativo/matricula/periodos/periodo-2027/matriculas',
    );
  });
});

describe('eliminar un periodo programado', () => {
  const abrirEliminar = async () => {
    await userEvent.click(
      await screen.findByRole('button', { name: 'Acciones de Matrícula 2027' }),
    );
    await userEvent.click(
      await screen.findByRole('menuitem', { name: 'Eliminar' }),
    );
    return screen.findByRole('dialog');
  };

  test('pide confirmación y, al confirmar, elimina el periodo, avisa y quita la fila', async () => {
    const filas = [programado2027, cerrado2026];
    let eliminado = '';
    servirPeriodos(filas);
    server.use(
      http.delete(`${API}/:id`, ({ params }) => {
        eliminado = String(params.id);
        servirPeriodos([cerrado2026]);
        return new HttpResponse(null, { status: 204 });
      }),
    );
    await mostrar();

    const dialogo = await abrirEliminar();
    expect(
      within(dialogo).getByText('¿Eliminar Matrícula 2027?'),
    ).toBeInTheDocument();
    expect(eliminado).toBe('');
    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Eliminar' }),
    );

    await waitFor(() => expect(eliminado).toBe('periodo-2027'));
    expect(
      await screen.findByText('Periodo de matrícula eliminado'),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    await waitFor(() =>
      expect(
        screen.queryByRole('row', { name: /Matrícula 2027/ }),
      ).not.toBeInTheDocument(),
    );
  });

  test('"Cancelar" cierra sin eliminar', async () => {
    let llamado = false;
    servirPeriodos([programado2027]);
    server.use(
      http.delete(`${API}/:id`, () => {
        llamado = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    await mostrar();

    const dialogo = await abrirEliminar();
    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Cancelar' }),
    );

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(llamado).toBe(false);
    expect(
      screen.getByRole('row', { name: /Matrícula 2027/ }),
    ).toBeInTheDocument();
  });

  test('si el backend lo rechaza (409) muestra su mensaje tal cual, cierra y el periodo sigue', async () => {
    const mensaje =
      'No se puede eliminar el periodo de matrícula: el año 2027 ya tiene matrículas.';
    servirPeriodos([programado2027]);
    server.use(
      http.delete(`${API}/:id`, () =>
        HttpResponse.json({ message: mensaje }, { status: 409 }),
      ),
    );
    await mostrar();

    const dialogo = await abrirEliminar();
    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Eliminar' }),
    );

    expect(await screen.findByText(mensaje)).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(
      screen.queryByText('Periodo de matrícula eliminado'),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole('row', { name: /Matrícula 2027/ }),
    ).toBeInTheDocument();
  });
});
