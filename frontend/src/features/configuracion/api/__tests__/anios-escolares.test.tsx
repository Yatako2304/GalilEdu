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
} from '@/testing/test-utils';
import {
  AnioEscolarDetalle,
  AnioEscolarResumen,
  EstadoAnioEscolar,
  RequisitosAnio,
} from '@/types/api';

import {
  anioActivo,
  useAnioEscolar,
  useAnioEscolarActual,
  useAniosEscolares,
  useRequisitosAnio,
} from '../anios-escolares';
import {
  useActivarAnio,
  useCambiarRegimen,
  useCerrarAnio,
  useCrearAnio,
  useEditarAnio,
  useEditarPeriodo,
} from '../mutaciones-anio';

// Figma > Personal Administrativo > CI-01, CI-03 y CI-06 · Año escolar (HU-74)

const API = env.API_URL;

const resumen = (
  anio: number,
  estado: EstadoAnioEscolar,
): AnioEscolarResumen => ({
  id: `anio-${anio}`,
  anio,
  fechaInicio: `${anio}-03-02`,
  fechaFin: `${anio}-12-18`,
  regimen: 'BIMESTRAL',
  cantidadPeriodos: 4,
  seccionesActivas: 10,
  estructurasHorarias: 2,
  estado,
});

const detalle = (
  anio: number,
  estado: EstadoAnioEscolar,
): AnioEscolarDetalle => ({
  id: `anio-${anio}`,
  anio,
  fechaInicio: `${anio}-03-02`,
  fechaFin: `${anio}-12-18`,
  regimen: 'BIMESTRAL',
  estado,
  cantidadSecciones: 10,
  cantidadEstructuras: 2,
  periodos: [],
});

const administrativo = async () =>
  createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] });

const Lista = () => {
  const anios = useAniosEscolares();
  const activo = anios.data ? anioActivo(anios.data) : undefined;
  const actual = useAnioEscolarActual(!!activo);
  return (
    <div>
      <p>
        Años:{' '}
        {anios.data?.map((a) => `${a.anio} ${a.estado}`).join(', ') ?? '…'}
      </p>
      <p>En curso: {actual.data ? actual.data.anio : 'ninguno'}</p>
    </div>
  );
};

test('la lista trae los años y no consulta el año en curso si no hay uno ACTIVO', async () => {
  let consultasActual = 0;
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([
        resumen(2027, 'PLANIFICADO'),
        resumen(2025, 'CERRADO'),
      ]),
    ),
    http.get(`${API}/configuracion/anios-escolares/actual`, () => {
      consultasActual++;
      return HttpResponse.json(
        { message: 'No hay un año escolar activo.' },
        { status: 404 },
      );
    }),
  );

  await renderApp(<Lista />, { user: await administrativo() });

  expect(
    await screen.findByText(/2027 PLANIFICADO, 2025 CERRADO/),
  ).toBeInTheDocument();
  expect(screen.getByText('En curso: ninguno')).toBeInTheDocument();
  expect(consultasActual).toBe(0);
});

test('con un año ACTIVO sí consulta el año en curso', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([
        resumen(2027, 'PLANIFICADO'),
        resumen(2026, 'ACTIVO'),
      ]),
    ),
    http.get(`${API}/configuracion/anios-escolares/actual`, () =>
      HttpResponse.json({ id: 'anio-2026', anio: 2026 }),
    ),
  );

  await renderApp(<Lista />, { user: await administrativo() });

  expect(await screen.findByText('En curso: 2026')).toBeInTheDocument();
});

const Detalle = ({ id }: { id: string | undefined }) => {
  const anio = useAnioEscolar(id);
  const requisitos = useRequisitosAnio(id, anio.data?.estado === 'PLANIFICADO');
  return (
    <div>
      <p>Año: {anio.data ? `${anio.data.anio} ${anio.data.estado}` : '…'}</p>
      <p>
        Puede activar:{' '}
        {requisitos.data ? String(requisitos.data.puedeActivar) : 'sin datos'}
      </p>
    </div>
  );
};

