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
import { EstudianteResumen, Pagina } from '@/types/api';

import { EstudiantesLista } from '../estudiantes-lista';

// Figma > Estudiantes > listado: los tres puntos de cada fila (Ver, Inactivar o Reactivar, Eliminar).

const estudiantes: EstudianteResumen[] = [
  {
    id: 'a1',
    nombreCompleto: 'Mateo Alonso Paredes',
    documento: 'DNI 78945612',
    grado: '1.° de primaria',
    anioAcademico: 2027,
    estado: 'INSCRITO',
    responsable: 'Luis Paredes',
  },
  {
    id: 'b2',
    nombreCompleto: 'Sofía Paredes Rojas',
    documento: 'DNI 78945613',
    grado: '4.° de secundaria',
    anioAcademico: 2026,
    estado: 'RETIRADO',
    responsable: 'Luis Paredes',
  },
];

let llamadas: string[] = [];
let ultimaConsulta: URLSearchParams | null = null;

beforeEach(() => {
  llamadas = [];
  ultimaConsulta = null;
  server.use(
    http.get(`${env.API_URL}/estudiantes`, ({ request }) => {
      ultimaConsulta = new URL(request.url).searchParams;
      const pagina: Pagina<EstudianteResumen> = {
        data: estudiantes,
        meta: { page: 1, size: 8, total: 2, totalPages: 1 },
      };
      return HttpResponse.json(pagina);
    }),
    http.post(`${env.API_URL}/estudiantes/:id/:accion`, ({ params }) => {
      llamadas.push(`POST ${params.id}/${params.accion}`);
      return new HttpResponse(null, { status: 204 });
    }),
    http.delete(`${env.API_URL}/estudiantes/:id`, ({ params }) => {
      llamadas.push(`DELETE ${params.id}`);
      return new HttpResponse(null, { status: 204 });
    }),
  );
});

const abrirMenu = async (nombre: string) => {
  await screen.findByText(nombre);
  await userEvent.click(screen.getByText(`Acciones de ${nombre}`));
};

const renderLista = async () =>
  renderApp(<EstudiantesLista hrefFicha={(id) => `/ficha/${id}`} />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

test('el menú de un estudiante activo ofrece Ver, Inactivar y Eliminar', async () => {
  await renderLista();
  await abrirMenu('Mateo Alonso Paredes');

  expect(
    await screen.findByRole('menuitem', { name: /ver/i }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('menuitem', { name: /inactivar/i }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('menuitem', { name: /eliminar/i }),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole('menuitem', { name: /reactivar/i }),
  ).not.toBeInTheDocument();
});

test('el menú de un estudiante retirado ofrece Reactivar en vez de Inactivar', async () => {
  await renderLista();
  await abrirMenu('Sofía Paredes Rojas');

  expect(
    await screen.findByRole('menuitem', { name: /reactivar/i }),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole('menuitem', { name: /inactivar/i }),
  ).not.toBeInTheDocument();
});

test('Inactivar pide confirmación y llama a retirar', async () => {
  await renderLista();
  await abrirMenu('Mateo Alonso Paredes');
  await userEvent.click(
    await screen.findByRole('menuitem', { name: /inactivar/i }),
  );

  const dialogo = within(await screen.findByRole('dialog'));
  expect(
    dialogo.getByText('¿Inactivar a Mateo Alonso Paredes?'),
  ).toBeInTheDocument();
  expect(llamadas).toEqual([]);

  await userEvent.click(dialogo.getByRole('button', { name: 'Confirmar' }));

  await vi.waitFor(() => expect(llamadas).toEqual(['POST a1/retirar']));
});

test('Reactivar llama a reactivar', async () => {
  await renderLista();
  await abrirMenu('Sofía Paredes Rojas');
  await userEvent.click(
    await screen.findByRole('menuitem', { name: /reactivar/i }),
  );

  const dialogo = within(await screen.findByRole('dialog'));
  await userEvent.click(dialogo.getByRole('button', { name: 'Confirmar' }));

  await vi.waitFor(() => expect(llamadas).toEqual(['POST b2/reactivar']));
});

test('Eliminar pide confirmación y llama a DELETE', async () => {
  await renderLista();
  await abrirMenu('Mateo Alonso Paredes');
  await userEvent.click(
    await screen.findByRole('menuitem', { name: /eliminar/i }),
  );

  const dialogo = within(await screen.findByRole('dialog'));
  await userEvent.click(dialogo.getByRole('button', { name: 'Eliminar' }));

  await vi.waitFor(() => expect(llamadas).toEqual(['DELETE a1']));
});

test('cancelar la confirmación no cambia nada', async () => {
  await renderLista();
  await abrirMenu('Mateo Alonso Paredes');
  await userEvent.click(
    await screen.findByRole('menuitem', { name: /eliminar/i }),
  );

  const dialogo = within(await screen.findByRole('dialog'));
  await userEvent.click(dialogo.getByRole('button', { name: 'Cancelar' }));

  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(llamadas).toEqual([]);
});

test('el filtro de estado incluye Retirado y se envía al backend', async () => {
  await renderLista();
  await screen.findByText('Mateo Alonso Paredes');

  await userEvent.selectOptions(
    screen.getByLabelText(/filtrar por estado/i),
    'RETIRADO',
  );

  await vi.waitFor(() =>
    expect(ultimaConsulta?.get('estado')).toBe('RETIRADO'),
  );
});
