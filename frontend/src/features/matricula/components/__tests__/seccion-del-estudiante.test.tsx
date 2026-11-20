import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import {
  renderApp,
  screen,
  userEvent,
  waitFor,
  within,
} from '@/testing/test-utils';
import { DetalleMatricula } from '@/types/api';

import { SeccionDelEstudiante } from '../seccion-del-estudiante';

// Ficha del estudiante > Datos personales: tarjeta con la sección y el botón "Cambiar sección".

const API = `${env.API_URL}/matricula/matriculas`;

const matricula = (extras: Partial<DetalleMatricula> = {}) =>
  ({
    id: 'm-1',
    anio: 2027,
    estudianteId: 'e-1',
    nombre: 'Mateo Paredes Rojas',
    grado: '1.° de secundaria',
    seccion: {
      id: 's-a',
      etiqueta: '1.° A',
      tutor: 'Raúl Vargas',
      capacidad: 30,
      ocupadas: 12,
    },
    puedeCambiarSeccion: true,
    ...extras,
  }) as DetalleMatricula;

const conMatricula = (m: DetalleMatricula, cambios: unknown[] = []) =>
  server.use(
    http.get(`${API}/de-estudiante/e-1`, () =>
      HttpResponse.json({ matriculaId: 'm-1' }),
    ),
    http.get(`${API}/m-1`, () => HttpResponse.json(m)),
    http.get(`${API}/m-1/cambios-seccion`, () => HttpResponse.json(cambios)),
  );

test('muestra el historial de cambios de sección cuando hubo cambios', async () => {
  conMatricula(matricula(), [
    {
      id: 'c-1',
      origen: '1.° B',
      destino: '1.° A',
      motivo: 'Reorganización',
      observacion: null,
      realizadoPor: 'Ana Ruiz',
      realizadoEn: '2026-11-10T15:00:00Z',
    },
  ]);
  await renderApp(<SeccionDelEstudiante estudianteId="e-1" />);

  expect(
    await screen.findByText('Historial de cambios de sección'),
  ).toBeInTheDocument();
  expect(screen.getByText(/1.° B/)).toBeInTheDocument();
  expect(screen.getByText(/Reorganización/)).toBeInTheDocument();
});

test('sin cambios no aparece el historial', async () => {
  conMatricula(matricula());
  await renderApp(<SeccionDelEstudiante estudianteId="e-1" />);

  await screen.findByText('1.° A');
  expect(
    screen.queryByText('Historial de cambios de sección'),
  ).not.toBeInTheDocument();
});

test('muestra la sección del estudiante y el botón para cambiarla', async () => {
  conMatricula(matricula());
  await renderApp(<SeccionDelEstudiante estudianteId="e-1" />);

  expect(await screen.findByText('1.° A')).toBeInTheDocument();
  expect(screen.getByText(/Tutor: Raúl Vargas/)).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Cambiar sección' })).toBeEnabled();
});

test('si la matrícula ya no permite cambiar de sección, el botón queda deshabilitado', async () => {
  conMatricula(matricula({ puedeCambiarSeccion: false }));
  await renderApp(<SeccionDelEstudiante estudianteId="e-1" />);

  await screen.findByText('1.° A');
  expect(
    screen.getByRole('button', { name: 'Cambiar sección' }),
  ).toBeDisabled();
});

test('un estudiante sin matrícula no tiene sección ni botón', async () => {
  server.use(
    http.get(`${API}/de-estudiante/e-1`, () =>
      HttpResponse.json({ matriculaId: null }),
    ),
  );
  await renderApp(<SeccionDelEstudiante estudianteId="e-1" />);

  expect(await screen.findByText(/Aún no tiene matrícula/)).toBeVisible();
  expect(
    screen.queryByRole('button', { name: 'Cambiar sección' }),
  ).not.toBeInTheDocument();
});

test('tras cambiar de sección, al abrir otra vez el modal ya marca la nueva como actual', async () => {
  const A = {
    id: 's-a',
    etiqueta: '1.° A',
    tutor: 'Raúl Vargas',
    capacidad: 30,
    ocupadas: 12,
  };
  const B = {
    id: 's-b',
    etiqueta: '1.° B',
    tutor: 'Ana Ríos',
    capacidad: 30,
    ocupadas: 8,
  };
  let actual = A;
  server.use(
    http.get(`${API}/de-estudiante/e-1`, () =>
      HttpResponse.json({ matriculaId: 'm-1' }),
    ),
    http.get(`${API}/m-1`, () =>
      HttpResponse.json(matricula({ seccion: actual })),
    ),
    http.get(`${API}/m-1/cambios-seccion`, () => HttpResponse.json([])),
    http.get(`${API}/m-1/secciones`, () =>
      HttpResponse.json({
        grado: '1.° de secundaria',
        seleccionadaId: actual.id,
        secciones: [A, B],
      }),
    ),
    http.put(`${API}/m-1/seccion`, () => {
      actual = B;
      return HttpResponse.json(matricula({ seccion: actual }));
    }),
  );
  await renderApp(<SeccionDelEstudiante estudianteId="e-1" />);

  await userEvent.click(
    await screen.findByRole('button', { name: 'Cambiar sección' }),
  );
  await userEvent.click(
    await screen.findByRole('radio', { name: 'Sección 1.° B' }),
  );
  const modal = screen.getByRole('dialog', { name: 'Cambiar sección' });
  await userEvent.selectOptions(
    within(modal).getByRole('combobox', { name: 'Motivo del cambio' }),
    'SOLICITUD_APODERADO',
  );
  await userEvent.click(
    within(modal).getByRole('button', { name: 'Cambiar sección' }),
  );
  await userEvent.click(
    within(
      await screen.findByRole('dialog', { name: '¿Cambiar sección?' }),
    ).getByRole('button', { name: 'Cambiar sección' }),
  );
  await waitFor(() =>
    expect(
      screen.queryByRole('dialog', { name: 'Cambiar sección' }),
    ).not.toBeInTheDocument(),
  );

  // Sin recargar: el modal debe partir de 1.° B y no de la sección anterior.
  await userEvent.click(
    screen.getByRole('button', { name: 'Cambiar sección' }),
  );
  await userEvent.click(
    await screen.findByRole('radio', { name: 'Sección 1.° A' }),
  );
  expect(await screen.findByText(/1\.° B: 8 → 7/)).toBeVisible();
});
