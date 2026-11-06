import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { api } from '@/lib/api-client';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent, waitFor } from '@/testing/test-utils';
import {
  ArchivoGuardado,
  DocumentosApoderado as DocumentosDto,
} from '@/types/api';

import { DocumentosApoderado } from '../documentos-apoderado';

// HU-28 y HU-29: el apoderado descarga la plantilla, sube el firmado y lo envía a validación.

const URL = `${env.API_URL}/matricula/apoderado/estudiantes/mateo`;

const pendiente = (id: string, nombre: string, tienePlantilla = false) => ({
  id,
  nombre,
  descripcion: null,
  tienePlantilla,
  archivo: null,
});

const subido = (
  id: string,
  nombre: string,
  estado: ArchivoGuardado['estado'] = 'EN_REVISION',
) => ({
  id,
  nombre,
  descripcion: null,
  tienePlantilla: false,
  archivo: {
    nombre: 'firmado.pdf',
    tipoMime: 'application/pdf',
    tamanoBytes: 2048,
    estado,
    observacion: null,
    subidoEn: '2027-01-10T10:00:00-05:00',
  },
});

const servir = (datos: DocumentosDto) =>
  server.use(http.get(`${URL}/documentos`, () => HttpResponse.json(datos)));

const abrir = () => renderApp(<DocumentosApoderado estudianteId="mateo" />);

test('lista los documentos pendientes con su plantilla y no deja enviar', async () => {
  servir({
    estado: 'SECCION_RESERVADA',
    puedeSubir: true,
    documentos: [
      pendiente('d-1', 'Ficha de matrícula', true),
      pendiente('d-2', 'DNI del estudiante'),
    ],
  });
  await abrir();

  expect(await screen.findByText('Ficha de matrícula')).toBeVisible();
  expect(screen.getAllByText('Pendiente')).toHaveLength(2);
  expect(
    screen.getByRole('button', {
      name: 'Descargar plantilla de Ficha de matrícula',
    }),
  ).toBeVisible();
  expect(
    screen.queryByRole('button', {
      name: 'Descargar plantilla de DNI del estudiante',
    }),
  ).not.toBeInTheDocument();
  expect(screen.getByText('Te faltan 2 documento(s) por subir.')).toBeVisible();
  expect(
    screen.getByRole('button', { name: 'Enviar a validación' }),
  ).toBeDisabled();
});

test('sube el documento firmado y rechaza un archivo que no es PDF ni imagen', async () => {
  servir({
    estado: 'SECCION_RESERVADA',
    puedeSubir: true,
    documentos: [pendiente('d-1', 'Ficha de matrícula')],
  });
  const put = vi.spyOn(api, 'put').mockResolvedValue({
    estado: 'SECCION_RESERVADA',
    puedeSubir: true,
    documentos: [subido('d-1', 'Ficha de matrícula')],
  });
  await abrir();

  await userEvent.click(
    await screen.findByRole('button', { name: 'Subir Ficha de matrícula' }),
  );
  await userEvent.upload(
    screen.getByLabelText('Archivo del documento'),
    new File(['x'], 'nota.txt', { type: 'text/plain' }),
    { applyAccept: false },
  );
  expect(
    await screen.findByText('El archivo debe ser PDF, JPG o PNG.'),
  ).toBeVisible();
  expect(put).not.toHaveBeenCalled();

  await userEvent.click(
    screen.getByRole('button', { name: 'Subir Ficha de matrícula' }),
  );
  await userEvent.upload(
    screen.getByLabelText('Archivo del documento'),
    new File(['contenido'], 'firmado.pdf', { type: 'application/pdf' }),
  );

  await waitFor(() => expect(put).toHaveBeenCalled());
  expect(put.mock.calls[0][0]).toBe(
    '/matricula/apoderado/estudiantes/mateo/documentos/d-1',
  );
  expect(((put.mock.calls[0][1] as FormData).get('archivo') as File).name).toBe(
    'firmado.pdf',
  );
  expect(await screen.findByText('En revisión')).toBeVisible();
  expect(
    screen.getByRole('button', { name: 'Reemplazar Ficha de matrícula' }),
  ).toBeVisible();
});

test('con todos los documentos subidos envía a validación', async () => {
  servir({
    estado: 'SECCION_RESERVADA',
    puedeSubir: true,
    documentos: [subido('d-1', 'Ficha de matrícula')],
  });
  const post = vi.spyOn(api, 'post').mockResolvedValue({});
  await abrir();

  expect(
    await screen.findByText('Ya subiste todos los documentos.'),
  ).toBeVisible();
  await userEvent.click(
    screen.getByRole('button', { name: 'Enviar a validación' }),
  );

  await waitFor(() =>
    expect(post).toHaveBeenCalledWith(
      '/matricula/apoderado/estudiantes/mateo/enviar-validacion',
    ),
  );
  expect(await screen.findByText('Enviado a validación')).toBeVisible();
});

