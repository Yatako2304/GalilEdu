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
import { EstadoAnioEscolar, EstructuraHorariaResumen } from '@/types/api';

import { EstructuraHorariaPestana } from '../estructura-horaria-pestana';

import { detalleDe, primaria, secundaria } from './datos';

// Figma > CI-12 · Estructura horaria (HU-44)

const API = env.API_URL;

const mostrar = async ({
  estructuras = [primaria, secundaria],
  anioModificable = true,
  estadoAnio = 'PLANIFICADO',
}: {
  estructuras?: EstructuraHorariaResumen[];
  anioModificable?: boolean;
  estadoAnio?: EstadoAnioEscolar;
} = {}) => {
  const pedidos: string[] = [];
  server.use(
    http.get(
      `${API}/configuracion/anios-escolares/anio-2026/estructuras-horarias`,
      () => HttpResponse.json({ anioModificable, estructuras }),
    ),
    http.get(`${API}/configuracion/estructuras-horarias/:id`, ({ params }) => {
      pedidos.push(String(params.id));
      const e = estructuras.find((x) => x.id === params.id);
      return e
        ? HttpResponse.json(detalleDe(e))
        : HttpResponse.json({ message: 'No existe' }, { status: 404 });
    }),
  );
  await renderApp(
    <EstructuraHorariaPestana anioId="anio-2026" estadoAnio={estadoAnio} />,
    { user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }) },
  );
  return pedidos;
};

test('la lista muestra el título, cuántas hay registradas y una tarjeta por estructura', async () => {
  await mostrar();

  expect(
    await screen.findByRole('heading', { name: 'Estructuras del año' }),
  ).toBeInTheDocument();
  expect(screen.getByText('2 registradas')).toBeInTheDocument();

  const lista = screen.getByRole('list');
  const [pri, sec] = within(lista).getAllByRole('listitem');
  expect(within(pri).getByText('EH-PRI-26')).toBeInTheDocument();
  expect(within(pri).getByText('5 días')).toBeInTheDocument();
  expect(
    within(pri).getByText('Primaria · jornada de mañana'),
  ).toBeInTheDocument();
  expect(
    within(pri).getByText('07:45 – 14:20 · 8 bloques de 45 min'),
  ).toBeInTheDocument();
  expect(within(sec).getByText('EH-SEC-26')).toBeInTheDocument();
  expect(within(sec).getByText('3 días')).toBeInTheDocument();
  expect(
    within(sec).getByText('07:30 – 14:50 · 9 bloques de 45 min'),
  ).toBeInTheDocument();
});

test('cada tarjeta marca si es la principal de su nivel', async () => {
  await mostrar({
    estructuras: [primaria, { ...secundaria, esPrincipal: false }],
  });

  const [pri, sec] = within(await screen.findByRole('list')).getAllByRole(
    'listitem',
  );
  expect(within(pri).getByText('Principal')).toBeInTheDocument();
  expect(within(sec).queryByText('Principal')).not.toBeInTheDocument();
});

