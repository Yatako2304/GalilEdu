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
import {
  AnioEscolarDetalle,
  EstadoAnioEscolar,
  SolicitudSeccion,
} from '@/types/api';

import { SeccionesPestana } from '../secciones-pestana';

import { gradosDeEjemplo, seccion } from './datos';

// Figma > "Modal - Nueva sección" (HU-75), abierto desde CI-09.

const API = env.API_URL;
const URL_SECCIONES = `${API}/configuracion/anios-escolares/anio-2026/secciones`;

const anio = (estado: EstadoAnioEscolar): AnioEscolarDetalle => ({
  id: 'anio-2026',
  anio: 2026,
  fechaInicio: '2026-03-02',
  fechaFin: '2026-12-18',
  regimen: 'BIMESTRAL',
  estado,
  cantidadSecciones: 6,
  cantidadEstructuras: 0,
  periodos: [],
});

/** Muestra la pestaña con un año del estado dado y devuelve lo que llegue al POST de secciones. */
const mostrar = async (
  estadoAnio: EstadoAnioEscolar = 'ACTIVO',
  respuestaPost: () => Response = () =>
    HttpResponse.json(seccion('PRI-2', 'C', 0, null), { status: 201 }),
) => {
  const enviados: SolicitudSeccion[] = [];
  server.use(
    http.get(URL_SECCIONES, () => HttpResponse.json(gradosDeEjemplo)),
    http.get(`${API}/configuracion/anios-escolares/anio-2026`, () =>
      HttpResponse.json(anio(estadoAnio)),
    ),
    http.post(URL_SECCIONES, async ({ request }) => {
      enviados.push((await request.json()) as SolicitudSeccion);
      return respuestaPost();
    }),
  );
  await renderApp(
    <SeccionesPestana anioId="anio-2026" estadoAnio={estadoAnio} />,
    { user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }) },
  );
  await screen.findByRole('table');
  return enviados;
};

const abrirDesdeBotonSuperior = async () => {
  await userEvent.click(screen.getByRole('button', { name: 'Nueva sección' }));
  return screen.findByRole('dialog');
};

const selectorDeGrado = (dialogo: HTMLElement) =>
  within(dialogo).getByRole('combobox', { expanded: false });

const elegirGrado = async (dialogo: HTMLElement, nombre: string) => {
  await userEvent.click(selectorDeGrado(dialogo));
  await userEvent.click(
    within(screen.getByRole('listbox')).getByRole('option', { name: nombre }),
  );
};

const campos = (dialogo: HTMLElement) => ({
  nombre: within(dialogo).getByLabelText(/Nombre de la sección/),
  capacidad: within(dialogo).getByLabelText(/Capacidad máxima/),
  crear: within(dialogo).getByRole('button', { name: 'Crear sección' }),
});

test('"Nueva sección" abre el modal con su título, el año y los campos vacíos', async () => {
  await mostrar();

  const dialogo = await abrirDesdeBotonSuperior();

  expect(
    within(dialogo).getByRole('heading', { name: 'Nueva sección' }),
  ).toBeInTheDocument();
  expect(await within(dialogo).findByText('Año escolar 2026')).toBeVisible();
  expect(within(dialogo).getByLabelText('Grado *')).toHaveValue('');
  expect(campos(dialogo).nombre).toHaveValue('');
  expect(campos(dialogo).capacidad).toHaveValue('');
  expect(selectorDeGrado(dialogo)).toHaveTextContent('Elige un grado');
  expect(within(dialogo).getByText('Ej.: A, B, C')).toBeInTheDocument();
  expect(
    within(dialogo).getByText('Entre 1 y 45 alumnos.'),
  ).toBeInTheDocument();
});

test('"Agregar sección" de un grado abre el modal con ese grado elegido', async () => {
  await mostrar();

  const grupo = screen.getByText('2.° de primaria').closest('tr');
  await userEvent.click(
    within(grupo as HTMLElement).getByRole('button', {
      name: 'Agregar sección',
    }),
  );

  const dialogo = await screen.findByRole('dialog');
  expect(within(dialogo).getByLabelText('Grado *')).toHaveValue('PRI-2');
});

test('el modal ofrece los grados del año', async () => {
  await mostrar();
  const dialogo = await abrirDesdeBotonSuperior();

  await userEvent.click(selectorDeGrado(dialogo));

  expect(
    within(screen.getByRole('listbox'))
      .getAllByRole('option')
      .map((o) => o.textContent),
  ).toEqual(gradosDeEjemplo.map((g) => g.nombre));
});

test('el nombre se pasa a mayúsculas mientras se escribe', async () => {
  await mostrar();
  const dialogo = await abrirDesdeBotonSuperior();

  await userEvent.type(campos(dialogo).nombre, 'c');

  expect(campos(dialogo).nombre).toHaveValue('C');
});

