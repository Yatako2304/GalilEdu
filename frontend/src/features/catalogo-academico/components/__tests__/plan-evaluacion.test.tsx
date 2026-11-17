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
  AnioEscolarOpcion,
  CursoDisponible,
  OrigenPlan,
  PesosCompetencia,
  PlanGrado,
  SolicitudCopiaPlan,
  SolicitudPesosCompetencia,
  SolicitudPlanCurso,
} from '@/types/api';

import { PlanEvaluacion } from '../plan-evaluacion';

// Figma > Personal Administrativo > CI-17 y CI-20 · Plan de evaluación (HU-56)

const anios: AnioEscolarOpcion[] = [
  { id: 'a27', anio: 2027, estado: 'PLANIFICADO' },
  { id: 'a26', anio: 2026, estado: 'ACTIVO' },
];

const plan1ro: PlanGrado = {
  anioEscolarId: 'a26',
  anio: 2026,
  estadoAnio: 'ACTIVO',
  gradoCodigo: 'SEC-1',
  grado: '1.° de secundaria',
  bloquesDisponibles: 45,
  horasAsignadas: 5,
  competenciasEvaluadas: 3,
  cursosConfigurados: 1,
  docentesAsignados: 1,
  docentesRequeridos: 4,
  reglaNota: 'PROMEDIO_PONDERADO',
  pesosIncompletos: 1,
  secciones: [
    { id: 's1', nombre: 'A', tutor: 'María Fernández Ríos' },
    { id: 's2', nombre: 'B', tutor: null },
  ],
  cursos: [
    {
      id: 'p1',
      cursoId: 'c1',
      curso: 'Aritmética',
      area: 'Matemática',
      horasSemanales: 2,
      competencias: 3,
      escala: 'VIGESIMAL',
      docentesAsignados: 1,
    },
    {
      id: 'p2',
      cursoId: 'c2',
      curso: 'Física',
      area: 'Ciencia y Tecnología',
      horasSemanales: 3,
      competencias: 0,
      escala: null,
      docentesAsignados: 0,
    },
  ],
};

const vacio = (grado: string, nombre: string): PlanGrado => ({
  ...plan1ro,
  anioEscolarId: 'a27',
  anio: 2027,
  estadoAnio: 'PLANIFICADO',
  gradoCodigo: grado,
  grado: nombre,
  horasAsignadas: 0,
  competenciasEvaluadas: 0,
  cursosConfigurados: 0,
  docentesAsignados: 0,
  docentesRequeridos: 0,
  cursos: [],
});

const disponibles: CursoDisponible[] = [
  {
    id: 'fis',
    nombre: 'Física',
    area: 'Ciencia y Tecnología',
    competencias: [
      {
        id: 'k1',
        areaId: 'ct',
        nombre: 'Indaga mediante métodos científicos',
        descripcion: null,
        orden: 1,
        estado: 'ACTIVA',
      },
      {
        id: 'k2',
        areaId: 'ct',
        nombre: 'Explica el mundo físico',
        descripcion: null,
        orden: 2,
        estado: 'ACTIVA',
      },
    ],
  },
];

const origenes: OrigenPlan[] = [
  {
    anioEscolarId: 'a26',
    anio: 2026,
    gradoCodigo: 'SEC-1',
    grado: '1.° de secundaria',
    cursos: 16,
    horas: 35,
    competencias: 20,
  },
];

const pesos: PesosCompetencia[] = [
  {
    competenciaId: 'cantidad',
    competencia: 'Resuelve problemas de cantidad',
    area: 'Matemática',
    total: 60,
    completo: false,
    cursos: [
      { planCursoId: 'p1', curso: 'Aritmética', pesoCurso: 60 },
      { planCursoId: 'p3', curso: 'Geometría', pesoCurso: null },
    ],
  },
  {
    competenciaId: 'datos',
    competencia: 'Resuelve problemas de gestión de datos e incertidumbre',
    area: 'Matemática',
    total: 100,
    completo: true,
    cursos: [{ planCursoId: 'p1', curso: 'Aritmética', pesoCurso: 100 }],
  },
];

