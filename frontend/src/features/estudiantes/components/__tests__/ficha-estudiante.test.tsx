import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import { createUser, renderApp, screen } from '@/testing/test-utils';
import { FichaEstudiante as Ficha } from '@/types/api';

import { FichaEstudiante } from '../ficha-estudiante';

// Figma > Estudiantes > ficha: "Ver" abre en Datos personales; tras inscribir se abre en Familia.

const ficha = {
  id: 'a1',
  estado: 'INSCRITO',
  grado: '1.° de primaria',
  anioAcademico: 2027,
  persona: {
    nombres: 'Mateo',
    apellidoPaterno: 'Alonso',
    apellidoMaterno: 'Paredes',
    tipoDocumento: 'DNI',
    numeroDocumento: '78945612',
  },
  datos: {},
  fichaMedica: null,
  familia: [],
  contactos: [],
  modoSalida: null,
  autorizados: [],
  hermanos: [],
} as unknown as Ficha;

beforeEach(() => {
  server.use(
    http.get(`${env.API_URL}/estudiantes/a1`, () => HttpResponse.json(ficha)),
  );
});

const renderFicha = async (aviso?: string) =>
  renderApp(<FichaEstudiante id="a1" aviso={aviso} />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

const pestanaActiva = async () =>
  (await screen.findAllByRole('tab')).find(
    (t) => t.getAttribute('aria-selected') === 'true',
  );

test('al ver un estudiante se abre en Datos personales', async () => {
  await renderFicha();

  expect(await pestanaActiva()).toHaveTextContent('Datos personales');
  expect(screen.getByText('Apellidos y nombres')).toBeInTheDocument();
});

test('tras registrar una inscripción se abre en Familia', async () => {
  await renderFicha('Se creó la cuenta del responsable.');

  expect(await pestanaActiva()).toHaveTextContent('Familia');
  expect(screen.getByText('Padres y apoderado')).toBeInTheDocument();
});
