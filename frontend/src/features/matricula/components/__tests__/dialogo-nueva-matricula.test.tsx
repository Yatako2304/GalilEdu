import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { api } from '@/lib/api-client';
import { server } from '@/testing/mocks/server';
import {
  renderApp,
  screen,
  userEvent,
  waitFor,
  within,
} from '@/testing/test-utils';
import { EstudianteSinMatricula, PreparacionMatricula } from '@/types/api';

import { DialogoNuevaMatricula } from '../dialogo-nueva-matricula';

// HU-24 · A6 "Nueva matrícula": buscar al alumno, ver sus datos y su sección, subir documentos y guardar.

const API = `${env.API_URL}/matricula/periodos/periodo-2027`;

// Las pruebas espían `api`: se devuelve a su estado original para que no afecte a la siguiente.
afterEach(() => {
  vi.restoreAllMocks();
});

const CAMILA: EstudianteSinMatricula = {
  id: 'e-camila',
  nombre: 'Camila Torres Huamán',
  documento: '77894512',
  gradoCodigo: 'SEC-3',
  grado: '3.° de secundaria',
};

const documentos = [
  ['d-1', 'Ficha de matrícula firmada'],
  ['d-2', 'DNI del estudiante'],
].map(([id, nombre]) => ({
  id,
  nombre,
  descripcion: null,
  tienePlantilla: false,
}));

const preparacion = (
  extras: Partial<PreparacionMatricula> = {},
): PreparacionMatricula => ({
  estudianteId: 'e-camila',
  nombre: CAMILA.nombre,
  documento: CAMILA.documento,
  gradoCodigo: 'SEC-3',
  grado: CAMILA.grado,
  tipo: 'NUEVO',
  seccion: {
    id: 's-a',
    etiqueta: '3.° A',
    tutor: 'Raúl Vargas',
    capacidad: 30,
    ocupadas: 12,
  },
  apoderado: 'Lucía Huamán Paz',
  contacto: '945210778 · lhuaman@correo.com',
  contactoEmergencia: 'Rosa Medina (Abuela) · 987654321',
  informacionMedica: 'Alergias',
  documentos,
  ...extras,
});

// MSW no puede leer un multipart bajo jsdom: lo que se envía se comprueba en la llamada a axios.
const servir = (prep: PreparacionMatricula = preparacion()) => {
  const guardados: FormData[] = [];
  vi.spyOn(api, 'post').mockImplementation(async (url, datos) => {
    expect(url).toBe('/matricula/periodos/periodo-2027/matriculas');
    guardados.push(datos as FormData);
    return {
      id: 'm-1',
      estado: 'SECCION_RESERVADA',
      seccion: '3.° A',
      documentosSubidos: 1,
      seccionReasignada: false,
    };
  });
  server.use(
    http.get(`${API}/estudiantes`, () => HttpResponse.json([CAMILA])),
    http.get(`${API}/estudiantes/e-camila/preparacion`, () =>
      HttpResponse.json(prep),
    ),
  );
  return guardados;
};

const abrir = async (onCerrar = vi.fn()) => {
  await renderApp(
    <DialogoNuevaMatricula periodoId="periodo-2027" onCerrar={onCerrar} />,
  );
  await userEvent.click(await screen.findByText('Camila Torres Huamán'));
  await screen.findByText('Datos del estudiante y apoderado');
  return onCerrar;
};

test('se llama "Nueva matrícula" y empieza buscando al alumno', async () => {
  servir();
  await renderApp(
    <DialogoNuevaMatricula periodoId="periodo-2027" onCerrar={vi.fn()} />,
  );

  expect(
    await screen.findByRole('heading', { name: 'Nueva matrícula' }),
  ).toBeVisible();
  expect(screen.queryByText(/Completar matrícula/)).not.toBeInTheDocument();
  expect(await screen.findByText('Camila Torres Huamán')).toBeVisible();
  expect(screen.getByText('DNI 77894512 · 3.° de secundaria')).toBeVisible();
  expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
});

test('al elegirlo muestra sus datos, su sección y los documentos pendientes', async () => {
  servir();
  await abrir();

  expect(screen.getByText('3.° A · Tutor: Raúl Vargas')).toBeVisible();
  expect(screen.getByText('Lucía Huamán Paz')).toBeVisible();
  expect(screen.getByText('Rosa Medina (Abuela) · 987654321')).toBeVisible();
  expect(screen.getByText('Alergias')).toBeVisible();
  expect(screen.getAllByText('Pendiente')).toHaveLength(2);
  expect(screen.getByText('Ficha de matrícula firmada')).toBeVisible();
  expect(screen.getByRole('button', { name: 'Cambiar sección' })).toBeEnabled();
  expect(screen.getByRole('button', { name: 'Guardar' })).toBeEnabled();
});

