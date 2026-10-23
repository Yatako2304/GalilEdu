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
  DocenteElegible,
  GradoConSecciones,
  SeccionResumen,
} from '@/types/api';

import { useAnioEscolar } from '../anios-escolares';
import { useAsignarTutores, useCrearSeccion } from '../mutaciones-seccion';
import { useDocentesElegibles, useGradosConSecciones } from '../secciones';

// Figma > Personal Administrativo > CI-09 · Secciones por grado y tutores (HU-75)

const API = env.API_URL;

const seccion = (nombre: string, tutor: string | null): SeccionResumen => ({
  id: `seccion-1${nombre}`,
  gradoCodigo: 'SEC-1',
  nombre,
  capacidadMaxima: 30,
  alumnosOcupanVacante: 0,
  tutorId: tutor ? 'docente-1' : null,
  tutor,
  cotutorId: null,
  cotutor: null,
  estado: 'ACTIVA',
});

const grados: GradoConSecciones[] = [
  {
    codigo: 'PRI-1',
    nombre: '1.° de primaria',
    nivel: 'PRIMARIA',
    numero: 1,
    cantidadSeccionesActivas: 0,
    secciones: [],
  },
  {
    codigo: 'SEC-1',
    nombre: '1.° de secundaria',
    nivel: 'SECUNDARIA',
    numero: 1,
    cantidadSeccionesActivas: 1,
    secciones: [seccion('A', 'María Fernández Ríos')],
  },
];

const docentes: DocenteElegible[] = [
  { id: 'docente-1', nombre: 'María Fernández Ríos', esTutorEsteAnio: true },
  { id: 'docente-2', nombre: 'Rosa Prueba', esTutorEsteAnio: false },
];

const administrativo = async () =>
  createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] });

const Grados = ({ anioId }: { anioId: string | undefined }) => {
  const consulta = useGradosConSecciones(anioId);
  return (
    <p>
      Grados:{' '}
      {consulta.data
        ? consulta.data
            .map((g) => `${g.codigo} (${g.secciones.length})`)
            .join(', ')
        : '…'}
    </p>
  );
};

test('la consulta trae todos los grados del año, también los que no tienen secciones', async () => {
  let ruta = '';
  server.use(
    http.get(
      `${API}/configuracion/anios-escolares/:anioId/secciones`,
      ({ params }) => {
        ruta = String(params.anioId);
        return HttpResponse.json(grados);
      },
    ),
  );

  await renderApp(<Grados anioId="anio-2026" />, {
    user: await administrativo(),
  });

  expect(
    await screen.findByText('Grados: PRI-1 (0), SEC-1 (1)'),
  ).toBeInTheDocument();
  expect(ruta).toBe('anio-2026');
});

test('sin id de año no consulta las secciones', async () => {
  let consultas = 0;
  server.use(
    http.get(`${API}/configuracion/anios-escolares/:anioId/secciones`, () => {
      consultas++;
      return HttpResponse.json(grados);
    }),
  );

  await renderApp(<Grados anioId={undefined} />, {
    user: await administrativo(),
  });

  expect(screen.getByText('Grados: …')).toBeInTheDocument();
  expect(consultas).toBe(0);
});

const Docentes = ({ habilitado }: { habilitado: boolean }) => {
  const consulta = useDocentesElegibles('anio-2026', habilitado);
  return (
    <p>
      Docentes:{' '}
      {consulta.data
        ? consulta.data
            .map((d) => `${d.nombre} ${d.esTutorEsteAnio ? '(tutor)' : ''}`)
            .join(', ')
        : '…'}
    </p>
  );
};

test('los docentes elegibles se piden con el año y traen quién ya es tutor', async () => {
  let anioPedido: string | null = '';
  server.use(
    http.get(
      `${API}/configuracion/secciones/tutores-disponibles`,
      ({ request }) => {
        anioPedido = new URL(request.url).searchParams.get('anioEscolarId');
        return HttpResponse.json(docentes);
      },
    ),
  );

  await renderApp(<Docentes habilitado />, { user: await administrativo() });

  expect(
    await screen.findByText(/María Fernández Ríos \(tutor\), Rosa Prueba/),
  ).toBeInTheDocument();
  expect(anioPedido).toBe('anio-2026');
});

test('los docentes elegibles no se piden mientras el modal está cerrado', async () => {
  let consultas = 0;
  server.use(
    http.get(`${API}/configuracion/secciones/tutores-disponibles`, () => {
      consultas++;
      return HttpResponse.json(docentes);
    }),
  );

  await renderApp(<Docentes habilitado={false} />, {
    user: await administrativo(),
  });

  expect(screen.getByText('Docentes: …')).toBeInTheDocument();
  expect(consultas).toBe(0);
});

// --- Mutaciones ---

