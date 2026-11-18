import { HttpResponse, http } from 'msw';
import { useState } from 'react';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  renderApp,
  screen,
  userEvent,
  waitFor,
} from '@/testing/test-utils';
import { AmbienteResumen, ResumenAmbientes } from '@/types/api';

import {
  FiltrosAmbientes,
  useAmbientes,
  useCrearAmbiente,
  useDesactivarAmbiente,
  useEditarAmbiente,
  useReactivarAmbiente,
  useResumenAmbientes,
} from '../ambientes';

// Figma > Personal Administrativo > CI-26 · Ambientes físicos (HU-43)

const API = env.API_URL;

const ambiente = (cambios: Partial<AmbienteResumen> = {}): AmbienteResumen => ({
  id: 'amb-1',
  codigo: 'A-101',
  nombre: 'Aula 101',
  tipo: 'AULA_REGULAR',
  aforoMaximo: 30,
  recursos: ['Pizarra acrílica', 'Proyector'],
  estado: 'ACTIVO',
  ...cambios,
});

const resumen: ResumenAmbientes = {
  activos: 7,
  total: 8,
  aulasRegulares: 3,
  espaciosEspeciales: 4,
  aforoTotal: 377,
};

const administrativo = async () =>
  createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] });

// --- Consultas ---

const Resumen = () => {
  const consulta = useResumenAmbientes();
  return (
    <p>
      {consulta.data
        ? `${consulta.data.activos} de ${consulta.data.total} · aforo ${consulta.data.aforoTotal}`
        : '…'}
    </p>
  );
};

const Lista = ({ filtros }: { filtros?: FiltrosAmbientes }) => {
  const consulta = useAmbientes(filtros);
  return (
    <p>
      Ambientes:{' '}
      {consulta.data ? consulta.data.map((a) => a.nombre).join(', ') : '…'}
    </p>
  );
};

test('el resumen trae las 4 tarjetas con el total de registrados', async () => {
  server.use(
    http.get(`${API}/ambientes/resumen`, () => HttpResponse.json(resumen)),
  );

  await renderApp(<Resumen />, { user: await administrativo() });

  expect(await screen.findByText('7 de 8 · aforo 377')).toBeInTheDocument();
});

test('sin filtros la lista se pide sin parámetros', async () => {
  let parametros: string | null = null;
  server.use(
    http.get(`${API}/ambientes`, ({ request }) => {
      parametros = new URL(request.url).search;
      return HttpResponse.json([ambiente(), ambiente({ nombre: 'Auditorio' })]);
    }),
  );

  await renderApp(<Lista />, { user: await administrativo() });

  expect(
    await screen.findByText('Ambientes: Aula 101, Auditorio'),
  ).toBeInTheDocument();
  expect(parametros).toBe('');
});

test('los filtros de tipo, estado y búsqueda viajan como parámetros', async () => {
  let consulta: URLSearchParams | null = null;
  server.use(
    http.get(`${API}/ambientes`, ({ request }) => {
      consulta = new URL(request.url).searchParams;
      return HttpResponse.json([ambiente({ estado: 'INACTIVO' })]);
    }),
  );

  await renderApp(
    <Lista
      filtros={{ tipo: 'AULA_REGULAR', estado: 'INACTIVO', q: 'aula 10' }}
    />,
    { user: await administrativo() },
  );

  expect(await screen.findByText('Ambientes: Aula 101')).toBeInTheDocument();
  expect(consulta!.get('tipo')).toBe('AULA_REGULAR');
  expect(consulta!.get('estado')).toBe('INACTIVO');
  expect(consulta!.get('q')).toBe('aula 10');
});

/** Cambia los filtros con botones, como lo haría la pantalla. */
const ListaInteractiva = () => {
  const [filtros, setFiltros] = useState<FiltrosAmbientes>({
    q: '   ',
    tipo: undefined,
  });
  const consulta = useAmbientes(filtros);
  return (
    <div>
      <p>
        Ambientes:{' '}
        {consulta.data ? consulta.data.map((a) => a.nombre).join(', ') : '…'}
      </p>
      <button onClick={() => setFiltros({ q: '  Laboratorio ' })}>
        buscar
      </button>
      <button onClick={() => setFiltros({ tipo: 'ESPACIO_ESPECIAL' })}>
        espacios
      </button>
    </div>
  );
};

test('una búsqueda en blanco y los filtros vacíos no se envían, y la búsqueda se recorta', async () => {
  const consultas: string[] = [];
  server.use(
    http.get(`${API}/ambientes`, ({ request }) => {
      consultas.push(new URL(request.url).search);
      return HttpResponse.json([ambiente()]);
    }),
  );

  await renderApp(<ListaInteractiva />, { user: await administrativo() });
  await screen.findByText('Ambientes: Aula 101');
  expect(consultas).toEqual(['']);

  await userEvent.click(screen.getByRole('button', { name: 'buscar' }));

  await waitFor(() => expect(consultas).toHaveLength(2));
  expect(consultas[1]).toBe('?q=Laboratorio');
});