test('los errores salen en vivo, al escribir, con los mensajes de Figma', async () => {
  await mostrar();
  const dialogo = await abrirDesdeBotonSuperior();
  const { nombre, capacidad } = campos(dialogo);

  await userEvent.type(nombre, 'abcdef');
  expect(
    await within(dialogo).findByText(
      'El nombre de la sección admite hasta 5 caracteres.',
    ),
  ).toBeInTheDocument();

  await userEvent.clear(nombre);
  await userEvent.type(nombre, 'a-');
  expect(
    await within(dialogo).findByText(
      'El nombre de la sección solo admite letras y números.',
    ),
  ).toBeInTheDocument();

  await userEvent.type(capacidad, '0');
  expect(
    await within(dialogo).findByText('Entre 1 y 45 alumnos.', {
      selector: '[role="alert"], p, span',
    }),
  ).toBeInTheDocument();
  await userEvent.clear(capacidad);
  await userEvent.type(capacidad, '46');
  await waitFor(() =>
    expect(capacidad).toHaveAttribute('aria-invalid', 'true'),
  );
});

test('con los campos vacíos no envía y pide cada dato', async () => {
  const enviados = await mostrar();
  const dialogo = await abrirDesdeBotonSuperior();

  await userEvent.click(campos(dialogo).crear);

  expect(
    await within(dialogo).findByText('Elige un grado.'),
  ).toBeInTheDocument();
  expect(
    within(dialogo).getByText('Ingresa el nombre de la sección.'),
  ).toBeInTheDocument();
  expect(
    within(dialogo).getByText('Ingresa la capacidad máxima.'),
  ).toBeInTheDocument();
  expect(enviados).toEqual([]);
});

test('crea la sección con el cuerpo correcto, avisa y cierra el modal', async () => {
  const enviados = await mostrar();
  const dialogo = await abrirDesdeBotonSuperior();

  await elegirGrado(dialogo, '2.° de primaria');
  await userEvent.type(campos(dialogo).nombre, 'c');
  await userEvent.type(campos(dialogo).capacidad, '28');
  await userEvent.click(campos(dialogo).crear);

  expect(await screen.findByText('Sección creada')).toBeInTheDocument();
  expect(screen.getByText('2.° de primaria · Sección C')).toBeInTheDocument();
  expect(enviados).toEqual([
    { gradoCodigo: 'PRI-2', nombre: 'C', capacidadMaxima: 28 },
  ]);
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
});

test('desde "Agregar sección" envía el grado que ya traía', async () => {
  const enviados = await mostrar();

  const grupo = screen.getByText('4.° de primaria').closest('tr');
  await userEvent.click(
    within(grupo as HTMLElement).getByRole('button', {
      name: 'Agregar sección',
    }),
  );
  const dialogo = await screen.findByRole('dialog');
  await userEvent.type(campos(dialogo).nombre, 'a');
  await userEvent.type(campos(dialogo).capacidad, '30');
  await userEvent.click(campos(dialogo).crear);

  await waitFor(() =>
    expect(enviados).toEqual([
      { gradoCodigo: 'PRI-4', nombre: 'A', capacidadMaxima: 30 },
    ]),
  );
});

test('un error del backend (nombre repetido) se muestra una sola vez y el modal sigue abierto', async () => {
  const mensaje = 'Ya existe la sección B en este grado.';
  await mostrar('ACTIVO', () =>
    HttpResponse.json({ message: mensaje }, { status: 409 }),
  );
  const dialogo = await abrirDesdeBotonSuperior();

  await elegirGrado(dialogo, '2.° de primaria');
  await userEvent.type(campos(dialogo).nombre, 'b');
  await userEvent.type(campos(dialogo).capacidad, '30');
  await userEvent.click(campos(dialogo).crear);

  expect(await screen.findAllByText(mensaje)).toHaveLength(1);
  expect(screen.getByRole('dialog')).toBeInTheDocument();
  expect(campos(dialogo).nombre).toHaveValue('B');
  expect(screen.queryByText('Sección creada')).not.toBeInTheDocument();
});

test('Cancelar cierra el modal sin enviar nada', async () => {
  const enviados = await mostrar();
  const dialogo = await abrirDesdeBotonSuperior();

  await userEvent.click(
    within(dialogo).getByRole('button', { name: 'Cancelar' }),
  );

  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  expect(enviados).toEqual([]);
});

test('al volver a abrirlo no conserva lo escrito antes', async () => {
  await mostrar();
  let dialogo = await abrirDesdeBotonSuperior();
  await userEvent.type(campos(dialogo).nombre, 'x');
  await userEvent.click(
    within(dialogo).getByRole('button', { name: 'Cancelar' }),
  );
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );

  dialogo = await abrirDesdeBotonSuperior();

  expect(campos(dialogo).nombre).toHaveValue('');
});

test('año CERRADO: ningún botón abre el modal', async () => {
  await mostrar('CERRADO');

  await userEvent.click(screen.getByRole('button', { name: 'Nueva sección' }));
  for (const boton of screen.getAllByRole('button', {
    name: 'Agregar sección',
  })) {
    await userEvent.click(boton);
  }

  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

test.each<EstadoAnioEscolar>(['PLANIFICADO', 'ACTIVO'])(
  'año %s: "Nueva sección" abre el modal',
  async (estado) => {
    await mostrar(estado);

    expect(await abrirDesdeBotonSuperior()).toBeInTheDocument();
  },
);
