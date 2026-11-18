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
import { AmbienteResumen, ResumenAmbientes } from '@/types/api';

import { Ambientes } from '../ambientes';

// Figma > Personal Administrativo > CI-26 · Ambientes físicos (HU-43), nodo 857:10215

const API = env.API_URL;

const ambiente = (cambios: Partial<AmbienteResumen>): AmbienteResumen => ({
  id: 'amb',
  codigo: 'A-101',
  nombre: 'Aula',
  tipo: 'AULA_REGULAR',
  aforoMaximo: 30,
  recursos: [],
  estado: 'ACTIVO',
  ...cambios,
});

const AMBIENTES: AmbienteResumen[] = [
  ambiente({
    id: 'a101',
    codigo: 'A-101',
    nombre: 'Aula 101',
    aforoMaximo: 30,
    recursos: ['Pizarra acrílica', 'Proyector'],
  }),
  ambiente({
    id: 'a205',
    codigo: 'A-205',
    nombre: 'Aula 205',
    aforoMaximo: 32,
    recursos: ['Pizarra acrílica'],
    estado: 'INACTIVO',
  }),
  ambiente({
    id: 'a3',
    codigo: 'A-301',
    nombre: 'Aula 3',
    aforoMaximo: 28,
    recursos: [],
  }),
  ambiente({
    id: 'lab',
    codigo: 'LAB-01',
    nombre: 'Laboratorio de ciencias',
    tipo: 'ESPACIO_ESPECIAL',
    aforoMaximo: 30,
    recursos: ['Microscopios', 'Campana extractora'],
  }),
  ambiente({
    id: 'aud',
    codigo: 'AUD-01',
    nombre: 'Auditorio',
    tipo: 'ESPACIO_ESPECIAL',
    aforoMaximo: 180,
    recursos: ['Sonido', 'Escenario'],
  }),
];

const RESUMEN: ResumenAmbientes = {
  activos: 4,
  total: 5,
  aulasRegulares: 2,
  espaciosEspeciales: 2,
  aforoTotal: 268,
};

/** Responde como el backend: filtra por tipo, estado y nombre. */
const filtrar = (params: URLSearchParams): AmbienteResumen[] => {
  const q = (params.get('q') ?? '').toLowerCase();
  return AMBIENTES.filter(
    (a) =>
      (!params.get('tipo') || a.tipo === params.get('tipo')) &&
      (!params.get('estado') || a.estado === params.get('estado')) &&
      a.nombre.toLowerCase().includes(q),
  );
};

let pedidos: URLSearchParams[] = [];
let resumenes = 0;

beforeEach(() => {
  pedidos = [];
  resumenes = 0;
  server.use(
    http.get(`${API}/ambientes/resumen`, () => {
      resumenes++;
      return HttpResponse.json(RESUMEN);
    }),
    http.get(`${API}/ambientes`, ({ request }) => {
      const params = new URL(request.url).searchParams;
      pedidos.push(params);
      return HttpResponse.json(filtrar(params));
    }),
  );
});

