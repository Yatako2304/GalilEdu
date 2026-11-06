import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { horarioValeria2026 } from '@/testing/mocks/fixtures/horario-apoderado';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  renderApp,
  screen,
  userEvent,
  within,
} from '@/testing/test-utils';

import { HorarioApoderado } from '../horario-apoderado';

// HU-51: el apoderado consulta el horario de la sección de su hijo (Figma > Apoderado (real) > "Horario").

const API = env.API_URL;

const dosHijos = () =>
  server.use(
    http.get(`${API}/horarios/apoderado/estudiantes`, () =>
      HttpResponse.json([
        {
          estudianteId: 'est-valeria',
          nombreCompleto: 'Valeria Sánchez Ferrara',
          matriculas: [
            {
              anio: 2026,
              grado: '5.° de secundaria',
              nivel: 'SECUNDARIA',
              seccion: 'A',
            },
          ],
        },
        {
          estudianteId: 'est-tomas',
          nombreCompleto: 'Tomás Sánchez Ferrara',
          matriculas: [
            {
              anio: 2026,
              grado: '2.° de primaria',
              nivel: 'PRIMARIA',
              seccion: 'B',
            },
          ],
        },
      ]),
    ),
    http.get(
      `${API}/horarios/apoderado/estudiantes/:id/horario`,
      ({ params }) =>
        HttpResponse.json(
          params.id === 'est-valeria'
            ? horarioValeria2026
            : {
                ...horarioValeria2026,
                estudianteId: 'est-tomas',
                nombreCompleto: 'Tomás Sánchez Ferrara',
                grado: '2.° de primaria',
                seccion: 'B',
                tutor: null,
                disponible: false,
                jornada: null,
                clases: [],
              },
        ),
    ),
  );

const renderHorario = async () =>
  renderApp(<HorarioApoderado />, {
    user: await createUser({ roles: [ROLES.APODERADO] }),
  });

test('muestra el horario del primer hijo con su grado, sección y tutora', async () => {
  dosHijos();
  await renderHorario();

  const tabla = await screen.findByRole('table', { name: /matriz horaria/i });
  expect(screen.getByRole('heading', { name: 'Horario' })).toBeInTheDocument();
  expect(screen.getByLabelText('Estudiante')).toHaveValue('est-valeria');
  expect(screen.getAllByText('5.° de secundaria · Sección A')).toHaveLength(2);
  expect(screen.getByText('Lucía Ayala Prado')).toBeInTheDocument();
  expect(screen.getByText('987456123')).toBeInTheDocument();
  expect(screen.getByText('lucia.ayala@galiledu.edu.pe')).toBeInTheDocument();
  // 2 recreos x 5 días; los bloques con su hora.
  expect(within(tabla).getAllByText('Recreo')).toHaveLength(10);
  expect(within(tabla).getByText('Bloque 1')).toBeInTheDocument();
  expect(
    within(tabla).getAllByText('Lógica y estrategia').length,
  ).toBeGreaterThan(0);
  expect(
    screen.getByRole('button', { name: /descargar horario/i }),
  ).toBeEnabled();
});

test('al elegir otro hijo muestra su sección; sin horario armado avisa y no deja descargar', async () => {
  dosHijos();
  await renderHorario();
  await screen.findByRole('table', { name: /matriz horaria/i });

  await userEvent.selectOptions(
    screen.getByLabelText('Estudiante'),
    'est-tomas',
  );

  expect(
    await screen.findByText(
      'El horario de esta sección aún no está disponible.',
    ),
  ).toBeInTheDocument();
  expect(screen.getAllByText('2.° de primaria · Sección B')).toHaveLength(2);
  expect(
    screen.queryByRole('table', { name: /matriz horaria/i }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: /descargar horario/i }),
  ).toBeDisabled();
});

test('sin hijos matriculados lo dice', async () => {
  server.use(
    http.get(`${API}/horarios/apoderado/estudiantes`, () =>
      HttpResponse.json([]),
    ),
  );
  await renderHorario();

  expect(
    await screen.findByText('No tienes estudiantes matriculados.'),
  ).toBeInTheDocument();
});

test('"Descargar Horario" abre el horario para imprimirlo o guardarlo en PDF', async () => {
  dosHijos();
  const documento = { write: vi.fn(), close: vi.fn() };
  const ventana = { document: documento, focus: vi.fn(), print: vi.fn() };
  const abrir = vi
    .spyOn(window, 'open')
    .mockReturnValue(ventana as unknown as Window);
  await renderHorario();
  await screen.findByRole('table', { name: /matriz horaria/i });

  await userEvent.click(
    screen.getByRole('button', { name: /descargar horario/i }),
  );

  expect(abrir).toHaveBeenCalledWith('', '_blank');
  expect(documento.write).toHaveBeenCalledWith(
    expect.stringContaining('Valeria Sánchez Ferrara'),
  );
  expect(ventana.print).toHaveBeenCalled();
  abrir.mockRestore();
});
