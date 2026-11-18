import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  renderApp,
  screen,
  userEvent,
  waitFor,
  within,
} from '@/testing/test-utils';
import { AmbienteResumen } from '@/types/api';

import AdministrativoAmbientesRoute from '../ambientes';

// Figma > Personal Administrativo > CI-26 · Ambientes físicos (HU-43), nodos 857:10215 y 857:11500

const API = env.API_URL;

let ambientes: AmbienteResumen[] = [];
let cuerpoAlta: unknown = null;
let cuerpoEdicion: unknown = null;
let accion = '';

beforeEach(() => {
  cuerpoAlta = null;
  cuerpoEdicion = null;
  accion = '';
  ambientes = [
    {
      id: 'a101',
      codigo: 'A-101',
      nombre: 'Aula 101',
      tipo: 'AULA_REGULAR',
      aforoMaximo: 30,
      recursos: ['Pizarra acrílica', 'Proyector'],
      estado: 'ACTIVO',
    },
  ];
  server.use(
    http.get(`${API}/ambientes/resumen`, () =>
      HttpResponse.json({
        activos: ambientes.length,
        total: ambientes.length,
        aulasRegulares: ambientes.length,
        espaciosEspeciales: 0,
        aforoTotal: ambientes.reduce((suma, a) => suma + a.aforoMaximo, 0),
      }),
    ),
    http.get(`${API}/ambientes`, () => HttpResponse.json(ambientes)),
    http.post(`${API}/ambientes`, async ({ request }) => {
      cuerpoAlta = await request.json();
      const nuevo: AmbienteResumen = {
        ...(cuerpoAlta as Omit<AmbienteResumen, 'id' | 'codigo' | 'estado'>),
        id: 'nuevo',
        codigo: 'A-206',
        estado: 'ACTIVO',
      };
      ambientes = [...ambientes, nuevo];
      return HttpResponse.json(nuevo, { status: 201 });
    }),
    http.put(`${API}/ambientes/a101`, async ({ request }) => {
      cuerpoEdicion = await request.json();
      ambientes = [{ ...ambientes[0], ...(cuerpoEdicion as object) }];
      return HttpResponse.json(ambientes[0]);
    }),
    http.post(`${API}/ambientes/:id/:accion`, ({ params }) => {
      accion = `${params.id}/${params.accion}`;
      ambientes = [{ ...ambientes[0], estado: 'INACTIVO' }];
      return HttpResponse.json(ambientes[0]);
    }),
  );
});

const renderizar = async () =>
  renderApp(<AdministrativoAmbientesRoute />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

test('muestra el encabezado de Figma: título, migas, descripción y el botón «Nuevo ambiente»', async () => {
  await renderizar();

  expect(
    screen.getByRole('heading', { name: 'Ambientes físicos' }),
  ).toBeInTheDocument();
  const migas = within(screen.getByRole('navigation', { name: 'Ruta' }));
  expect(migas.getByText('Configuración')).toBeInTheDocument();
  expect(migas.getByText('Ambientes físicos')).toHaveAttribute(
    'aria-current',
    'page',
  );
  expect(
    screen.getByText(
      'Registra las aulas y espacios especiales del colegio con su aforo y recursos. Se usan al asignar aulas a las secciones y al generar horarios.',
    ),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'Nuevo ambiente' }),
  ).toBeInTheDocument();
  // Ya no es el marcador de posición de la HU.
  expect(screen.queryByText(/HU-43/)).not.toBeInTheDocument();
});

test('muestra las tarjetas y la tabla de ambientes', async () => {
  await renderizar();

  expect(await screen.findByText('Aula 101')).toBeInTheDocument();
  expect(screen.getByText('de 1 registrado')).toBeInTheDocument();
  expect(screen.getByText('30 personas')).toBeInTheDocument();
});

