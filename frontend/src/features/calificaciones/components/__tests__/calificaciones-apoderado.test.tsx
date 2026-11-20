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
import { NotaResultado, NotasApoderado } from '@/types/api';

import { CalificacionesApoderado } from '../calificaciones-apoderado';

// HU-69: el apoderado consulta las notas de su hijo (Figma > Apoderado > «HU-69 Consulta de notas…»).

const API = env.API_URL;

const nota = (
  valor: string,
  nivel: NotaResultado['nivel'],
  logro: NotaResultado['logro'],
): NotaResultado => ({
  nota: valor,
  nivel,
  logro,
  pendiente: false,
  calificadas: 1,
  total: 1,
});
const pendiente = nota('—', null, 'Pendiente');

const periodos = [
  ['p1', 1, 'I Bimestre', '2026-03-02', '2026-05-08', 'CERRADO'],
  ['p2', 2, 'II Bimestre', '2026-05-18', '2026-07-24', 'CERRADO'],
  ['p3', 3, 'III Bimestre', '2026-08-03', '2026-10-09', 'EN_CURSO'],
  ['p4', 4, 'IV Bimestre', '2026-10-19', '2026-12-18', 'PLANIFICADO'],
].map(([id, numero, nombre, fechaInicio, fechaFin, estado]) => ({
  id,
  numero,
  nombre,
  fechaInicio,
  fechaFin,
  estado,
})) as NotasApoderado['periodos'];

const notasAna = (finalizado = false): NotasApoderado => ({
  estudianteId: 'est-ana',
  estudiante: 'Ana Lucía García López',
  anio: 2026,
  estadoAnio: finalizado ? 'CERRADO' : 'ACTIVO',
  finalizado,
  grado: '1.° de secundaria',
  seccion: 'A',
  escala: 'VIGESIMAL',
  reglaNota: 'PROMEDIO_PONDERADO',
  consolidacion: 'PROMEDIO_PERIODOS',
  periodos,
  cursos: [
    {
      planCursoId: 'aritmetica',
      curso: 'Aritmética',
      area: 'Matemática',
      docente: 'María Fernández Ruiz',
      periodos: periodos.map((p) => ({
        periodoId: p.id,
        resultado:
          p.id === 'p3' ? nota('16', 'A', 'Logro esperado') : pendiente,
      })),
      competencias: [
        {
          competenciaId: 'cantidad',
          competencia: 'Resuelve problemas de cantidad',
          periodos: periodos.map((p) => ({
            periodoId: p.id,
            aporte:
              p.id === 'p3' ? nota('16', 'A', 'Logro esperado') : pendiente,
            items:
              p.id === 'p3'
                ? [
                    {
                      itemId: 'i1',
                      item: 'Examen bimestral',
                      tipo: 'EXAMEN',
                      nota: '16',
                      nivel: 'A',
                    },
                  ]
                : [],
          })),
        },
      ],
    },
  ],
  competencias: [
    {
      competenciaId: 'cantidad',
      competencia: 'Resuelve problemas de cantidad',
      area: 'Matemática',
      cursos: ['Aritmética', 'Geometría'],
      calculable: true,
      periodos: periodos.map((p) => ({
        ...(p.id === 'p3' ? nota('14', 'A', 'Logro esperado') : pendiente),
        periodoId: p.id,
        aportes: [],
      })),
      notaFinal: nota('14', 'A', 'Logro esperado'),
    },
    {
      competenciaId: 'forma',
      competencia: 'Resuelve problemas de forma',
      area: 'Matemática',
      cursos: ['Geometría'],
      calculable: true,
      periodos: periodos.map((p) => ({
        ...(p.id === 'p3' ? nota('11', 'B', 'En proceso') : pendiente),
        periodoId: p.id,
        aportes: [],
      })),
      notaFinal: nota('11', 'B', 'En proceso'),
    },
  ],
});

const conNotas = (finalizado = false) =>
  server.use(
    http.get(`${API}/calificaciones/apoderado/estudiantes`, () =>
      HttpResponse.json([
        {
          estudianteId: 'est-ana',
          estudiante: 'Ana Lucía García López',
          matriculas: [
            { anio: 2026, grado: '1.° de secundaria', seccion: 'A' },
          ],
        },
      ]),
    ),
    http.get(`${API}/calificaciones/apoderado/estudiantes/:id/notas`, () =>
      HttpResponse.json(notasAna(finalizado)),
    ),
  );

const renderNotas = async () =>
  renderApp(<CalificacionesApoderado />, {
    user: await createUser({ roles: [ROLES.APODERADO] }),
  });

test('muestra el periodo en curso con los cursos, sus ítems y la nota de cada competencia', async () => {
  conNotas();
  await renderNotas();

  expect(await screen.findByText('Cursos · III Bimestre')).toBeInTheDocument();
  expect(screen.getByLabelText('Estudiante')).toHaveValue('est-ana');
  expect(
    screen.getByRole('button', { name: 'III Bimestre', pressed: true }),
  ).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'IV Bimestre' })).toBeDisabled();
  // Una de dos competencias con A o AD; la otra está en proceso.
  expect(screen.getByText('1 de 2')).toBeInTheDocument();
  expect(
    screen.getByText('Por reforzar: Resuelve problemas de forma'),
  ).toBeInTheDocument();

  await userEvent.click(screen.getByRole('button', { name: /aritmética/i }));
  expect(screen.getByText('Examen bimestral')).toBeInTheDocument();
  expect(screen.getByText('Prof. María Fernández Ruiz')).toBeInTheDocument();

  const tabla = screen.getByRole('table');
  const fila = within(tabla)
    .getByText('Resuelve problemas de cantidad')
    .closest('tr')!;
  expect(
    within(fila).getAllByRole('img', { name: 'A · Logro esperado' }),
  ).toHaveLength(2);
  expect(screen.queryByText(/aprobado/i)).not.toBeInTheDocument();
});

test('con el año terminado muestra el resultado final por niveles de logro, sin «Aprobado»', async () => {
  conNotas(true);
  await renderNotas();

  expect(
    await screen.findByText('Notas finales por competencia'),
  ).toBeInTheDocument();
  expect(screen.getByText('Año escolar 2026 · Finalizado')).toBeInTheDocument();
  expect(screen.getByText('1 de 2 competencias')).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: /descargar libreta final/i }),
  ).toBeEnabled();
  expect(screen.queryByText(/aprobado/i)).not.toBeInTheDocument();
});

test('sin hijos matriculados lo dice', async () => {
  server.use(
    http.get(`${API}/calificaciones/apoderado/estudiantes`, () =>
      HttpResponse.json([]),
    ),
  );
  await renderNotas();

  expect(
    await screen.findByText('No tienes estudiantes matriculados.'),
  ).toBeInTheDocument();
});