test('abre con la primera estructura elegida y muestra sus datos', async () => {
  const pedidos = await mostrar();

  expect(
    await screen.findByRole('heading', { name: 'EH-PRI-26' }),
  ).toBeInTheDocument();
  expect(pedidos).toEqual(['estructura-pri']);
  expect(screen.getByRole('button', { name: /EH-PRI-26/ })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  expect(screen.getByRole('button', { name: /EH-SEC-26/ })).toHaveAttribute(
    'aria-pressed',
    'false',
  );

  const datos = Object.fromEntries(
    screen
      .getAllByRole('term')
      .map((t) => [t.textContent, t.nextElementSibling?.textContent]),
  );
  expect(datos).toEqual({
    'Días lectivos': 'Lun – Vie',
    'Hora de entrada': '07:45',
    'Hora de salida': '14:20',
    'Bloques por día': '8 × 45 min',
    Recesos: '20 + 15 min',
    'Bloques / semana': '40',
  });
});

test('la jornada semanal tiene una columna por día lectivo, los bloques con su horario y los recesos en toda la fila', async () => {
  await mostrar();

  const grilla = await screen.findByRole('table', { name: 'Jornada semanal' });
  expect(
    within(grilla)
      .getAllByRole('columnheader')
      .map((c) => c.textContent),
  ).toEqual(['Bloque', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes']);

  const filas = within(grilla).getAllByRole('row');
  // Encabezado, Bloque 1, Bloque 2, Receso, Bloque 3
  expect(filas).toHaveLength(5);
  expect(within(filas[1]).getByText('Bloque 1')).toBeInTheDocument();
  expect(within(filas[1]).getByText('07:45 – 08:30')).toBeInTheDocument();
  expect(within(filas[3]).getByText('09:15 – 09:35')).toBeInTheDocument();
  expect(within(filas[3]).getByText('Receso · 20 min')).toBeInTheDocument();
  // El receso ocupa una sola celda para todos los días; el bloque tiene una por día.
  expect(within(filas[3]).getAllByRole('cell')).toHaveLength(2);
  expect(within(filas[1]).getAllByRole('cell')).toHaveLength(6);
  expect(
    screen.getByText(
      'Los bloques se llenarán con cursos y docentes al generar los horarios de cada sección.',
    ),
  ).toBeInTheDocument();
});

test('al elegir otra estructura pide su detalle y muestra sus días y su jornada', async () => {
  const pedidos = await mostrar();
  await screen.findByRole('heading', { name: 'EH-PRI-26' });

  await userEvent.click(screen.getByRole('button', { name: /EH-SEC-26/ }));

  expect(
    await screen.findByRole('heading', { name: 'EH-SEC-26' }),
  ).toBeInTheDocument();
  expect(pedidos).toEqual(['estructura-pri', 'estructura-sec']);
  expect(screen.getByText('Lun, Mié, Vie')).toBeInTheDocument();
  expect(
    within(screen.getByRole('table', { name: 'Jornada semanal' }))
      .getAllByRole('columnheader')
      .map((c) => c.textContent),
  ).toEqual(['Bloque', 'Lunes', 'Miércoles', 'Viernes']);
  expect(screen.getByRole('button', { name: /EH-SEC-26/ })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
});

test('con el año modificable "Nueva" está habilitado', async () => {
  await mostrar({ anioModificable: true });

  expect(
    await screen.findByRole('button', { name: 'Nueva' }),
  ).not.toHaveAttribute('aria-disabled');
});

test('con el año no modificable "Nueva" queda bloqueado y un tooltip dice por qué', async () => {
  await mostrar({ anioModificable: false, estadoAnio: 'ACTIVO' });

  const nueva = await screen.findByRole('button', { name: 'Nueva' });
  expect(nueva).toHaveAttribute('aria-disabled', 'true');

  await userEvent.hover(nueva);
  expect(
    await screen.findByText(
      'Solo se puede modificar la estructura horaria de un año planificado.',
    ),
  ).toBeInTheDocument();
});

test('el bloqueo lo decide `anioModificable` del backend, no el estado del año', async () => {
  await mostrar({ anioModificable: false, estadoAnio: 'PLANIFICADO' });

  expect(await screen.findByRole('button', { name: 'Nueva' })).toHaveAttribute(
    'aria-disabled',
    'true',
  );
});

test('sin estructuras avisa y no muestra ninguna tarjeta', async () => {
  const pedidos = await mostrar({ estructuras: [] });

  expect(
    await screen.findByText('Este año aún no tiene estructuras horarias.'),
  ).toBeInTheDocument();
  expect(screen.getByText('0 registradas')).toBeInTheDocument();
  expect(screen.queryByRole('table')).not.toBeInTheDocument();
  expect(pedidos).toEqual([]);
});

test('con una sola estructura el contador va en singular', async () => {
  await mostrar({ estructuras: [primaria] });

  expect(await screen.findByText('1 registrada')).toBeInTheDocument();
});

test('si no se pueden cargar las estructuras avisa sin mostrar la lista', async () => {
  server.use(
    http.get(
      `${API}/configuracion/anios-escolares/anio-2026/estructuras-horarias`,
      () => HttpResponse.json({ message: 'Error' }, { status: 500 }),
    ),
  );
  await renderApp(
    <EstructuraHorariaPestana anioId="anio-2026" estadoAnio="PLANIFICADO" />,
    { user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }) },
  );

  expect(
    await screen.findByText('No pudimos cargar las estructuras horarias.'),
  ).toBeInTheDocument();
  expect(screen.queryByRole('list')).not.toBeInTheDocument();
});

test('si no se puede cargar el detalle avisa en la tarjeta y la lista sigue', async () => {
  server.use(
    http.get(
      `${API}/configuracion/anios-escolares/anio-2026/estructuras-horarias`,
      () =>
        HttpResponse.json({
          anioModificable: true,
          estructuras: [primaria],
        }),
    ),
    http.get(`${API}/configuracion/estructuras-horarias/:id`, () =>
      HttpResponse.json({ message: 'Error' }, { status: 500 }),
    ),
  );
  await renderApp(
    <EstructuraHorariaPestana anioId="anio-2026" estadoAnio="PLANIFICADO" />,
    { user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }) },
  );

  expect(
    await screen.findByText('No pudimos cargar la estructura horaria.'),
  ).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /EH-PRI-26/ })).toBeInTheDocument();
});