const Acciones = ({ onCreada }: { onCreada?: (s: SeccionResumen) => void }) => {
  const consulta = useGradosConSecciones('anio-2026');
  const anio = useAnioEscolar('anio-2026');
  const crear = useCrearSeccion('anio-2026', onCreada);
  const asignar = useAsignarTutores();
  return (
    <div>
      <p>Grados: {consulta.data ? consulta.data.length : '…'}</p>
      <p>Año: {anio.data ? anio.data.cantidadSecciones : '…'} secciones</p>
      <button
        onClick={() =>
          crear.mutate({
            gradoCodigo: 'SEC-2',
            nombre: 'C',
            capacidadMaxima: 25,
          })
        }
      >
        crear
      </button>
      <button
        onClick={() =>
          asignar.mutate({
            id: 'seccion-1A',
            data: { tutorId: 'docente-2', cotutorId: null },
          })
        }
      >
        asignar
      </button>
    </div>
  );
};

const detalleAnio = (cantidadSecciones: number) => ({
  id: 'anio-2026',
  anio: 2026,
  estado: 'ACTIVO',
  cantidadSecciones,
  cantidadEstructuras: 2,
  periodos: [],
});

test('crear una sección la envía al año y recarga las secciones y los datos del año', async () => {
  let cuerpo: unknown = null;
  let ruta = '';
  let consultasGrados = 0;
  let consultasAnio = 0;
  const creada: SeccionResumen[] = [];
  server.use(
    http.get(`${API}/configuracion/anios-escolares/anio-2026/secciones`, () => {
      consultasGrados++;
      return HttpResponse.json(grados);
    }),
    http.get(`${API}/configuracion/anios-escolares/anio-2026`, () => {
      consultasAnio++;
      return HttpResponse.json(detalleAnio(10 + consultasAnio - 1));
    }),
    http.post(
      `${API}/configuracion/anios-escolares/:anioId/secciones`,
      async ({ request, params }) => {
        ruta = String(params.anioId);
        cuerpo = await request.json();
        return HttpResponse.json(seccion('C', null), { status: 201 });
      },
    ),
  );

  await renderApp(<Acciones onCreada={(s) => creada.push(s)} />, {
    user: await administrativo(),
  });
  expect(await screen.findByText('Año: 10 secciones')).toBeInTheDocument();

  await userEvent.click(screen.getByRole('button', { name: 'crear' }));

  expect(await screen.findByText('Año: 11 secciones')).toBeInTheDocument();
  expect(ruta).toBe('anio-2026');
  expect(cuerpo).toEqual({
    gradoCodigo: 'SEC-2',
    nombre: 'C',
    capacidadMaxima: 25,
  });
  expect(consultasGrados).toBe(2);
  expect(creada.map((s) => s.nombre)).toEqual(['C']);
});

test('asignar tutores envía el tutor y el cotutor en null, y recarga lo mismo', async () => {
  let cuerpo: unknown = null;
  let consultasGrados = 0;
  server.use(
    http.get(`${API}/configuracion/anios-escolares/anio-2026/secciones`, () => {
      consultasGrados++;
      return HttpResponse.json(grados);
    }),
    http.get(`${API}/configuracion/anios-escolares/anio-2026`, () =>
      HttpResponse.json(detalleAnio(10)),
    ),
    http.put(
      `${API}/configuracion/secciones/seccion-1A/tutores`,
      async ({ request }) => {
        cuerpo = await request.json();
        return HttpResponse.json(seccion('A', 'Rosa Prueba'));
      },
    ),
  );

  await renderApp(<Acciones />, { user: await administrativo() });
  await screen.findByText('Grados: 2');

  await userEvent.click(screen.getByRole('button', { name: 'asignar' }));

  await waitFor(() => expect(consultasGrados).toBe(2));
  expect(cuerpo).toEqual({ tutorId: 'docente-2', cotutorId: null });
});

test('si el backend rechaza la creación (409) no se ejecuta onSuccess ni se recarga nada', async () => {
  let consultasGrados = 0;
  const creada: SeccionResumen[] = [];
  server.use(
    http.get(`${API}/configuracion/anios-escolares/anio-2026/secciones`, () => {
      consultasGrados++;
      return HttpResponse.json(grados);
    }),
    http.get(`${API}/configuracion/anios-escolares/anio-2026`, () =>
      HttpResponse.json(detalleAnio(10)),
    ),
    http.post(`${API}/configuracion/anios-escolares/anio-2026/secciones`, () =>
      HttpResponse.json(
        { message: 'Ya existe la sección C en 2.° de secundaria.' },
        { status: 409 },
      ),
    ),
  );

  await renderApp(<Acciones onCreada={(s) => creada.push(s)} />, {
    user: await administrativo(),
  });
  await screen.findByText('Grados: 2');

  await userEvent.click(screen.getByRole('button', { name: 'crear' }));

  expect(
    await screen.findByText('Ya existe la sección C en 2.° de secundaria.'),
  ).toBeInTheDocument();
  expect(creada).toEqual([]);
  expect(consultasGrados).toBe(1);
});
