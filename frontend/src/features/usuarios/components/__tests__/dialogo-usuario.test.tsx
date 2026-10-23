import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import { createUser, renderApp, screen, userEvent } from '@/testing/test-utils';

import { DialogoUsuario } from '../dialogo-usuario';

// CAHU6-33 y 34: Apoderado y Tutor se muestran, pero no se asignan desde "Nuevo usuario".

const roles = [
  { codigo: ROLES.ADMINISTRADOR, nombre: 'Administrador' },
  { codigo: ROLES.APODERADO, nombre: 'Apoderado' },
  { codigo: ROLES.DOCENTE, nombre: 'Docente' },
  { codigo: ROLES.TUTOR, nombre: 'Tutor' },
];

beforeEach(() => {
  server.use(
    http.get(`${env.API_URL}/usuarios/roles`, () => HttpResponse.json(roles)),
  );
});

test('Apoderado y Tutor están bloqueados; los demás roles se pueden elegir', async () => {
  await renderApp(
    <DialogoUsuario
      estado={{ modo: 'crear' }}
      onCerrar={vi.fn()}
      onCreado={vi.fn()}
    />,
    { user: await createUser({ roles: [ROLES.ADMINISTRADOR] }) },
  );

  const apoderado = await screen.findByRole('button', { name: 'Apoderado' });
  const tutor = screen.getByRole('button', { name: 'Tutor' });
  const docente = screen.getByRole('button', { name: 'Docente' });

  expect(apoderado).toBeDisabled();
  expect(tutor).toBeDisabled();
  expect(docente).toBeEnabled();

  await userEvent.click(docente);
  expect(docente).toHaveAttribute('aria-pressed', 'true');
  expect(tutor).toHaveAttribute('aria-pressed', 'false');
});

const detalle = (extra: object) => ({
  id: 'u1',
  username: 'U20260007',
  estado: 'ACTIVO',
  creadoEn: '2026-10-20T10:00:00Z',
  ultimoAcceso: null,
  personaId: 'p1',
  persona: {
    tipoDocumento: 'DNI',
    numeroDocumento: '44556677',
    apellidoPaterno: 'Ríos',
    apellidoMaterno: 'Paz',
    nombres: 'Carmen',
    correo: 'carmen@gmail.com',
    celular: '987111222',
  },
  ...extra,
});

test('apoderado y tutor: muestra sus estudiantes a cargo y sus secciones', async () => {
  server.use(
    http.get(`${env.API_URL}/usuarios/u1`, () =>
      HttpResponse.json(
        detalle({
          roles: [ROLES.APODERADO, ROLES.TUTOR],
          estudiantesACargo: [
            {
              estudianteId: 'e1',
              nombreCompleto: 'Lucía Ríos Paz',
              documento: 'DNI 77889900',
              grado: '3.° de primaria',
              anioAcademico: 2026,
              estado: 'MATRICULADO',
              parentesco: 'APODERADO',
              relacion: 'Tío(a)',
              viveConEstudiante: true,
              tienePatriaPotestad: false,
              esResponsable: true,
              acreditacion: {
                tipo: 'Carta poder',
                nombreArchivo: 'carta.pdf',
                subidoEn: '2026-10-20T10:00:00Z',
              },
            },
          ],
          tutorDe: [
            { seccionId: 's1', seccion: '3.° de primaria A', cargo: 'TUTOR' },
          ],
        }),
      ),
    ),
  );

  await renderApp(
    <DialogoUsuario
      estado={{ modo: 'editar', id: 'u1' }}
      onCerrar={vi.fn()}
      onCreado={vi.fn()}
    />,
    { user: await createUser({ roles: [ROLES.ADMINISTRADOR] }) },
  );

  expect(await screen.findByText('Lucía Ríos Paz')).toBeInTheDocument();
  expect(
    screen.getByText('DNI 77889900 · 3.° de primaria (2026)'),
  ).toBeInTheDocument();
  expect(screen.getByText('Tío(a)')).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'Carta poder' }),
  ).toBeInTheDocument();
  expect(screen.getByText('3.° de primaria A')).toBeInTheDocument();
  // Los datos personales siguen editables.
  expect(screen.getByDisplayValue('Carmen')).toBeEnabled();
});

test('un usuario sin esos roles no muestra estudiantes a cargo ni tutoría', async () => {
  server.use(
    http.get(`${env.API_URL}/usuarios/u1`, () =>
      HttpResponse.json(detalle({ roles: [ROLES.DOCENTE] })),
    ),
  );

  await renderApp(
    <DialogoUsuario
      estado={{ modo: 'ver', id: 'u1' }}
      onCerrar={vi.fn()}
      onCreado={vi.fn()}
    />,
    { user: await createUser({ roles: [ROLES.ADMINISTRADOR] }) },
  );

  expect(await screen.findByDisplayValue('Carmen')).toBeInTheDocument();
  expect(screen.queryByText('Estudiantes a cargo')).not.toBeInTheDocument();
  expect(screen.queryByText('Tutoría del año actual')).not.toBeInTheDocument();
});

test('el documento de otro usuario se marca en el campo junto con los demás errores', async () => {
  server.use(
    http.get(`${env.API_URL}/usuarios/u1`, () =>
      HttpResponse.json(detalle({ roles: [ROLES.DOCENTE] })),
    ),
    http.get(`${env.API_URL}/estudiantes/personas`, () =>
      HttpResponse.json({
        personaId: 'p2',
        persona: { numeroDocumento: '11111111', correo: 'otro@gmail.com' },
        tieneCuenta: true,
        estudiantesACargo: [],
      }),
    ),
  );

  await renderApp(
    <DialogoUsuario
      estado={{ modo: 'editar', id: 'u1' }}
      onCerrar={vi.fn()}
      onCreado={vi.fn()}
    />,
    { user: await createUser({ roles: [ROLES.ADMINISTRADOR] }) },
  );

  const documento = await screen.findByDisplayValue('44556677');
  await userEvent.clear(documento);
  await userEvent.type(documento, '11111111');
  await userEvent.clear(screen.getByDisplayValue('Carmen'));
  await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

  expect(
    await screen.findByText(
      'Ya existe un usuario registrado con este documento',
    ),
  ).toBeInTheDocument();
  expect(screen.getByText('Ingresa los nombres')).toBeInTheDocument();
  expect(screen.queryByText(/¿Guardar los cambios/)).not.toBeInTheDocument();
});
