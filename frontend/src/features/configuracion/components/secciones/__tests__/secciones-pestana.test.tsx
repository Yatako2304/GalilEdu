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
import { EstadoAnioEscolar, GradoConSecciones } from '@/types/api';

import { SeccionesPestana } from '../secciones-pestana';

import { gradosDeEjemplo, seccion } from './datos';

// Figma > CI-09 · Secciones por grado y tutores (HU-75) > "Card - Secciones"

const API = env.API_URL;

const mostrar = async (
  estadoAnio: EstadoAnioEscolar = 'ACTIVO',
  grados: GradoConSecciones[] = gradosDeEjemplo,
) => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares/anio-2026/secciones`, () =>
      HttpResponse.json(grados),
    ),
  );
  return renderApp(
    <SeccionesPestana anioId="anio-2026" estadoAnio={estadoAnio} />,
    { user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }) },
  );
};

const celdas = (fila: HTMLElement) =>
  within(fila)
    .getAllByRole('cell')
    .map((c) => c.textContent);

test('muestra el título, el botón "Nueva sección" y las columnas de Figma', async () => {
  await mostrar();

  expect(
    await screen.findByRole('heading', { name: 'Secciones por grado' }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'Nueva sección' }),
  ).toBeInTheDocument();
  expect(
    within(await screen.findByRole('table'))
      .getAllByRole('columnheader')
      .map((c) => c.textContent),
  ).toEqual([
    'Sección',
    'Capacidad máx.',
    'Matriculados',
    'Tutor',
    'Cotutor',
    'Estado',
    '',
  ]);
});

test('cada grado es una fila de grupo con sus secciones activas y su botón "Agregar sección"', async () => {
  await mostrar();

  const tabla = await screen.findByRole('table');
  expect(within(tabla).getByText('1.° de primaria')).toBeInTheDocument();
  expect(within(tabla).getByText('3 secciones activas')).toBeInTheDocument();
  expect(within(tabla).getByText('2 secciones activas')).toBeInTheDocument();
  // En singular, y un grado sin secciones también aparece con su botón para agregar la primera.
  expect(within(tabla).getByText('1 sección activa')).toBeInTheDocument();
  expect(within(tabla).getByText('4.° de primaria')).toBeInTheDocument();
  expect(within(tabla).getByText('0 secciones activas')).toBeInTheDocument();
  expect(
    within(tabla).getAllByRole('button', { name: 'Agregar sección' }),
  ).toHaveLength(4);
});

// Figma > CI-09: "Nueva sección" es el botón principal (oscuro) y "Agregar sección" es Tertiary, Small, con el
// ícono a la izquierda. Se fija con la clase de fondo de cada variante para no confundirlo con el azul (Secondary).
test('"Nueva sección" es el botón principal y "Agregar sección" es terciario', async () => {
  await mostrar();

  expect(
    await screen.findByRole('button', { name: 'Nueva sección' }),
  ).toHaveClass('bg-surface-accent-primary');
  for (const boton of screen.getAllByRole('button', {
    name: 'Agregar sección',
  })) {
    expect(boton).toHaveClass('bg-surface-tertiary', 'h-8');
    expect(boton).not.toHaveClass('bg-surface-accent-secondary');
    // El ícono va antes del texto.
    expect(boton.firstElementChild?.tagName.toLowerCase()).toBe('svg');
  }
});

test('cada sección muestra su etiqueta, capacidad, matriculados, tutor, cotutor y estado', async () => {
  await mostrar();

  const filas = within(await screen.findByRole('table')).getAllByRole('row');
  // Encabezado, grupo 1.°, 1.° A, 1.° B, 1.° C, grupo 2.°, 2.° A, 2.° B…
  expect(celdas(filas[2])).toEqual([
    '1.° ASección A',
    '30',
    '29 / 30',
    'Rosa Huamán Quispe',
    'Silvia Herrera Luna',
    'Activa',
    'Acciones de 1.° A',
  ]);
  // Sin cotutor se muestra "—". La última celda es el menú ⋮ (su texto accesible).
  expect(celdas(filas[7])).toEqual([
    '2.° BSección B',
    '30',
    '23 / 30',
    'Carmen Rojas Ugarte',
    '—',
    'Activa',
    'Acciones de 2.° B',
  ]);
});

test('un nombre largo de tutor o cotutor se recorta en una línea y se ve completo al pasar el mouse', async () => {
  const tutor = 'María de los Ángeles Fernández de la Torre Villanueva';
  const cotutor = 'Juan Carlos Alberto Rodríguez de las Casas Benavides';
  await mostrar('ACTIVO', [
    {
      ...gradosDeEjemplo[0],
      secciones: [seccion('PRI-1', 'A', 10, tutor, cotutor)],
    },
  ]);

  for (const nombre of [tutor, cotutor]) {
    const celda = await screen.findByText(nombre);
    expect(celda).toHaveAttribute('title', nombre);
    expect(celda).toHaveClass('truncate');
  }
});

test('la barra de matriculados refleja los alumnos sobre la capacidad', async () => {
  await mostrar();

  const valor = (nombre: string) =>
    screen
      .getByRole('progressbar', { name: nombre })
      .getAttribute('aria-valuenow');
  await screen.findByRole('progressbar', { name: 'Matriculados de 1.° A' });
  expect(valor('Matriculados de 1.° A')).toBe('97');
  expect(valor('Matriculados de 1.° B')).toBe('80');
});

test('año ACTIVO: los botones están habilitados y cada sección tiene su menú ⋮ con "Asignar tutor"', async () => {
  await mostrar('ACTIVO');

  expect(
    await screen.findByRole('button', { name: 'Nueva sección' }),
  ).not.toHaveAttribute('aria-disabled');
  for (const boton of screen.getAllByRole('button', {
    name: 'Agregar sección',
  })) {
    expect(boton).not.toHaveAttribute('aria-disabled');
  }

  await userEvent.click(
    screen.getByRole('button', { name: 'Acciones de 1.° A' }),
  );
  expect(
    await screen.findByRole('menuitem', { name: /Asignar tutor/ }),
  ).toBeInTheDocument();
});

test('año CERRADO: los botones para agregar quedan deshabilitados y no hay menú ⋮', async () => {
  await mostrar('CERRADO');

  const nueva = await screen.findByRole('button', { name: 'Nueva sección' });
  expect(nueva).toHaveAttribute('aria-disabled', 'true');
  for (const boton of screen.getAllByRole('button', {
    name: 'Agregar sección',
  })) {
    expect(boton).toHaveAttribute('aria-disabled', 'true');
  }
  expect(
    screen.queryByRole('button', { name: /^Acciones de/ }),
  ).not.toBeInTheDocument();
});

test('año PLANIFICADO también admite cambios', async () => {
  await mostrar('PLANIFICADO');

  expect(
    await screen.findByRole('button', { name: 'Nueva sección' }),
  ).not.toHaveAttribute('aria-disabled');
  expect(
    screen.getByRole('button', { name: 'Acciones de 1.° A' }),
  ).toBeInTheDocument();
});

test('si no se pueden cargar las secciones avisa sin mostrar la tabla', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares/anio-2026/secciones`, () =>
      HttpResponse.json({ message: 'Error' }, { status: 500 }),
    ),
  );
  await renderApp(<SeccionesPestana anioId="anio-2026" estadoAnio="ACTIVO" />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

  expect(
    await screen.findByText('No pudimos cargar las secciones.'),
  ).toBeInTheDocument();
  expect(screen.queryByRole('table')).not.toBeInTheDocument();
});
