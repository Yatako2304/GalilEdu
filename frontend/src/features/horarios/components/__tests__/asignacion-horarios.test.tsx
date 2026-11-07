import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import {
  AMBIENTE_A401,
  CRUCE_DE_AMBIENTE_NUEVO,
  CRUCE_DE_DOCENTE,
  MENSAJE_SIN_PRINCIPAL,
  PLAN_MATEMATICA,
  SECCION_5A,
  SECCION_5B,
  gradosCoordinador,
  horarioDe,
  mensajeDeVersion,
  opcionesCoordinador,
} from '@/testing/mocks/fixtures/horarios-coordinador';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  renderApp,
  screen,
  userEvent,
  waitFor,
  within,
} from '@/testing/test-utils';

import { AsignacionHorarios } from '../asignacion-horarios';

// Figma > Coordinador > "HU-45: Asignación de docente y curso a un bloque de horario"

const API = env.API_URL;

const mostrar = async () =>
  renderApp(<AsignacionHorarios />, {
    user: await createUser({ roles: [ROLES.COORDINADOR] }),
  });

/** El campo (etiqueta + selector) de un Select de la pantalla o de un modal. */
const campo = (etiqueta: string, raiz: HTMLElement = document.body) => {
  const texto = within(raiz)
    .getAllByText(etiqueta)
    .find((elemento) => elemento.closest('label'));
  return texto!.closest('label') as HTMLElement;
};

// Cada Select tiene un <select> nativo (oculto) y un botón con rol combobox que es el que se ve.
const selectNativo = (etiqueta: string, raiz: HTMLElement = document.body) =>
  within(campo(etiqueta, raiz))
    .getAllByRole('combobox')
    .find((e) => e.tagName === 'SELECT') as HTMLSelectElement;

const botonDelSelect = (etiqueta: string, raiz: HTMLElement = document.body) =>
  within(campo(etiqueta, raiz))
    .getAllByRole('combobox')
    .find((e) => e.tagName === 'BUTTON') as HTMLElement;

// La lista que se abre (no las <option> del select nativo, que tienen el mismo rol).
const opcionDeLaLista = async (nombre: string) =>
  within(await screen.findByRole('listbox')).findByRole('option', {
    name: nombre,
  });

const elegir = async (
  etiqueta: string,
  opcionElegida: string,
  raiz: HTMLElement = document.body,
) => {
  // El grado y la sección se habilitan cuando llegan sus opciones.
  await waitFor(() => expect(botonDelSelect(etiqueta, raiz)).toBeEnabled());
  await userEvent.click(botonDelSelect(etiqueta, raiz));
  await userEvent.click(await opcionDeLaLista(opcionElegida));
};

/** Abre la sección (el año activo ya viene elegido): 5.° de secundaria y la letra. */
const abrirSeccion = async (letra: 'A' | 'B' | 'C' | 'D') => {
  await elegir('Año académico', '2026');
  await elegir('Grado', '5.° de secundaria');
  await elegir('Sección', letra);
  await screen.findByRole('table', { name: 'Matriz horaria' });
};

const celda = (dia: string, bloque: number) =>
  screen.getByRole('cell', { name: `${dia}, bloque ${bloque}` });

const editar = async () =>
  userEvent.click(screen.getByRole('button', { name: 'Editar horario' }));

const modal = () => screen.findByRole('dialog');

/** Abre el modal de un bloque y espera a que lleguen las opciones (los cursos). */
const abrirBloque = async (dia: string, bloque: number) => {
  await userEvent.click(within(celda(dia, bloque)).getByRole('button'));
  const dialogo = await modal();
  await within(dialogo).findByRole('group', { name: 'Curso' });
  return dialogo;
};

const etiquetaDeCurso = (dialogo: HTMLElement, texto: string) =>
  within(dialogo).getByRole('button', { name: texto });

const SIN_CAMBIOS = 'Tienes cambios sin guardar';

test('los selectores van en cascada: nada viene elegido y cada uno se habilita con el anterior', async () => {
  await mostrar();
  await screen.findByRole('heading', { name: 'Datos del Horario' });

  // Al abrir, el año está vacío: el grado y la sección esperan.
  expect(await screen.findByText('Año académico')).toBeInTheDocument();
  expect(selectNativo('Año académico')).toHaveValue('');
  expect(selectNativo('Año académico')).toBeEnabled();
  expect(selectNativo('Grado')).toBeDisabled();
  expect(selectNativo('Sección')).toBeDisabled();
  expect(
    screen.getByText(/Elige un año, un grado y una sección/),
  ).toBeInTheDocument();

  await elegir('Año académico', '2026');
  await waitFor(() => expect(selectNativo('Grado')).toBeEnabled());
  expect(selectNativo('Sección')).toBeDisabled();

  await elegir('Grado', '5.° de secundaria');
  await waitFor(() => expect(selectNativo('Sección')).toBeEnabled());

  await elegir('Sección', 'A');
  await screen.findByRole('table', { name: 'Matriz horaria' });
  expect(screen.getByLabelText('Tutor')).toHaveValue('Lucía Ayala Prado');
  expect(screen.getByLabelText('Tutor')).toBeDisabled();

  await elegir('Sección', 'B');
  await waitFor(() =>
    expect(screen.getByLabelText('Tutor')).toHaveValue('Sin asignar'),
  );
});

