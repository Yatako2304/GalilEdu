import { ROLES } from '@/config/roles';
import {
  createUser,
  renderApp,
  screen,
  userEvent,
  within,
} from '@/testing/test-utils';
import { AnioEscolarDetalle } from '@/types/api';

import { PeriodosPestana } from '../periodos-pestana';

import { anio2026, anio2027 } from './datos';

// Figma > CI-03 (año activo) y CI-06 (año planificado) > "Card - Periodos"

const mostrar = async (anio: AnioEscolarDetalle) =>
  renderApp(<PeriodosPestana anio={anio} />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

test('año ACTIVO: explica el régimen y el límite de notas, y no ofrece cambiar el régimen', async () => {
  await mostrar(anio2026);

  expect(
    screen.getByRole('heading', { name: 'Periodos académicos' }),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      'Régimen bimestral: el año se divide en 4 periodos. La fecha límite de notas es el último día en que los docentes pueden registrar calificaciones del periodo.',
    ),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'Cambiar régimen' }),
  ).not.toBeInTheDocument();
});

test('la tabla muestra cada periodo con sus fechas, duración y estado', async () => {
  await mostrar(anio2026);

  const tabla = screen.getByRole('table');
  expect(
    within(tabla)
      .getAllByRole('columnheader')
      .map((c) => c.textContent),
  ).toEqual([
    'N.°',
    'Periodo',
    'Inicio',
    'Fin',
    'Límite de notas',
    'Duración',
    'Estado',
    '',
  ]);

  const filas = within(tabla).getAllByRole('row').slice(1);
  expect(filas).toHaveLength(4);
  const celdas = (fila: HTMLElement) =>
    within(fila)
      .getAllByRole('cell')
      .map((c) => c.textContent);
  expect(celdas(filas[0]).slice(0, 7)).toEqual([
    '1',
    'I Bimestre',
    '02/03/2026',
    '08/05/2026',
    '15/05/2026',
    '10 sem.',
    'Cerrado',
  ]);
  expect(celdas(filas[2]).slice(0, 7)).toEqual([
    '3',
    'III Bimestre',
    '03/08/2026',
    '09/10/2026',
    '16/10/2026',
    '10 sem.',
    'En curso',
  ]);
  expect(celdas(filas[3]).slice(0, 7)).toEqual([
    '4',
    'IV Bimestre',
    '19/10/2026',
    '18/12/2026',
    '23/12/2026',
    '9 sem.',
    'Planificado',
  ]);
});

test('muestra la línea de tiempo del año', async () => {
  await mostrar(anio2026);

  const linea = screen.getByRole('list', {
    name: 'Línea de tiempo de los periodos',
  });
  expect(within(linea).getAllByRole('listitem').length).toBeGreaterThanOrEqual(
    4,
  );
  expect(screen.getByText('Inicio 02/03/2026')).toBeInTheDocument();
  expect(screen.getByText('Fin 18/12/2026')).toBeInTheDocument();
});

test('el menú ⋮ de un periodo ofrece "Editar periodo" y abre el modal', async () => {
  await mostrar(anio2026);

  await userEvent.click(
    screen.getByRole('button', { name: 'Acciones de III Bimestre' }),
  );
  const opciones = await screen.findAllByRole('menuitem');
  expect(opciones.map((o) => o.textContent?.trim())).toEqual([
    'Editar periodo',
  ]);
  await userEvent.click(opciones[0]);

  expect(
    await screen.findByRole('heading', { name: 'Editar III Bimestre' }),
  ).toBeInTheDocument();
});

test('año PLANIFICADO: ofrece "Cambiar régimen" y lo explica', async () => {
  await mostrar(anio2027);

  expect(
    screen.getByText(
      'Régimen trimestral: el año se divide en 3 periodos. Mientras el año esté planificado puedes cambiar el régimen.',
    ),
  ).toBeInTheDocument();
  await userEvent.click(
    screen.getByRole('button', { name: 'Cambiar régimen' }),
  );

  expect(
    await screen.findByRole('heading', { name: 'Cambiar régimen de periodos' }),
  ).toBeInTheDocument();
  expect(screen.getAllByText('Planificado')).toHaveLength(3);
});

test('año CERRADO: los periodos son de solo lectura, sin menú ⋮', async () => {
  await mostrar({ ...anio2026, estado: 'CERRADO' });

  expect(screen.getAllByRole('columnheader')).toHaveLength(7);
  expect(
    screen.queryByRole('button', { name: /Acciones de/ }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'Cambiar régimen' }),
  ).not.toBeInTheDocument();
});