test('«Nuevo ambiente» abre el modal, registra, avisa y la lista se actualiza', async () => {
  await renderizar();
  await screen.findByText('Aula 101');

  await userEvent.click(screen.getByRole('button', { name: 'Nuevo ambiente' }));
  const modal = within(await screen.findByRole('dialog'));
  expect(modal.getByText('Nuevo ambiente físico')).toBeInTheDocument();
  await userEvent.type(
    modal.getByRole('textbox', { name: /^Nombre/ }),
    'Aula 206',
  );
  await userEvent.click(modal.getByRole('radio', { name: /Aula regular/ }));
  await userEvent.type(modal.getByRole('textbox', { name: /^Aforo/ }), '35');
  await userEvent.type(
    modal.getByRole('textbox', { name: /^Recursos/ }),
    'Pizarra digital, Proyector',
  );
  await userEvent.click(
    modal.getByRole('button', { name: 'Registrar ambiente' }),
  );

  expect(await screen.findByText('Ambiente registrado.')).toBeInTheDocument();
  expect(cuerpoAlta).toEqual({
    tipo: 'AULA_REGULAR',
    nombre: 'Aula 206',
    aforoMaximo: 35,
    recursos: ['Pizarra digital', 'Proyector'],
  });
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  // La lista y las tarjetas se recargaron con el ambiente nuevo (el código no se muestra).
  expect(await screen.findByText('Aula 206')).toBeInTheDocument();
  expect(screen.getByText('de 2 registrados')).toBeInTheDocument();
  expect(screen.queryByText('A-206')).not.toBeInTheDocument();
});

test('«Editar» del menú de la fila abre el modal con los datos y guarda los cambios', async () => {
  await renderizar();
  await userEvent.click(
    await screen.findByRole('button', { name: 'Acciones de Aula 101' }),
  );
  await userEvent.click(
    await screen.findByRole('menuitem', { name: /editar/i }),
  );

  const modal = within(await screen.findByRole('dialog'));
  expect(modal.getByText('Editar ambiente físico')).toBeInTheDocument();
  const aforo = modal.getByRole('textbox', { name: /^Aforo/ });
  expect(aforo).toHaveValue('30');
  await userEvent.clear(aforo);
  await userEvent.type(aforo, '32');
  await userEvent.click(modal.getByRole('button', { name: 'Guardar cambios' }));

  expect(await screen.findByText('Cambios guardados.')).toBeInTheDocument();
  expect(cuerpoEdicion).toEqual({
    nombre: 'Aula 101',
    aforoMaximo: 32,
    recursos: ['Pizarra acrílica', 'Proyector'],
  });
  expect(await screen.findByText('32 personas')).toBeInTheDocument();
});

test('abrir «Editar» en otro ambiente no arrastra los datos del anterior', async () => {
  ambientes = [
    ...ambientes,
    {
      id: 'aud',
      codigo: 'AUD-01',
      nombre: 'Auditorio',
      tipo: 'ESPACIO_ESPECIAL',
      aforoMaximo: 180,
      recursos: ['Sonido'],
      estado: 'ACTIVO',
    },
  ];
  await renderizar();
  await userEvent.click(
    await screen.findByRole('button', { name: 'Acciones de Auditorio' }),
  );
  await userEvent.click(
    await screen.findByRole('menuitem', { name: /editar/i }),
  );

  const modal = within(await screen.findByRole('dialog'));

  expect(modal.getByRole('textbox', { name: /^Nombre/ })).toHaveValue(
    'Auditorio',
  );
  expect(modal.getByRole('radio', { name: /Espacio especial/ })).toBeChecked();
});

test('«Desactivar» pide confirmación, desactiva y avisa', async () => {
  await renderizar();
  await userEvent.click(
    await screen.findByRole('button', { name: 'Acciones de Aula 101' }),
  );
  await userEvent.click(
    await screen.findByRole('menuitem', { name: /desactivar/i }),
  );

  const modal = within(await screen.findByRole('dialog'));
  expect(
    modal.getByText('¿Desactivar el ambiente Aula 101?'),
  ).toBeInTheDocument();
  await userEvent.click(modal.getByRole('button', { name: 'Desactivar' }));

  expect(await screen.findByText('Ambiente desactivado.')).toBeInTheDocument();
  expect(accion).toBe('a101/desactivar');
});
