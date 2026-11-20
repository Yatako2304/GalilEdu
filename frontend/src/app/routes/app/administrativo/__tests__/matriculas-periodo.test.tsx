import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent } from '@/testing/test-utils';
import { PeriodoMatriculas } from '@/types/api';

import AdministrativoMatriculasPeriodoRoute from '../matriculas-periodo';

const API = `${env.API_URL}/matricula/periodos/periodo-2027`;
const RUTA = '/app/administrativo/matricula/periodos/:periodoId/matriculas';
const URL = '/app/administrativo/matricula/periodos/periodo-2027/matriculas';

const periodo = (
  extras: Partial<PeriodoMatriculas> = {},
): PeriodoMatriculas => ({
  id: 'periodo-2027',
  anio: 2027,
  inicio: '2026-09-15T13:00:00Z',
  cierre: '2026-11-01T04:59:00Z',
  estado: 'CERRADO',
  permiteExtemporanea: true,
  indicadores: {
    estudiantes: 0,
    matriculados: 0,
    enProceso: 0,
    requierenAccion: 0,
  },
  ...extras,
});

const servir = (datos: PeriodoMatriculas) =>
  server.use(
    http.get(API, () => HttpResponse.json(datos)),
    http.get(`${API}/matriculas`, () =>
      HttpResponse.json({
        data: [],
        meta: { page: 1, size: 10, total: 0, totalPages: 0 },
      }),
    ),
    http.get(`${API}/estudiantes`, () => HttpResponse.json([])),
  );

const mostrar = () =>
  renderApp(<AdministrativoMatriculasPeriodoRoute />, {
    path: RUTA,
    url: URL,
  });

test('HU-25: un periodo cerrado con permiso habilita Matrícula extemporánea', async () => {
  servir(periodo());
  await mostrar();

  const boton = await screen.findByRole('button', {
    name: 'Matrícula extemporánea',
  });
  expect(boton).toBeEnabled();
  await userEvent.click(boton);
  expect(
    await screen.findByRole('heading', { name: 'Matrícula extemporánea' }),
  ).toBeVisible();
});

test('HU-25: sin permiso el botón extemporáneo permanece deshabilitado', async () => {
  servir(periodo({ permiteExtemporanea: false }));
  await mostrar();

  expect(
    await screen.findByRole('button', { name: 'Matrícula extemporánea' }),
  ).toBeDisabled();
});