test('cambiar un filtro vuelve a consultar y no mezcla las listas', async () => {
  server.use(
    http.get(`${API}/ambientes`, ({ request }) => {
      const tipo = new URL(request.url).searchParams.get('tipo');
      return HttpResponse.json(
        tipo === 'ESPACIO_ESPECIAL'
          ? [ambiente({ nombre: 'Auditorio', tipo })]
          : [ambiente()],
      );
    }),
  );

  await renderApp(<ListaInteractiva />, { user: await administrativo() });
  await screen.findByText('Ambientes: Aula 101');

  await userEvent.click(screen.getByRole('button', { name: 'espacios' }));

  expect(await screen.findByText('Ambientes: Auditorio')).toBeInTheDocument();
});

// --- Mutaciones ---

const Acciones = ({
  onCreado,
  onEditado,
  onDesactivado,
  onReactivado,
}: {
  onCreado?: (a: AmbienteResumen) => void;
  onEditado?: (a: AmbienteResumen) => void;
  onDesactivado?: (a: AmbienteResumen) => void;
  onReactivado?: (a: AmbienteResumen) => void;
}) => {
  const resumenConsulta = useResumenAmbientes();
  const lista = useAmbientes();
  const filtrada = useAmbientes({ estado: 'INACTIVO' });
  const crear = useCrearAmbiente(onCreado);
  const editar = useEditarAmbiente(onEditado);
  const desactivar = useDesactivarAmbiente(onDesactivado);
  const reactivar = useReactivarAmbiente(onReactivado);
  return (
    <div>
      <p>
        Activos: {resumenConsulta.data ? resumenConsulta.data.activos : '…'}
      </p>
      <p>Lista: {lista.data ? lista.data.length : '…'}</p>
      <p>Inactivos: {filtrada.data ? filtrada.data.length : '…'}</p>
      <button
        onClick={() =>
          crear.mutate({
            tipo: 'ESPACIO_ESPECIAL',
            nombre: 'Laboratorio de robótica',
            aforoMaximo: 25,
            recursos: ['Kits de robótica', 'Proyector'],
          })
        }
      >
        crear
      </button>
      <button
        onClick={() =>
          editar.mutate({
            id: 'amb-1',
            data: { nombre: 'Aula 101 B', aforoMaximo: 28, recursos: [] },
          })
        }
      >
        editar
      </button>
      <button onClick={() => desactivar.mutate('amb-1')}>desactivar</button>
      <button onClick={() => reactivar.mutate('amb-1')}>reactivar</button>
    </div>
  );
};

/** Registra cuántas veces se pidió cada consulta, para comprobar qué se recarga. */
const consultasDelServidor = () => {
  const contador = { resumen: 0, lista: 0, inactivos: 0 };
  server.use(
    http.get(`${API}/ambientes/resumen`, () => {
      contador.resumen++;
      return HttpResponse.json({ ...resumen, activos: 6 + contador.resumen });
    }),
    http.get(`${API}/ambientes`, ({ request }) => {
      const estado = new URL(request.url).searchParams.get('estado');
      if (estado === 'INACTIVO') {
        contador.inactivos++;
        return HttpResponse.json([ambiente({ estado: 'INACTIVO' })]);
      }
      contador.lista++;
      return HttpResponse.json([ambiente()]);
    }),
  );
  return contador;
};

test('crear envía el tipo, el nombre, el aforo y los recursos, sin código, y recarga las tarjetas y las listas', async () => {
  let cuerpo: unknown = null;
  const contador = consultasDelServidor();
  const creados: AmbienteResumen[] = [];
  server.use(
    http.post(`${API}/ambientes`, async ({ request }) => {
      cuerpo = await request.json();
      return HttpResponse.json(
        ambiente({
          id: 'amb-9',
          codigo: 'E-001',
          nombre: 'Laboratorio de robótica',
        }),
        { status: 201 },
      );
    }),
  );

  await renderApp(<Acciones onCreado={(a) => creados.push(a)} />, {
    user: await administrativo(),
  });
  expect(await screen.findByText('Activos: 7')).toBeInTheDocument();

  await userEvent.click(screen.getByRole('button', { name: 'crear' }));

  // Se recargan el resumen y las dos listas (sin filtro y con el filtro de estado).
  expect(await screen.findByText('Activos: 8')).toBeInTheDocument();
  expect(cuerpo).toEqual({
    tipo: 'ESPACIO_ESPECIAL',
    nombre: 'Laboratorio de robótica',
    aforoMaximo: 25,
    recursos: ['Kits de robótica', 'Proyector'],
  });
  expect(cuerpo).not.toHaveProperty('codigo');
  await waitFor(() => {
    expect(contador).toEqual({ resumen: 2, lista: 2, inactivos: 2 });
  });
  // onSuccess recibe el ambiente creado, con su código generado.
  expect(creados.map((a) => a.codigo)).toEqual(['E-001']);
});

