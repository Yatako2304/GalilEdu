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

import { ReportesNotas } from '../reportes-notas';

// HU-63 (por curso y grado) y HU-64 (institucional).

const API = env.API_URL;

const celda = (
  promedio: string,
  nivel: 'AD' | 'A' | 'B' | 'C' | null,
  conNota: number,
  logroEsperado: number,
) => ({
  promedio,
  nivel,
  logro: nivel === 'B' ? 'En proceso' : nivel ? 'Logro esperado' : 'Pendiente',
  conNota,
  logroEsperado,
  porcentajeLogro: conNota ? Math.round((logroEsperado * 100) / conNota) : null,
});

const distribucion = (
  ad: number,
  a: number,
  b: number,
  c: number,
  porCompletar = 0,
) => {
  const calificaciones = ad + a + b + c;
  return {
    calificaciones,
    ad,
    a,
    b,
    c,
    porCompletar,
    porcentajeLogro: calificaciones
      ? Math.round(((ad + a) * 100) / calificaciones)
      : null,
  };
};

const preparar = () => {
  const peticiones: URL[] = [];
  server.use(
    http.get(`${API}/calificaciones/reportes/anios`, () =>
      HttpResponse.json([
        {
          id: 'anio-2026',
          anio: 2026,
          estado: 'ACTIVO',
          escala: 'VIGESIMAL',
          reglaNota: 'PROMEDIO_PONDERADO',
          consolidacion: 'PROMEDIO_PERIODOS',
          periodos: [{ id: 'p3', numero: 3, nombre: 'III Bimestre' }],
          grados: [
            {
              codigo: 'SEC-1',
              nombre: '1.° de secundaria',
              cursos: [{ planCursoId: 'aritmetica', curso: 'Aritmética' }],
            },
          ],
        },
      ]),
    ),
    http.get(`${API}/calificaciones/reportes/grado`, ({ request }) => {
      const url = new URL(request.url);
      peticiones.push(url);
      return HttpResponse.json({
        anio: 2026,
        grado: '1.° de secundaria',
        curso: url.searchParams.get('planCursoId') ? 'Aritmética' : null,
        periodo: 'Nota final',
        escala: 'VIGESIMAL',
        reglaNota: 'PROMEDIO_PONDERADO',
        secciones: [
          { id: 's-a', nombre: 'A', estudiantes: 3 },
          { id: 's-b', nombre: 'B', estudiantes: 2 },
        ],
        filas: [
          {
            competenciaId: 'cantidad',
            competencia: 'Resuelve problemas de cantidad',
            area: 'Matemática',
            calculable: true,
            secciones: [celda('15.4', 'A', 5, 4), celda('—', null, 0, 0)],
            general: celda('12.5', 'B', 10, 3),
          },
        ],
      });
    }),
    http.get(`${API}/calificaciones/reportes/secciones/:id/notas`, () => {
      const nota = (valor: string, nivel: 'A' | 'B' | null) => ({
        nota: valor,
        nivel,
        logro: nivel ? 'Logro esperado' : 'Pendiente',
        pendiente: nivel === null,
        calificadas: nivel === null ? 1 : 2,
        total: 2,
      });
      const fila = (
        matriculaId: string,
        estudiante: string,
        valor: string,
        nivel: 'A' | 'B' | null,
        porPeriodo: ['A' | 'B' | null, 'A' | 'B' | null],
      ) => ({
        matriculaId,
        estudiante,
        competencias: [
          {
            competenciaId: 'cantidad',
            periodos: porPeriodo.map((n, i) => ({
              ...nota(n ? '14' : '—', n),
              periodoId: `p${i + 1}`,
              aportes: [],
            })),
            notaFinal: nota(valor, nivel),
          },
        ],
      });
      return HttpResponse.json({
        seccionId: 's-a',
        anio: 2026,
        grado: '1.° de secundaria',
        seccion: 'A',
        notas: {
          escala: 'VIGESIMAL',
          reglaNota: 'PROMEDIO_PONDERADO',
          consolidacion: 'PROMEDIO_PERIODOS',
          periodos: [
            { id: 'p1', numero: 1, nombre: 'I Bimestre' },
            { id: 'p2', numero: 2, nombre: 'II Bimestre' },
          ],
          competencias: [
            {
              competenciaId: 'cantidad',
              competencia: 'Resuelve problemas de cantidad',
              usaPesos: true,
              pesosCompletos: true,
              cursos: [],
            },
          ],
          filas: [
            fila('m-ana', 'Ana Lucía García López', '14.2', 'A', ['A', 'B']),
            fila('m-camila', 'Camila Sofía Torres Vargas', '—', null, [
              'A',
              null,
            ]),
          ],
        },
      });
    }),
    http.get(`${API}/calificaciones/reportes/matriculas/:id/notas`, () => {
      const resultado = (nota: string, nivel: 'A' | 'B') => ({
        nota,
        nivel,
        logro: nivel === 'A' ? 'Logro esperado' : 'En proceso',
        pendiente: false,
        calificadas: 1,
        total: 1,
      });
      return HttpResponse.json({
        estudianteId: 'e-ana',
        estudiante: 'Ana Lucía García López',
        anio: 2026,
        estadoAnio: 'ACTIVO',
        finalizado: false,
        grado: '1.° de secundaria',
        seccion: 'A',
        escala: 'VIGESIMAL',
        reglaNota: 'PROMEDIO_PONDERADO',
        consolidacion: 'PROMEDIO_PERIODOS',
        periodos: [
          {
            id: 'p1',
            numero: 1,
            nombre: 'I Bimestre',
            fechaInicio: '2026-03-01',
            fechaFin: '2026-05-10',
            estado: 'CERRADO',
          },
          {
            id: 'p2',
            numero: 2,
            nombre: 'II Bimestre',
            fechaInicio: '2026-05-18',
            fechaFin: '2026-07-20',
            estado: 'EN_CURSO',
          },
        ],
        cursos: [
          {
            planCursoId: 'aritmetica',
            curso: 'Aritmética',
            area: 'Matemática',
            docente: 'María Fernández Ríos',
            periodos: [
              { periodoId: 'p1', resultado: resultado('16', 'A') },
              { periodoId: 'p2', resultado: resultado('12', 'B') },
            ],
            competencias: [
              {
                competenciaId: 'cantidad',
                competencia: 'Resuelve problemas de cantidad',
                periodos: [
                  {
                    periodoId: 'p1',
                    aporte: resultado('16', 'A'),
                    items: [
                      {
                        itemId: 'i1',
                        item: 'Examen de cantidad',
                        tipo: 'EXAMEN',
                        nota: '16',
                        nivel: 'A',
                      },
                    ],
                  },
                  {
                    periodoId: 'p2',
                    aporte: resultado('12', 'B'),
                    items: [
                      {
                        itemId: 'i2',
                        item: 'Práctica de fracciones',
                        tipo: 'PRACTICA',
                        nota: '12',
                        nivel: 'B',
                      },
                    ],
                  },
                ],
              },
            ],
          },
        ],
        competencias: [
          {
            competenciaId: 'cantidad',
            competencia: 'Resuelve problemas de cantidad',
            area: 'Matemática',
            cursos: ['Aritmética'],
            calculable: true,
            periodos: [
              { ...resultado('16', 'A'), periodoId: 'p1', aportes: [] },
              { ...resultado('12', 'B'), periodoId: 'p2', aportes: [] },
            ],
            notaFinal: resultado('14', 'A'),
          },
        ],
      });
    }),
    http.get(`${API}/calificaciones/reportes/institucional`, () =>
      HttpResponse.json({
        anio: 2026,
        periodo: 'Nota final',
        escala: 'VIGESIMAL',
        estudiantes: 5,
        filas: [
          {
            gradoCodigo: 'SEC-1',
            grado: '1.° de secundaria',
            seccion: 'A',
            estudiantes: 3,
            distribucion: distribucion(1, 2, 1, 0, 2),
          },
          {
            gradoCodigo: 'SEC-1',
            grado: '1.° de secundaria',
            seccion: null,
            estudiantes: 3,
            distribucion: distribucion(1, 2, 1, 0, 2),
          },
        ],
        areas: [
          { area: 'Matemática', distribucion: distribucion(1, 2, 1, 0, 2) },
          { area: 'Comunicación', distribucion: distribucion(0, 0, 0, 0) },
        ],
        total: distribucion(1, 2, 1, 0, 2),
        sinCalcular: [
          {
            grado: '2.° de secundaria',
            competencia: 'Resuelve problemas de forma',
          },
        ],
      }),
    ),
  );
  return peticiones;
};