test('el selector de grado solo ofrece los grados con secciones', async () => {
  await mostrar();
  await elegir('Año académico', '2026');

  await waitFor(() => expect(botonDelSelect('Grado')).toBeEnabled());
  await userEvent.click(botonDelSelect('Grado'));
  const lista = await screen.findByRole('listbox');
  // El backend manda los 11 grados; los de primaria no tienen secciones y no se ofrecen.
  expect(
    within(lista)
      .getAllByRole('option')
      .map((o) => o.textContent),
  ).toEqual(['4.° de secundaria', '5.° de secundaria']);
});

test('si el año no tiene grados con secciones, el selector de grado queda sin opciones y deshabilitado', async () => {
  server.use(
    http.get(`${API}/horarios/anios-escolares/:anioId/secciones`, () =>
      HttpResponse.json(
        gradosCoordinador.map((g) => ({
          ...g,
          cantidadSeccionesActivas: 0,
          secciones: [],
        })),
      ),
    ),
  );
  await mostrar();
  await elegir('Año académico', '2027');

  await waitFor(() => expect(selectNativo('Grado')).toBeDisabled());
  expect(within(selectNativo('Grado')).queryAllByRole('option')).toHaveLength(
    0,
  );
});

test('las tarjetas llevan su título en Body/Body-1/Medium (18 px, medium)', async () => {
  await mostrar();
  const datos = await screen.findByRole('heading', {
    name: 'Datos del Horario',
  });
  await abrirSeccion('A');
  const matriz = screen.getByRole('heading', { name: 'Matriz horaria' });

  for (const titulo of [datos, matriz]) {
    expect(titulo).toHaveClass('text-body-1', 'font-medium');
    expect(titulo).not.toHaveClass('text-h6');
  }
});

test('ningún campo de la pantalla usa placeholder: los vacíos muestran solo su etiqueta', async () => {
  await mostrar();
  await screen.findByText('Año académico');

  expect(
    screen.queryByText(/Elige un grado|Elige una sección/),
  ).not.toBeInTheDocument();
  // Los selectores vacíos no muestran texto en el campo, solo la etiqueta que lo acompaña.
  for (const etiqueta of ['Año académico', 'Grado', 'Sección']) {
    expect(botonDelSelect(etiqueta)).toHaveTextContent('');
  }
  expect(screen.getByLabelText('Tutor')).not.toHaveAttribute(
    'placeholder',
    'Tutor',
  );
  // La única opción "vacía" de cada select es la oculta, sin texto.
  screen
    .getAllByRole('option', { hidden: true })
    .filter((o) => (o as HTMLOptionElement).value === '')
    .forEach((opcion) => {
      expect(opcion).toHaveTextContent('');
      expect(opcion).toHaveAttribute('hidden');
    });
});

