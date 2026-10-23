import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import { createUser, renderApp, screen, within } from '@/testing/test-utils';

import AdministrativoAnioEscolarRoute from '../anio-escolar';

// Figma > Personal Administrativo > CI-01 · Año escolar — listado de años (HU-74)

test('muestra el encabezado de CI-01 con el botón "Nuevo año escolar" y la tabla de años', async () => {
  server.use(
    http.get(`${env.API_URL}/configuracion/anios-escolares`, () =>
      HttpResponse.json([
        {
          id: 'anio-2027',
          anio: 2027,
          fechaInicio: '2027-03-01',
          fechaFin: '2027-12-17',
          regimen: 'TRIMESTRAL',
          cantidadPeriodos: 3,
          seccionesActivas: 10,
          estructurasHorarias: 2,
          estado: 'PLANIFICADO',
        },
      ]),
    ),
  );
  await renderApp(<AdministrativoAnioEscolarRoute />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

  expect(
    screen.getByRole('heading', { name: 'Año escolar' }),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      'Gestiona los años escolares con sus periodos académicos, secciones por grado y estructura horaria. Solo un año puede estar activo a la vez.',
    ),
  ).toBeInTheDocument();
  expect(
    within(screen.getByRole('navigation', { name: 'Ruta' })).getByText(
      'Año escolar',
    ),
  ).toHaveAttribute('aria-current', 'page');
  expect(
    screen.getByRole('button', { name: 'Nuevo año escolar' }),
  ).toBeInTheDocument();
  expect(await screen.findByText('1 año registrado')).toBeInTheDocument();
});