const renderReportes = async (vistas: ('grado' | 'institucional')[]) =>
  renderApp(<ReportesNotas vistas={vistas} />, {
    user: await createUser({ roles: [ROLES.COORDINADOR] }),
  });

test('el reporte por grado muestra cada competencia por sección y en todo el grado', async () => {
  preparar();
  await renderReportes(['grado', 'institucional']);

  const tabla = await screen.findByRole('table');
  const fila = within(tabla)
    .getByText('Resuelve problemas de cantidad')
    .closest('tr')!;
  expect(within(fila).getByText('15,4/20')).toBeInTheDocument();
  expect(within(fila).getByText('80 % con A o AD')).toBeInTheDocument();
  expect(within(fila).getByText('— Sin notas')).toBeInTheDocument();
  expect(within(fila).getByText('12,5/20')).toBeInTheDocument();
  expect(screen.queryByText(/aprobad/i)).not.toBeInTheDocument();
});

test('al elegir un curso pide su aporte y lo aclara', async () => {
  const peticiones = preparar();
  await renderReportes(['grado']);
  await screen.findByRole('table');

  await userEvent.selectOptions(screen.getByLabelText('Curso'), 'aritmetica');

  expect(await screen.findByText('Aporte de Aritmética')).toBeInTheDocument();
  await waitFor(() =>
    expect(peticiones.at(-1)?.searchParams.get('planCursoId')).toBe(
      'aritmetica',
    ),
  );
  // «Nota final del año» no envía periodoId.
  expect(peticiones.at(-1)?.searchParams.has('periodoId')).toBe(false);
});

