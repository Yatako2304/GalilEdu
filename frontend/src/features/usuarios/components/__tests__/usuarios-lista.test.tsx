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
import { Pagina, UsuarioResumen } from '@/types/api';

import { UsuariosLista } from '../usuarios-lista';

// Figma > Administrador > Usuarios (HU-6, HU-9, HU-12)

const usuarios: UsuarioResumen[] = [
  {
    id: '1',
    username: 'U20260004',
    nombre: 'María Fernández',
    nombreCompleto: 'María Fernández Ríos',
    correo: 'maria.fernandez@galiledu.edu.pe',
    roles: [ROLES.DOCENTE, ROLES.TUTOR],
    estado: 'ACTIVO',
    ultimoAcceso: '2026-06-05T15:00:00Z',
  },
  {
    id: '2',
    username: 'U20260007',
    nombre: 'Leonardo Montoya',
    nombreCompleto: 'Leonardo Montoya Spray',
    correo: 'leo.montoya@galiledu.edu.pe',
    roles: [ROLES.ADMINISTRADOR],
    estado: 'INACTIVO',
    ultimoAcceso: null,
  },
];

let ultimaConsulta: URLSearchParams | null = null;

beforeEach(() => {
  ultimaConsulta = null;
  server.use(
    http.get(`${env.API_URL}/usuarios`, ({ request }) => {
      ultimaConsulta = new URL(request.url).searchParams;
      const pagina: Pagina<UsuarioResumen> = {
        data: usuarios,
        meta: { page: 1, size: 8, total: 2, totalPages: 1 },
      };
      return HttpResponse.json(pagina);
    }),
  );
});

test('muestra usuarios con su rol, el contador de roles extra y el estado', async () => {
  await renderApp(<UsuariosLista onAbrir={vi.fn()} />, {
    user: await createUser({ roles: [ROLES.ADMINISTRADOR] }),
  });

  expect(await screen.findByText('María Fernández')).toBeInTheDocument();
  const tabla = within(screen.getByRole('table'));
  expect(tabla.getByText('Docente')).toBeInTheDocument();
  expect(tabla.getByText('+1')).toBeInTheDocument();
  expect(tabla.getByText('Inactivo')).toBeInTheDocument();
  expect(screen.getByText('Sin accesos')).toBeInTheDocument();
  expect(screen.getByText('1-2')).toBeInTheDocument();
});

test('el filtro de estado se envía al backend', async () => {
  await renderApp(<UsuariosLista onAbrir={vi.fn()} />, {
    user: await createUser({ roles: [ROLES.ADMINISTRADOR] }),
  });
  await screen.findByText('María Fernández');

  await userEvent.selectOptions(
    screen.getByLabelText(/filtrar por estado/i),
    'INACTIVO',
  );

  await vi.waitFor(() =>
    expect(ultimaConsulta?.get('estado')).toBe('INACTIVO'),
  );
});

test('el menú de una fila abre "Editar" con el id del usuario', async () => {
  const onAbrir = vi.fn();
  await renderApp(<UsuariosLista onAbrir={onAbrir} />, {
    user: await createUser({ roles: [ROLES.ADMINISTRADOR] }),
  });
  await screen.findByText('María Fernández');

  await userEvent.click(screen.getByText('Acciones de María Fernández'));
  await userEvent.click(
    await screen.findByRole('menuitem', { name: /editar/i }),
  );

  expect(onAbrir).toHaveBeenCalledWith({ modo: 'editar', id: '1' });
});
