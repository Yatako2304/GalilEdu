import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import { createUser, renderApp, screen, within } from '@/testing/test-utils';

import AdministrativoPeriodoMatriculaNuevoRoute from '../periodo-matricula-nuevo';

// /app/administrativo/matricula/periodos/nuevo: "Nuevo periodo de matrícula" (HU-23, pantalla 13)

const RUTA = '/app/administrativo/matricula/periodos/nuevo';
const API = `${env.API_URL}/configuracion/periodos-matricula`;

test('muestra el título, el subtítulo de Figma y el camino de vuelta al listado', async () => {
  server.use(http.get(`${API}/anios-disponibles`, () => HttpResponse.json([])));
  renderApp(<AdministrativoPeriodoMatriculaNuevoRoute />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
    path: RUTA,
    url: RUTA,
  });

  expect(
    await screen.findByRole('heading', { name: 'Nuevo periodo de matrícula' }),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      'Define el periodo y los requisitos de la matrícula del año académico. Los apoderados podrán matricular a sus hijos una vez abierta.',
    ),
  ).toBeInTheDocument();
  const ruta = within(screen.getByRole('navigation', { name: 'Ruta' }));
  expect(
    ruta.getByRole('link', { name: 'Periodos de matrícula' }),
  ).toHaveAttribute('href', '/app/administrativo/matricula');
  expect(ruta.getByText('Nuevo periodo de matrícula')).toHaveAttribute(
    'aria-current',
    'page',
  );
  expect(
    await screen.findByText(
      'No hay años escolares disponibles para un nuevo periodo de matrícula.',
    ),
  ).toBeInTheDocument();
});