test('el reporte institucional muestra porcentajes por nivel, por grado, sección y área', async () => {
  preparar();
  await renderReportes(['grado', 'institucional']);
  await screen.findByRole('table');

  await userEvent.click(screen.getByRole('tab', { name: 'Institucional' }));

  const porGrado = within(
    await screen.findByRole('table', {
      name: 'Resultados por grado y sección',
    }),
  );
  const seccion = porGrado.getByText('Sección A').closest('tr')!;
  // 4 calificaciones: AD 1 (25 %), A 2 (50 %), B 1 (25 %), C 0; A o AD = 75 %.
  expect(within(seccion).getByText('4')).toBeInTheDocument();
  expect(within(seccion).getByText('+2 por completar')).toBeInTheDocument();
  expect(within(seccion).getByText('50 %')).toBeInTheDocument();
  expect(within(seccion).getByText('75 %')).toBeInTheDocument();
  expect(
    within(seccion).getByRole('img', {
      name: 'AD: 25 % (1), A: 50 % (2), B: 25 % (1), C: 0 % (0)',
    }),
  ).toBeInTheDocument();
  expect(porGrado.getByText('1.° de secundaria')).toBeInTheDocument();
  expect(porGrado.getByText('Todo el colegio')).toBeInTheDocument();

  // Ya no hay columnas «Pendientes» ni «Sin notas».
  expect(screen.queryByText('Sin notas')).not.toBeInTheDocument();
  expect(screen.queryByText('Pendientes')).not.toBeInTheDocument();

  // Lo que no se cuenta se explica aparte.
  expect(
    screen.getByText('2 calificaciones por completar no se cuentan todavía'),
  ).toBeInTheDocument();
  expect(
    screen.getByText(/2\.° de secundaria · Resuelve problemas de forma/),
  ).toBeInTheDocument();

  const porArea = within(
    screen.getByRole('table', { name: 'Resultados por área' }),
  );
  expect(porArea.getByText('Matemática')).toBeInTheDocument();
  expect(porArea.queryByText('Comunicación')).not.toBeInTheDocument();
  expect(
    screen.getByText('Aún sin calificaciones en este periodo: Comunicación.'),
  ).toBeInTheDocument();
});

