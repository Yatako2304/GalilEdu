import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { SECCION_5A } from '@/testing/mocks/fixtures/horarios-coordinador';
import {
  AMBIENTE_LAB02,
  RESPONSABLE_MARIA,
  reservasSembradas,
} from '@/testing/mocks/fixtures/reservas-ambiente';
import {
  reiniciarReservas,
  reservasEnMemoria,
} from '@/testing/mocks/handlers/reservas-ambiente';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  renderApp,
  screen,
  userEvent,
  waitFor,
  within,
} from '@/testing/test-utils';

import { ReservasEspacios } from '../reservas-espacios';

// Figma > Coordinador > «Reserva de espacios especiales» (308:1874) y «Nueva reserva» (308:6867, 308:7130).
// «Hoy» es el miércoles 18/11/2026, 10:00: lo mismo para el reloj de la pantalla (solo `Date`; los temporizadores de
// MSW y de testing-library siguen reales) y para los simuladores de reservas.

const API = env.API_URL;
const RUTA = `${API}/horarios/reservas`;

beforeEach(() => {
  vi.useFakeTimers({ toFake: ['Date'] });
  vi.setSystemTime(new Date('2026-11-18T10:00:00'));
});
afterEach(() => {
  vi.useRealTimers();
  reiniciarReservas();
});

const mostrar = async () =>
  renderApp(<ReservasEspacios />, {
    user: await createUser({ roles: [ROLES.COORDINADOR] }),
  });

/** La pantalla con la primera página ya cargada. */
const abrirPantalla = async () => {
  await mostrar();
  await screen.findByText('Práctica de laboratorio de química');
};

// ---------- Ayudas de Select y de fila ----------

const campo = (etiqueta: string, raiz: HTMLElement = document.body) => {
  const texto = within(raiz)
    .getAllByText(etiqueta)
    .find((elemento) => elemento.closest('label'));
  return texto!.closest('label') as HTMLElement;
};

const botonDelSelect = (etiqueta: string, raiz: HTMLElement = document.body) =>
  within(campo(etiqueta, raiz))
    .getAllByRole('combobox')
    .find((e) => e.tagName === 'BUTTON') as HTMLElement;

const abrirLista = async (etiqueta: string) => {
  await waitFor(() => expect(botonDelSelect(etiqueta)).toBeEnabled());
  await userEvent.click(botonDelSelect(etiqueta));
  return screen.findByRole('listbox');
};

const elegir = async (etiqueta: string, opcion: string) => {
  const lista = await abrirLista(etiqueta);
  // Las opciones de los selectores llegan con sus peticiones: se espera a la que se quiere elegir.
  await userEvent.click(
    await within(lista).findByRole('option', { name: opcion }),
  );
};

/** Las opciones de la lista abierta de un Select, con si están deshabilitadas. */
const opcionesDe = async (etiqueta: string) => {
  const lista = await abrirLista(etiqueta);
  await within(lista).findAllByRole('option');
  const opciones = within(lista)
    .getAllByRole('option')
    .map((o) => ({
      texto: o.textContent,
      deshabilitada: o.getAttribute('aria-disabled') === 'true',
    }));
  // «Escape» cerraría también el modal: la lista se cierra con el mismo botón que la abrió.
  await userEvent.click(botonDelSelect(etiqueta));
  return opciones;
};

const fila = (texto: string) =>
  screen.getByText(texto).closest('tr') as HTMLElement;

const dialogo = () => screen.findByRole('dialog');

const abrirNuevaReserva = async () => {
  await userEvent.click(screen.getByRole('button', { name: /Nueva reserva/ }));
  return dialogo();
};

const elegirFecha = async (fecha: string) => {
  await userEvent.click(screen.getByRole('button', { name: /Fecha/ }));
  await userEvent.click(await screen.findByRole('button', { name: fecha }));
};

