import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import { createUser, renderApp, screen, userEvent } from '@/testing/test-utils';
import { DatosPersona } from '@/types/api';

import { MisDatos } from '../mis-datos';

// «Mis datos» (ampliación de HU-9): primero los datos tal cual; los campos aparecen recién con «Editar datos». Igual
// para todos los roles (la familia del apoderado está en «Datos de la Familia»). GET/PUT /auth/mis-datos.

const RUTA = `${env.API_URL}/auth/mis-datos`;

const DATOS: DatosPersona = {
  tipoDocumento: 'DNI',
  numeroDocumento: '45678912',
  apellidoPaterno: 'Flores',
  apellidoMaterno: 'Rojas',
  nombres: 'Ana',
  fechaNacimiento: '1990-04-12',
  nacionalidad: 'Peruana',
  lugarNacimiento: 'Perú',
  sexo: 'FEMENINO',
  estadoCivil: 'Soltero(a)',
  gradoInstruccion: 'Superior',
  celular: '999888777',
  correo: 'ana.flores@galiledu.edu.pe',
  telefonoCasa: '014445555',
  profesion: 'Docente',
  centroTrabajo: 'Colegio Galileo',
  cargo: 'Docente',
  distrito: 'Lince',
  direccion: 'Av. Arequipa 2500',
  esExalumno: false,
};

const conDatos = (datos: DatosPersona = DATOS) =>
  server.use(http.get(RUTA, () => HttpResponse.json(datos)));

/** El rol activo sale de la URL (/app/<rol>/mis-datos). */
const renderMisDatos = async (
  roles: ROLES[] = [ROLES.DOCENTE],
  segmento = 'docente',
) =>
  renderApp(<MisDatos />, {
    user: await createUser({ roles }),
    url: `/app/${segmento}/mis-datos`,
    path: `/app/${segmento}/mis-datos`,
  });

test('primero muestra los datos tal cual, sin campos para escribir', async () => {
  conDatos();
  await renderMisDatos();

  expect(await screen.findByText('Ana')).toBeInTheDocument();
  expect(screen.getByText('DNI 45678912')).toBeInTheDocument();
  expect(screen.getByText('ana.flores@galiledu.edu.pe')).toBeInTheDocument();
  expect(screen.getByText('12/04/1990')).toBeInTheDocument();
  expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'Guardar cambios' }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole('link', { name: 'Cambiar contraseña' }),
  ).toHaveAttribute('href', '/app/cambiar-contrasena');
});

test('«Editar datos» muestra los campos; al guardar vuelve a la vista con los datos nuevos', async () => {
  conDatos();
  let enviado: DatosPersona | undefined;
  server.use(
    http.put(RUTA, async ({ request }) => {
      enviado = (await request.json()) as DatosPersona;
      conDatos(enviado);
      return HttpResponse.json(await createUser({ roles: [ROLES.DOCENTE] }));
    }),
  );
  await renderMisDatos();

  await userEvent.click(
    await screen.findByRole('button', { name: 'Editar datos' }),
  );
  const nombres = screen.getByDisplayValue('Ana');
  await userEvent.clear(nombres);
  await userEvent.type(nombres, 'Ana Lucía');
  await userEvent.click(
    screen.getByRole('button', { name: 'Guardar cambios' }),
  );

  await screen.findByText('Tus datos se actualizaron correctamente.');
  expect(enviado?.nombres).toBe('Ana Lucía');
  expect(await screen.findByText('Ana Lucía')).toBeInTheDocument();
  expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
});

test('«Cancelar» descarta lo escrito y no guarda nada', async () => {
  conDatos();
  let guardo = false;
  server.use(
    http.put(RUTA, () => {
      guardo = true;
      return HttpResponse.json({});
    }),
  );
  await renderMisDatos();

  await userEvent.click(
    await screen.findByRole('button', { name: 'Editar datos' }),
  );
  const nombres = screen.getByDisplayValue('Ana');
  await userEvent.clear(nombres);
  await userEvent.type(nombres, 'Cambio por accidente');
  await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

  expect(screen.getByText('Ana')).toBeInTheDocument();
  expect(screen.queryByText('Cambio por accidente')).not.toBeInTheDocument();
  expect(guardo).toBe(false);

  // Al volver a editar, el formulario parte de lo guardado.
  await userEvent.click(screen.getByRole('button', { name: 'Editar datos' }));
  expect(screen.getByDisplayValue('Ana')).toBeInTheDocument();
});

test('conserva «exalumno» aunque la casilla no se muestre (solo se pide a apoderados)', async () => {
  conDatos({ ...DATOS, esExalumno: true });
  let enviado: DatosPersona | undefined;
  server.use(
    http.put(RUTA, async ({ request }) => {
      enviado = (await request.json()) as DatosPersona;
      return HttpResponse.json(await createUser({ roles: [ROLES.DOCENTE] }));
    }),
  );
  await renderMisDatos();

  await userEvent.click(
    await screen.findByRole('button', { name: 'Editar datos' }),
  );
  expect(screen.queryByText(/exalumno/i)).not.toBeInTheDocument();
  await userEvent.click(
    screen.getByRole('button', { name: 'Guardar cambios' }),
  );

  await screen.findByText('Tus datos se actualizaron correctamente.');
  expect(enviado?.esExalumno).toBe(true);
});

test('es igual para el apoderado: «Mis datos» no muestra la familia', async () => {
  conDatos();
  let pidioFamilia = false;
  server.use(
    http.get(`${env.API_URL}/auth/mi-familia`, () => {
      pidioFamilia = true;
      return HttpResponse.json([]);
    }),
  );
  await renderMisDatos([ROLES.APODERADO], 'apoderado');

  await screen.findByText('Ana');
  expect(screen.getByText('Exalumno(a) del colegio')).toBeInTheDocument();
  expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
  expect(pidioFamilia).toBe(false);
});