test('modo lectura: celdas asignadas con curso, docente y aula; las vacías son grises, sin «+» y no se abren', async () => {
  await mostrar();
  await abrirSeccion('A');

  const lunes = within(celda('Lunes', 1));
  expect(lunes.getByText('Matemática')).toBeInTheDocument();
  expect(lunes.getByText('Pedro García Ramos')).toBeInTheDocument();
  // La celda muestra el código del ambiente (Figma: «A-502»), no su nombre («Aula 502»).
  expect(lunes.getByText('A-502')).toBeInTheDocument();
  expect(lunes.queryByText('Aula 502')).not.toBeInTheDocument();
  // Un curso al que ya no se le asignó docente se ve sin docente.
  expect(within(celda('Miércoles', 3)).getByText('Geometría')).toBeVisible();
  expect(
    within(celda('Miércoles', 3)).queryByText(/Pedro|Alessandra/),
  ).not.toBeInTheDocument();

  // Vacías: sin botón ni «Asignar».
  expect(
    within(celda('Martes', 1)).queryByRole('button'),
  ).not.toBeInTheDocument();
  expect(screen.queryByText('Asignar')).not.toBeInTheDocument();

  // Días de la jornada (sin sábado) y el recreo entre los bloques 3 y 4.
  expect(screen.getAllByRole('columnheader').map((c) => c.textContent)).toEqual(
    ['Bloque', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'],
  );
  expect(screen.getByText('Recreo 1')).toBeInTheDocument();
  expect(screen.getByText('10:15 - 10:30am')).toBeInTheDocument();
  expect(screen.getByText('8:00 - 8:45am')).toBeInTheDocument();

  // Barra de abajo: solo «Editar horario».
  expect(screen.getByRole('button', { name: 'Editar horario' })).toBeVisible();
  expect(
    screen.queryByRole('button', { name: 'Guardar cambios' }),
  ).not.toBeInTheDocument();
});

test('los días salen de la jornada: con sábado aparece la columna del sábado', async () => {
  server.use(
    http.get(`${API}/horarios/secciones/${SECCION_5A}`, () => {
      const horario = horarioDe(SECCION_5A)!;
      return HttpResponse.json({
        ...horario,
        jornada: { ...horario.jornada, dias: [...horario.jornada.dias, 'SAB'] },
      });
    }),
  );
  await mostrar();
  await abrirSeccion('A');

  expect(
    screen.getByRole('columnheader', { name: 'Sábado' }),
  ).toBeInTheDocument();
});

test('«Editar horario» no aparece si el año está cerrado (editable = false)', async () => {
  await mostrar();
  await abrirSeccion('D');

  expect(within(celda('Lunes', 1)).getByText('Matemática')).toBeVisible();
  expect(
    screen.queryByRole('button', { name: 'Editar horario' }),
  ).not.toBeInTheDocument();
});

test('un horario con cruces marca la celda y avisa en la página con el mensaje del cruce', async () => {
  await mostrar();
  await abrirSeccion('C');

  expect(screen.getByText('Este horario tiene cruces')).toBeInTheDocument();
  // El mensaje sale en el aviso de la página y en la celda marcada.
  expect(screen.getAllByText(CRUCE_DE_DOCENTE.mensaje)).toHaveLength(2);
  expect(
    within(celda('Martes', 2)).getByText(CRUCE_DE_DOCENTE.mensaje),
  ).toBeVisible();
});

test('sin horario ni estructura principal muestra el mensaje del backend como estado vacío', async () => {
  const { container } = await mostrar();
  await elegir('Año académico', '2026');
  await elegir('Grado', '4.° de secundaria');
  await elegir('Sección', 'A');

  expect(await screen.findByText(MENSAJE_SIN_PRINCIPAL)).toBeInTheDocument();
  expect(
    screen.queryByRole('table', { name: 'Matriz horaria' }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'Editar horario' }),
  ).not.toBeInTheDocument();
  // El estado vacío reemplaza a la notificación de error: no hay una segunda copia del mensaje.
  expect(within(container).getAllByText(MENSAJE_SIN_PRINCIPAL)).toHaveLength(1);
});

test('una sección que no existe muestra el mensaje del backend', async () => {
  server.use(
    http.get(`${API}/horarios/secciones/${SECCION_5B}`, () =>
      HttpResponse.json({ message: 'La sección no existe.' }, { status: 404 }),
    ),
  );
  await mostrar();
  await elegir('Año académico', '2026');
  await elegir('Grado', '5.° de secundaria');
  await elegir('Sección', 'B');

  expect(await screen.findByText('La sección no existe.')).toBeInTheDocument();
});

test('modo edición: la barra pasa a «Cancelar» y «Guardar cambios» y las celdas vacías muestran «+»', async () => {
  await mostrar();
  await abrirSeccion('A');
  await editar();

  expect(screen.getByRole('button', { name: 'Cancelar' })).toBeVisible();
  const guardar = screen.getByRole('button', { name: 'Guardar cambios' });
  // Sin cambios no hay aviso y no se puede guardar.
  expect(guardar).toBeDisabled();
  expect(screen.queryByText(SIN_CAMBIOS)).not.toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'Editar horario' }),
  ).not.toBeInTheDocument();
  expect(
    within(celda('Martes', 1)).getByRole('button', { name: /Asignar/ }),
  ).toBeVisible();
  // Una celda asignada también se abre.
  expect(within(celda('Lunes', 1)).getByRole('button')).toBeVisible();
});

test('«Cancelar» descarta el borrador y vuelve a modo lectura', async () => {
  await mostrar();
  await abrirSeccion('A');
  await editar();

  const dialogo = await abrirBloque('Martes', 1);
  await userEvent.click(etiquetaDeCurso(dialogo, 'Matemática · 1/3 h'));
  await userEvent.click(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  );
  expect(await screen.findByText(SIN_CAMBIOS)).toBeVisible();

  await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

  expect(screen.getByRole('button', { name: 'Editar horario' })).toBeVisible();
  expect(
    within(celda('Martes', 1)).queryByText('Matemática'),
  ).not.toBeInTheDocument();
  expect(screen.queryByText(SIN_CAMBIOS)).not.toBeInTheDocument();
});

