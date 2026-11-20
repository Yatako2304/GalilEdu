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
import { DatosPersona, FamiliaApoderado } from '@/types/api';

import { FamiliaDelApoderado } from '../familia-apoderado';

// Apoderado › «Datos de la Familia» (ítem «Familia» del menú lateral), de solo vista. GET /auth/mi-familia.

const PERSONA: DatosPersona = {
  tipoDocumento: 'DNI',
  numeroDocumento: '70000003',
  apellidoPaterno: 'Paredes',
  apellidoMaterno: 'Silva',
  nombres: 'Luis',
  fechaNacimiento: '1985-01-05',
  nacionalidad: 'Peruana',
  lugarNacimiento: 'Perú',
  sexo: 'MASCULINO',
  celular: '987000003',
  correo: 'luis.paredes@galiledu.edu.pe',
  distrito: 'Magdalena del Mar',
  direccion: 'Calle 123',
  esExalumno: false,
};

const FAMILIA: FamiliaApoderado = {
  nombre: 'Paredes Rojas',
  estudiantes: [
    {
      id: 'e1',
      nombreCompleto: 'Mateo Paredes Rojas',
      grado: '1.° de secundaria',
      anioAcademico: 2027,
      estado: 'INSCRITO',
    },
    {
      id: 'e2',
      nombreCompleto: 'Sofía Paredes Rojas',
      grado: '4.° de secundaria',
      anioAcademico: 2026,
      estado: 'MATRICULADO',
    },
  ],
  padre: {
    persona: PERSONA,
    fallecido: false,
    motivoNoRegistra: null,
    relacion: null,
    esUsted: true,
  },
  madre: {
    persona: null,
    fallecido: false,
    motivoNoRegistra: 'No vive con el estudiante',
    relacion: null,
    esUsted: false,
  },
  apoderado: null,
  contactosEmergencia: [
    {
      nombreCompleto: 'Rosa Medina Torres',
      parentesco: 'Abuela',
      celular: '987654321',
      telefonoFijo: null,
    },
  ],
};

const RUTA = `${env.API_URL}/auth/mi-familia`;

const renderFamilia = async (familias: FamiliaApoderado[] = [FAMILIA]) => {
  server.use(http.get(RUTA, () => HttpResponse.json(familias)));
  return renderApp(<FamiliaDelApoderado />, {
    user: await createUser({ roles: [ROLES.APODERADO] }),
  });
};

test('la pestaña «Hijos(as)» lista los estudiantes y los contactos de emergencia, sin distrito ni dirección', async () => {
  await renderFamilia();

  expect(await screen.findByText('Familia Paredes Rojas')).toBeInTheDocument();
  expect(
    screen.getByRole('tab', { name: 'Hijos(as)', selected: true }),
  ).toBeInTheDocument();
  expect(screen.getByText('Mateo Paredes Rojas')).toBeInTheDocument();
  expect(
    screen.getByText('4.° de secundaria · 2026 · Matriculado(a)'),
  ).toBeInTheDocument();
  expect(screen.getByText('Abuela · 987654321')).toBeInTheDocument();
  expect(screen.queryByText('Distrito')).not.toBeInTheDocument();
  expect(screen.queryByText('Dirección')).not.toBeInTheDocument();
});

test('padre, madre y apoderado en sus pestañas; quien consulta lleva «(tú)»', async () => {
  await renderFamilia();
  const pestanas = await screen.findByRole('tablist', {
    name: 'Secciones de la familia',
  });

  await userEvent.click(
    within(pestanas).getByRole('tab', { name: 'Padre (tú)' }),
  );
  expect(screen.getByText('Luis')).toBeInTheDocument();
  // Su propio domicilio sí se ve: es un dato de la persona.
  expect(screen.getByText('Calle 123')).toBeInTheDocument();

  await userEvent.click(within(pestanas).getByRole('tab', { name: 'Madre' }));
  expect(
    screen.getByText('No registra: No vive con el estudiante.'),
  ).toBeInTheDocument();

  await userEvent.click(
    within(pestanas).getByRole('tab', { name: 'Apoderado' }),
  );
  expect(
    screen.getByText(/No se registró un apoderado aparte/),
  ).toBeInTheDocument();
});

test('es de solo vista: no hay campos ni botones para editar', async () => {
  await renderFamilia();

  await screen.findByText('Familia Paredes Rojas');
  expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: /editar|guardar/i }),
  ).not.toBeInTheDocument();
});

test('con más de una familia aparece el selector para elegir cuál ver', async () => {
  await renderFamilia([
    FAMILIA,
    {
      ...FAMILIA,
      nombre: 'Paredes Quispe',
      estudiantes: [
        {
          id: 'e3',
          nombreCompleto: 'Lucía Paredes Quispe',
          grado: '2.° de primaria',
          anioAcademico: 2026,
          estado: 'MATRICULADO',
        },
      ],
    },
  ]);

  expect(await screen.findByText('Familia Paredes Rojas')).toBeInTheDocument();
  expect(screen.getByLabelText('Familia')).toBeInTheDocument();
});

test('sin estudiantes a cargo lo dice', async () => {
  await renderFamilia([]);

  expect(
    await screen.findByText('Aún no tienes estudiantes a cargo registrados.'),
  ).toBeInTheDocument();
});
