import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import { createUser, renderApp, screen, within } from '@/testing/test-utils';
import { FormularioPeriodoMatricula } from '@/types/api';

import AdministrativoPeriodoMatriculaRoute from '../periodo-matricula';
import AdministrativoPeriodoMatriculaEditarRoute from '../periodo-matricula-editar';

// /app/administrativo/matricula/periodos/:periodoId (ver) y .../editar (HU-23, pantalla 13)

const API = `${env.API_URL}/configuracion/periodos-matricula`;
const RUTA = '/app/administrativo/matricula/periodos/:periodoId';

const periodo = (
  extras: Partial<FormularioPeriodoMatricula> = {},
): FormularioPeriodoMatricula => ({
  id: 'periodo-2027',
  anioEscolarId: 'anio-2027',
  anio: 2027,
  titulo: 'Matrícula 2027',
  estado: 'PROGRAMADO',
  editable: true,
  eliminable: true,
  fechaInicio: '2026-12-01',
  horaInicio: '10:00:00',
  fechaCierre: '2027-02-15',
  horaCierre: '10:00:00',
  diasDelPeriodo: 76,
  montoMatricula: 450,
  tarifarioConfirmado: true,
  vacantesTotales: 0,
  matriculasRegistradas: 0,
  cantidadSecciones: 0,
  resumenSecciones: '',
  bloquearPorMorosidad: true,
  permitirExtemporanea: true,
  secciones: [],
  documentos: [],
  ...extras,
});

const servir = (datos: FormularioPeriodoMatricula) =>
  server.use(http.get(`${API}/periodo-2027`, () => HttpResponse.json(datos)));

const mostrar = async (Ruta: () => React.JSX.Element, sufijo = '') =>
  renderApp(<Ruta />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
    path: `${RUTA}${sufijo}`,
    url: `/app/administrativo/matricula/periodos/periodo-2027${sufijo}`,
  });

test('ver: el título es el nombre del periodo, con el subtítulo de Figma y su camino de vuelta', async () => {
  servir(periodo());
  await mostrar(AdministrativoPeriodoMatriculaRoute);

  expect(
    await screen.findByRole('heading', { name: 'Matrícula 2027' }),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      'El periodo y los requisitos de la matrícula del año académico. Los apoderados pueden matricular a sus hijos.',
    ),
  ).toBeInTheDocument();
  const ruta = within(screen.getByRole('navigation', { name: 'Ruta' }));
  expect(
    ruta.getByRole('link', { name: 'Periodos de matrícula' }),
  ).toHaveAttribute('href', '/app/administrativo/matricula');
  expect(ruta.getByText('Matrícula 2027')).toHaveAttribute(
    'aria-current',
    'page',
  );
  expect(
    await screen.findByRole('link', {
      name: 'Volver a los periodos de matrícula',
    }),
  ).toBeInTheDocument();
});

test('editar: el título lleva el nombre del periodo y la miga vuelve a "Ver"', async () => {
  servir(periodo());
  await mostrar(AdministrativoPeriodoMatriculaEditarRoute, '/editar');

  expect(
    await screen.findByRole('heading', { name: 'Editar Matrícula 2027' }),
  ).toBeInTheDocument();
  const ruta = within(screen.getByRole('navigation', { name: 'Ruta' }));
  expect(ruta.getByRole('link', { name: 'Matrícula 2027' })).toHaveAttribute(
    'href',
    '/app/administrativo/matricula/periodos/periodo-2027',
  );
  expect(ruta.getByText('Editar')).toHaveAttribute('aria-current', 'page');
  expect(
    await screen.findByRole('button', { name: 'Guardar configuración' }),
  ).toBeInTheDocument();
});