/** Llena el modal con una reserva válida de 5.° B (la jornada del simulador empieza a las 8:00). */
const llenarReserva = async (
  cambios: Partial<{
    ambiente: string;
    actividad: string;
    fecha: string;
    hora: string;
    duracion: string;
  }> = {},
) => {
  const datos = {
    ambiente: 'Laboratorio de cómputo',
    actividad: 'Práctica de redes',
    fecha: '23/11/2026',
    hora: '08:00 am',
    duracion: '90 min',
    ...cambios,
  };
  await elegir('Ambiente', datos.ambiente);
  await elegir('Responsable', 'María Fernández Ríos');
  await userEvent.type(screen.getByLabelText('Actividad'), datos.actividad);
  await elegir('Grado', '5.° de secundaria');
  await elegir('Sección', 'B');
  await elegirFecha(datos.fecha);
  await elegir('Hora', datos.hora);
  await elegir('Duración', datos.duracion);
};

const guardar = (nombre: string) =>
  userEvent.click(screen.getByRole('button', { name: nombre }));

/** «Mostrando 11-20 de 23»: el texto está partido en varios elementos. */
const mostrando = (texto: string) =>
  waitFor(() =>
    expect(screen.getByText(/Mostrando/)).toHaveTextContent(
      `Mostrando ${texto}`,
    ),
  );

const filtrar = (nombre: string) =>
  userEvent.click(screen.getByRole('button', { name: nombre }));

// ---------- Listado ----------

test('la pantalla lleva el título, las migas, «Nueva reserva» y la tarjeta «Historial de reservas»', async () => {
  await abrirPantalla();

  expect(
    screen.getByRole('heading', { name: 'Reserva de espacios especiales' }),
  ).toBeInTheDocument();
  const migas = screen.getByRole('navigation', { name: 'Ruta' });
  expect(within(migas).getByText('Horarios')).toBeInTheDocument();
  expect(
    within(migas).getByText('Reserva de espacios especiales'),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: /Nueva reserva/ }),
  ).toBeInTheDocument();
  expect(screen.getByText('Historial de reservas')).toBeInTheDocument();
});

test('lista la semana de hoy: espacio con código y nombre, actividad con sección y responsable, estado, fecha y hora de 12 horas', async () => {
  await abrirPantalla();

  const primera = fila('Práctica de laboratorio de química');
  expect(within(primera).getByText('LAB-01')).toBeInTheDocument();
  expect(
    within(primera).getByText('Laboratorio de ciencias'),
  ).toBeInTheDocument();
  expect(
    within(primera).getByText('1.° A · María Fernández Ríos'),
  ).toBeInTheDocument();
  expect(within(primera).getByText('Pendiente')).toBeInTheDocument();
  expect(within(primera).getByText('19/11/2026')).toBeInTheDocument();
  expect(within(primera).getByText('08:15 am')).toBeInTheDocument();
  expect(
    within(fila('Taller de programación')).getByText('LAB-02'),
  ).toBeInTheDocument();
  expect(screen.getAllByRole('row')).toHaveLength(3); // encabezado y dos reservas
  expect(screen.getByText(/Mostrando/)).toHaveTextContent('Mostrando 1-2 de 2');
});

test('las columnas son Espacio, Actividad, Estado, Fecha, Hora y Acciones', async () => {
  await abrirPantalla();

  const encabezados = screen
    .getAllByRole('columnheader')
    .map((c) => c.textContent);
  expect(encabezados).toEqual([
    'Espacio',
    'Actividad',
    'Estado',
    'Fecha',
    'Hora',
    'Acciones',
  ]);
});