let cursoEnviado: SolicitudPlanCurso | null = null;
let copiaEnviada: SolicitudCopiaPlan | null = null;
let pesosEnviados: {
  competenciaId: string;
  cuerpo: SolicitudPesosCompetencia;
} | null = null;
let reglaDelPlan: PlanGrado['reglaNota'] = 'PROMEDIO_PONDERADO';

beforeEach(() => {
  cursoEnviado = null;
  copiaEnviada = null;
  pesosEnviados = null;
  reglaDelPlan = 'PROMEDIO_PONDERADO';
  server.use(
    http.get(`${env.API_URL}/planes-evaluacion/pesos-competencias`, () =>
      HttpResponse.json(pesos),
    ),
    http.put(
      `${env.API_URL}/planes-evaluacion/pesos-competencias/:competenciaId`,
      async ({ request, params }) => {
        pesosEnviados = {
          competenciaId: params.competenciaId as string,
          cuerpo: (await request.json()) as SolicitudPesosCompetencia,
        };
        return HttpResponse.json({ ...pesos[0], completo: true, total: 100 });
      },
    ),
    http.get(`${env.API_URL}/planes-evaluacion/anios`, () =>
      HttpResponse.json(anios),
    ),
    http.get(`${env.API_URL}/planes-evaluacion`, ({ request }) => {
      const params = new URL(request.url).searchParams;
      return HttpResponse.json(
        params.get('anioEscolarId') === 'a26' && params.get('grado') === 'SEC-1'
          ? { ...plan1ro, reglaNota: reglaDelPlan }
          : vacio(params.get('grado')!, '3.° de secundaria'),
      );
    }),
    http.get(`${env.API_URL}/planes-evaluacion/cursos-disponibles`, () =>
      HttpResponse.json(disponibles),
    ),
    http.get(`${env.API_URL}/planes-evaluacion/origenes`, () =>
      HttpResponse.json(origenes),
    ),
    http.post(`${env.API_URL}/planes-evaluacion`, async ({ request }) => {
      cursoEnviado = (await request.json()) as SolicitudPlanCurso;
      return HttpResponse.json({}, { status: 201 });
    }),
    http.post(
      `${env.API_URL}/planes-evaluacion/copias`,
      async ({ request }) => {
        copiaEnviada = (await request.json()) as SolicitudCopiaPlan;
        return HttpResponse.json(plan1ro, { status: 201 });
      },
    ),
  );
});

