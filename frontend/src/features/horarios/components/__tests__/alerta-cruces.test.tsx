import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { CRUCE_DE_AMBIENTE_NUEVO } from '@/testing/mocks/fixtures/horarios-coordinador';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  renderApp,
  screen,
  userEvent,
  waitFor,
  within,
} from '@/testing/test-utils';

import { CruceDetectado } from '../../types';
import { AsignacionHorarios } from '../asignacion-horarios';

// Figma > HU-46 > modal con cruce (296:17801): una alerta por tipo de cruce, con su título, y el campo en conflicto
// marcado. El backend manda el mensaje sin título; el título lo pone el frontend según el `tipo`.

const API = env.API_URL;

const AYUDA_DEL_DOCENTE =
  'Elige otro bloque o cambia el titular en Plan de evaluación.';

const CRUCE_DOCENTE: CruceDetectado = {
  tipo: 'DOCENTE',
  dia: 'LUN',
  numeroBloque: 1,
  mensaje:
    'Alessandra Huertas Villarreal ya dicta Física con 5.° C el lunes de 8:00 a 8:45.',
  seccion: '5.° C',
  curso: 'Física',
};

const CRUCE_AMBIENTE: CruceDetectado = {
  tipo: 'AMBIENTE',
  dia: 'LUN',
  numeroBloque: 1,
  mensaje: CRUCE_DE_AMBIENTE_NUEVO,
  seccion: '5.° A',
  curso: 'Física',
};

/** El backend de los cruces responde esta lista, sea cual sea el borrador. */
const responderCruces = (cruces: CruceDetectado[]) => {
  const estado = { llamado: false };
  server.use(
    http.post(`${API}/horarios/secciones/:seccionId/cruces`, () => {
      estado.llamado = true;
      return HttpResponse.json(cruces);
    }),
  );
  return estado;
};

const campo = (etiqueta: string, raiz: HTMLElement) => {
  const texto = within(raiz)
    .getAllByText(etiqueta)
    .find((elemento) => elemento.closest('label'));
  return texto!.closest('label') as HTMLElement;
};

const selectNativo = (etiqueta: string, raiz: HTMLElement) =>
  within(campo(etiqueta, raiz))
    .getAllByRole('combobox')
    .find((e) => e.tagName === 'SELECT') as HTMLSelectElement;

const botonDelSelect = (etiqueta: string, raiz: HTMLElement = document.body) =>
  within(campo(etiqueta, raiz))
    .getAllByRole('combobox')
    .find((e) => e.tagName === 'BUTTON') as HTMLElement;

const elegir = async (
  etiqueta: string,
  opcionElegida: string,
  raiz: HTMLElement = document.body,
) => {
  await waitFor(() => expect(botonDelSelect(etiqueta, raiz)).toBeEnabled());
  await userEvent.click(botonDelSelect(etiqueta, raiz));
  await userEvent.click(
    await within(await screen.findByRole('listbox')).findByRole('option', {
      name: opcionElegida,
    }),
  );
};

/** Abre el modal del lunes, bloque 1, de 5.° B (sin horario) con Física y el ambiente indicado ya elegidos. */
const abrirModalConAmbiente = async (ambiente: string) => {
  renderApp(<AsignacionHorarios />, {
    user: await createUser({ roles: [ROLES.COORDINADOR] }),
  });
  await elegir('Año académico', '2026');
  await elegir('Grado', '5.° de secundaria');
  await elegir('Sección', 'B');
  await screen.findByRole('table', { name: 'Matriz horaria' });
  await userEvent.click(screen.getByRole('button', { name: 'Editar horario' }));

  await userEvent.click(
    within(screen.getByRole('cell', { name: 'Lunes, bloque 1' })).getByRole(
      'button',
    ),
  );
  const dialogo = await screen.findByRole('dialog');
  await within(dialogo).findByRole('group', { name: 'Curso' });
  await userEvent.click(
    within(dialogo).getByRole('button', { name: 'Física · 0/2 h' }),
  );
  await elegir('Ambiente', ambiente, dialogo);
  return dialogo;
};

const esperarAlerta = (dialogo: HTMLElement, titulo: string) =>
  within(dialogo).findByText(titulo, undefined, { timeout: 3000 });

