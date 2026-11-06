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