test('en validación solo se ven los documentos, sin acciones de carga', async () => {
  servir({
    estado: 'EN_VALIDACION',
    puedeSubir: false,
    documentos: [
      subido('d-1', 'Ficha de matrícula'),
      subido('d-2', 'DNI del estudiante', 'APROBADO'),
    ],
  });
  await abrir();

  expect(await screen.findByText('Aprobado')).toBeVisible();
  expect(screen.getByText('En revisión')).toBeVisible();
  expect(
    screen.getByRole('button', { name: 'Ver Ficha de matrícula' }),
  ).toBeVisible();
  expect(
    screen.queryByRole('button', { name: /Reemplazar/ }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'Enviar a validación' }),
  ).not.toBeInTheDocument();
});

test('con un documento observado solo se reemplaza ese y no deja enviar hasta corregirlo', async () => {
  const base = subido('d-2', 'DNI del estudiante', 'OBSERVADO');
  const observado = {
    ...base,
    archivo: { ...base.archivo, observacion: 'La foto está borrosa' },
  };
  servir({
    estado: 'SECCION_RESERVADA',
    puedeSubir: true,
    documentos: [subido('d-1', 'Ficha de matrícula', 'APROBADO'), observado],
  });
  await abrir();

  expect(
    await screen.findByText('El colegio observó 1 documento(s)'),
  ).toBeVisible();
  expect(screen.getByText(/La foto está borrosa/)).toBeVisible();
  expect(
    screen.queryByRole('button', { name: 'Reemplazar Ficha de matrícula' }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'Reemplazar DNI del estudiante' }),
  ).toBeEnabled();
  expect(
    screen.getByRole('button', { name: 'Enviar a validación' }),
  ).toBeDisabled();
});

// Figma > Apoderado > HU-28: sin matrícula, el apoderado la inicia él mismo. Los archivos quedan en la pantalla y
// «Enviar a validación» los manda todos juntos; recién ahí se crea la matrícula y le llega al colegio.
test('sin matrícula guarda los archivos en la pantalla y al enviar crea la matrícula con todos', async () => {
  servir({
    estado: null,
    puedeSubir: true,
    documentos: [
      pendiente('d-1', 'Ficha de matrícula'),
      pendiente('d-2', 'DNI del estudiante'),
    ],
  });
  const put = vi.spyOn(api, 'put');
  const post = vi.spyOn(api, 'post').mockResolvedValue({});
  await abrir();

  expect(
    await screen.findByText(/Se guardan al enviarlos a validación/),
  ).toBeVisible();
  await userEvent.click(
    screen.getByRole('button', { name: 'Subir Ficha de matrícula' }),
  );
  await userEvent.upload(
    screen.getByLabelText('Archivo del documento'),
    new File(['a'], 'ficha.pdf', { type: 'application/pdf' }),
  );

  // Aún no se sube nada: el archivo queda listo para enviar y falta el otro.
  expect(await screen.findByText('ficha.pdf')).toBeVisible();
  expect(screen.getByText('Por enviar')).toBeVisible();
  expect(screen.getByText('Te faltan 1 documento(s) por subir.')).toBeVisible();
  expect(
    screen.getByRole('button', { name: 'Enviar a validación' }),
  ).toBeDisabled();
  expect(put).not.toHaveBeenCalled();

  await userEvent.click(
    screen.getByRole('button', { name: 'Subir DNI del estudiante' }),
  );
  await userEvent.upload(
    screen.getByLabelText('Archivo del documento'),
    new File(['b'], 'dni.png', { type: 'image/png' }),
  );
  expect(
    await screen.findByText('Ya subiste todos los documentos.'),
  ).toBeVisible();
  await userEvent.click(
    screen.getByRole('button', { name: 'Enviar a validación' }),
  );

  await waitFor(() => expect(post).toHaveBeenCalled());
  expect(post.mock.calls[0][0]).toBe(
    '/matricula/apoderado/estudiantes/mateo/matricula',
  );
  const enviados = post.mock.calls[0][1] as FormData;
  expect((enviados.get('d-1') as File).name).toBe('ficha.pdf');
  expect((enviados.get('d-2') as File).name).toBe('dni.png');
  expect(put).not.toHaveBeenCalled();
  expect(await screen.findByText('Enviado a validación')).toBeVisible();
});