test('modal: el docente es un campo deshabilitado que se llena al elegir el curso y el ambiente va con el aula de la sección', async () => {
  await mostrar();
  await abrirSeccion('A');
  await editar();

  const dialogo = await abrirBloque('Martes', 1);
  expect(
    within(dialogo).getByText('Asignar bloque horario'),
  ).toBeInTheDocument();
  expect(
    within(dialogo).getByRole('heading', { name: 'Martes - Bloque 1' }),
  ).toBeInTheDocument();
  expect(within(dialogo).getByText('8:00 - 8:45am')).toBeInTheDocument();

  // Antes de elegir curso: docente vacío y deshabilitado; el ambiente ya trae el aula de la sección (A-502).
  expect(selectNativo('Docente', dialogo)).toBeDisabled();
  expect(selectNativo('Docente', dialogo)).toHaveValue('');
  expect(selectNativo('Ambiente', dialogo)).toHaveValue('amb-a502');
  expect(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  ).toBeDisabled();

  await userEvent.click(etiquetaDeCurso(dialogo, 'Física · 0/2 h'));

  expect(selectNativo('Docente', dialogo)).toHaveValue('doc-pedro');
  expect(selectNativo('Docente', dialogo)).toBeDisabled();
  expect(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  ).toBeEnabled();
  // No hay subgrupos en el modal.
  expect(within(dialogo).queryByText(/subgrupo/i)).not.toBeInTheDocument();
});

test('modal: un curso completo o sin docente está deshabilitado y el sin docente lleva un aviso con enlace a Plan de evaluación', async () => {
  await mostrar();
  await abrirSeccion('A');
  await editar();

  const dialogo = await abrirBloque('Martes', 1);
  // Arte y Cultura ya tiene su única hora; Geometría no tiene docente.
  expect(etiquetaDeCurso(dialogo, 'Arte y Cultura · 1/1 h')).toBeDisabled();
  expect(etiquetaDeCurso(dialogo, 'Geometría · 1/2 h')).toBeDisabled();
  expect(etiquetaDeCurso(dialogo, 'Matemática · 1/3 h')).toBeEnabled();

  expect(within(dialogo).getByText('Sin docente: Geometría')).toBeVisible();
  expect(
    within(dialogo).getByRole('link', { name: 'Ir a Plan de evaluación' }),
  ).toHaveAttribute('href', expect.stringContaining('plan-evaluacion'));
});

test('modal: una sección sin aula no trae ambiente por defecto y hay que elegir uno antes de asignar', async () => {
  await mostrar();
  await abrirSeccion('B');
  await editar();

  const dialogo = await abrirBloque('Lunes', 2);
  expect(selectNativo('Ambiente', dialogo)).toHaveValue('');

  await userEvent.click(etiquetaDeCurso(dialogo, 'Matemática · 0/3 h'));
  expect(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  ).toBeDisabled();

  await elegir('Ambiente', 'Aula 401', dialogo);

  expect(selectNativo('Ambiente', dialogo)).toHaveValue(AMBIENTE_A401);
  expect(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  ).toBeEnabled();
});

test('modal: «Asignar» pone el bloque en el borrador, suma sus horas y avisa que hay cambios', async () => {
  await mostrar();
  await abrirSeccion('A');
  await editar();

  let dialogo = await abrirBloque('Martes', 1);
  await userEvent.click(etiquetaDeCurso(dialogo, 'Matemática · 1/3 h'));
  await userEvent.click(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  );

  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(within(celda('Martes', 1)).getByText('Matemática')).toBeVisible();
  expect(
    within(celda('Martes', 1)).getByText('Pedro García Ramos'),
  ).toBeVisible();
  // Una celda recién asignada también muestra el código del ambiente.
  expect(within(celda('Martes', 1)).getByText('A-502')).toBeVisible();
  expect(await screen.findByText(SIN_CAMBIOS)).toBeVisible();
  expect(screen.getByRole('button', { name: 'Guardar cambios' })).toBeEnabled();

  // Las horas del borrador cuentan: ahora Matemática lleva 2 de 3.
  dialogo = await abrirBloque('Viernes', 1);
  expect(etiquetaDeCurso(dialogo, 'Matemática · 2/3 h')).toBeEnabled();
});

