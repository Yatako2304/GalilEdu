/* eslint-disable testing-library/no-container --
   Los <select> nativos de los desplegables están ocultos (sr-only) y comparten etiqueta con su botón,
   así que se localizan por su "name" de react-hook-form. */
import { HttpResponse, http } from 'msw';

import { useNotifications } from '@/components/ui/notifications';
import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  fireEvent,
  renderApp,
  screen,
  userEvent,
  waitFor,
} from '@/testing/test-utils';
import { VerificacionDocumento } from '@/types/api';

import { InscripcionWizard } from '../inscripcion-wizard';

// HU-7, paso 1: el documento se revisa antes de pasar al paso 2, no al registrar la inscripción.

const anio = String(new Date().getFullYear() + 1);

// jsdom no implementa scrollTo (el wizard sube al inicio al cambiar de paso).
beforeAll(() => {
  window.scrollTo = vi.fn() as unknown as typeof window.scrollTo;
});

const respuesta = (v: VerificacionDocumento) =>
  server.use(
    http.get(`${env.API_URL}/estudiantes/documentos`, () =>
      HttpResponse.json(v),
    ),
  );

/** Llena el paso 1 con datos válidos (país distinto de Perú: no hace falta el catálogo de ubigeo). */
const llenarPaso1 = async (container: HTMLElement) => {
  const campo = (nombre: string) =>
    container.querySelector(`[name="${nombre}"]`) as HTMLInputElement;
  const poner = async (nombre: string, valor: string) => {
    // Los campos que dependen de otro (documento, grado) se habilitan después de re-renderizar.
    await waitFor(() => expect(campo(nombre)).toBeEnabled());
    fireEvent.change(campo(nombre), { target: { value: valor } });
  };

  await poner('estudiante.persona.tipoDocumento', 'DNI');
  await poner('estudiante.persona.numeroDocumento', '78945612');
  await poner('estudiante.persona.apellidoPaterno', 'Quispe');
  await poner('estudiante.persona.nombres', 'Mateo');
  await poner('estudiante.persona.sexo', 'MASCULINO');
  await poner('estudiante.persona.fechaNacimiento', '2015-05-10');
  await poner('estudiante.persona.nacionalidad', 'Peruana');
  await poner('estudiante.paisNacimiento', 'Chile');
  await poner('estudiante.persona.direccion', 'Av. La Marina 1234');
  await poner('estudiante.persona.distrito', 'San Miguel');
  await poner('estudiante.tieneInternet', 'SI');
  await poner('estudiante.lenguaMaterna', 'Castellano');
  await poner('estudiante.colegioProcedencia', 'I.E.I. Mis Pequeños Pasos');
  await poner('estudiante.gestionProcedencia', 'Privada');
  await poner('estudiante.anioAcademico', anio);
  await poner('estudiante.nivel', 'PRIMARIA');
  await poner('estudiante.grado', 'PRI-1');
  await poner('estudiante.formaIngreso', 'Edad normativa');
};