// --- "Nueva" y el modal ---

test('"Nueva" abre el modal "Nueva estructura horaria"', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares/anio-2026`, () =>
      HttpResponse.json({
        id: 'anio-2026',
        anio: 2026,
        fechaInicio: '2026-03-02',
        fechaFin: '2026-12-18',
        regimen: 'BIMESTRAL',
        estado: 'PLANIFICADO',
        cantidadSecciones: 0,
        cantidadEstructuras: 2,
        periodos: [],
      }),
    ),
  );
  await mostrar();

  await userEvent.click(await screen.findByRole('button', { name: 'Nueva' }));

  expect(
    await screen.findByRole('heading', { name: 'Nueva estructura horaria' }),
  ).toBeInTheDocument();
});

test('con el año no modificable "Nueva" no abre el modal', async () => {
  await mostrar({ anioModificable: false });

  await userEvent.click(await screen.findByRole('button', { name: 'Nueva' }));

  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

test('al crear una estructura el modal se cierra, la lista se recarga y la nueva queda elegida', async () => {
  const nueva = { ...secundaria, id: 'estructura-nueva', codigo: 'EH-TAR-26' };
  let creada = false;
  server.use(
    http.get(`${API}/configuracion/anios-escolares/anio-2026`, () =>
      HttpResponse.json({
        id: 'anio-2026',
        anio: 2026,
        fechaInicio: '2026-03-02',
        fechaFin: '2026-12-18',
        regimen: 'BIMESTRAL',
        estado: 'PLANIFICADO',
        cantidadSecciones: 0,
        cantidadEstructuras: 2,
        periodos: [],
      }),
    ),
    http.get(
      `${API}/configuracion/anios-escolares/anio-2026/estructuras-horarias`,
      () =>
        HttpResponse.json({
          anioModificable: true,
          estructuras: creada
            ? [primaria, secundaria, nueva]
            : [primaria, secundaria],
        }),
    ),
    http.get(`${API}/configuracion/estructuras-horarias/:id`, ({ params }) => {
      const todas = [primaria, secundaria, nueva];
      const e = todas.find((x) => x.id === params.id);
      return e
        ? HttpResponse.json(detalleDe(e))
        : HttpResponse.json({ message: 'No existe' }, { status: 404 });
    }),
    http.post(
      `${API}/configuracion/anios-escolares/anio-2026/estructuras-horarias/vista-previa`,
      () => HttpResponse.json({ valida: true, errores: [], jornada: null }),
    ),
    http.post(
      `${API}/configuracion/anios-escolares/anio-2026/estructuras-horarias`,
      () => {
        creada = true;
        return HttpResponse.json(
          { estructura: detalleDe(nueva), advertencias: [] },
          { status: 201 },
        );
      },
    ),
  );
  await renderApp(
    <EstructuraHorariaPestana anioId="anio-2026" estadoAnio="PLANIFICADO" />,
    { user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }) },
  );
  await userEvent.click(await screen.findByRole('button', { name: 'Nueva' }));
  const dialogo = await screen.findByRole('dialog');

  await userEvent.type(within(dialogo).getByLabelText('Código *'), 'eh-tar-26');
  await userEvent.selectOptions(
    within(dialogo).getByLabelText('Nivel *'),
    'SECUNDARIA',
  );
  await userEvent.click(within(dialogo).getByRole('button', { name: 'Lun' }));
  await userEvent.type(
    within(dialogo).getByLabelText('Hora de entrada *'),
    '1445',
  );
  await userEvent.type(
    within(dialogo).getByLabelText('Bloques por día *'),
    '4',
  );
  await userEvent.type(
    within(dialogo).getByLabelText(/Duración del bloque/),
    '45',
  );
  await userEvent.click(
    within(dialogo).getByRole('button', { name: 'Crear estructura' }),
  );

  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  expect(await screen.findByText('3 registradas')).toBeInTheDocument();
  expect(
    await screen.findByRole('heading', { name: 'EH-TAR-26' }),
  ).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /EH-TAR-26/ })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
});

test('el aviso de que no hay estructuras es texto simple, no una caja que parezca un campo', async () => {
  await mostrar({ estructuras: [] });

  const aviso = await screen.findByText(
    'Este año aún no tiene estructuras horarias.',
  );
  expect(aviso.className).not.toMatch(/border/);
});