test('el detalle y los requisitos se consultan por id, y los requisitos solo con el año PLANIFICADO', async () => {
  let consultasRequisitos = 0;
  const requisitos: RequisitosAnio = {
    puedeActivar: true,
    obligatorios: [],
    recomendados: [],
  };
  server.use(
    http.get(`${API}/configuracion/anios-escolares/anio-2027`, () =>
      HttpResponse.json(detalle(2027, 'PLANIFICADO')),
    ),
    http.get(`${API}/configuracion/anios-escolares/anio-2026`, () =>
      HttpResponse.json(detalle(2026, 'ACTIVO')),
    ),
    http.get(`${API}/configuracion/anios-escolares/:id/requisitos`, () => {
      consultasRequisitos++;
      return HttpResponse.json(requisitos);
    }),
  );

  const { unmount } = await renderApp(<Detalle id="anio-2027" />, {
    user: await administrativo(),
  });
  expect(await screen.findByText('Año: 2027 PLANIFICADO')).toBeInTheDocument();
  expect(await screen.findByText('Puede activar: true')).toBeInTheDocument();
  unmount();

  await renderApp(<Detalle id="anio-2026" />, { user: await administrativo() });
  expect(await screen.findByText('Año: 2026 ACTIVO')).toBeInTheDocument();
  expect(screen.getByText('Puede activar: sin datos')).toBeInTheDocument();
  expect(consultasRequisitos).toBe(1);
});

test('sin id no consulta el detalle', async () => {
  let consultas = 0;
  server.use(
    http.get(`${API}/configuracion/anios-escolares/:id`, () => {
      consultas++;
      return HttpResponse.json(detalle(2027, 'PLANIFICADO'));
    }),
  );

  await renderApp(<Detalle id={undefined} />, { user: await administrativo() });

  expect(screen.getByText('Año: …')).toBeInTheDocument();
  expect(consultas).toBe(0);
});

// --- Mutaciones ---

const Acciones = () => {
  const lista = useAniosEscolares();
  const activo = lista.data ? anioActivo(lista.data) : undefined;
  const actual = useAnioEscolarActual(!!activo);
  const crear = useCrearAnio();
  const editar = useEditarAnio();
  const regimen = useCambiarRegimen();
  const activar = useActivarAnio();
  const cerrar = useCerrarAnio();
  const periodo = useEditarPeriodo();
  return (
    <div>
      <p>En curso: {actual.data ? actual.data.anio : 'ninguno'}</p>
      <p>
        Estados: {lista.data?.map((a) => `${a.anio} ${a.estado}`).join(', ')}
      </p>
      <button
        onClick={() =>
          crear.mutate({
            anio: 2028,
            fechaInicio: '2028-03-01',
            fechaFin: '2028-12-15',
            regimen: 'BIMESTRAL',
          })
        }
      >
        crear
      </button>
      <button
        onClick={() =>
          editar.mutate({
            id: 'anio-2027',
            data: { fechaInicio: '2027-03-08', fechaFin: '2027-12-10' },
          })
        }
      >
        editar
      </button>
      <button
        onClick={() =>
          regimen.mutate({ id: 'anio-2027', regimen: 'TRIMESTRAL' })
        }
      >
        regimen
      </button>
      <button onClick={() => activar.mutate('anio-2027')}>activar</button>
      <button onClick={() => cerrar.mutate('anio-2026')}>cerrar</button>
      <button
        onClick={() =>
          periodo.mutate({
            id: 'periodo-1',
            data: {
              nombre: 'I Bimestre',
              fechaInicio: '2026-03-02',
              fechaFin: '2026-05-08',
              fechaLimiteNotas: '2026-05-15',
            },
          })
        }
      >
        periodo
      </button>
    </div>
  );
};

