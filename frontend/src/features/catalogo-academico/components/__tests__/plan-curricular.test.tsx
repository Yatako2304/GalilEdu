import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  renderApp,
  screen,
  userEvent,
  within,
} from '@/testing/test-utils';
import {
  AreaDetalle,
  AreaResumen,
  ResumenPlanCurricular,
  SolicitudCurso,
} from '@/types/api';

import { PlanCurricular } from '../plan-curricular';

// Figma > Personal Administrativo > CI-14 · Plan curricular (HU-52, HU-53, HU-54)

const resumen: ResumenPlanCurricular = {
  areasActivas: 11,
  areasRegistradas: 12,
  competenciasActivas: 32,
  competenciasRegistradas: 34,
  cursosActivos: 21,
  cursosRegistrados: 22,
  anioActivo: 2026,
};

const areas: AreaResumen[] = [
  {
    id: 'mat',
    nombre: 'Matemática',
    descripcion: 'Desarrolla el pensamiento matemático.',
    estado: 'ACTIVA',
    competencias: 1,
    cursos: 2,
  },
  {
    id: 'fc',
    nombre: 'Formación complementaria',
    descripcion: null,
    estado: 'INACTIVA',
    competencias: 0,
    cursos: 0,
  },
];

const matematica: AreaDetalle = {
  id: 'mat',
  nombre: 'Matemática',
  descripcion: 'Desarrolla el pensamiento matemático.',
  estado: 'ACTIVA',
  competencias: [
    {
      id: 'c1',
      areaId: 'mat',
      nombre: 'Resuelve problemas de cantidad',
      descripcion: 'Traduce cantidades a expresiones numéricas.',
      orden: 1,
      estado: 'ACTIVA',
    },
  ],
  cursos: [
    {
      id: 'alg',
      areaId: 'mat',
      nombre: 'Álgebra',
      estado: 'ACTIVO',
      grados: ['SEC-1', 'SEC-2'],
    },
    {
      id: 'rm',
      areaId: 'mat',
      nombre: 'Razonamiento matemático',
      estado: 'INACTIVO',
      grados: ['SEC-1'],
    },
  ],
};

let cursoEnviado: SolicitudCurso | null = null;
let accionEnviada: string | null = null;

beforeEach(() => {
  cursoEnviado = null;
  accionEnviada = null;
  server.use(
    http.get(`${env.API_URL}/areas/resumen`, () => HttpResponse.json(resumen)),
    http.get(`${env.API_URL}/areas`, () => HttpResponse.json(areas)),
    http.get(`${env.API_URL}/areas/:id`, () => HttpResponse.json(matematica)),
    http.post(`${env.API_URL}/areas/:id/cursos`, async ({ request }) => {
      cursoEnviado = (await request.json()) as SolicitudCurso;
      return HttpResponse.json({}, { status: 201 });
    }),
    http.post(`${env.API_URL}/cursos/:id/:accion`, ({ params }) => {
      accionEnviada = `${params.id}/${params.accion}`;
      return HttpResponse.json({});
    }),
  );
});

const renderizar = async () =>
  renderApp(<PlanCurricular />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

test('muestra el resumen, las áreas y el detalle de la primera área', async () => {
  await renderizar();

  expect(await screen.findByText('de 12 registradas')).toBeInTheDocument();
  const lista = within(
    screen.getByRole('list', { name: /áreas curriculares/i }),
  );
  expect(lista.getByText('1 competencia · 2 cursos')).toBeInTheDocument();
  expect(lista.getByText('Inactiva')).toBeInTheDocument();

  expect(
    await screen.findByText('Resuelve problemas de cantidad'),
  ).toBeInTheDocument();
  const tabla = within(screen.getByRole('table'));
  expect(tabla.getByText('Se dicta en 2026')).toBeInTheDocument();
  expect(tabla.getByText('Secundaria 1.°, 2.°')).toBeInTheDocument();
  expect(tabla.getByText('No se dicta en 2026')).toBeInTheDocument();
});

test('la búsqueda filtra las áreas sin importar las tildes', async () => {
  await renderizar();
  await screen.findByText('Formación complementaria');

  await userEvent.type(screen.getByPlaceholderText('Buscar área'), 'formacion');

  const lista = within(
    screen.getByRole('list', { name: /áreas curriculares/i }),
  );
  expect(lista.queryByText('Matemática')).not.toBeInTheDocument();
  expect(lista.getByText('Formación complementaria')).toBeInTheDocument();
});

test('agrega un curso con los grados elegidos', async () => {
  await renderizar();
  await userEvent.click(
    await screen.findByRole('button', { name: /agregar curso/i }),
  );

  const modal = within(await screen.findByRole('dialog'));
  expect(modal.getByText('Área: Matemática')).toBeInTheDocument();
  await userEvent.click(modal.getByRole('button', { name: 'Agregar curso' }));
  expect(
    await modal.findByText('Ingresa el nombre del curso.'),
  ).toBeInTheDocument();

  await userEvent.type(
    modal.getByPlaceholderText('Ej. Álgebra'),
    'Estadística',
  );
  await userEvent.click(
    modal.getByRole('button', { name: '5.° grado de secundaria' }),
  );
  await userEvent.click(modal.getByRole('button', { name: 'Agregar curso' }));

  await vi.waitFor(() =>
    expect(cursoEnviado).toEqual({ nombre: 'Estadística', grados: ['SEC-5'] }),
  );
});

test('desactivar un curso pide confirmación con el texto de Figma', async () => {
  await renderizar();
  await userEvent.click(
    await screen.findByRole('button', { name: /acciones de álgebra/i }),
  );
  await userEvent.click(
    await screen.findByRole('menuitem', { name: /desactivar/i }),
  );

  const modal = within(await screen.findByRole('dialog'));
  expect(modal.getByText('¿Desactivar el curso Álgebra?')).toBeInTheDocument();
  expect(
    modal.getByText('Ya no podrá agregarse a nuevos planes de evaluación.'),
  ).toBeInTheDocument();
  await userEvent.click(modal.getByRole('button', { name: 'Desactivar' }));

  await vi.waitFor(() => expect(accionEnviada).toBe('alg/desactivar'));
});