test('editar envía nombre, aforo y recursos a /ambientes/{id} y recarga lo mismo', async () => {
  let cuerpo: unknown = null;
  const contador = consultasDelServidor();
  const editados: AmbienteResumen[] = [];
  server.use(
    http.put(`${API}/ambientes/amb-1`, async ({ request }) => {
      cuerpo = await request.json();
      return HttpResponse.json(
        ambiente({ nombre: 'Aula 101 B', aforoMaximo: 28, recursos: [] }),
      );
    }),
  );

  await renderApp(<Acciones onEditado={(a) => editados.push(a)} />, {
    user: await administrativo(),
  });
  await screen.findByText('Activos: 7');

  await userEvent.click(screen.getByRole('button', { name: 'editar' }));

  await waitFor(() => {
    expect(contador).toEqual({ resumen: 2, lista: 2, inactivos: 2 });
  });
  expect(cuerpo).toEqual({
    nombre: 'Aula 101 B',
    aforoMaximo: 28,
    recursos: [],
  });
  expect(editados.map((a) => a.nombre)).toEqual(['Aula 101 B']);
});

test('desactivar y reactivar llaman a su ruta sin cuerpo y recargan las tarjetas y las listas', async () => {
  const llamadas: string[] = [];
  const contador = consultasDelServidor();
  const estados: string[] = [];
  server.use(
    http.post(`${API}/ambientes/:id/:accion`, ({ params }) => {
      llamadas.push(`${params.id}/${params.accion}`);
      return HttpResponse.json(
        ambiente({
          estado: params.accion === 'desactivar' ? 'INACTIVO' : 'ACTIVO',
        }),
      );
    }),
  );

  await renderApp(
    <Acciones
      onDesactivado={(a) => estados.push(a.estado)}
      onReactivado={(a) => estados.push(a.estado)}
    />,
    { user: await administrativo() },
  );
  await screen.findByText('Activos: 7');

  await userEvent.click(screen.getByRole('button', { name: 'desactivar' }));
  await waitFor(() => expect(contador.resumen).toBe(2));
  await userEvent.click(screen.getByRole('button', { name: 'reactivar' }));
  await waitFor(() => expect(contador.resumen).toBe(3));

  expect(llamadas).toEqual(['amb-1/desactivar', 'amb-1/reactivar']);
  expect(estados).toEqual(['INACTIVO', 'ACTIVO']);
  await waitFor(() => expect(contador.lista).toBe(3));
  await waitFor(() => expect(contador.inactivos).toBe(3));
});

test('si el backend rechaza el alta (409) no se ejecuta onSuccess ni se recarga nada', async () => {
  const contador = consultasDelServidor();
  const creados: AmbienteResumen[] = [];
  server.use(
    http.post(`${API}/ambientes`, () =>
      HttpResponse.json(
        { message: 'Ya existe un ambiente llamado "Aula 101".' },
        { status: 409 },
      ),
    ),
  );

  await renderApp(<Acciones onCreado={(a) => creados.push(a)} />, {
    user: await administrativo(),
  });
  await screen.findByText('Activos: 7');

  await userEvent.click(screen.getByRole('button', { name: 'crear' }));

  // El interceptor muestra el mensaje del backend tal cual.
  expect(
    await screen.findByText('Ya existe un ambiente llamado "Aula 101".'),
  ).toBeInTheDocument();
  expect(creados).toEqual([]);
  expect(contador).toEqual({ resumen: 1, lista: 1, inactivos: 1 });
});

test('si el backend rechaza el cambio de aforo (409) no se recarga nada', async () => {
  const contador = consultasDelServidor();
  const editados: AmbienteResumen[] = [];
  server.use(
    http.put(`${API}/ambientes/amb-1`, () =>
      HttpResponse.json(
        {
          message:
            'No puedes bajar el aforo a 2: la sección A de 1.° de secundaria tiene 3 estudiantes matriculados.',
        },
        { status: 409 },
      ),
    ),
  );

  await renderApp(<Acciones onEditado={(a) => editados.push(a)} />, {
    user: await administrativo(),
  });
  await screen.findByText('Activos: 7');

  await userEvent.click(screen.getByRole('button', { name: 'editar' }));

  expect(
    await screen.findByText(/No puedes bajar el aforo a 2/),
  ).toBeInTheDocument();
  expect(editados).toEqual([]);
  expect(contador).toEqual({ resumen: 1, lista: 1, inactivos: 1 });
});