test('modal: «Actualizar» y «Quitar» aparecen al abrir un bloque asignado, y las horas no cuentan el bloque editado', async () => {
  await mostrar();
  await abrirSeccion('A');
  await editar();

  const dialogo = await abrirBloque('Lunes', 1);
  expect(
    within(dialogo).getByRole('button', { name: 'Actualizar' }),
  ).toBeVisible();
  expect(within(dialogo).getByRole('button', { name: 'Quitar' })).toBeVisible();
  expect(
    within(dialogo).queryByRole('button', { name: 'Asignar' }),
  ).not.toBeInTheDocument();
  // Matemática tiene 1 hora en el backend y es la de este bloque: queda en 0/3 (se puede volver a elegir).
  expect(etiquetaDeCurso(dialogo, 'Matemática · 0/3 h')).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  // Al abrir un bloque ya asignado, el docente viene lleno con el del curso.
  expect(selectNativo('Docente', dialogo)).toHaveValue('doc-pedro');

  await userEvent.click(etiquetaDeCurso(dialogo, 'Física · 0/2 h'));
  await userEvent.click(
    within(dialogo).getByRole('button', { name: 'Actualizar' }),
  );

  expect(within(celda('Lunes', 1)).getByText('Física')).toBeVisible();
  expect(
    within(celda('Lunes', 1)).queryByText('Matemática'),
  ).not.toBeInTheDocument();

  const otra = await abrirBloque('Lunes', 1);
  await userEvent.click(within(otra).getByRole('button', { name: 'Quitar' }));

  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(
    within(celda('Lunes', 1)).queryByText('Física'),
  ).not.toBeInTheDocument();
  expect(
    within(celda('Lunes', 1)).getByRole('button', { name: /Asignar/ }),
  ).toBeVisible();
});

test('modal: «Cancelar» cierra sin cambiar el borrador', async () => {
  await mostrar();
  await abrirSeccion('A');
  await editar();

  const dialogo = await abrirBloque('Martes', 1);
  await userEvent.click(etiquetaDeCurso(dialogo, 'Física · 0/2 h'));
  await userEvent.click(
    within(dialogo).getByRole('button', { name: 'Cancelar' }),
  );

  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(
    within(celda('Martes', 1)).queryByText('Física'),
  ).not.toBeInTheDocument();
  expect(screen.queryByText(SIN_CAMBIOS)).not.toBeInTheDocument();
});

test('modal: un cruce que devuelve el backend se muestra y deshabilita «Asignar»', async () => {
  await mostrar();
  await abrirSeccion('B');
  await editar();

  const dialogo = await abrirBloque('Lunes', 1);
  await userEvent.click(etiquetaDeCurso(dialogo, 'Física · 0/2 h'));
  await elegir('Ambiente', 'Laboratorio de ciencias', dialogo);

  expect(
    await within(dialogo).findByText(CRUCE_DE_AMBIENTE_NUEVO, undefined, {
      timeout: 3000,
    }),
  ).toBeVisible();
  expect(within(dialogo).getByText('Ambiente no disponible')).toBeVisible();
  expect(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  ).toBeDisabled();
});

test('modal: si la consulta de cruces falla (404 o red) no hay alerta ni notificación y se puede asignar', async () => {
  server.use(
    http.post(`${API}/horarios/secciones/:seccionId/cruces`, () =>
      HttpResponse.json({ message: 'No encontrado' }, { status: 404 }),
    ),
  );
  await mostrar();
  await abrirSeccion('B');
  await editar();

  const dialogo = await abrirBloque('Lunes', 1);
  await userEvent.click(etiquetaDeCurso(dialogo, 'Física · 0/2 h'));
  await elegir('Ambiente', 'Laboratorio de ciencias', dialogo);

  // Se espera a que la consulta termine (pausa de 300 ms + respuesta) y no pasa nada.
  await new Promise((resolver) => setTimeout(resolver, 900));
  expect(
    within(dialogo).queryByText('Ambiente no disponible'),
  ).not.toBeInTheDocument();
  expect(screen.queryByText('No encontrado')).not.toBeInTheDocument();
  expect(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  ).toBeEnabled();
});