const renderizar = async () =>
  renderApp(<PlanEvaluacion />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

test('muestra el plan del año activo con sus tarjetas y cursos', async () => {
  await renderizar();

  expect(
    await screen.findByText('1.° de secundaria · 2026'),
  ).toBeInTheDocument();
  expect(screen.getByText('1 / 4')).toBeInTheDocument();
  expect(screen.getByText('2 secciones × 2 cursos')).toBeInTheDocument();
  const tabla = within(screen.getAllByRole('table')[0]);
  expect(tabla.getByText('Vigesimal')).toBeInTheDocument();
  expect(tabla.getByText('Sin configurar')).toBeInTheDocument();
  expect(tabla.getByText('1 / 2')).toBeInTheDocument();
});

test('un grado sin cursos muestra el estado vacío y copia el plan elegido', async () => {
  await renderizar();
  await screen.findByText('1.° de secundaria · 2026');

  await userEvent.selectOptions(screen.getByLabelText('Año escolar'), 'a27');
  await userEvent.click(screen.getByRole('button', { name: '3.° grado' }));

  expect(
    await screen.findByText('3.° de secundaria aún no tiene cursos en 2027'),
  ).toBeInTheDocument();
  await userEvent.click(
    screen.getByRole('button', { name: /copiar plan de otro grado/i }),
  );

  const modal = within(await screen.findByRole('dialog'));
  expect(await modal.findByText('35 de 45 disponibles')).toBeInTheDocument();
  await userEvent.click(modal.getByRole('button', { name: 'Copiar plan' }));

  await vi.waitFor(() =>
    expect(copiaEnviada).toEqual({
      anioEscolarId: 'a27',
      gradoCodigo: 'SEC-3',
      origenAnioEscolarId: 'a26',
      origenGradoCodigo: 'SEC-1',
    }),
  );
});

test('agrega un curso con las competencias marcadas', async () => {
  await renderizar();
  await screen.findByText('1.° de secundaria · 2026');
  await userEvent.click(screen.getByRole('button', { name: 'Agregar curso' }));

  const modal = within(await screen.findByRole('dialog'));
  expect(modal.getByText('Disponibles: 40 bloques')).toBeInTheDocument();
  await userEvent.selectOptions(
    await modal.findByLabelText(/curso \*/i),
    'fis',
  );
  await userEvent.type(modal.getByLabelText(/horas semanales/i), '2');
  await userEvent.click(
    modal.getByRole('checkbox', { name: 'Explica el mundo físico' }),
  );
  expect(
    modal.queryByText('Consolidación de periodos'),
  ).not.toBeInTheDocument();
  await userEvent.click(modal.getByRole('button', { name: 'Agregar curso' }));

  await vi.waitFor(() =>
    expect(cursoEnviado).toEqual({
      anioEscolarId: 'a26',
      gradoCodigo: 'SEC-1',
      cursoId: 'fis',
      horasSemanales: 2,
      competencias: [
        {
          competenciaId: 'k2',
        },
      ],
    }),
  );
});

test('muestra cuánto aporta cada curso a cada competencia del grado', async () => {
  await renderizar();

  const seccion = within(
    (await screen.findByText('Competencias del grado')).closest('div.p-5')!,
  );
  expect(
    await seccion.findByText('Resuelve problemas de cantidad'),
  ).toBeInTheDocument();
  expect(seccion.getByText('Aritmética · 60 %')).toBeInTheDocument();
  expect(seccion.getByText('Geometría · sin peso')).toBeInTheDocument();
  expect(seccion.getByText('Suma 60 %')).toBeInTheDocument();
  expect(seccion.getByText('Un solo curso')).toBeInTheDocument();
  expect(
    seccion.getByText('1 competencia con pesos incompletos'),
  ).toBeInTheDocument();
  // Una competencia con un solo curso no tiene nada que repartir.
  expect(
    seccion.getAllByRole('button', { name: 'Ajustar pesos' }),
  ).toHaveLength(1);
});

test('ajusta los pesos de una competencia y exige que sumen 100 %', async () => {
  await renderizar();
  await userEvent.click(
    await screen.findByRole('button', { name: 'Ajustar pesos' }),
  );

  const modal = within(await screen.findByRole('dialog'));
  await userEvent.click(modal.getByRole('button', { name: 'Guardar' }));
  expect(modal.getByText('Ingresa el peso del curso.')).toBeInTheDocument();
  expect(modal.getByText('Los pesos deben sumar 100 %.')).toBeInTheDocument();
  expect(pesosEnviados).toBeNull();

  await userEvent.click(
    modal.getByRole('button', { name: 'Repartir en partes iguales' }),
  );
  expect(modal.getByText('100 %')).toBeInTheDocument();
  await userEvent.click(modal.getByRole('button', { name: 'Guardar' }));

  await vi.waitFor(() =>
    expect(pesosEnviados).toEqual({
      competenciaId: 'cantidad',
      cuerpo: {
        anioEscolarId: 'a26',
        gradoCodigo: 'SEC-1',
        pesos: [
          { planCursoId: 'p1', pesoCurso: 50 },
          { planCursoId: 'p3', pesoCurso: 50 },
        ],
      },
    }),
  );
});

test('sin promedio ponderado los cursos aportan por igual y no hay pesos que editar', async () => {
  reglaDelPlan = 'PROMEDIO_SIMPLE';
  await renderizar();

  expect(
    await screen.findByText(
      'El año usa promedio simple: los cursos de una competencia aportan por igual.',
    ),
  ).toBeInTheDocument();
  expect(await screen.findByText('Aportan por igual')).toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'Ajustar pesos' }),
  ).not.toBeInTheDocument();
});