test('el filtro «Esta semana» viene elegido y es el único marcado', async () => {
  await abrirPantalla();

  expect(screen.getByRole('button', { name: 'Esta semana' })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  expect(screen.getByRole('button', { name: 'Este mes' })).toHaveAttribute(
    'aria-pressed',
    'false',
  );
  expect(screen.getByRole('button', { name: 'Este año' })).toHaveAttribute(
    'aria-pressed',
    'false',
  );
});

test('cambiar de filtro pide ese rango: este mes suma las finalizadas y este año trae las cinco activas', async () => {
  await abrirPantalla();

  await filtrar('Este mes');
  await screen.findByText('Experimento de densidad');
  expect(screen.getAllByRole('row')).toHaveLength(5);
  expect(screen.getAllByText('Finalizado')).toHaveLength(2);
  expect(screen.getByRole('button', { name: 'Este mes' })).toHaveAttribute(
    'aria-pressed',
    'true',
  );

  await filtrar('Este año');
  await screen.findByText('Sesión de lectura y búsqueda de fuentes');
  expect(screen.getAllByRole('row')).toHaveLength(6);
  // La cancelada («Práctica de microscopía») no sale en ningún filtro.
  expect(screen.queryByText('Práctica de microscopía')).not.toBeInTheDocument();
});

test('la paginación pide la página elegida y al cambiar de filtro se vuelve a la primera', async () => {
  const pedidas: string[] = [];
  const reservas = reservasSembradas().slice(0, 3);
  server.use(
    http.get(RUTA, ({ request }) => {
      const url = new URL(request.url);
      const page = Number(url.searchParams.get('page'));
      pedidas.push(`${url.searchParams.get('filtro')}:${page}`);
      return HttpResponse.json({
        data: [reservas[page - 1]].map(({ estadoGuardado, ...r }) => ({
          ...r,
          estado: 'PENDIENTE',
          editable: estadoGuardado === 'ACTIVA',
        })),
        meta: { page, size: 10, total: 23, totalPages: 3 },
      });
    }),
  );
  await mostrar();
  await mostrando('1-10 de 23');

  await userEvent.click(screen.getByRole('button', { name: '2' }));
  await mostrando('11-20 de 23');
  await filtrar('Este mes');
  await mostrando('1-10 de 23');

  expect(pedidas).toEqual(['SEMANA:1', 'SEMANA:2', 'MES:1']);
});

test('sin reservas en el rango muestra el estado vacío y no la paginación', async () => {
  server.use(
    http.get(RUTA, () =>
      HttpResponse.json({
        data: [],
        meta: { page: 1, size: 10, total: 0, totalPages: 1 },
      }),
    ),
  );
  await mostrar();

  expect(
    await screen.findByText('No hay reservas en este periodo.'),
  ).toBeInTheDocument();
  expect(screen.queryByText(/Mostrando/)).not.toBeInTheDocument();
});

test('una reserva finalizada trae «Finalizado» con «Editar» y «Eliminar reserva» deshabilitados; las pendientes, habilitados', async () => {
  await abrirPantalla();
  await filtrar('Este mes');
  await screen.findByText('Experimento de densidad');

  const finalizada = fila('Experimento de densidad');
  expect(within(finalizada).getByText('Finalizado')).toBeInTheDocument();
  expect(
    within(finalizada).getByRole('button', { name: 'Editar' }),
  ).toBeDisabled();
  expect(
    within(finalizada).getByRole('button', { name: 'Eliminar reserva' }),
  ).toBeDisabled();
  const pendiente = fila('Taller de programación');
  expect(
    within(pendiente).getByRole('button', { name: 'Editar' }),
  ).toBeEnabled();
  expect(
    within(pendiente).getByRole('button', { name: 'Eliminar reserva' }),
  ).toBeEnabled();
});

// ---------- Crear ----------

test('«Nueva reserva» abre el modal con sus campos, sin campo de año; hora y duración esperan a la sección', async () => {
  await abrirPantalla();

  const modal = await abrirNuevaReserva();

  expect(
    within(modal).getByText('Reservar espacios especiales'),
  ).toBeInTheDocument();
  expect(within(modal).getByText('Nueva reserva')).toBeInTheDocument();
  for (const etiqueta of [
    'Ambiente',
    'Responsable',
    'Grado',
    'Sección',
    'Hora',
    'Duración',
  ]) {
    expect(campo(etiqueta, modal)).toBeInTheDocument();
  }
  expect(within(modal).getByLabelText('Actividad')).toBeInTheDocument();
  expect(within(modal).queryByText('Año académico')).not.toBeInTheDocument();
  expect(
    within(modal).getByRole('button', { name: 'Cancelar' }),
  ).toBeInTheDocument();
  expect(
    within(modal).getByRole('button', { name: 'Reservar' }),
  ).toBeInTheDocument();
  expect(botonDelSelect('Sección')).toBeDisabled();
  expect(botonDelSelect('Hora')).toBeDisabled();
  expect(botonDelSelect('Duración')).toBeDisabled();
});

test('el modal ofrece solo espacios especiales por nombre, a María y los grados con secciones activas', async () => {
  await abrirPantalla();
  await abrirNuevaReserva();

  expect((await opcionesDe('Ambiente')).map((o) => o.texto)).toEqual([
    'Biblioteca',
    'Laboratorio de ciencias',
    'Laboratorio de cómputo',
  ]);
  expect((await opcionesDe('Responsable')).map((o) => o.texto)).toEqual([
    'María Fernández Ríos',
  ]);
  // Primaria 1.° no tiene secciones: no se ofrece.
  expect((await opcionesDe('Grado')).map((o) => o.texto)).toEqual([
    '4.° de secundaria',
    '5.° de secundaria',
  ]);
});

test('las secciones inactivas no se ofrecen', async () => {
  server.use(
    http.get(`${API}/horarios/anios-escolares/:anioId/secciones`, () =>
      HttpResponse.json([
        {
          codigo: 'SEC-5',
          nombre: '5.° de secundaria',
          nivel: 'SECUNDARIA',
          numero: 5,
          cantidadSeccionesActivas: 1,
          secciones: [
            { id: SECCION_5A, nombre: 'A', estado: 'ACTIVA' },
            { id: 'sec-5z', nombre: 'Z', estado: 'INACTIVA' },
          ],
        },
      ]),
    ),
  );
  await abrirPantalla();
  await abrirNuevaReserva();
  await elegir('Grado', '5.° de secundaria');

  expect((await opcionesDe('Sección')).map((o) => o.texto)).toEqual(['A']);
});

test('la hora ofrece los inicios de bloque de la jornada y la duración 1×, 2× y 3× el bloque, sin las que pasan del fin', async () => {
  await abrirPantalla();
  await abrirNuevaReserva();
  await elegir('Grado', '5.° de secundaria');
  await elegir('Sección', 'B');

  expect((await opcionesDe('Hora')).map((o) => o.texto)).toEqual([
    '08:00 am',
    '08:45 am',
    '09:30 am',
    '10:30 am',
  ]);
  expect(botonDelSelect('Duración')).toBeDisabled();

  // La jornada termina a las 11:15: desde las 10:30 solo cabe un bloque.
  await elegir('Hora', '10:30 am');
  expect(await opcionesDe('Duración')).toEqual([
    { texto: '45 min', deshabilitada: false },
    { texto: '90 min', deshabilitada: true },
    { texto: '135 min', deshabilitada: true },
  ]);
});

test('cambiar la hora a una desde la que la duración elegida no cabe, la limpia', async () => {
  await abrirPantalla();
  await abrirNuevaReserva();
  await elegir('Grado', '5.° de secundaria');
  await elegir('Sección', 'B');
  await elegir('Hora', '08:00 am');
  await elegir('Duración', '135 min');

  await elegir('Hora', '10:30 am');

  await waitFor(() =>
    expect(botonDelSelect('Duración')).not.toHaveTextContent('135 min'),
  );
});

test('cambiar el grado vacía la sección, la hora y la duración', async () => {
  await abrirPantalla();
  await abrirNuevaReserva();
  await elegir('Grado', '5.° de secundaria');
  await elegir('Sección', 'B');
  await elegir('Hora', '08:00 am');
  await elegir('Duración', '90 min');

  await elegir('Grado', '4.° de secundaria');

  await waitFor(() =>
    expect(botonDelSelect('Sección')).not.toHaveTextContent('B'),
  );
  expect(botonDelSelect('Hora')).toBeDisabled();
  expect(botonDelSelect('Duración')).toBeDisabled();
});

test('crear con éxito: cierra el modal, avisa «Reserva registrada.», guarda la hora de fin calculada y se ve en el listado', async () => {
  await abrirPantalla();
  await abrirNuevaReserva();
  await llenarReserva();

  await guardar('Reservar');

  expect(await screen.findByText('Reserva registrada.')).toBeInTheDocument();
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  const guardada = reservasEnMemoria().at(-1)!;
  expect(guardada).toMatchObject({
    ambiente: { id: AMBIENTE_LAB02 },
    responsable: { id: RESPONSABLE_MARIA.id },
    actividad: 'Práctica de redes',
    seccion: { etiqueta: '5.° B' },
    fecha: '2026-11-23',
    horaInicio: '08:00',
    horaFin: '09:30',
  });
  await filtrar('Este mes');
  expect(await screen.findByText('Práctica de redes')).toBeInTheDocument();
});

test('«Cancelar» cierra el modal sin guardar', async () => {
  await abrirPantalla();
  const modal = await abrirNuevaReserva();

  await userEvent.click(
    within(modal).getByRole('button', { name: 'Cancelar' }),
  );

  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  expect(reservasEnMemoria()).toHaveLength(6);
});

test('un formulario vacío muestra un mensaje en español bajo cada campo y no llama al backend', async () => {
  let llamadas = 0;
  server.use(
    http.post(RUTA, () => {
      llamadas += 1;
      return HttpResponse.json({}, { status: 201 });
    }),
  );
  await abrirPantalla();
  await abrirNuevaReserva();

  await guardar('Reservar');

  for (const mensaje of [
    'Elige el ambiente.',
    'Elige el responsable.',
    'Escribe la actividad.',
    'Elige el grado.',
    'Elige la sección.',
    'Elige la fecha.',
    'Elige la hora.',
    'Elige la duración.',
  ]) {
    expect(await screen.findByText(mensaje)).toBeInTheDocument();
  }
  expect(llamadas).toBe(0);
});

test('una actividad de más de 120 caracteres se rechaza en el formulario', async () => {
  await abrirPantalla();
  await abrirNuevaReserva();
  await llenarReserva({ actividad: 'x'.repeat(121) });

  await guardar('Reservar');

  expect(
    await screen.findByText('La actividad admite hasta 120 caracteres.'),
  ).toBeInTheDocument();
  expect(reservasEnMemoria()).toHaveLength(6);
});

test('un día que no es lectivo se rechaza en el cliente con el nombre del día', async () => {
  await abrirPantalla();
  await abrirNuevaReserva();
  await llenarReserva({ fecha: '21/11/2026' });

  await guardar('Reservar');

  expect(
    await screen.findByText(
      'El sábado no es un día de clases de esta sección.',
    ),
  ).toBeInTheDocument();
  expect(reservasEnMemoria()).toHaveLength(6);
});

test('el calendario no ofrece fechas anteriores a hoy', async () => {
  await abrirPantalla();
  await abrirNuevaReserva();

  await userEvent.click(screen.getByRole('button', { name: /Fecha/ }));

  expect(
    await screen.findByRole('button', { name: '17/11/2026' }),
  ).toBeDisabled();
  expect(screen.getByRole('button', { name: '19/11/2026' })).toBeEnabled();
});

// ---------- Errores del backend ----------

test('un 409 por cruce se muestra en el modal con el título aparte y el modal sigue abierto', async () => {
  await abrirPantalla();
  await abrirNuevaReserva();
  // LAB-01 ya está reservado el jueves 19/11 de 8:15 a 9:45.
  await llenarReserva({
    ambiente: 'Laboratorio de ciencias',
    fecha: '19/11/2026',
    hora: '08:00 am',
    duracion: '90 min',
  });

  await guardar('Reservar');

  const aviso = await screen.findByRole('alert');
  expect(within(aviso).getByText('Ambiente no disponible')).toBeInTheDocument();
  expect(
    within(aviso).getByText(
      /Laboratorio de ciencias ya está reservado por 1\.° A para Práctica de laboratorio de química el jueves 19\/11\/2026 de 8:15 a 9:45\./,
    ),
  ).toBeInTheDocument();
  expect(screen.getByRole('dialog')).toBeInTheDocument();
  expect(reservasEnMemoria()).toHaveLength(6);
});

test('el 409 de responsable lleva su título', async () => {
  await abrirPantalla();
  await abrirNuevaReserva();
  // María ya es responsable de la reserva de LAB-01 del 19/11 de 8:15 a 9:45; otro espacio, misma hora.
  await llenarReserva({
    fecha: '19/11/2026',
    hora: '08:00 am',
    duracion: '90 min',
  });

  await guardar('Reservar');

  const aviso = await screen.findByRole('alert');
  expect(
    within(aviso).getByText('Responsable no disponible'),
  ).toBeInTheDocument();
});

test('un 409 sin título (año cerrado) va como texto del aviso, sin título', async () => {
  server.use(
    http.post(RUTA, () =>
      HttpResponse.json(
        { message: 'No se puede reservar un espacio en un año cerrado.' },
        { status: 409 },
      ),
    ),
  );
  await abrirPantalla();
  await abrirNuevaReserva();
  await llenarReserva();

  await guardar('Reservar');

  const aviso = await screen.findByRole('alert');
  expect(
    within(aviso).getByText(
      'No se puede reservar un espacio en un año cerrado.',
    ),
  ).toBeInTheDocument();
  expect(within(aviso).queryByText(/no disponible/)).not.toBeInTheDocument();
  expect(
    within(aviso).queryByRole('button', { name: 'Recargar' }),
  ).not.toBeInTheDocument();
});

test('un 400 de regla (por ejemplo, la sección no tiene ese día) va en el aviso del modal', async () => {
  server.use(
    http.post(RUTA, () =>
      HttpResponse.json(
        {
          message:
            'La fecha no está dentro de ningún periodo académico del año.',
        },
        { status: 400 },
      ),
    ),
  );
  await abrirPantalla();
  await abrirNuevaReserva();
  await llenarReserva();

  await guardar('Reservar');

  expect(
    within(await screen.findByRole('alert')).getByText(
      'La fecha no está dentro de ningún periodo académico del año.',
    ),
  ).toBeInTheDocument();
});

test('un 400 de @Valid con errores por campo pone cada mensaje bajo su campo (la hora de fin va bajo «Duración»)', async () => {
  server.use(
    http.post(RUTA, () =>
      HttpResponse.json(
        {
          message: 'Escribe la actividad.',
          errores: {
            actividad: 'Escribe la actividad.',
            horaFin: 'Elige la hora de fin.',
          },
        },
        { status: 400 },
      ),
    ),
  );
  await abrirPantalla();
  await abrirNuevaReserva();
  await llenarReserva();

  await guardar('Reservar');

  expect(await screen.findByText('Escribe la actividad.')).toBeInTheDocument();
  expect(screen.getByText('Elige la hora de fin.')).toBeInTheDocument();
});

test('los errores de crear no salen como notificación global: se ven solo en el modal', async () => {
  await abrirPantalla();
  await abrirNuevaReserva();
  await llenarReserva({
    ambiente: 'Laboratorio de ciencias',
    fecha: '19/11/2026',
  });

  await guardar('Reservar');

  await screen.findByRole('alert');
  expect(screen.queryByText('Error')).not.toBeInTheDocument();
});

// ---------- Editar ----------

/** Abre «Editar» de la reserva de 5.° A (9/12, 8:15 a 9:45, Biblioteca): la jornada del simulador es la de 5.° A. */
const abrirEdicion = async () => {
  await filtrar('Este año');
  await screen.findByText('Sesión de lectura y búsqueda de fuentes');
  await userEvent.click(
    within(fila('Sesión de lectura y búsqueda de fuentes')).getByRole(
      'button',
      {
        name: 'Editar',
      },
    ),
  );
  const modal = await dialogo();
  // Espera a que lleguen la jornada (la hora guardada se ve) y las opciones de cada selector (cada una es una petición).
  const esperar = (etiqueta: string, texto: string) =>
    waitFor(() =>
      expect(botonDelSelect(etiqueta, modal)).toHaveTextContent(texto),
    );
  await esperar('Hora', '08:15 am');
  await esperar('Ambiente', 'Biblioteca');
  await esperar('Responsable', 'María Fernández Ríos');
  await esperar('Grado', '5.° de secundaria');
  await esperar('Sección', 'A');
  return modal;
};

test('«Editar» abre el mismo modal como «Editar reserva», con los datos de la reserva y «Guardar cambios»', async () => {
  await abrirPantalla();

  const modal = await abrirEdicion();

  expect(within(modal).getByText('Editar reserva')).toBeInTheDocument();
  expect(
    within(modal).getByRole('button', { name: 'Guardar cambios' }),
  ).toBeInTheDocument();
  expect(botonDelSelect('Ambiente', modal)).toHaveTextContent('Biblioteca');
  expect(botonDelSelect('Responsable', modal)).toHaveTextContent(
    'María Fernández Ríos',
  );
  expect(within(modal).getByLabelText('Actividad')).toHaveValue(
    'Sesión de lectura y búsqueda de fuentes',
  );
  expect(botonDelSelect('Grado', modal)).toHaveTextContent('5.° de secundaria');
  expect(botonDelSelect('Sección', modal)).toHaveTextContent('A');
  expect(
    within(modal).getByRole('button', { name: /Fecha/ }),
  ).toHaveTextContent('09/12/2026');
  expect(botonDelSelect('Duración', modal)).toHaveTextContent('90 min');
});

test('al editar, la hora guardada (8:15, que no es inicio de bloque) se conserva como opción', async () => {
  await abrirPantalla();
  await abrirEdicion();

  expect((await opcionesDe('Hora')).map((o) => o.texto)).toEqual([
    '08:00 am',
    '08:15 am',
    '08:45 am',
    '09:30 am',
    '10:30 am',
  ]);
});

test('editar con éxito manda la versión, avisa «Reserva actualizada.» y el listado muestra el cambio', async () => {
  await abrirPantalla();
  const modal = await abrirEdicion();
  const actividad = within(modal).getByLabelText('Actividad');
  await userEvent.clear(actividad);
  await userEvent.type(actividad, 'Sesión de lectura corregida');

  await guardar('Guardar cambios');

  expect(await screen.findByText('Reserva actualizada.')).toBeInTheDocument();
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  expect(
    await screen.findByText('Sesión de lectura corregida'),
  ).toBeInTheDocument();
  expect(reservasEnMemoria().find((r) => r.id === 'reserva-3')).toMatchObject({
    actividad: 'Sesión de lectura corregida',
    version: 1,
    horaInicio: '08:15',
    horaFin: '09:45',
  });
});

test('un 409 de versión avisa y «Recargar» vuelve a leer la reserva y permite guardar con la versión nueva', async () => {
  await abrirPantalla();
  const modal = await abrirEdicion();
  // Otra persona la cambió mientras tanto: la versión de la pantalla (0) quedó vieja.
  const enMemoria = reservasEnMemoria().find((r) => r.id === 'reserva-3')!;
  enMemoria.version = 5;
  enMemoria.actividad = 'Cambiada por otra persona';
  await userEvent.type(within(modal).getByLabelText('Actividad'), ' (mía)');

  await guardar('Guardar cambios');

  const aviso = await screen.findByRole('alert');
  expect(
    within(aviso).getByText(
      'La reserva cambió mientras la editabas. Vuelve a cargarla.',
    ),
  ).toBeInTheDocument();
  await userEvent.click(
    within(aviso).getByRole('button', { name: 'Recargar' }),
  );
  await waitFor(() =>
    expect(within(modal).getByLabelText('Actividad')).toHaveValue(
      'Cambiada por otra persona',
    ),
  );
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();

  await guardar('Guardar cambios');

  expect(await screen.findByText('Reserva actualizada.')).toBeInTheDocument();
  expect(reservasEnMemoria().find((r) => r.id === 'reserva-3')?.version).toBe(
    6,
  );
});

// ---------- Eliminar ----------

test('«Eliminar reserva» pide confirmación con el espacio, la fecha y la hora; «Cancelar» no llama al backend', async () => {
  let llamadas = 0;
  server.use(
    http.delete(`${RUTA}/:id`, () => {
      llamadas += 1;
      return new HttpResponse(null, { status: 204 });
    }),
  );
  await abrirPantalla();

  await userEvent.click(
    within(fila('Práctica de laboratorio de química')).getByRole('button', {
      name: 'Eliminar reserva',
    }),
  );

  const confirmacion = await dialogo();
  expect(
    within(confirmacion).getByText('¿Eliminar la reserva?'),
  ).toBeInTheDocument();
  expect(
    within(confirmacion).getByText(/Laboratorio de ciencias \(LAB-01\)/),
  ).toBeInTheDocument();
  expect(within(confirmacion).getByText(/19\/11\/2026/)).toBeInTheDocument();
  expect(within(confirmacion).getByText(/08:15 am/)).toBeInTheDocument();
  await userEvent.click(
    within(confirmacion).getByRole('button', { name: 'Cancelar' }),
  );
  await waitFor(() =>
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
  );
  expect(llamadas).toBe(0);
  expect(
    screen.getByText('Práctica de laboratorio de química'),
  ).toBeInTheDocument();
});

test('confirmar elimina la reserva: avisa «Reserva eliminada.» y desaparece del listado', async () => {
  await abrirPantalla();
  await userEvent.click(
    within(fila('Práctica de laboratorio de química')).getByRole('button', {
      name: 'Eliminar reserva',
    }),
  );

  await userEvent.click(
    within(await dialogo()).getByRole('button', { name: 'Eliminar' }),
  );

  expect(await screen.findByText('Reserva eliminada.')).toBeInTheDocument();
  await waitFor(() =>
    expect(
      screen.queryByText('Práctica de laboratorio de química'),
    ).not.toBeInTheDocument(),
  );
  expect(
    reservasEnMemoria().find((r) => r.id === 'reserva-1')?.estadoGuardado,
  ).toBe('CANCELADA');
});

test('eliminar la última fila de una página que no es la primera vuelve a la anterior', async () => {
  const [primera] = reservasSembradas();
  const { estadoGuardado, ...base } = primera;
  const dto = (id: string, actividad: string) => ({
    ...base,
    id,
    actividad,
    estado: 'PENDIENTE',
    editable: estadoGuardado === 'ACTIVA',
  });
  const pedidas: number[] = [];
  let eliminada = false;
  server.use(
    http.get(RUTA, ({ request }) => {
      const page = Number(new URL(request.url).searchParams.get('page'));
      pedidas.push(page);
      const pagina1 = Array.from({ length: 10 }, (_, i) =>
        dto(`r-${i}`, `Actividad ${i}`),
      );
      if (page === 1) {
        return HttpResponse.json({
          data: pagina1,
          meta: {
            page: 1,
            size: 10,
            total: eliminada ? 10 : 11,
            totalPages: eliminada ? 1 : 2,
          },
        });
      }
      return HttpResponse.json({
        data: eliminada ? [] : [dto('r-10', 'Actividad 10')],
        meta: {
          page: 2,
          size: 10,
          total: eliminada ? 10 : 11,
          totalPages: eliminada ? 1 : 2,
        },
      });
    }),
    http.delete(`${RUTA}/:id`, () => {
      eliminada = true;
      return new HttpResponse(null, { status: 204 });
    }),
  );
  await mostrar();
  await mostrando('1-10 de 11');
  await userEvent.click(screen.getByRole('button', { name: '2' }));
  await screen.findByText('Actividad 10');

  await userEvent.click(
    screen.getByRole('button', { name: 'Eliminar reserva' }),
  );
  await userEvent.click(
    within(await dialogo()).getByRole('button', { name: 'Eliminar' }),
  );

  expect(await screen.findByText('Reserva eliminada.')).toBeInTheDocument();
  await mostrando('1-10 de 10');
  expect(screen.getByText('Actividad 0')).toBeInTheDocument();
  expect(pedidas.at(-1)).toBe(1);
});

test('un error al eliminar (409) sale como notificación global y la reserva sigue en el listado', async () => {
  server.use(
    http.delete(`${RUTA}/:id`, () =>
      HttpResponse.json(
        { message: 'La reserva ya finalizó y no se puede modificar.' },
        { status: 409 },
      ),
    ),
  );
  await abrirPantalla();
  await userEvent.click(
    within(fila('Taller de programación')).getByRole('button', {
      name: 'Eliminar reserva',
    }),
  );

  await userEvent.click(
    within(await dialogo()).getByRole('button', { name: 'Eliminar' }),
  );

  expect(
    await screen.findByText('La reserva ya finalizó y no se puede modificar.'),
  ).toBeInTheDocument();
  expect(screen.getByText('Taller de programación')).toBeInTheDocument();
});