const renderizar = async (props: React.ComponentProps<typeof Ambientes> = {}) =>
  renderApp(<Ambientes {...props} />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

/** Nombres de las filas, en el orden en que se ven (sin el encabezado). */
const nombresEnPantalla = () =>
  within(screen.getByRole('table'))
    .getAllByRole('row')
    .slice(1)
    .map((fila) => within(fila).getAllByRole('cell')[0].textContent);

const ultimoPedido = () => pedidos[pedidos.length - 1];

// ---------- Tarjetas ----------

test('muestra las 4 tarjetas con los textos de Figma', async () => {
  await renderizar();

  expect(await screen.findByText('de 5 registrados')).toBeInTheDocument();
  // "Aulas regulares" y "Espacios especiales" son también chips de filtro: el título de la tarjeta es el <p>.
  const tarjeta = (titulo: string) =>
    within(
      screen.getByText(titulo, { selector: 'p' }).closest('div') as HTMLElement,
    );
  expect(tarjeta('Ambientes activos').getByText('4')).toBeInTheDocument();
  expect(tarjeta('Aulas regulares').getByText('2')).toBeInTheDocument();
  expect(screen.getByText('para secciones')).toBeInTheDocument();
  expect(tarjeta('Espacios especiales').getByText('2')).toBeInTheDocument();
  expect(
    screen.getByText('laboratorios, talleres y otros'),
  ).toBeInTheDocument();
  expect(tarjeta('Aforo total').getByText('268')).toBeInTheDocument();
  expect(screen.getByText('personas en ambientes activos')).toBeInTheDocument();
});

test('las tarjetas no cambian con los filtros de la lista', async () => {
  await renderizar();
  await screen.findByText('de 5 registrados');

  await userEvent.click(
    screen.getByRole('button', { name: 'Espacios especiales' }),
  );
  await waitFor(() =>
    expect(ultimoPedido().get('tipo')).toBe('ESPACIO_ESPECIAL'),
  );

  expect(screen.getByText('de 5 registrados')).toBeInTheDocument();
  expect(resumenes).toBe(1);
});

// ---------- Tabla ----------

test('las columnas van en el orden de Figma y no hay columna Código', async () => {
  await renderizar();
  await screen.findByText('Aula 101');

  expect(screen.getAllByRole('columnheader').map((c) => c.textContent)).toEqual(
    ['Nombre', 'Tipo', 'Aforo máx.', 'Recursos', 'Estado', ''],
  );
  expect(screen.queryByText('Código')).not.toBeInTheDocument();
  // El código es interno: tampoco se ve en ninguna fila.
  expect(screen.queryByText('A-101')).not.toBeInTheDocument();
  expect(screen.queryByText('LAB-01')).not.toBeInTheDocument();
});

test('cada fila muestra nombre, tipo, aforo, recursos como chips y estado', async () => {
  await renderizar();
  await screen.findByText('Aula 101');

  const fila = within(
    screen.getByText('Aula 101').closest('tr') as HTMLElement,
  );
  expect(fila.getByText('Aula regular')).toBeInTheDocument();
  expect(fila.getByText('30 personas')).toBeInTheDocument();
  const recursos = within(
    fila.getByRole('list', { name: 'Recursos de Aula 101' }),
  );
  expect(recursos.getAllByRole('listitem').map((i) => i.textContent)).toEqual([
    'Pizarra acrílica',
    'Proyector',
  ]);
  expect(fila.getByText('Activo')).toBeInTheDocument();

  const laboratorio = within(
    screen.getByText('Laboratorio de ciencias').closest('tr') as HTMLElement,
  );
  expect(laboratorio.getByText('Espacio especial')).toBeInTheDocument();
});

test('un ambiente sin recursos muestra una raya', async () => {
  await renderizar();

  const fila = within(
    (await screen.findByText('Aula 3')).closest('tr') as HTMLElement,
  );

  expect(fila.getByText('—')).toBeInTheDocument();
});

test('abre con los activos y ordenados por nombre, y envía estado=ACTIVO', async () => {
  await renderizar();
  await screen.findByText('Aula 101');

  expect(pedidos[0].get('estado')).toBe('ACTIVO');
  expect(pedidos[0].has('tipo')).toBe(false);
  expect(pedidos[0].has('q')).toBe(false);
  // Orden natural por nombre: "Auditorio" antes que "Aula", y "Aula 3" antes que "Aula 101".
  expect(nombresEnPantalla()).toEqual([
    'Auditorio',
    'Aula 3',
    'Aula 101',
    'Laboratorio de ciencias',
  ]);
  expect(screen.queryByText('Aula 205')).not.toBeInTheDocument();
});

test('el pie dice cuántos se muestran de los registrados', async () => {
  await renderizar();

  expect(
    await screen.findByText('Mostrando 4 de 5 ambientes'),
  ).toBeInTheDocument();
});

// ---------- Filtros ----------

test('el filtro de estado abre en Activos y permite Todos e Inactivos', async () => {
  await renderizar();
  await screen.findByText('Aula 101');
  const estado = screen.getByRole('combobox', { name: 'Filtrar por estado' });
  expect(estado).toHaveValue('ACTIVO');
  expect(
    within(estado)
      .getAllByRole('option')
      .map((o) => o.textContent),
  ).toEqual(['Todos los estados', 'Activos', 'Inactivos']);

  await userEvent.selectOptions(estado, 'INACTIVO');
  expect(await screen.findByText('Aula 205')).toBeInTheDocument();
  expect(ultimoPedido().get('estado')).toBe('INACTIVO');
  expect(screen.queryByText('Aula 101')).not.toBeInTheDocument();

  await userEvent.selectOptions(estado, 'TODOS');
  expect(await screen.findByText('Aula 101')).toBeInTheDocument();
  expect(screen.getByText('Aula 205')).toBeInTheDocument();
  expect(ultimoPedido().has('estado')).toBe(false);
  expect(screen.getByText('Mostrando 5 de 5 ambientes')).toBeInTheDocument();
});

test('los chips filtran por tipo y el marcado se ve con aria-pressed', async () => {
  await renderizar();
  await screen.findByText('Aula 101');
  const chip = (nombre: string) => screen.getByRole('button', { name: nombre });
  expect(chip('Todos')).toHaveAttribute('aria-pressed', 'true');
  expect(chip('Aulas regulares')).toHaveAttribute('aria-pressed', 'false');

  await userEvent.click(chip('Aulas regulares'));

  await waitFor(() => expect(ultimoPedido().get('tipo')).toBe('AULA_REGULAR'));
  expect(chip('Aulas regulares')).toHaveAttribute('aria-pressed', 'true');
  expect(chip('Todos')).toHaveAttribute('aria-pressed', 'false');
  await waitFor(() =>
    expect(nombresEnPantalla()).toEqual(['Aula 3', 'Aula 101']),
  );

  await userEvent.click(chip('Todos'));

  // Vuelve a la lista sin tipo (ya consultada al abrir: la caché la muestra sin pedirla otra vez).
  await waitFor(() => expect(nombresEnPantalla()).toHaveLength(4));
  expect(chip('Todos')).toHaveAttribute('aria-pressed', 'true');
  expect(chip('Aulas regulares')).toHaveAttribute('aria-pressed', 'false');
});

test('el buscador filtra por nombre y espera a que se haga una pausa para consultar', async () => {
  await renderizar();
  await screen.findByText('Aula 101');
  const consultasAntes = pedidos.length;

  await userEvent.type(
    screen.getByRole('textbox', { name: 'Buscar ambiente por nombre' }),
    'audit',
  );

  // Mientras se escribe no se consulta tecla por tecla: una sola consulta, con el texto completo.
  await waitFor(() => expect(ultimoPedido().get('q')).toBe('audit'), {
    timeout: 3000,
  });
  expect(pedidos.length).toBe(consultasAntes + 1);
  await waitFor(() => expect(nombresEnPantalla()).toEqual(['Auditorio']));
});

test('el buscador no tiene texto de ayuda: Figma solo dibuja la lupa', async () => {
  await renderizar();
  await screen.findByText('Aula 101');

  expect(
    screen.getByRole('textbox', { name: 'Buscar ambiente por nombre' }),
  ).not.toHaveAttribute('placeholder');
});

// ---------- Estados vacío, de carga y de error ----------

test('si no hay ambientes activos dice «No hay ambientes disponibles.»', async () => {
  server.use(http.get(`${API}/ambientes`, () => HttpResponse.json([])));

  await renderizar();

  expect(
    await screen.findByText('No hay ambientes disponibles.'),
  ).toBeInTheDocument();
});

test('si los filtros no devuelven resultados dice «No hay ambientes que coincidan con los filtros.»', async () => {
  await renderizar();
  await screen.findByText('Aula 101');

  await userEvent.type(
    screen.getByRole('textbox', { name: 'Buscar ambiente por nombre' }),
    'zzzz',
  );

  expect(
    await screen.findByText(
      'No hay ambientes que coincidan con los filtros.',
      {},
      { timeout: 3000 },
    ),
  ).toBeInTheDocument();
  expect(
    screen.queryByText('No hay ambientes disponibles.'),
  ).not.toBeInTheDocument();
});

test('cambiar el estado o el tipo y no encontrar nada también es «no coinciden con los filtros»', async () => {
  server.use(
    http.get(`${API}/ambientes`, ({ request }) =>
      HttpResponse.json(
        new URL(request.url).searchParams.get('estado') === 'INACTIVO'
          ? []
          : AMBIENTES,
      ),
    ),
  );
  await renderizar();
  await screen.findByText('Aula 101');

  await userEvent.selectOptions(
    screen.getByRole('combobox', { name: 'Filtrar por estado' }),
    'INACTIVO',
  );

  expect(
    await screen.findByText('No hay ambientes que coincidan con los filtros.'),
  ).toBeInTheDocument();
});

test('muestra un indicador mientras carga', async () => {
  server.use(
    http.get(`${API}/ambientes`, async () => {
      // Más que lo que tarda renderApp en iniciar sesión: así el indicador sigue a la vista.
      await new Promise((resolver) => setTimeout(resolver, 1500));
      return HttpResponse.json(AMBIENTES);
    }),
  );

  // renderApp espera a que desaparezca cualquier "Loading" (el Spinner lo lleva oculto), así que no se espera
  // aquí: se mira el indicador mientras la lista todavía carga.
  const view = renderizar();
  expect(
    await screen.findByRole(
      'status',
      { name: 'Cargando ambientes' },
      { timeout: 3000 },
    ),
  ).toBeInTheDocument();
  await view;
  expect(
    await screen.findByText('Aula 101', {}, { timeout: 4000 }),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole('status', { name: 'Cargando ambientes' }),
  ).not.toBeInTheDocument();
});

test('si la lista falla muestra el error y permite reintentar', async () => {
  server.use(
    http.get(
      `${API}/ambientes`,
      () => HttpResponse.json({ message: 'Error interno' }, { status: 500 }),
      { once: true },
    ),
  );

  await renderizar();

  expect(
    await screen.findByText('No pudimos cargar los ambientes.'),
  ).toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));
  expect(await screen.findByText('Aula 101')).toBeInTheDocument();
  expect(
    screen.queryByText('No pudimos cargar los ambientes.'),
  ).not.toBeInTheDocument();
});