test('cada mutación llama a su endpoint con su cuerpo', async () => {
  const llamadas: string[] = [];
  const cuerpos: Record<string, unknown> = {};
  const registrar =
    (clave: string) =>
    async ({ request }: { request: Request }) => {
      llamadas.push(clave);
      const texto = await request.text();
      cuerpos[clave] = texto ? JSON.parse(texto) : null;
      return HttpResponse.json(detalle(2027, 'PLANIFICADO'));
    };
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([
        resumen(2027, 'PLANIFICADO'),
        resumen(2026, 'ACTIVO'),
      ]),
    ),
    http.get(`${API}/configuracion/anios-escolares/actual`, () =>
      HttpResponse.json({ id: 'anio-2026', anio: 2026 }),
    ),
    http.post(`${API}/configuracion/anios-escolares`, registrar('crear')),
    http.put(
      `${API}/configuracion/anios-escolares/anio-2027`,
      registrar('editar'),
    ),
    http.put(
      `${API}/configuracion/anios-escolares/anio-2027/regimen`,
      registrar('regimen'),
    ),
    http.post(
      `${API}/configuracion/anios-escolares/anio-2027/activar`,
      registrar('activar'),
    ),
    http.post(
      `${API}/configuracion/anios-escolares/anio-2026/cerrar`,
      registrar('cerrar'),
    ),
    http.put(
      `${API}/configuracion/periodos-academicos/periodo-1`,
      registrar('periodo'),
    ),
  );

  await renderApp(<Acciones />, { user: await administrativo() });
  await screen.findByText('En curso: 2026');

  for (const nombre of [
    'crear',
    'editar',
    'regimen',
    'activar',
    'cerrar',
    'periodo',
  ]) {
    await userEvent.click(screen.getByRole('button', { name: nombre }));
    await waitFor(() => expect(llamadas).toContain(nombre));
  }

  expect(cuerpos.crear).toEqual({
    anio: 2028,
    fechaInicio: '2028-03-01',
    fechaFin: '2028-12-15',
    regimen: 'BIMESTRAL',
  });
  expect(cuerpos.editar).toEqual({
    fechaInicio: '2027-03-08',
    fechaFin: '2027-12-10',
  });
  expect(cuerpos.regimen).toEqual({ regimen: 'TRIMESTRAL' });
  expect(cuerpos.periodo).toEqual({
    nombre: 'I Bimestre',
    fechaInicio: '2026-03-02',
    fechaFin: '2026-05-08',
    fechaLimiteNotas: '2026-05-15',
  });
});

test('al cerrar el único año ACTIVO recarga la lista y no vuelve a pedir el año en curso (evita el 404)', async () => {
  let cerrado = false;
  let consultasActual = 0;
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([
        resumen(2027, 'PLANIFICADO'),
        resumen(2026, cerrado ? 'CERRADO' : 'ACTIVO'),
      ]),
    ),
    http.get(`${API}/configuracion/anios-escolares/actual`, () => {
      consultasActual++;
      return cerrado
        ? HttpResponse.json(
            { message: 'No hay un año escolar activo.' },
            { status: 404 },
          )
        : HttpResponse.json({ id: 'anio-2026', anio: 2026 });
    }),
    http.post(`${API}/configuracion/anios-escolares/anio-2026/cerrar`, () => {
      cerrado = true;
      return HttpResponse.json(detalle(2026, 'CERRADO'));
    }),
  );

  await renderApp(<Acciones />, { user: await administrativo() });
  await screen.findByText('En curso: 2026');
  expect(consultasActual).toBe(1);

  await userEvent.click(screen.getByRole('button', { name: 'cerrar' }));

  expect(await screen.findByText(/2026 CERRADO/)).toBeInTheDocument();
  expect(screen.getByText('En curso: ninguno')).toBeInTheDocument();
  expect(consultasActual).toBe(1);
  expect(
    screen.queryByText('No hay un año escolar activo.'),
  ).not.toBeInTheDocument();
});

test('al activar un año recarga la lista y también el año en curso', async () => {
  let activado = false;
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([
        resumen(2027, activado ? 'ACTIVO' : 'PLANIFICADO'),
        resumen(2026, 'CERRADO'),
      ]),
    ),
    http.get(`${API}/configuracion/anios-escolares/actual`, () =>
      HttpResponse.json({ id: 'anio-2027', anio: 2027 }),
    ),
    http.post(`${API}/configuracion/anios-escolares/anio-2027/activar`, () => {
      activado = true;
      return HttpResponse.json(detalle(2027, 'ACTIVO'));
    }),
  );

  await renderApp(<Acciones />, { user: await administrativo() });
  expect(await screen.findByText(/2027 PLANIFICADO/)).toBeInTheDocument();
  expect(screen.getByText('En curso: ninguno')).toBeInTheDocument();

  await userEvent.click(screen.getByRole('button', { name: 'activar' }));

  expect(await screen.findByText(/2027 ACTIVO/)).toBeInTheDocument();
  expect(await screen.findByText('En curso: 2027')).toBeInTheDocument();
});