test('guardar: un solo PUT con la versión del GET y todas las celdas del borrador; al terminar vuelve a lectura', async () => {
  let cuerpo: any = null;
  server.use(
    http.put(`${API}/horarios/secciones/${SECCION_5A}`, async ({ request }) => {
      cuerpo = await request.json();
      const horario = horarioDe(SECCION_5A)!;
      return HttpResponse.json({
        ...horario,
        version: 4,
        celdas: [
          ...horario.celdas,
          {
            dia: 'MAR',
            numeroBloque: 1,
            planCursoId: PLAN_MATEMATICA,
            curso: 'Matemática',
            docente: { id: 'doc-pedro', nombre: 'Pedro García Ramos' },
            ambiente: { id: 'amb-a502', nombre: 'Aula 502', codigo: 'A-502' },
            cruces: [],
          },
        ],
      });
    }),
  );
  await mostrar();
  await abrirSeccion('A');
  await editar();

  const dialogo = await abrirBloque('Martes', 1);
  await userEvent.click(etiquetaDeCurso(dialogo, 'Matemática · 1/3 h'));
  await userEvent.click(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Guardar cambios' }),
  );

  expect(
    await screen.findByRole('button', { name: 'Editar horario' }),
  ).toBeVisible();
  expect(cuerpo.version).toBe(3);
  // La matriz completa: las 4 celdas que ya estaban y la nueva, sin docente ni nombres.
  expect(cuerpo.celdas).toHaveLength(5);
  expect(cuerpo.celdas).toContainEqual({
    dia: 'MAR',
    numeroBloque: 1,
    planCursoId: PLAN_MATEMATICA,
    ambienteId: 'amb-a502',
  });
  expect(cuerpo.celdas).toContainEqual({
    dia: 'LUN',
    numeroBloque: 1,
    planCursoId: PLAN_MATEMATICA,
    ambienteId: 'amb-a502',
  });
  expect(Object.keys(cuerpo.celdas[0]).sort()).toEqual([
    'ambienteId',
    'dia',
    'numeroBloque',
    'planCursoId',
  ]);
  expect(await screen.findByText('Horario guardado')).toBeVisible();
  expect(within(celda('Martes', 1)).getByText('Matemática')).toBeVisible();
  expect(
    screen.queryByRole('button', { name: 'Guardar cambios' }),
  ).not.toBeInTheDocument();
});

test('guardar con el primer horario envía version null', async () => {
  let cuerpo: any = null;
  server.use(
    http.put(`${API}/horarios/secciones/${SECCION_5B}`, async ({ request }) => {
      cuerpo = await request.json();
      return HttpResponse.json({ ...horarioDe(SECCION_5B)!, version: 0 });
    }),
  );
  await mostrar();
  await abrirSeccion('B');
  await editar();

  const dialogo = await abrirBloque('Lunes', 2);
  await userEvent.click(etiquetaDeCurso(dialogo, 'Matemática · 0/3 h'));
  await elegir('Ambiente', 'Aula 401', dialogo);
  await userEvent.click(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Guardar cambios' }),
  );

  await screen.findByRole('button', { name: 'Editar horario' });
  expect(cuerpo.version).toBeNull();
  expect(cuerpo.celdas).toEqual([
    {
      dia: 'LUN',
      numeroBloque: 2,
      planCursoId: PLAN_MATEMATICA,
      ambienteId: AMBIENTE_A401,
    },
  ]);
});

test('un 409 de versión deja la edición abierta con el mensaje y una forma de recargar', async () => {
  const mensaje = mensajeDeVersion('5.° de secundaria', 'A');
  server.use(
    http.put(`${API}/horarios/secciones/${SECCION_5A}`, () =>
      HttpResponse.json({ message: mensaje }, { status: 409 }),
    ),
  );
  await mostrar();
  await abrirSeccion('A');
  await editar();

  const dialogo = await abrirBloque('Martes', 1);
  await userEvent.click(etiquetaDeCurso(dialogo, 'Matemática · 1/3 h'));
  await userEvent.click(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Guardar cambios' }),
  );

  // El mensaje sale una sola vez (en la página, no también como notificación) y se sigue editando.
  expect(await screen.findByText(mensaje)).toBeVisible();
  expect(screen.getAllByText(mensaje)).toHaveLength(1);
  expect(screen.getByRole('button', { name: 'Guardar cambios' })).toBeVisible();
  expect(within(celda('Martes', 1)).getByText('Matemática')).toBeVisible();

  // «Recargar» vuelve a leer lo guardado (ahora con una celda más) y descarta el borrador, sin salir de la edición.
  server.use(
    http.get(`${API}/horarios/secciones/${SECCION_5A}`, () => {
      const horario = horarioDe(SECCION_5A)!;
      return HttpResponse.json({
        ...horario,
        version: 4,
        celdas: horario.celdas.slice(0, 2),
      });
    }),
  );
  await userEvent.click(screen.getByRole('button', { name: 'Recargar' }));

  await waitFor(() =>
    expect(screen.queryByText(mensaje)).not.toBeInTheDocument(),
  );
  expect(
    within(celda('Martes', 1)).queryByText('Matemática'),
  ).not.toBeInTheDocument();
  expect(
    within(celda('Miércoles', 3)).queryByText('Geometría'),
  ).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Cancelar' })).toBeVisible();
  expect(screen.queryByText(SIN_CAMBIOS)).not.toBeInTheDocument();
});