test('cambia la sección propuesta y la envía al guardar', async () => {
  const guardados = servir();
  server.use(
    http.get(`${API}/estudiantes/e-camila/secciones`, () =>
      HttpResponse.json({
        grado: '3.° de secundaria',
        seleccionadaId: 's-a',
        secciones: [
          {
            id: 's-a',
            etiqueta: '3.° A',
            tutor: 'Raúl Vargas',
            capacidad: 30,
            ocupadas: 12,
          },
          {
            id: 's-b',
            etiqueta: '3.° B',
            tutor: 'Ana Ríos',
            capacidad: 30,
            ocupadas: 30,
          },
          {
            id: 's-c',
            etiqueta: '3.° C',
            tutor: null,
            capacidad: 30,
            ocupadas: 5,
          },
        ],
      }),
    ),
  );
  const onCerrar = await abrir();

  await userEvent.click(
    screen.getByRole('button', { name: 'Cambiar sección' }),
  );
  expect(
    await screen.findByRole('radio', { name: 'Sección 3.° B' }),
  ).toBeDisabled();
  await userEvent.click(screen.getByRole('radio', { name: 'Sección 3.° C' }));
  expect(screen.getByText('Efecto en las vacantes')).toBeVisible();
  await userEvent.click(
    within(screen.getByRole('dialog', { name: 'Cambiar sección' })).getByRole(
      'button',
      { name: 'Cambiar sección' },
    ),
  );
  await userEvent.click(
    within(
      await screen.findByRole('dialog', { name: '¿Cambiar sección?' }),
    ).getByRole('button', {
      name: 'Cambiar sección',
    }),
  );

  expect(await screen.findByText('3.° C')).toBeVisible();
  await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
  await waitFor(() => expect(onCerrar).toHaveBeenCalled());
  expect(guardados[0].get('seccionId')).toBe('s-c');
});

test('sube un documento, lo muestra con Ver y Reemplazar y lo envía al guardar', async () => {
  const guardados = servir();
  const onCerrar = await abrir();

  await userEvent.click(
    screen.getByRole('button', { name: 'Subir Ficha de matrícula firmada' }),
  );
  await userEvent.upload(
    screen.getByLabelText('Archivo del documento'),
    new File(['contenido'], 'ficha_firmada.pdf', { type: 'application/pdf' }),
  );

  expect(await screen.findByText('ficha_firmada.pdf')).toBeVisible();
  expect(
    screen.getByRole('button', { name: 'Ver Ficha de matrícula firmada' }),
  ).toBeVisible();
  expect(screen.getByRole('button', { name: 'Reemplazar' })).toBeVisible();
  expect(screen.getAllByText('Pendiente')).toHaveLength(1);

  await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));
  await waitFor(() => expect(onCerrar).toHaveBeenCalled());

  expect(guardados).toHaveLength(1);
  expect(guardados[0].get('estudianteId')).toBe('e-camila');
  expect((guardados[0].get('d-1') as File).name).toBe('ficha_firmada.pdf');
  expect(guardados[0].has('d-2')).toBe(false);
});

test('descarga la plantilla de un documento que la tiene', async () => {
  URL.createObjectURL = vi.fn(() => 'blob:plantilla');
  URL.revokeObjectURL = vi.fn();
  const clic = vi
    .spyOn(HTMLAnchorElement.prototype, 'click')
    .mockImplementation(() => undefined);
  servir(
    preparacion({
      documentos: [{ ...documentos[0], tienePlantilla: true }, documentos[1]],
    }),
  );
  await abrir();
  const get = vi
    .spyOn(api, 'get')
    .mockResolvedValue(new Blob(['%PDF'], { type: 'application/pdf' }));

  await userEvent.click(
    screen.getByRole('button', {
      name: 'Descargar plantilla de Ficha de matrícula firmada',
    }),
  );

  await waitFor(() => expect(clic).toHaveBeenCalled());
  expect(get).toHaveBeenCalledWith(
    '/matricula/periodos/periodo-2027/documentos/d-1/plantilla',
    { responseType: 'blob' },
  );
  expect(
    screen.queryByRole('button', {
      name: 'Descargar plantilla de DNI del estudiante',
    }),
  ).not.toBeInTheDocument();
});

test('rechaza un archivo que no es PDF ni imagen', async () => {
  servir();
  await abrir();

  await userEvent.click(
    screen.getByRole('button', { name: 'Subir DNI del estudiante' }),
  );
  await userEvent.upload(
    screen.getByLabelText('Archivo del documento'),
    new File(['x'], 'nota.txt', { type: 'text/plain' }),
    { applyAccept: false },
  );

  expect(
    await screen.findByText('El archivo debe ser PDF, JPG o PNG.'),
  ).toBeVisible();
  expect(screen.getAllByText('Pendiente')).toHaveLength(2);
});

test('sin vacantes en el grado avisa y no deja guardar', async () => {
  servir(preparacion({ seccion: null }));
  await abrir();

  expect(
    screen.getByText(
      'No hay vacantes en ninguna sección de 3.° de secundaria.',
    ),
  ).toBeVisible();
  expect(screen.getByRole('button', { name: 'Guardar' })).toBeDisabled();
});