test('el coordinador abre una sección y ve la nota de cada estudiante, con su libreta', async () => {
  preparar();
  await renderReportes(['grado']);
  await screen.findByRole('table');

  await userEvent.click(
    screen.getByRole('button', { name: 'Ver estudiantes de la sección A' }),
  );

  const detalle = within(
    await screen.findByRole('table', { name: 'Notas por estudiante' }),
  );
  expect(
    screen.getByText('1.° de secundaria · Sección A · notas por estudiante'),
  ).toBeVisible();
  expect(detalle.getByText('Ana Lucía García López')).toBeVisible();
  expect(
    detalle.getByRole('img', { name: 'A · Logro esperado' }),
  ).toBeVisible();
  expect(detalle.getByText('14,2')).toBeVisible();
  // Camila tiene notas por registrar: pendiente, sin letra.
  expect(detalle.getByText('Pendiente · 1 de 2 ítems')).toBeVisible();
  expect(
    detalle.getByRole('button', {
      name: 'Descargar la libreta de Camila Sofía Torres Vargas',
    }),
  ).toBeEnabled();
});

test('«Todo el año» muestra el nivel de cada periodo y la nota final de cada estudiante', async () => {
  preparar();
  await renderReportes(['grado']);
  await screen.findByRole('table');
  await userEvent.click(
    screen.getByRole('button', { name: 'Ver estudiantes de la sección A' }),
  );
  await screen.findByRole('table', { name: 'Notas por estudiante' });

  await userEvent.click(screen.getByRole('tab', { name: 'Todo el año' }));

  const tabla = within(
    screen.getByRole('table', { name: 'Notas de todo el año por estudiante' }),
  );
  expect(tabla.getByText('Resuelve problemas de cantidad')).toBeVisible();
  expect(tabla.getByTitle('I Bimestre')).toHaveTextContent('I');
  expect(tabla.getByTitle('II Bimestre')).toHaveTextContent('II');
  expect(tabla.getByText('Final')).toBeVisible();

  // Ana: A (I) · B (II) → A final.
  const ana = within(tabla.getByText('Ana Lucía García López').closest('tr')!);
  expect(ana.getAllByRole('img', { name: 'A · Logro esperado' })).toHaveLength(
    2,
  );
  expect(ana.getByRole('img', { name: 'B · En proceso' })).toBeVisible();
  // Camila: A (I) y pendiente en el II y en la final.
  const camila = within(
    tabla.getByText('Camila Sofía Torres Vargas').closest('tr')!,
  );
  expect(camila.getAllByText('Pend.')).toHaveLength(2);
});

test('el coordinador ve los ítems que calificó el docente, sin poder cambiarlos', async () => {
  preparar();
  await renderReportes(['grado']);
  await screen.findByRole('table');
  await userEvent.click(
    screen.getByRole('button', { name: 'Ver estudiantes de la sección A' }),
  );
  await screen.findByRole('table', { name: 'Notas por estudiante' });

  await userEvent.click(
    screen.getByRole('button', {
      name: 'Ver los ítems de Ana Lucía García López',
    }),
  );

  expect(
    await screen.findByText(
      'Ana Lucía García López · 1.° de secundaria · Sección A',
    ),
  ).toBeVisible();
  // Empieza en el periodo en curso.
  expect(screen.getByRole('tab', { name: 'II Bimestre' })).toHaveAttribute(
    'aria-selected',
    'true',
  );
  expect(screen.getByText('Prof. María Fernández Ríos')).toBeVisible();
  await userEvent.click(screen.getByRole('button', { name: /Aritmética/ }));
  expect(await screen.findByText('Práctica de fracciones')).toBeVisible();

  await userEvent.click(screen.getByRole('tab', { name: 'I Bimestre' }));
  expect(await screen.findByText('Examen de cantidad')).toBeVisible();

  // Solo lectura: no hay cómo registrar ni modificar notas.
  expect(
    screen.queryByRole('button', { name: /modificar|registrar|guardar/i }),
  ).not.toBeInTheDocument();
  expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
});