// ---------- Orden en el cliente ----------

test('se ordena en el cliente al pulsar un encabezado y se invierte al pulsarlo otra vez', async () => {
  await renderizar();
  await screen.findByText('Aula 101');
  const consultas = pedidos.length;

  await userEvent.click(screen.getByRole('button', { name: 'Aforo máx.' }));
  expect(nombresEnPantalla()).toEqual([
    'Aula 3', // 28
    'Aula 101', // 30
    'Laboratorio de ciencias', // 30 (empate: por nombre)
    'Auditorio', // 180
  ]);

  await userEvent.click(screen.getByRole('button', { name: 'Aforo máx.' }));
  expect(nombresEnPantalla()).toEqual([
    'Auditorio',
    'Aula 101',
    'Laboratorio de ciencias',
    'Aula 3',
  ]);
  // El orden no vuelve a pedir nada al servidor.
  expect(pedidos.length).toBe(consultas);
  expect(
    screen.getByRole('columnheader', { name: 'Aforo máx.' }),
  ).toHaveAttribute('aria-sort', 'descending');
});

// ---------- Fila inactiva ----------

test('la fila de un ambiente inactivo se ve atenuada y la de uno activo no', async () => {
  await renderizar();
  await screen.findByText('Aula 101');
  await userEvent.selectOptions(
    screen.getByRole('combobox', { name: 'Filtrar por estado' }),
    'TODOS',
  );

  const inactiva = await screen.findByText('Aula 205');

  expect(inactiva.closest('.opacity-50')).not.toBeNull();
  expect(screen.queryByText('Aula 101')?.closest('.opacity-50')).toBeNull();
  expect(
    within(inactiva.closest('tr') as HTMLElement).getByText('Inactivo'),
  ).toBeInTheDocument();
});

