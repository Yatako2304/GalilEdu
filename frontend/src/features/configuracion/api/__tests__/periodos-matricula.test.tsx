import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import { createUser, renderApp, screen } from '@/testing/test-utils';
import {
  FilaPeriodoMatricula,
  FormularioPeriodoMatricula,
  Pagina,
} from '@/types/api';

import {
  FiltrosPeriodosMatricula,
  getPlantilla,
  useAniosDisponiblesMatricula,
  useFormularioNuevoPeriodo,
  usePeriodoMatricula,
  usePeriodoMatriculaVigente,
  usePeriodosMatricula,
  usePeriodosMatriculaTodos,
} from '../periodos-matricula';

// Figma > Personal Administrativo > pantalla 13 "Periodos de matrícula" (HU-23)

const API = `${env.API_URL}/configuracion/periodos-matricula`;

const fila = (
  anio: number,
  estado: FilaPeriodoMatricula['estado'],
  vigente = false,
): FilaPeriodoMatricula => ({
  id: `periodo-${anio}`,
  nombre: `Matrícula ${anio}`,
  anioEscolarId: `anio-${anio}`,
  anio,
  inicio: `${anio - 1}-12-01T13:00:00Z`,
  cierre: `${anio}-02-15T15:00:00Z`,
  matriculas: 0,
  estado,
  vigente,
});

const pagina = (
  filas: FilaPeriodoMatricula[],
): Pagina<FilaPeriodoMatricula> => ({
  data: filas,
  meta: { page: 1, size: 8, total: filas.length, totalPages: 1 },
});

const administrativo = async () =>
  createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] });

const Lista = ({ filtros }: { filtros: FiltrosPeriodosMatricula }) => {
  const lista = usePeriodosMatricula({ filtros });
  return <p>Periodos: {lista.data?.data.map((f) => f.nombre).join(', ')}</p>;
};

test('el listado pide la página con sus filtros y deja fuera los vacíos', async () => {
  let consulta: URLSearchParams | null = null;
  server.use(
    http.get(API, ({ request }) => {
      consulta = new URL(request.url).searchParams;
      return HttpResponse.json(pagina([fila(2026, 'CERRADO')]));
    }),
  );

  const { unmount } = await renderApp(
    <Lista filtros={{ anio: 2026, estado: 'CERRADO', page: 2 }} />,
    { user: await administrativo() },
  );
  expect(
    await screen.findByText('Periodos: Matrícula 2026'),
  ).toBeInTheDocument();
  expect(Object.fromEntries(consulta!.entries())).toEqual({
    anio: '2026',
    estado: 'CERRADO',
    page: '2',
    size: '8',
  });
  unmount();

  await renderApp(<Lista filtros={{ anio: '', estado: '', page: 1 }} />, {
    user: await administrativo(),
  });
  expect(
    await screen.findByText('Periodos: Matrícula 2026'),
  ).toBeInTheDocument();
  expect(Object.fromEntries(consulta!.entries())).toEqual({
    page: '1',
    size: '8',
  });
});

test('la lista completa pide todos los periodos de una vez', async () => {
  let consulta: URLSearchParams | null = null;
  server.use(
    http.get(API, ({ request }) => {
      consulta = new URL(request.url).searchParams;
      return HttpResponse.json(pagina([fila(2027, 'ABIERTO', true)]));
    }),
  );
  const Todos = () => {
    const todos = usePeriodosMatriculaTodos();
    return <p>Vigente: {todos.data?.data.find((f) => f.vigente)?.anio}</p>;
  };

  await renderApp(<Todos />, { user: await administrativo() });

  expect(await screen.findByText('Vigente: 2027')).toBeInTheDocument();
  expect(Object.fromEntries(consulta!.entries())).toEqual({
    page: '1',
    size: '100',
  });
});

test('/vigente solo se pide si hay un periodo vigente: sin él respondería 404 y saldría como error', async () => {
  let consultas = 0;
  server.use(
    http.get(API, () =>
      HttpResponse.json(pagina([fila(2026, 'CERRADO'), fila(2025, 'CERRADO')])),
    ),
    http.get(`${API}/vigente`, () => {
      consultas++;
      return HttpResponse.json(
        { message: 'No hay un periodo de matrícula vigente.' },
        { status: 404 },
      );
    }),
  );
  const Tarjeta = () => {
    const todos = usePeriodosMatriculaTodos();
    const hay = !!todos.data?.data.some((f) => f.vigente);
    const vigente = usePeriodoMatriculaVigente(hay);
    return (
      <p>
        {todos.data
          ? `Vigente: ${vigente.data ? vigente.data.nombre : 'ninguno'}`
          : 'cargando'}
      </p>
    );
  };

  await renderApp(<Tarjeta />, { user: await administrativo() });

  expect(await screen.findByText('Vigente: ninguno')).toBeInTheDocument();
  expect(consultas).toBe(0);
  expect(
    screen.queryByText('No hay un periodo de matrícula vigente.'),
  ).not.toBeInTheDocument();
});