const renderWizard = async () =>
  renderApp(<InscripcionWizard onCancelar={vi.fn()} onRegistrada={vi.fn()} />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

const siguiente = () =>
  userEvent.click(
    screen.getByRole('button', { name: 'Siguiente: Ficha médica' }),
  );

test('paso 3: sin botón "Buscar", busca al salir del N.° de documento y solo si está completo', async () => {
  respuesta({ estado: 'LIBRE', permitido: true, mensaje: null });
  const consultados: string[] = [];
  server.use(
    http.get(`${env.API_URL}/estudiantes/personas`, ({ request }) => {
      consultados.push(new URL(request.url).searchParams.get('documento')!);
      return HttpResponse.json({
        personaId: 'id-luis',
        tieneCuenta: true,
        cuentaEliminada: false,
        estudiantesACargo: [],
        persona: {
          tipoDocumento: 'DNI',
          numeroDocumento: '70000003',
          apellidoPaterno: 'Paredes',
          nombres: 'Luis',
        },
      });
    }),
  );
  const { container } = await renderWizard();
  await llenarPaso1(container);
  await siguiente();
  await screen.findByText('Ficha médica del estudiante');
  for (const [nombre, valor] of [
    ['fichaMedica.tipoSangre', 'O+'],
    ['fichaMedica.pesoKg', '22.0'],
    ['fichaMedica.estaturaM', '1.18'],
    ['fichaMedica.tipoSeguro', 'EsSalud'],
    ['fichaMedica.fiebreMaxima', '39'],
  ]) {
    fireEvent.change(
      container.querySelector(`[name="${nombre}"]`) as HTMLInputElement,
      { target: { value: valor } },
    );
  }
  await userEvent.click(
    screen.getByRole('button', { name: 'Siguiente: Padres y apoderado' }),
  );
  await screen.findByText('Registra al menos al padre o a la madre');
  expect(
    screen.queryByRole('button', { name: /Buscar/ }),
  ).not.toBeInTheDocument();

  fireEvent.change(
    container.querySelector(
      '[name="padre.persona.tipoDocumento"]',
    ) as HTMLSelectElement,
    { target: { value: 'DNI' } },
  );
  const documento = container.querySelector(
    '[name="padre.persona.numeroDocumento"]',
  ) as HTMLInputElement;
  // Incompleto: al salir no se consulta.
  fireEvent.change(documento, { target: { value: '7000' } });
  fireEvent.blur(documento);
  // Completo: al salir se busca y se vincula.
  fireEvent.change(documento, { target: { value: '70000003' } });
  fireEvent.blur(documento);

  expect(await screen.findByText('Ya registrado')).toBeInTheDocument();
  expect(consultados).toEqual(['70000003']);
});

test('un documento libre deja pasar al paso 2', async () => {
  respuesta({ estado: 'LIBRE', permitido: true, mensaje: null });
  const { container } = await renderWizard();
  await llenarPaso1(container);

  await siguiente();

  expect(
    await screen.findByText('Ficha médica del estudiante'),
  ).toBeInTheDocument();
});

test('un estudiante activo no deja pasar y el aviso sale en el N.° de documento', async () => {
  respuesta({
    estado: 'ESTUDIANTE_ACTIVO',
    permitido: false,
    mensaje: 'El documento ya está registrado dentro del sistema',
  });
  const { container } = await renderWizard();
  await llenarPaso1(container);

  await siguiente();

  expect(
    await screen.findByText(
      'El documento ya está registrado dentro del sistema',
    ),
  ).toBeInTheDocument();
  expect(screen.getByText('Datos del estudiante')).toBeInTheDocument();
  expect(
    screen.queryByText('Ficha médica del estudiante'),
  ).not.toBeInTheDocument();
});

test('un estudiante retirado (inactivo) no se puede inscribir como nuevo', async () => {
  respuesta({
    estado: 'ESTUDIANTE_RETIRADO',
    permitido: false,
    mensaje:
      'El documento ya está registrado dentro del sistema: es de un estudiante inactivo (retirado). Reactívalo desde el listado.',
  });
  const { container } = await renderWizard();
  await llenarPaso1(container);

  await siguiente();

  expect(
    await screen.findByText(/estudiante inactivo \(retirado\)/),
  ).toBeInTheDocument();
  expect(
    screen.queryByText('Ficha médica del estudiante'),
  ).not.toBeInTheDocument();
});

test('un estudiante eliminado sí deja continuar (sus datos se actualizan al registrar)', async () => {
  useNotifications.setState({ notifications: [] });
  respuesta({
    estado: 'REUTILIZABLE',
    permitido: true,
    mensaje: null,
  });
  const { container } = await renderWizard();
  await llenarPaso1(container);

  await siguiente();

  expect(
    await screen.findByText('Ficha médica del estudiante'),
  ).toBeInTheDocument();
  // Pasar al Paso 2 no muestra ningún aviso: reutilizar el registro es interno.
  expect(useNotifications.getState().notifications).toHaveLength(0);
});

test('si el documento no pasa, se vuelve a consultar al corregirlo', async () => {
  let consultas = 0;
  server.use(
    http.get(`${env.API_URL}/estudiantes/documentos`, () => {
      consultas++;
      return HttpResponse.json(
        consultas === 1
          ? {
              estado: 'ESTUDIANTE_ACTIVO',
              permitido: false,
              mensaje: 'Ya existe',
            }
          : { estado: 'LIBRE', permitido: true, mensaje: null },
      );
    }),
  );
  const { container } = await renderWizard();
  await llenarPaso1(container);

  await siguiente();
  expect(await screen.findByText('Ya existe')).toBeInTheDocument();

  fireEvent.change(
    container.querySelector(
      '[name="estudiante.persona.numeroDocumento"]',
    ) as HTMLInputElement,
    { target: { value: '78945699' } },
  );
  await siguiente();

  expect(
    await screen.findByText('Ficha médica del estudiante'),
  ).toBeInTheDocument();
  expect(consultas).toBe(2);
});

test('N.° de documento y Grado esperan al tipo de documento y al nivel', async () => {
  const { container } = await renderWizard();
  const campo = (nombre: string) =>
    container.querySelector(`[name="${nombre}"]`) as HTMLInputElement;

  expect(campo('estudiante.persona.numeroDocumento')).toBeDisabled();
  expect(campo('estudiante.grado')).toBeDisabled();

  fireEvent.change(campo('estudiante.persona.tipoDocumento'), {
    target: { value: 'DNI' },
  });
  fireEvent.change(campo('estudiante.nivel'), {
    target: { value: 'PRIMARIA' },
  });

  await waitFor(() =>
    expect(campo('estudiante.persona.numeroDocumento')).toBeEnabled(),
  );
  await waitFor(() => expect(campo('estudiante.grado')).toBeEnabled());
});

test('el teléfono de casa solo acepta números, hasta 15 (el largo de la base de datos)', async () => {
  const { container } = await renderWizard();
  const telefono = container.querySelector(
    '[name="estudiante.persona.telefonoCasa"]',
  ) as HTMLInputElement;

  fireEvent.change(telefono, {
    target: { value: '01 452-3344 abc 99999999999' },
  });

  expect(telefono.value).toBe('014523344999999');
});