// ---------- Menú de la fila ----------

test('desactivar pide confirmación y, al confirmar, desactiva y recarga la lista y las tarjetas', async () => {
  let accion = '';
  server.use(
    http.post(`${API}/ambientes/:id/:accion`, ({ params }) => {
      accion = `${params.id}/${params.accion}`;
      return HttpResponse.json({ ...AMBIENTES[0], estado: 'INACTIVO' });
    }),
  );
  await renderizar();
  await screen.findByText('Aula 101');
  const consultas = pedidos.length;

  await userEvent.click(
    screen.getByRole('button', { name: 'Acciones de Aula 101' }),
  );
  await userEvent.click(
    await screen.findByRole('menuitem', { name: /desactivar/i }),
  );

  const modal = within(await screen.findByRole('dialog'));
  expect(
    modal.getByText('¿Desactivar el ambiente Aula 101?'),
  ).toBeInTheDocument();
  expect(accion).toBe(''); // todavía no se envió nada
  await userEvent.click(modal.getByRole('button', { name: 'Desactivar' }));

  await waitFor(() => expect(accion).toBe('a101/desactivar'));
  await waitFor(() => expect(pedidos.length).toBeGreaterThan(consultas));
  await waitFor(() => expect(resumenes).toBeGreaterThan(1));
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
});