test('con un periodo vigente sí pide /vigente', async () => {
  server.use(
    http.get(API, () =>
      HttpResponse.json(pagina([fila(2027, 'ABIERTO', true)])),
    ),
    http.get(`${API}/vigente`, () =>
      HttpResponse.json({ ...fila(2027, 'ABIERTO'), vacantesTotales: 300 }),
    ),
  );
  const Tarjeta = () => {
    const todos = usePeriodosMatriculaTodos();
    const vigente = usePeriodoMatriculaVigente(
      !!todos.data?.data.some((f) => f.vigente),
    );
    return <p>Vigente: {vigente.data?.nombre ?? 'ninguno'}</p>;
  };

  await renderApp(<Tarjeta />, { user: await administrativo() });

  expect(
    await screen.findByText('Vigente: Matrícula 2027'),
  ).toBeInTheDocument();
});

test('los años disponibles salen de /anios-disponibles', async () => {
  server.use(
    http.get(`${API}/anios-disponibles`, () =>
      HttpResponse.json([
        { id: 'anio-2027', anio: 2027, estado: 'PLANIFICADO' },
        { id: 'anio-2028', anio: 2028, estado: 'PLANIFICADO' },
      ]),
    ),
  );
  const Anios = () => {
    const anios = useAniosDisponiblesMatricula();
    return <p>Años: {anios.data?.map((a) => a.anio).join(', ')}</p>;
  };

  await renderApp(<Anios />, { user: await administrativo() });

  expect(await screen.findByText('Años: 2027, 2028')).toBeInTheDocument();
});

const formulario = (
  extras: Partial<FormularioPeriodoMatricula> = {},
): FormularioPeriodoMatricula => ({
  id: null,
  anioEscolarId: 'anio-2028',
  anio: 2028,
  titulo: 'Matrícula 2028',
  estado: null,
  editable: true,
  eliminable: false,
  fechaInicio: null,
  horaInicio: null,
  fechaCierre: null,
  horaCierre: null,
  diasDelPeriodo: null,
  montoMatricula: 450,
  tarifarioConfirmado: true,
  vacantesTotales: 150,
  matriculasRegistradas: 0,
  cantidadSecciones: 5,
  resumenSecciones: '1.° a 5.°, A',
  bloquearPorMorosidad: true,
  permitirExtemporanea: true,
  secciones: [],
  documentos: [],
  ...extras,
});

test('el formulario nuevo se pide para el año elegido, y no se pide sin año', async () => {
  const consultas: URLSearchParams[] = [];
  server.use(
    http.get(`${API}/nuevo`, ({ request }) => {
      consultas.push(new URL(request.url).searchParams);
      return HttpResponse.json(formulario());
    }),
  );
  const Nuevo = ({ anio }: { anio?: string }) => {
    const nuevo = useFormularioNuevoPeriodo(anio);
    return <p>Título: {nuevo.data?.titulo ?? 'sin datos'}</p>;
  };

  const { unmount } = await renderApp(<Nuevo />, {
    user: await administrativo(),
  });
  expect(screen.getByText('Título: sin datos')).toBeInTheDocument();
  expect(consultas).toHaveLength(0);
  unmount();

  await renderApp(<Nuevo anio="anio-2028" />, { user: await administrativo() });
  expect(await screen.findByText('Título: Matrícula 2028')).toBeInTheDocument();
  expect(consultas.map((c) => c.get('anioEscolarId'))).toEqual(['anio-2028']);
});

test('el detalle de un periodo se pide por id', async () => {
  server.use(
    http.get(`${API}/periodo-2027`, () =>
      HttpResponse.json(
        formulario({
          id: 'periodo-2027',
          anio: 2027,
          titulo: 'Matrícula 2027',
          estado: 'ABIERTO',
          editable: false,
        }),
      ),
    ),
  );
  const Detalle = ({ id }: { id?: string }) => {
    const periodo = usePeriodoMatricula(id);
    return (
      <p>
        {periodo.data
          ? `${periodo.data.titulo} · ${periodo.data.estado} · editable ${periodo.data.editable}`
          : 'sin datos'}
      </p>
    );
  };

  const { unmount } = await renderApp(<Detalle />, {
    user: await administrativo(),
  });
  expect(screen.getByText('sin datos')).toBeInTheDocument();
  unmount();

  await renderApp(<Detalle id="periodo-2027" />, {
    user: await administrativo(),
  });
  expect(
    await screen.findByText('Matrícula 2027 · ABIERTO · editable false'),
  ).toBeInTheDocument();
});

test('la plantilla se descarga como archivo', async () => {
  server.use(
    http.get(
      `${API}/periodo-2027/documentos/doc-1/plantilla`,
      () =>
        new HttpResponse('nombre,dni\nAna,1\n', {
          headers: {
            'Content-Type': 'text/csv',
            'Content-Disposition': 'attachment; filename="ficha.csv"',
          },
        }),
    ),
  );

  const archivo = await getPlantilla('periodo-2027', 'doc-1');

  expect(archivo).toBeInstanceOf(Blob);
  expect(archivo.size).toBe('nombre,dni\nAna,1\n'.length);
});
