import { HttpResponse, http } from 'msw';

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
import { PeriodoMatriculaVigente } from '@/types/api';

import { CardPeriodoVigente } from '../card-periodo-vigente';

import {
  API,
  cerrado2026,
  detalle2027,
  programado2027,
  servirPeriodos,
  vigente2027,
} from './datos';

// Figma > Personal Administrativo > pantalla 13 > tarjeta "Vigente" (HU-23)

const { enlaces } = vi.hoisted(() => ({
  enlaces: {
    MATRICULAS_DEL_PERIODO_DISPONIBLE: false,
    TARIFARIO_DISPONIBLE: false,
    TEXTO_DISPONIBLE_PRONTO: 'Disponible pronto',
  },
}));
vi.mock('../enlaces', () => enlaces);

beforeEach(() => {
  enlaces.MATRICULAS_DEL_PERIODO_DISPONIBLE = false;
});

const mostrar = async () =>
  renderApp(<CardPeriodoVigente />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

const servirVigente = (vigente: PeriodoMatriculaVigente) =>
  server.use(
    http.get(`${API}/vigente`, () => HttpResponse.json(vigente)),
    http.get(`${API}/${vigente.id}`, () => HttpResponse.json(detalle2027())),
  );

test('muestra el periodo vigente con sus fechas, año y matrículas registradas', async () => {
  servirPeriodos([programado2027, cerrado2026]);
  servirVigente(vigente2027());
  await mostrar();

  expect(
    await screen.findByRole('heading', { name: 'Matrícula 2027' }),
  ).toBeInTheDocument();
  expect(screen.getByText('Vigente')).toHaveClass('rounded-full');
  expect(
    screen.getByText(
      'Abierto el 01/12/2026 10:00 a.m. · cierra el 15/02/2027 10:00 a.m.',
    ),
  ).toBeInTheDocument();
  expect(screen.getByText('Año escolar').nextSibling).toHaveTextContent('2027');
  expect(screen.getByText('Inicio de matrícula').nextSibling).toHaveTextContent(
    '01/12/2026',
  );
  expect(screen.getByText('Fin de matrícula').nextSibling).toHaveTextContent(
    '15/02/2027',
  );
  expect(
    screen.getByText('Matrículas registradas').nextSibling,
  ).toHaveTextContent('8 de 300 vacantes');
});

test('un periodo programado dice cuándo abre', async () => {
  servirPeriodos([programado2027]);
  servirVigente(vigente2027({ estado: 'PROGRAMADO' }));
  await mostrar();

  expect(
    await screen.findByText(
      'Abre el 01/12/2026 10:00 a.m. · cierra el 15/02/2027 10:00 a.m.',
    ),
  ).toBeInTheDocument();
});

test('sin periodo vigente no hay tarjeta y no se pide /vigente (respondería 404 y saldría como error)', async () => {
  let consultasVigente = 0;
  servirPeriodos([cerrado2026]);
  server.use(
    http.get(`${API}/vigente`, () => {
      consultasVigente++;
      return HttpResponse.json(
        { message: 'No hay un periodo de matrícula vigente.' },
        { status: 404 },
      );
    }),
  );
  await mostrar();

  await new Promise((resolver) => setTimeout(resolver, 100));

  expect(consultasVigente).toBe(0);
  expect(screen.queryByRole('heading')).not.toBeInTheDocument();
  expect(
    screen.queryByText('No hay un periodo de matrícula vigente.'),
  ).not.toBeInTheDocument();
});

describe('botón "Ver matrículas"', () => {
  test('mientras la pantalla de Matrícula no exista queda deshabilitado, con el aviso "Disponible pronto"', async () => {
    servirPeriodos([programado2027]);
    servirVigente(vigente2027());
    await mostrar();

    const boton = await screen.findByRole('button', {
      name: 'Ver matrículas',
    });

    expect(boton).toHaveAttribute('aria-disabled', 'true');
    expect(boton).toHaveAccessibleDescription('Disponible pronto');
    expect(screen.getByRole('tooltip')).toHaveTextContent('Disponible pronto');
    expect(
      screen.queryByRole('link', { name: 'Ver matrículas' }),
    ).not.toBeInTheDocument();
  });

  test('con la pantalla disponible es un enlace a las matrículas del periodo', async () => {
    enlaces.MATRICULAS_DEL_PERIODO_DISPONIBLE = true;
    servirPeriodos([programado2027]);
    servirVigente(vigente2027());
    await mostrar();

    expect(
      await screen.findByRole('link', { name: 'Ver matrículas' }),
    ).toHaveAttribute(
      'href',
      '/app/administrativo/matricula/periodos/periodo-2027/matriculas',
    );
  });
});

test('"Ver configuración" abre el modal con la configuración del periodo', async () => {
  servirPeriodos([programado2027]);
  servirVigente(vigente2027());
  await mostrar();

  await userEvent.click(
    await screen.findByRole('button', { name: 'Ver configuración' }),
  );

  const modal = await screen.findByRole('dialog');
  expect(
    within(modal).getByRole('heading', {
      name: 'Configuración · Matrícula 2027',
    }),
  ).toBeInTheDocument();
  expect(
    await within(modal).findByText('15/09/2026 – 31/10/2026'),
  ).toBeInTheDocument();

  await userEvent.click(within(modal).getByRole('button', { name: 'Cerrar' }));
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
});
