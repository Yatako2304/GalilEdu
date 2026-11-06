import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import {
  renderApp,
  screen,
  userEvent,
  waitFor,
  within,
} from '@/testing/test-utils';
import { FilaMatriculaPeriodo, PeriodoMatriculas } from '@/types/api';

import { MatriculasPeriodo } from '../matriculas-periodo';

// HU-24 · A3 "Matrículas del periodo": todo viene de la API (periodo, indicadores y filas).

const API = `${env.API_URL}/matricula/periodos/periodo-2027`;

const periodo = (
  extras: Partial<PeriodoMatriculas> = {},
): PeriodoMatriculas => ({
  id: 'periodo-2027',
  anio: 2027,
  inicio: '2026-09-15T13:00:00Z',
  cierre: '2027-02-03T04:59:00Z', // 2 de febrero a las 23:59 en Lima
  estado: 'ABIERTO',
  permiteExtemporanea: true,
  indicadores: {
    estudiantes: 2,
    matriculados: 1,
    enProceso: 1,
    requierenAccion: 1,
  },
  ...extras,
});

const fila = (extras: Partial<FilaMatriculaPeriodo>): FilaMatriculaPeriodo => ({
  id: 'm-1',
  estudianteId: 'e-1',
  nombre: 'Mateo Alonso Paredes Rojas',
  documento: '74125836',
  gradoCodigo: 'SEC-1',
  grado: '1.° de secundaria',
  seccion: '1.° A',
  estado: 'SECCION_RESERVADA',
  codigo: null,
  tipo: 'NUEVO',
  ...extras,
});

const FILAS = [
  fila({}),
  fila({
    id: 'm-2',
    estudianteId: 'e-2',
    nombre: 'Valeria Sánchez Ferrara',
    documento: '70112233',
    gradoCodigo: 'SEC-5',
    grado: '5.° de secundaria',
    seccion: '5.° A',
    estado: 'MATRICULADO',
    codigo: 'MAT-2027-0001',
  }),
];

const servir = (
  datos: PeriodoMatriculas,
  filas: FilaMatriculaPeriodo[] = FILAS,
) => {
  const pedidos: URLSearchParams[] = [];
  server.use(
    http.get(API, () => HttpResponse.json(datos)),
    http.get(`${API}/matriculas`, ({ request }) => {
      pedidos.push(new URL(request.url).searchParams);
      return HttpResponse.json({
        data: filas,
        meta: { page: 1, size: 10, total: filas.length, totalPages: 1 },
      });
    }),
  );
  return pedidos;
};

test('muestra el aviso con la fecha de cierre real del periodo, no una fija', async () => {
  servir(periodo());
  await renderApp(<MatriculasPeriodo periodoId="periodo-2027" />);

  expect(
    await screen.findByText(
      'El periodo regular está en curso hasta el 02/02/2027.',
    ),
  ).toBeInTheDocument();
});

test('con el periodo cerrado el aviso pasa a advertencia', async () => {
  servir(periodo({ estado: 'CERRADO' }));
  await renderApp(<MatriculasPeriodo periodoId="periodo-2027" />);

  expect(
    await screen.findByText('El periodo de matrícula está cerrado.'),
  ).toBeInTheDocument();
  expect(screen.queryByText(/está en curso/)).not.toBeInTheDocument();
});

test('lista a quienes ya tienen matrícula, con indicadores del servidor', async () => {
  servir(periodo());
  await renderApp(<MatriculasPeriodo periodoId="periodo-2027" />);

  expect(await screen.findByText('Mateo Alonso Paredes Rojas')).toBeVisible();
  expect(screen.getByText('Valeria Sánchez Ferrara')).toBeVisible();
  expect(screen.getByText('DNI 70112233 · MAT-2027-0001')).toBeVisible();
  expect(
    within(screen.getByRole('table')).getByText('Sección reservada'),
  ).toBeVisible();
  expect(screen.getByText('Estudiantes en el periodo')).toBeVisible();
  expect(screen.getByText('Requieren tu acción')).toBeVisible();
});

test('sin matrículas dice que aún no hay estudiantes en el periodo', async () => {
  servir(
    periodo({
      indicadores: {
        estudiantes: 0,
        matriculados: 0,
        enProceso: 0,
        requierenAccion: 0,
      },
    }),
    [],
  );
  await renderApp(<MatriculasPeriodo periodoId="periodo-2027" />);

  expect(
    await screen.findByText(
      'Aún no hay estudiantes con matrícula en este periodo',
    ),
  ).toBeVisible();
});

test('el buscador, el filtro de grado y el orden se piden al servidor', async () => {
  const pedidos = servir(periodo());
  await renderApp(<MatriculasPeriodo periodoId="periodo-2027" />);
  await screen.findByText('Mateo Alonso Paredes Rojas');

  await userEvent.type(
    screen.getByRole('textbox', { name: 'Buscar por nombre, DNI o código' }),
    'mateo',
  );
  await waitFor(() =>
    expect(pedidos.some((p) => p.get('texto') === 'mateo')).toBe(true),
  );

  await userEvent.selectOptions(
    screen.getByRole('combobox', { name: 'Filtrar por grado' }),
    'SEC-1',
  );
  await waitFor(() =>
    expect(pedidos.some((p) => p.get('grado') === 'SEC-1')).toBe(true),
  );

  await userEvent.click(screen.getByRole('button', { name: 'Grado' }));
  await waitFor(() =>
    expect(
      pedidos.some(
        (p) => p.get('orden') === 'grado' && p.get('direccion') === 'asc',
      ),
    ).toBe(true),
  );
  await userEvent.click(screen.getByRole('button', { name: 'Grado' }));
  await waitFor(() =>
    expect(
      pedidos.some(
        (p) => p.get('orden') === 'grado' && p.get('direccion') === 'desc',
      ),
    ).toBe(true),
  );
});