test('cruce de docente: alerta «Docente no disponible» con su mensaje, la ayuda y el campo Docente marcado', async () => {
  responderCruces([CRUCE_DOCENTE]);
  const dialogo = await abrirModalConAmbiente('Laboratorio de ciencias');

  const titulo = await esperarAlerta(dialogo, 'Docente no disponible');
  const alerta = titulo.closest('[role="alert"]') as HTMLElement;
  expect(within(alerta).getByText(CRUCE_DOCENTE.mensaje)).toBeVisible();
  expect(alerta).toHaveClass('py-1.5');
  expect(within(dialogo).getByText(AYUDA_DEL_DOCENTE)).toBeVisible();
  expect(selectNativo('Docente', dialogo)).toHaveAttribute(
    'aria-invalid',
    'true',
  );
  expect(selectNativo('Ambiente', dialogo)).toHaveAttribute(
    'aria-invalid',
    'false',
  );
  expect(
    within(dialogo).queryByText('Ambiente no disponible'),
  ).not.toBeInTheDocument();
});

test('cruce de ambiente: alerta «Ambiente no disponible» con su mensaje, el campo Ambiente marcado y sin la ayuda del docente', async () => {
  responderCruces([CRUCE_AMBIENTE]);
  const dialogo = await abrirModalConAmbiente('Laboratorio de ciencias');

  const titulo = await esperarAlerta(dialogo, 'Ambiente no disponible');
  const alerta = titulo.closest('[role="alert"]') as HTMLElement;
  expect(within(alerta).getByText(CRUCE_AMBIENTE.mensaje)).toBeVisible();
  expect(selectNativo('Ambiente', dialogo)).toHaveAttribute(
    'aria-invalid',
    'true',
  );
  expect(selectNativo('Docente', dialogo)).toHaveAttribute(
    'aria-invalid',
    'false',
  );
  expect(
    within(dialogo).queryByText(AYUDA_DEL_DOCENTE),
  ).not.toBeInTheDocument();
  expect(
    within(dialogo).queryByText('Docente no disponible'),
  ).not.toBeInTheDocument();
});

test('con cruces de los dos tipos hay dos alertas, primero la del docente y luego la del ambiente', async () => {
  // El backend los devuelve en otro orden: la pantalla los ordena.
  responderCruces([CRUCE_AMBIENTE, CRUCE_DOCENTE]);
  const dialogo = await abrirModalConAmbiente('Laboratorio de ciencias');

  const docente = (
    await esperarAlerta(dialogo, 'Docente no disponible')
  ).closest('[role="alert"]') as HTMLElement;
  const ambiente = (
    await esperarAlerta(dialogo, 'Ambiente no disponible')
  ).closest('[role="alert"]') as HTMLElement;

  expect(docente).not.toBe(ambiente);
  expect(
    docente.compareDocumentPosition(ambiente) &
      Node.DOCUMENT_POSITION_FOLLOWING,
  ).toBeTruthy();
  expect(within(docente).getByText(CRUCE_DOCENTE.mensaje)).toBeVisible();
  expect(within(ambiente).getByText(CRUCE_AMBIENTE.mensaje)).toBeVisible();
  expect(within(dialogo).getByText(AYUDA_DEL_DOCENTE)).toBeVisible();
  expect(selectNativo('Docente', dialogo)).toHaveAttribute(
    'aria-invalid',
    'true',
  );
  expect(selectNativo('Ambiente', dialogo)).toHaveAttribute(
    'aria-invalid',
    'true',
  );
});

test('sin cruces no hay alertas de cruce ni campos marcados', async () => {
  const consulta = responderCruces([]);
  const dialogo = await abrirModalConAmbiente('Aula 401');

  await waitFor(() => expect(consulta.llamado).toBe(true));
  // Da tiempo a que la respuesta llegue y se pinte.
  await new Promise((resolver) => setTimeout(resolver, 150));

  expect(
    within(dialogo).queryByText('Docente no disponible'),
  ).not.toBeInTheDocument();
  expect(
    within(dialogo).queryByText('Ambiente no disponible'),
  ).not.toBeInTheDocument();
  expect(
    within(dialogo).queryByText(AYUDA_DEL_DOCENTE),
  ).not.toBeInTheDocument();
  expect(selectNativo('Docente', dialogo)).toHaveAttribute(
    'aria-invalid',
    'false',
  );
  expect(selectNativo('Ambiente', dialogo)).toHaveAttribute(
    'aria-invalid',
    'false',
  );
  expect(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  ).toBeEnabled();
});

test('con un cruce «Asignar» sigue deshabilitado', async () => {
  responderCruces([CRUCE_DOCENTE]);
  const dialogo = await abrirModalConAmbiente('Laboratorio de ciencias');

  await esperarAlerta(dialogo, 'Docente no disponible');

  expect(
    within(dialogo).getByRole('button', { name: 'Asignar' }),
  ).toBeDisabled();
});
