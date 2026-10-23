import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import { createUser, renderApp, screen, within } from '@/testing/test-utils';
import { FilaPeriodoMatricula } from '@/types/api';

import AdministrativoMatriculaRoute from '../matricula';

// /app/administrativo/matricula: el listado "Periodos de matrícula" (HU-23, pantalla 13) y, con ?matriculaId=,
// el acceso temporal de P4 al detalle de una matrícula (HU-36).

const RUTA = '/app/administrativo/matricula';
const API = `${env.API_URL}/configuracion/periodos-matricula`;

const periodo = (
  anio: number,
  estado: FilaPeriodoMatricula['estado'],
): FilaPeriodoMatricula => ({
  id: `periodo-${anio}`,
  nombre: `Matrícula ${anio}`,
  anioEscolarId: `anio-${anio}`,
  anio,
  inicio: `${anio - 1}-12-01T15:00:00Z`,
  cierre: `${anio}-02-15T15:00:00Z`,
  matriculas: 0,
  estado,
  vigente: false,
});

/** Responde el listado (paginado y completo) con estos periodos y ningún vigente. */
const servirPeriodos = (filas: FilaPeriodoMatricula[]) =>
  server.use(
    http.get(API, () =>
      HttpResponse.json({
        data: filas,
        meta: { page: 1, size: 8, total: filas.length, totalPages: 1 },
      }),
    ),
  );

const cuota = {
  cuotaId: 'cuota-1',
  compromisoId: 'compromiso-1',
  estudianteId: 'estudiante-1',
  anio: 2027,
  concepto: 'Matrícula 2027',
  montoBase: 450,
  montoDescuento: 0,
  monto: 450,
  montoPagado: 0,
  fechaVencimiento: '2027-02-28',
  estado: 'PENDIENTE' as const,
  fechaPago: null,
  responsable: 'Lucía Huamán Paz',
};

const mostrar = async (url: string) =>
  renderApp(<AdministrativoMatriculaRoute />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
    path: RUTA,
    url,
  });

test('sin parámetros muestra el listado de periodos de matrícula con su botón "Nuevo periodo"', async () => {
  servirPeriodos([periodo(2027, 'PROGRAMADO'), periodo(2026, 'CERRADO')]);
  await mostrar(RUTA);

  expect(
    screen.getByRole('heading', { name: 'Periodos de matrícula' }),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      'Consulta los periodos de matrícula, configura sus fechas y vacantes, y revisa las matrículas de los estudiantes en cada periodo.',
    ),
  ).toBeInTheDocument();
  expect(
    within(screen.getByRole('navigation', { name: 'Ruta' })).getByText(
      'Periodos de matrícula',
    ),
  ).toHaveAttribute('aria-current', 'page');
  expect(
    screen.getByRole('link', { name: 'Nuevo periodo de matrícula' }),
  ).toHaveAttribute('href', '/app/administrativo/matricula/periodos/nuevo');
  expect(await screen.findByText('Matrícula 2026')).toBeInTheDocument();
  expect(
    screen.queryByRole('heading', { name: 'Detalle de matrícula' }),
  ).not.toBeInTheDocument();
});

test('con ?matriculaId= sigue mostrando la tarjeta de P4, sin pedir ningún periodo', async () => {
  let consultasPeriodos = 0;
  server.use(
    http.get(`${env.API_URL}/pagos/matriculas/:id/cuota-matricula`, () =>
      HttpResponse.json(cuota),
    ),
    http.get(API, () => {
      consultasPeriodos++;
      return HttpResponse.json({ data: [], meta: {} });
    }),
  );
  await mostrar(`${RUTA}?matriculaId=matricula-1`);

  expect(
    screen.getByRole('heading', { name: 'Detalle de matrícula' }),
  ).toBeInTheDocument();
  const migas = screen.getByRole('navigation', { name: 'Ruta' });
  expect(within(migas).getByText('Matrícula')).toBeInTheDocument();
  expect(within(migas).getByText('Detalle')).toHaveAttribute(
    'aria-current',
    'page',
  );
  expect(await screen.findByText('Pendiente de pago')).toBeInTheDocument();
  expect(screen.getByText('S/ 450.00')).toBeInTheDocument();
  expect(
    screen.queryByRole('heading', { name: 'Periodos de matrícula' }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole('link', { name: 'Nuevo periodo de matrícula' }),
  ).not.toBeInTheDocument();
  expect(consultasPeriodos).toBe(0);
});