test('cancelar la confirmación no envía nada', async () => {
  let enviado = false;
  server.use(
    http.post(`${API}/ambientes/:id/:accion`, () => {
      enviado = true;
      return HttpResponse.json({});
    }),
  );
  await renderizar();
  await userEvent.click(
    await screen.findByRole('button', { name: 'Acciones de Aula 101' }),
  );
  await userEvent.click(
    await screen.findByRole('menuitem', { name: /desactivar/i }),
  );

  await userEvent.click(
    within(await screen.findByRole('dialog')).getByRole('button', {
      name: 'Cancelar',
    }),
  );

  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  expect(enviado).toBe(false);
});

test('un ambiente inactivo ofrece Reactivar', async () => {
  let accion = '';
  server.use(
    http.post(`${API}/ambientes/:id/:accion`, ({ params }) => {
      accion = `${params.id}/${params.accion}`;
      return HttpResponse.json({ ...AMBIENTES[1], estado: 'ACTIVO' });
    }),
  );
  await renderizar();
  await screen.findByText('Aula 101');
  await userEvent.selectOptions(
    screen.getByRole('combobox', { name: 'Filtrar por estado' }),
    'INACTIVO',
  );
  await userEvent.click(
    await screen.findByRole('button', { name: 'Acciones de Aula 205' }),
  );
  expect(
    screen.queryByRole('menuitem', { name: /desactivar/i }),
  ).not.toBeInTheDocument();

  await userEvent.click(
    await screen.findByRole('menuitem', { name: /reactivar/i }),
  );
  const modal = within(await screen.findByRole('dialog'));
  expect(
    modal.getByText('¿Reactivar el ambiente Aula 205?'),
  ).toBeInTheDocument();
  await userEvent.click(modal.getByRole('button', { name: 'Reactivar' }));

  await waitFor(() => expect(accion).toBe('a205/reactivar'));
});

test('Editar llama a onEditar con el ambiente de la fila', async () => {
  const editados: AmbienteResumen[] = [];
  await renderizar({ onEditar: (a) => editados.push(a) });
  await userEvent.click(
    await screen.findByRole('button', { name: 'Acciones de Auditorio' }),
  );

  await userEvent.click(
    await screen.findByRole('menuitem', { name: /editar/i }),
  );

  expect(editados.map((a) => a.id)).toEqual(['aud']);
});

test('sin modal de edición, Editar aparece deshabilitado', async () => {
  await renderizar();
  await userEvent.click(
    await screen.findByRole('button', { name: 'Acciones de Aula 101' }),
  );

  expect(
    await screen.findByRole('menuitem', { name: /editar/i }),
  ).toHaveAttribute('aria-disabled', 'true');
});

// ---------- Botón Nuevo ambiente ----------

test('el botón «Nuevo ambiente» solo se muestra si hay modal para abrir', async () => {
  const { unmount } = await renderizar();
  await screen.findByText('Aula 101');
  expect(
    screen.queryByRole('button', { name: 'Nuevo ambiente' }),
  ).not.toBeInTheDocument();
  unmount();

  let abierto = 0;
  await renderizar({ onNuevo: () => abierto++ });
  await userEvent.click(
    await screen.findByRole('button', { name: 'Nuevo ambiente' }),
  );

  expect(abierto).toBe(1);
});