test('un 400 del PUT se muestra tal cual y no cambia de modo', async () => {
  const mensaje =
    'El día SAB no es lectivo en la estructura horaria de la sección.';
  server.use(
    http.put(`${API}/horarios/secciones/${SECCION_5A}`, () =>
      HttpResponse.json({ message: mensaje }, { status: 400 }),
    ),
  );
  await mostrar();
  await abrirSeccion('A');
  await editar();

  const dialogo = await abrirBloque('Martes', 1);
  await userEvent.click(etiquetaDeCurso(dialogo, 'Matemática · 1/3 h'));
  await userEvent.click(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Guardar cambios' }),
  );

  expect(await screen.findByText(mensaje)).toBeVisible();
  // Un 400 no es un conflicto de versión: no se ofrece recargar.
  expect(
    screen.queryByRole('button', { name: 'Recargar' }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Guardar cambios' })).toBeVisible();
});

test('cambiar de sección con cambios sin guardar pide confirmación; sin cambios no', async () => {
  await mostrar();
  await abrirSeccion('A');
  await editar();

  // Sin cambios, cambiar de sección no pregunta.
  await elegir('Sección', 'C');
  await screen.findByText('Este horario tiene cruces');
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

  await elegir('Sección', 'A');
  await screen.findByText('Pedro García Ramos');
  await editar();
  const dialogo = await abrirBloque('Martes', 1);
  await userEvent.click(etiquetaDeCurso(dialogo, 'Matemática · 1/3 h'));
  await userEvent.click(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  );
  await screen.findByText(SIN_CAMBIOS);

  await elegir('Sección', 'B');
  const confirmacion = await screen.findByRole('dialog');
  expect(
    within(confirmacion).getByText('¿Descartar los cambios del horario?'),
  ).toBeVisible();

  // «Seguir editando» se queda con el borrador.
  await userEvent.click(
    within(confirmacion).getByRole('button', { name: 'Seguir editando' }),
  );
  expect(screen.getByLabelText('Tutor')).toHaveValue('Lucía Ayala Prado');
  expect(within(celda('Martes', 1)).getByText('Matemática')).toBeVisible();

  await elegir('Sección', 'B');
  await userEvent.click(
    within(await screen.findByRole('dialog')).getByRole('button', {
      name: 'Cambiar de sección',
    }),
  );
  await waitFor(() =>
    expect(screen.getByLabelText('Tutor')).toHaveValue('Sin asignar'),
  );
  expect(screen.getByRole('button', { name: 'Editar horario' })).toBeVisible();
});

test('cambiar de año o de grado con cambios sin guardar también pide confirmación', async () => {
  await mostrar();
  await abrirSeccion('A');
  await editar();
  const dialogo = await abrirBloque('Martes', 1);
  await userEvent.click(etiquetaDeCurso(dialogo, 'Matemática · 1/3 h'));
  await userEvent.click(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  );
  await screen.findByText(SIN_CAMBIOS);

  await elegir('Año académico', '2027');
  expect(
    await within(await screen.findByRole('dialog')).findByRole('button', {
      name: 'Cambiar de año',
    }),
  ).toBeVisible();
});

test('no aparece nada de subgrupos en la pantalla', async () => {
  const { container } = await mostrar();
  await abrirSeccion('A');
  expect(container).not.toHaveTextContent(/subgrupo/i);
  await editar();
  expect(container).not.toHaveTextContent(/subgrupo/i);
  const dialogo = await abrirBloque('Martes', 1);
  expect(dialogo).not.toHaveTextContent(/subgrupo/i);
});

test('modal: el campo Docente vacío muestra solo su etiqueta, sin texto de placeholder', async () => {
  await mostrar();
  await abrirSeccion('A');
  await editar();

  const dialogo = await abrirBloque('Martes', 1);
  // Antes de elegir curso: deshabilitado y vacío; «Docente» aparece una sola vez (la etiqueta).
  expect(selectNativo('Docente', dialogo)).toBeDisabled();
  expect(botonDelSelect('Docente', dialogo)).toHaveTextContent('');
  expect(within(dialogo).getAllByText('Docente')).toHaveLength(1);
  // Con valor, el campo muestra el nombre.
  await userEvent.click(etiquetaDeCurso(dialogo, 'Matemática · 1/3 h'));
  expect(botonDelSelect('Docente', dialogo)).toHaveTextContent(
    'Pedro García Ramos',
  );
});

test('modal: con muchos cursos sin docente el aviso es compacto y la lista completa va en el título', async () => {
  const nombres = [
    'Aritmética',
    'Arte y Cultura',
    'Biología',
    'Comunicación',
    'Educación Física',
    'Física',
    'Geometría',
    'Historia',
    'Inglés',
  ];
  server.use(
    http.get(`${API}/horarios/secciones/${SECCION_5A}/opciones`, () =>
      HttpResponse.json({
        ...opcionesCoordinador(SECCION_5A),
        cursos: nombres.map((nombre, i) => ({
          planCursoId: `plan-${i}`,
          nombre,
          horasSemanales: 2,
          horasAsignadas: 0,
          docente: null,
        })),
      }),
    ),
  );
  await mostrar();
  await abrirSeccion('A');
  await editar();

  const dialogo = await abrirBloque('Martes', 1);
  const resumen = within(dialogo).getByText(
    'Sin docente (9 cursos): Aritmética, Arte y Cultura, Biología y 6 más',
  );
  expect(resumen).toHaveAttribute(
    'title',
    `Sin docente (9 cursos): ${nombres.join(', ')}`,
  );
  // El aviso es la variante pequeña: letra y relleno reducidos, con el enlace discreto.
  const aviso = within(dialogo).getByRole('alert');
  expect(aviso).toHaveClass('py-1.5');
  expect(resumen.closest('p')).toHaveClass('text-label-3');
  expect(
    within(aviso).getByRole('link', { name: 'Ir a Plan de evaluación' }),
  ).toHaveClass('text-caption-1');
});

test('modal: el aviso de cruce también es la variante pequeña', async () => {
  await mostrar();
  await abrirSeccion('B');
  await editar();

  const dialogo = await abrirBloque('Lunes', 1);
  await userEvent.click(etiquetaDeCurso(dialogo, 'Física · 0/2 h'));
  await elegir('Ambiente', 'Laboratorio de ciencias', dialogo);

  const aviso = await within(dialogo).findByText(
    CRUCE_DE_AMBIENTE_NUEVO,
    undefined,
    {
      timeout: 3000,
    },
  );
  expect(aviso.closest('[role="alert"]')).toHaveClass('py-1.5');
});

test('modal: la lista del Ambiente flota fuera del diálogo (en el body) y elegir una opción no cierra el modal', async () => {
  await mostrar();
  await abrirSeccion('A');
  await editar();

  const dialogo = await abrirBloque('Martes', 1);
  await userEvent.click(botonDelSelect('Ambiente', dialogo));

  const lista = await screen.findByRole('listbox');
  // Cuelga del body y no del diálogo: no le suma altura ni scroll, y el borde del modal no la recorta.
  expect(dialogo.contains(lista)).toBe(false);
  expect(lista.parentElement).toBe(document.body);
  expect(lista).toHaveClass('fixed');
  expect(
    Number(getComputedStyle(lista).maxHeight.replace('px', '')),
  ).toBeGreaterThan(0);

  await userEvent.click(await opcionDeLaLista('Aula 401'));
  expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  expect(screen.getByRole('dialog')).toBeInTheDocument();
  expect(selectNativo('Ambiente', dialogo)).toHaveValue(AMBIENTE_A401);
});

test('modal: el aviso de cursos sin docente va arriba de los cursos', async () => {
  await mostrar();
  await abrirSeccion('A');
  await editar();

  const dialogo = await abrirBloque('Martes', 1);
  const aviso = within(dialogo).getByRole('alert');
  const cursos = within(dialogo).getByRole('group', { name: 'Curso' });
  expect(aviso).toHaveTextContent('Sin docente: Geometría');

  // El aviso está antes que el título «Curso» y que las etiquetas de los cursos.
  expect(
    aviso.compareDocumentPosition(cursos) & Node.DOCUMENT_POSITION_FOLLOWING,
  ).toBeTruthy();
  expect(
    aviso.compareDocumentPosition(within(dialogo).getByText('Curso')) &
      Node.DOCUMENT_POSITION_FOLLOWING,
  ).toBeTruthy();
});

test('modal: si la sección no tiene cursos en el plan muestra el mensaje y el enlace a Plan de evaluación', async () => {
  server.use(
    http.get(`${API}/horarios/secciones/${SECCION_5A}/opciones`, () =>
      HttpResponse.json({ ...opcionesCoordinador(SECCION_5A), cursos: [] }),
    ),
  );
  await mostrar();
  await abrirSeccion('A');
  await editar();

  await userEvent.click(within(celda('Martes', 1)).getByRole('button'));
  const dialogo = await modal();

  expect(
    await within(dialogo).findByText(
      'Esta sección no tiene cursos en el plan de evaluación.',
    ),
  ).toBeVisible();
  expect(
    within(dialogo).getByRole('link', { name: 'Ir a Plan de evaluación' }),
  ).toHaveAttribute('href', expect.stringContaining('plan-evaluacion'));
  // No hay etiquetas de cursos ni aviso de «sin docente», y no se puede asignar nada.
  expect(
    within(dialogo).queryByRole('group', { name: 'Curso' }),
  ).not.toBeInTheDocument();
  expect(within(dialogo).queryByText(/Sin docente/)).not.toBeInTheDocument();
  expect(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  ).toBeDisabled();
});
