import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import {
  ANIO_2025,
  ANIO_2026,
  horarioPrimariaYSecundaria,
  horarioSinClases,
} from '@/testing/mocks/fixtures/horario-docente';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  renderApp,
  screen,
  userEvent,
  waitFor,
  within,
} from '@/testing/test-utils';

import { HorarioDocente } from '../horario-docente';

// HU-49: el docente y el tutor consultan su horario de dictado (Figma > Docente / Tutor > Horario, 393:18119).

const API = env.API_URL;

const renderHorario = async (rol: ROLES = ROLES.DOCENTE) =>
  renderApp(<HorarioDocente />, { user: await createUser({ roles: [rol] }) });

const botonDelSelect = (etiqueta: string) => {
  const texto = screen
    .getAllByText(etiqueta)
    .find((elemento) => elemento.closest('label'));
  return within(texto!.closest('label') as HTMLElement)
    .getAllByRole('combobox')
    .find((e) => e.tagName === 'BUTTON') as HTMLElement;
};

test('docente solo con secundaria: una matriz sin título, con curso, aula y sección, y sin «Prof.»', async () => {
  await renderHorario();

  const tabla = await screen.findByRole('table', { name: /matriz horaria/i });
  expect(screen.getAllByRole('table')).toHaveLength(1);
  expect(screen.getByRole('heading', { name: 'Horario' })).toBeInTheDocument();
  expect(
    screen.getByText('Visualiza tu horario de un año académico seleccionado'),
  ).toBeInTheDocument();
  // Con un solo horario no hay título de nivel.
  expect(
    screen.queryByRole('heading', { name: 'Secundaria' }),
  ).not.toBeInTheDocument();

  // Álgebra del lunes: en 1.° A (A-401) y, en el bloque siguiente, en 1.° B (A-402).
  expect(within(tabla).getAllByText('Álgebra').length).toBeGreaterThan(0);
  expect(within(tabla).getAllByText('A-401').length).toBeGreaterThan(0);
  expect(within(tabla).getAllByText('1.° A').length).toBeGreaterThan(0);
  expect(within(tabla).getByText('2.° A')).toBeVisible();
  expect(within(tabla).getByText('Geometría')).toBeVisible();
  expect(within(tabla).queryByText(/Prof\./)).not.toBeInTheDocument();
  expect(within(tabla).getAllByText('Recreo')).toHaveLength(5);
  // El año por defecto es el que devolvió el backend.
  expect(botonDelSelect('Año académico')).toHaveTextContent('2026');
});

test('primaria y secundaria: dos tarjetas con su título, primaria primero, cada una con su jornada', async () => {
  server.use(
    http.get(`${API}/horarios/docente/horario`, () =>
      HttpResponse.json(horarioPrimariaYSecundaria),
    ),
  );
  await renderHorario();

  const primaria = await screen.findByRole('heading', { name: 'Primaria' });
  const secundaria = screen.getByRole('heading', { name: 'Secundaria' });
  expect(
    primaria.compareDocumentPosition(secundaria) &
      Node.DOCUMENT_POSITION_FOLLOWING,
  ).toBeTruthy();

  const tablas = screen.getAllByRole('table', { name: /matriz horaria/i });
  expect(tablas).toHaveLength(2);
  // Primaria entra a las 7:45; secundaria, a las 8:00.
  expect(within(tablas[0]).getByText('7:45am - 8:30am')).toBeVisible();
  expect(within(tablas[0]).getAllByText('Aritmética')).toHaveLength(2);
  expect(within(tablas[1]).getByText('8:00am - 8:45am')).toBeVisible();
  expect(within(tablas[1]).queryByText('Aritmética')).not.toBeInTheDocument();
});

test('al cambiar el año pide ese año y dibuja su horario', async () => {
  const pedidos: (string | null)[] = [];
  server.use(
    http.get(`${API}/horarios/docente/horario`, ({ request }) => {
      const anioId = new URL(request.url).searchParams.get('anioId');
      pedidos.push(anioId);
      return anioId === ANIO_2025.id
        ? HttpResponse.json({
            anio: ANIO_2025,
            horarios: [
              {
                ...horarioPrimariaYSecundaria.horarios[1],
                titulo: 'Secundaria',
                clases: [
                  {
                    dia: 'MIE',
                    numeroBloque: 2,
                    curso: 'Física',
                    seccion: '3.° C',
                    ambiente: 'Lab-1',
                  },
                ],
              },
            ],
          })
        : HttpResponse.json({
            anio: ANIO_2026,
            horarios: [
              {
                ...horarioPrimariaYSecundaria.horarios[1],
                clases: [
                  {
                    dia: 'LUN',
                    numeroBloque: 1,
                    curso: 'Álgebra',
                    seccion: '1.° A',
                    ambiente: 'A-401',
                  },
                ],
              },
            ],
          });
    }),
  );
  await renderHorario();

  await screen.findByText('Álgebra');
  await screen.findByText('Año académico');
  expect(pedidos).toEqual([null]);

  await userEvent.click(botonDelSelect('Año académico'));
  await userEvent.click(
    await within(await screen.findByRole('listbox')).findByRole('option', {
      name: '2025',
    }),
  );

  expect(await screen.findByText('Física')).toBeVisible();
  expect(screen.getByText('3.° C')).toBeVisible();
  expect(screen.queryByText('Álgebra')).not.toBeInTheDocument();
  expect(pedidos).toEqual([null, ANIO_2025.id]);
  expect(botonDelSelect('Año académico')).toHaveTextContent('2025');
});

test('un año sin clases lo dice y deja el selector para cambiar de año', async () => {
  server.use(
    http.get(`${API}/horarios/docente/horario`, () =>
      HttpResponse.json({ anio: ANIO_2026, horarios: [] }),
    ),
  );
  await renderHorario();

  expect(
    await screen.findByText('Aún no tienes bloques asignados en este año.'),
  ).toBeVisible();
  expect(screen.queryByRole('table')).not.toBeInTheDocument();
  await screen.findByText('Año académico');
  expect(botonDelSelect('Año académico')).toBeEnabled();
});

test('un docente sin años con clases lo dice y no muestra el selector', async () => {
  server.use(
    http.get(`${API}/horarios/docente/anios`, () => HttpResponse.json([])),
    http.get(`${API}/horarios/docente/horario`, () =>
      HttpResponse.json(horarioSinClases),
    ),
  );
  await renderHorario();

  expect(
    await screen.findByText('Aún no tienes horarios asignados.'),
  ).toBeVisible();
  await waitFor(() =>
    expect(screen.queryByText('Año académico')).not.toBeInTheDocument(),
  );
  expect(screen.queryByRole('table')).not.toBeInTheDocument();
});

test('mientras carga muestra el indicador y no el vacío', async () => {
  await renderHorario();

  expect(
    screen.queryByText('Aún no tienes horarios asignados.'),
  ).not.toBeInTheDocument();
  await screen.findByRole('table', { name: /matriz horaria/i });
});

test('el tutor ve la misma pantalla que el docente (las dos rutas usan este componente)', async () => {
  await renderHorario(ROLES.TUTOR);

  expect(
    await screen.findByRole('table', { name: /matriz horaria/i }),
  ).toBeVisible();
  expect(screen.getByText('Geometría')).toBeVisible();
  expect(await screen.findByText('Año académico')).toBeVisible();
  expect(botonDelSelect('Año académico')).toHaveTextContent('2026');
});
