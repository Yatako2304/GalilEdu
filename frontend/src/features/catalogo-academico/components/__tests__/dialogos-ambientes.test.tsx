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
import { AmbienteResumen } from '@/types/api';

import { DialogoAmbiente } from '../dialogos-ambientes';

// Figma > Personal Administrativo > CI-26 > "Modal - Nuevo ambiente físico" (nodo 857:11500)

const API = env.API_URL;

const aula101: AmbienteResumen = {
  id: 'a101',
  codigo: 'A-101',
  nombre: 'Aula 101',
  tipo: 'AULA_REGULAR',
  aforoMaximo: 30,
  recursos: ['Pizarra acrílica', 'Proyector'],
  estado: 'ACTIVO',
};

const creado: AmbienteResumen = {
  id: 'nuevo',
  codigo: 'E-001',
  nombre: 'Laboratorio de robótica',
  tipo: 'ESPACIO_ESPECIAL',
  aforoMaximo: 25,
  recursos: ['Kits de robótica', 'Proyector', '12 laptops'],
  estado: 'ACTIVO',
};

let cuerpo: unknown = null;
let envios = 0;

const alCerrar = vi.fn();

beforeEach(() => {
  cuerpo = null;
  envios = 0;
  alCerrar.mockClear();
});

const renderizar = async (ambiente: AmbienteResumen | null = null) =>
  renderApp(<DialogoAmbiente ambiente={ambiente} onCerrar={alCerrar} />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

/** Lo que responde el backend: un ambiente o un error con su mensaje. */
type Respuesta = AmbienteResumen | { message: string };

const respondeAlAlta = (estado = 201, cuerpoRespuesta: Respuesta = creado) =>
  server.use(
    http.post(`${API}/ambientes`, async ({ request }) => {
      envios++;
      cuerpo = await request.json();
      return HttpResponse.json(cuerpoRespuesta, { status: estado });
    }),
  );

const respondeALaEdicion = (
  estado = 200,
  cuerpoRespuesta: Respuesta = aula101,
) =>
  server.use(
    http.put(`${API}/ambientes/a101`, async ({ request }) => {
      envios++;
      cuerpo = await request.json();
      return HttpResponse.json(cuerpoRespuesta, { status: estado });
    }),
  );

const nombre = () => screen.getByRole('textbox', { name: /^Nombre/ });
const aforo = () => screen.getByRole('textbox', { name: /^Aforo máximo/ });
const recursos = () => screen.getByRole('textbox', { name: /^Recursos/ });
const tipo = (texto: RegExp) => screen.getByRole('radio', { name: texto });
const registrar = () =>
  userEvent.click(screen.getByRole('button', { name: 'Registrar ambiente' }));
const guardar = () =>
  userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

/** Llena el modal de Figma: Laboratorio de robótica, espacio especial, 25 personas. */
const llenarElModalDeFigma = async () => {
  await userEvent.type(nombre(), 'Laboratorio de robótica');
  await userEvent.click(tipo(/Espacio especial/));
  await userEvent.type(aforo(), '25');
  await userEvent.type(recursos(), 'Kits de robótica, Proyector, 12 laptops');
};

// ---------- Nuevo ambiente ----------

test('muestra el modal de Figma: título, ayuda, tipos y botones, sin campo Código', async () => {
  await renderizar();

  const modal = within(await screen.findByRole('dialog'));
  expect(modal.getByText('Nuevo ambiente físico')).toBeInTheDocument();
  expect(
    modal.getByText('Completa los datos del aula o espacio.'),
  ).toBeInTheDocument();
  expect(nombre()).toBeInTheDocument();
  expect(modal.getByText('Tipo de ambiente *')).toBeInTheDocument();
  expect(
    modal.getByText('Aula de clases asignable a una sección'),
  ).toBeInTheDocument();
  expect(
    modal.getByText('Laboratorio, taller, biblioteca, losa, auditorio…'),
  ).toBeInTheDocument();
  expect(
    modal.getByText('Personas. Un aula regular admite como máximo 45.'),
  ).toBeInTheDocument();
  expect(modal.getByRole('button', { name: 'Cancelar' })).toBeInTheDocument();
  expect(
    modal.getByRole('button', { name: 'Registrar ambiente' }),
  ).toBeInTheDocument();
  // Figma dibuja un campo «Código» junto al nombre: el código lo genera el backend y no se pide.
  expect(modal.queryByText(/Código/)).not.toBeInTheDocument();
  expect(
    modal.queryByRole('textbox', { name: /Código/ }),
  ).not.toBeInTheDocument();
});

test('los campos de texto abren vacíos y sin texto de ejemplo', async () => {
  await renderizar();
  await screen.findByRole('dialog');

  [nombre(), aforo(), recursos()].forEach((campo) => {
    expect(campo).toHaveValue('');
    // El componente pone un espacio como placeholder para flotar la etiqueta: no hay texto de ejemplo.
    expect((campo.getAttribute('placeholder') ?? '').trim()).toBe('');
  });
});

test('Recursos muestra la ayuda «Separa cada recurso con una coma.» bajo el campo, como la del aforo', async () => {
  await renderizar();
  const modal = within(await screen.findByRole('dialog'));

  const ayuda = modal.getByText('Separa cada recurso con una coma.');
  const ayudaDelAforo = modal.getByText(
    'Personas. Un aula regular admite como máximo 45.',
  );
  // Mismo componente y mismo estilo que la ayuda del aforo.
  expect(ayuda.tagName).toBe(ayudaDelAforo.tagName);
  expect(ayuda).toHaveClass(...ayudaDelAforo.className.split(' '));
  expect(
    recursos().compareDocumentPosition(ayuda) &
      Node.DOCUMENT_POSITION_FOLLOWING,
  ).toBeTruthy();
});

test('al editar, la ayuda de Recursos también se muestra', async () => {
  await renderizar(aula101);

  expect(
    await screen.findByText('Separa cada recurso con una coma.'),
  ).toBeInTheDocument();
});

test('al abrir no hay ningún tipo elegido: no se adivina un tipo que luego no se puede cambiar', async () => {
  await renderizar();
  await screen.findByRole('dialog');

  expect(tipo(/Aula regular/)).not.toBeChecked();
  expect(tipo(/Espacio especial/)).not.toBeChecked();
  expect(tipo(/Aula regular/)).toBeEnabled();
});

test('enviar vacío muestra los errores en cada campo y no envía nada', async () => {
  respondeAlAlta();
  await renderizar();
  await screen.findByRole('dialog');

  await registrar();

  expect(
    await screen.findByText('Ingresa el nombre del ambiente.'),
  ).toBeInTheDocument();
  expect(screen.getByText('Elige el tipo de ambiente.')).toBeInTheDocument();
  expect(screen.getByText('Ingresa el aforo máximo.')).toBeInTheDocument();
  expect(envios).toBe(0);
  expect(alCerrar).not.toHaveBeenCalled();
});

test('un aula regular de más de 45 se rechaza con el texto de Figma y un espacio especial no', async () => {
  respondeAlAlta();
  await renderizar();
  await screen.findByRole('dialog');
  await userEvent.type(nombre(), 'Aula 301');
  await userEvent.click(tipo(/Aula regular/));
  await userEvent.type(aforo(), '46');

  await registrar();

  expect(
    await screen.findByText('Un aula regular admite como máximo 45.'),
  ).toBeInTheDocument();
  expect(envios).toBe(0);

  // Con otro tipo el mismo aforo es válido.
  await userEvent.click(tipo(/Espacio especial/));
  await registrar();

  await waitFor(() => expect(envios).toBe(1));
  expect(cuerpo).toMatchObject({ tipo: 'ESPACIO_ESPECIAL', aforoMaximo: 46 });
});

test('el aforo debe ser un número entero mayor que 0', async () => {
  respondeAlAlta();
  await renderizar();
  await screen.findByRole('dialog');
  await userEvent.type(nombre(), 'Sala');
  await userEvent.click(tipo(/Espacio especial/));

  await userEvent.type(aforo(), 'veinte');
  await registrar();
  expect(
    await screen.findByText('Ingresa el aforo como un número entero.'),
  ).toBeInTheDocument();

  await userEvent.clear(aforo());
  await userEvent.type(aforo(), '0');
  await registrar();
  expect(
    await screen.findByText('El aforo debe ser mayor que 0.'),
  ).toBeInTheDocument();
  expect(envios).toBe(0);
});

test('registra el ambiente: envía los recursos como lista, sin código, avisa y cierra', async () => {
  respondeAlAlta();
  await renderizar();
  await screen.findByRole('dialog');
  await llenarElModalDeFigma();

  await registrar();

  await waitFor(() => expect(alCerrar).toHaveBeenCalledTimes(1));
  expect(cuerpo).toEqual({
    tipo: 'ESPACIO_ESPECIAL',
    nombre: 'Laboratorio de robótica',
    aforoMaximo: 25,
    recursos: ['Kits de robótica', 'Proyector', '12 laptops'],
  });
  expect(cuerpo).not.toHaveProperty('codigo');
  expect(await screen.findByText('Ambiente registrado.')).toBeInTheDocument();
});

test('el nombre y los recursos se normalizan antes de enviar', async () => {
  respondeAlAlta();
  await renderizar();
  await screen.findByRole('dialog');
  await userEvent.type(nombre(), '  Aula   nueva  ');
  await userEvent.click(tipo(/Aula regular/));
  await userEvent.type(aforo(), ' 30 ');
  await userEvent.type(recursos(), 'Proyector, , proyector,  Pizarra ,');

  await registrar();

  await waitFor(() => expect(envios).toBe(1));
  expect(cuerpo).toEqual({
    tipo: 'AULA_REGULAR',
    nombre: 'Aula nueva',
    aforoMaximo: 30,
    recursos: ['Proyector', 'Pizarra'],
  });
});

test('un ambiente sin recursos se envía con la lista vacía', async () => {
  respondeAlAlta();
  await renderizar();
  await screen.findByRole('dialog');
  await userEvent.type(nombre(), 'Patio');
  await userEvent.click(tipo(/Espacio especial/));
  await userEvent.type(aforo(), '100');

  await registrar();

  await waitFor(() => expect(envios).toBe(1));
  expect(cuerpo).toMatchObject({ recursos: [] });
});

test('el 409 de nombre repetido se muestra en el campo Nombre, una sola vez y sin cerrar el modal', async () => {
  respondeAlAlta(409, { message: 'Ya existe un ambiente llamado "Aula 101".' });
  await renderizar();
  await screen.findByRole('dialog');
  await userEvent.type(nombre(), 'Aula 101');
  await userEvent.click(tipo(/Aula regular/));
  await userEvent.type(aforo(), '30');

  await registrar();

  // Una sola vez: en el campo, no repetido como notificación.
  expect(
    await screen.findAllByText('Ya existe un ambiente llamado "Aula 101".'),
  ).toHaveLength(1);
  expect(nombre()).toHaveAttribute('aria-invalid', 'true');
  expect(alCerrar).not.toHaveBeenCalled();
  expect(screen.queryByText('Ambiente registrado.')).not.toBeInTheDocument();
});

test('tras el 409, corregir el nombre y volver a enviar registra el ambiente', async () => {
  let intento = 0;
  server.use(
    http.post(`${API}/ambientes`, async ({ request }) => {
      intento++;
      cuerpo = await request.json();
      return intento === 1
        ? HttpResponse.json(
            { message: 'Ya existe un ambiente llamado "Aula 101".' },
            { status: 409 },
          )
        : HttpResponse.json(creado, { status: 201 });
    }),
  );
  await renderizar();
  await screen.findByRole('dialog');
  await userEvent.type(nombre(), 'Aula 101');
  await userEvent.click(tipo(/Aula regular/));
  await userEvent.type(aforo(), '30');
  await registrar();
  await screen.findByText('Ya existe un ambiente llamado "Aula 101".');

  await userEvent.clear(nombre());
  await userEvent.type(nombre(), 'Aula 102');
  await registrar();

  await waitFor(() => expect(alCerrar).toHaveBeenCalledTimes(1));
  expect(cuerpo).toMatchObject({ nombre: 'Aula 102' });
  expect(
    screen.queryByText('Ya existe un ambiente llamado "Aula 101".'),
  ).not.toBeInTheDocument();
});

test('otro error del servidor se muestra en una alerta del modal con su mensaje', async () => {
  respondeAlAlta(500, { message: 'Ocurrió un error inesperado.' });
  await renderizar();
  await screen.findByRole('dialog');
  await llenarElModalDeFigma();

  await registrar();

  expect(
    await within(screen.getByRole('dialog')).findByRole('alert'),
  ).toHaveTextContent('Ocurrió un error inesperado.');
  expect(alCerrar).not.toHaveBeenCalled();
});

test('si no hay respuesta del servidor, la alerta dice que se vuelva a intentar', async () => {
  server.use(http.post(`${API}/ambientes`, () => HttpResponse.error()));
  await renderizar();
  await screen.findByRole('dialog');
  await llenarElModalDeFigma();

  await registrar();

  expect(
    await within(screen.getByRole('dialog')).findByRole('alert'),
  ).toHaveTextContent('No pudimos guardar el ambiente. Inténtalo de nuevo.');
});

test('Cancelar cierra sin enviar nada', async () => {
  respondeAlAlta();
  await renderizar();
  await screen.findByRole('dialog');
  await userEvent.type(nombre(), 'Algo');

  await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

  expect(alCerrar).toHaveBeenCalledTimes(1);
  expect(envios).toBe(0);
});

// ---------- Editar ambiente ----------

test('editar abre con los datos del ambiente, el tipo bloqueado y el botón «Guardar cambios»', async () => {
  await renderizar(aula101);

  const modal = within(await screen.findByRole('dialog'));
  expect(modal.getByText('Editar ambiente físico')).toBeInTheDocument();
  expect(nombre()).toHaveValue('Aula 101');
  expect(aforo()).toHaveValue('30');
  expect(recursos()).toHaveValue('Pizarra acrílica, Proyector');
  // El tipo no cambia: se ve el del ambiente y las dos opciones están bloqueadas.
  expect(tipo(/Aula regular/)).toBeChecked();
  expect(tipo(/Aula regular/)).toBeDisabled();
  expect(tipo(/Espacio especial/)).not.toBeChecked();
  expect(tipo(/Espacio especial/)).toBeDisabled();
  expect(modal.getByText('El tipo no se puede cambiar.')).toBeInTheDocument();
  expect(
    modal.getByRole('button', { name: 'Guardar cambios' }),
  ).toBeInTheDocument();
  expect(
    modal.queryByRole('button', { name: 'Registrar ambiente' }),
  ).not.toBeInTheDocument();
  // El código tampoco se muestra al editar.
  expect(modal.queryByText('A-101')).not.toBeInTheDocument();
});

test('guardar los cambios envía solo nombre, aforo y recursos, avisa y cierra', async () => {
  respondeALaEdicion();
  await renderizar(aula101);
  await screen.findByRole('dialog');
  await userEvent.clear(nombre());
  await userEvent.type(nombre(), 'Aula 101 renovada');
  await userEvent.clear(aforo());
  await userEvent.type(aforo(), '40');
  await userEvent.clear(recursos());
  await userEvent.type(recursos(), 'Pizarra digital, Proyector');

  await guardar();

  await waitFor(() => expect(alCerrar).toHaveBeenCalledTimes(1));
  expect(cuerpo).toEqual({
    nombre: 'Aula 101 renovada',
    aforoMaximo: 40,
    recursos: ['Pizarra digital', 'Proyector'],
  });
  expect(cuerpo).not.toHaveProperty('tipo');
  expect(cuerpo).not.toHaveProperty('codigo');
  expect(await screen.findByText('Cambios guardados.')).toBeInTheDocument();
});

test('al editar se puede dejar el ambiente sin recursos', async () => {
  respondeALaEdicion();
  await renderizar(aula101);
  await screen.findByRole('dialog');
  await userEvent.clear(recursos());

  await guardar();

  await waitFor(() => expect(envios).toBe(1));
  expect(cuerpo).toMatchObject({ recursos: [] });
});

test('un aula no puede pasar de 45 al editarla, aunque el tipo no se pueda tocar', async () => {
  respondeALaEdicion();
  await renderizar(aula101);
  await screen.findByRole('dialog');
  await userEvent.clear(aforo());
  await userEvent.type(aforo(), '46');

  await guardar();

  expect(
    await screen.findByText('Un aula regular admite como máximo 45.'),
  ).toBeInTheDocument();
  expect(envios).toBe(0);
});

test('un espacio especial sí puede pasar de 45 al editarlo', async () => {
  respondeALaEdicion();
  await renderizar({
    ...aula101,
    tipo: 'ESPACIO_ESPECIAL',
    nombre: 'Auditorio',
    aforoMaximo: 180,
  });
  await screen.findByRole('dialog');
  await userEvent.clear(aforo());
  await userEvent.type(aforo(), '300');

  await guardar();

  await waitFor(() => expect(envios).toBe(1));
  expect(cuerpo).toMatchObject({ aforoMaximo: 300 });
});

test('el 409 de nombre repetido al editar también se muestra en el campo Nombre', async () => {
  respondeALaEdicion(409, {
    message: 'Ya existe un ambiente llamado "Aula 102".',
  });
  await renderizar(aula101);
  await screen.findByRole('dialog');
  await userEvent.clear(nombre());
  await userEvent.type(nombre(), 'Aula 102');

  await guardar();

  expect(
    await screen.findAllByText('Ya existe un ambiente llamado "Aula 102".'),
  ).toHaveLength(1);
  expect(nombre()).toHaveAttribute('aria-invalid', 'true');
  expect(alCerrar).not.toHaveBeenCalled();
});

test('bajar el aforo por debajo de la matrícula (409) se muestra en el campo Aforo máximo', async () => {
  const mensaje =
    'No puedes bajar el aforo a 2: la sección A de 1.° de secundaria tiene 3 estudiantes matriculados.';
  respondeALaEdicion(409, { message: mensaje });
  await renderizar(aula101);
  await screen.findByRole('dialog');
  await userEvent.clear(aforo());
  await userEvent.type(aforo(), '2');

  await guardar();

  expect(await screen.findAllByText(mensaje)).toHaveLength(1);
  expect(aforo()).toHaveAttribute('aria-invalid', 'true');
  expect(nombre()).not.toHaveAttribute('aria-invalid', 'true');
  expect(alCerrar).not.toHaveBeenCalled();
});
