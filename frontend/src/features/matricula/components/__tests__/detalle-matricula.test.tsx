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
import { DetalleMatricula as DetalleDto } from '@/types/api';

import { DetalleMatricula } from '../detalle-matricula';

// HU-24 · A5 "Detalle de matrícula": datos, documentos y cambio de sección.

afterEach(() => {
  vi.restoreAllMocks();
});

const API = `${env.API_URL}/matricula/matriculas/m-1`;

const detalle = (extras: Partial<DetalleDto> = {}): DetalleDto => ({
  id: 'm-1',
  periodoId: 'periodo-2027',
  anio: 2027,
  estudianteId: 'e-1',
  nombre: 'Mateo Paredes Rojas',
  documento: '78945612',
  tipo: 'NUEVO',
  gradoCodigo: 'SEC-1',
  grado: '1.° de secundaria',
  seccion: {
    id: 's-a',
    etiqueta: '1.° A',
    tutor: 'Raúl Vargas',
    capacidad: 30,
    ocupadas: 12,
  },
  estado: 'SECCION_RESERVADA',
  codigo: null,
  pasoActual: 2,
  apoderado: 'Lucía Paredes',
  contacto: '945210778',
  contactoEmergencia: 'Rosa Medina (Abuela)',
  informacionMedica: 'Sin observaciones',
  puedeCambiarSeccion: true,
  puedeEditarDocumentos: true,
  puedeValidar: false,
  documentos: [
    {
      id: 'd-1',
      nombre: 'Ficha de matrícula firmada',
      descripcion: null,
      tienePlantilla: true,
      archivo: {
        nombre: 'ficha.pdf',
        tipoMime: 'application/pdf',
        tamanoBytes: 2048,
        estado: 'OBSERVADO',
        observacion: 'Borroso',
        subidoEn: '2026-11-01T10:00:00Z',
      },
    },
    {
      id: 'd-2',
      nombre: 'DNI del estudiante',
      descripcion: null,
      tienePlantilla: false,
      archivo: null,
    },
  ],
  ...extras,
});

const secciones = () =>
  HttpResponse.json({
    grado: '1.° de secundaria',
    seleccionadaId: 's-a',
    secciones: [
      {
        id: 's-a',
        etiqueta: '1.° A',
        tutor: 'Raúl Vargas',
        capacidad: 30,
        ocupadas: 12,
      },
      {
        id: 's-b',
        etiqueta: '1.° B',
        tutor: 'Ana Ríos',
        capacidad: 30,
        ocupadas: 8,
      },
    ],
  });

test('muestra datos, estado de cada documento y deja Validar deshabilitado', async () => {
  await renderApp(<DetalleMatricula matricula={detalle()} />);

  expect(screen.getByText('1.° A · Tutor: Raúl Vargas')).toBeVisible();
  expect(screen.getByText('Observado')).toBeVisible();
  expect(screen.getByText('Pendiente')).toBeVisible();
  expect(
    screen.getByRole('button', { name: 'Ver Ficha de matrícula firmada' }),
  ).toBeEnabled();
  expect(
    screen.getByRole('button', {
      name: 'Reemplazar Ficha de matrícula firmada',
    }),
  ).toBeVisible();
  expect(
    screen.getByRole('button', { name: 'Subir DNI del estudiante' }),
  ).toBeVisible();
  expect(screen.getByRole('button', { name: 'Validar' })).toBeDisabled();
  expect(screen.getByRole('button', { name: 'Cambiar sección' })).toBeEnabled();
});

test('con el periodo cerrado y la sección reservada no se puede cambiar de sección', async () => {
  await renderApp(
    <DetalleMatricula matricula={detalle({ puedeCambiarSeccion: false })} />,
  );
  expect(
    screen.getByRole('button', { name: 'Cambiar sección' }),
  ).toBeDisabled();
});

test('una matrícula retirada no permite subir ni reemplazar documentos', async () => {
  await renderApp(
    <DetalleMatricula
      matricula={detalle({
        estado: 'RETIRADO',
        puedeEditarDocumentos: false,
        puedeCambiarSeccion: false,
      })}
    />,
  );
  expect(
    screen.queryByRole('button', { name: /Subir|Reemplazar/ }),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'Validar' }),
  ).not.toBeInTheDocument();
});

test('sube un documento pendiente', async () => {
  const subidos: FormData[] = [];
  vi.spyOn(api, 'put').mockImplementation(async (url, datos) => {
    expect(url).toBe('/matricula/matriculas/m-1/documentos/d-2');
    subidos.push(datos as FormData);
    return detalle();
  });
  await renderApp(<DetalleMatricula matricula={detalle()} />);

  await userEvent.click(
    screen.getByRole('button', { name: 'Subir DNI del estudiante' }),
  );
  await userEvent.upload(
    screen.getByLabelText('Archivo del documento'),
    new File(['x'], 'dni.png', { type: 'image/png' }),
  );

  await waitFor(() => expect(subidos).toHaveLength(1));
  expect((subidos[0].get('archivo') as File).name).toBe('dni.png');
});

test('cambia de sección tras confirmar', async () => {
  server.use(http.get(`${API}/secciones`, secciones));
  const cambios: unknown[] = [];
  vi.spyOn(api, 'put').mockImplementation(async (url, datos) => {
    expect(url).toBe('/matricula/matriculas/m-1/seccion');
    cambios.push(datos);
    return detalle({
      seccion: {
        id: 's-b',
        etiqueta: '1.° B',
        tutor: 'Ana Ríos',
        capacidad: 30,
        ocupadas: 9,
      },
    });
  });
  await renderApp(<DetalleMatricula matricula={detalle()} />);

  await userEvent.click(
    screen.getByRole('button', { name: 'Cambiar sección' }),
  );
  expect(await screen.findByText('Sección actual')).toBeVisible();
  await userEvent.click(
    await screen.findByRole('radio', { name: 'Sección 1.° B' }),
  );
  expect(screen.getByText(/1\.° A: 12 → 11/)).toBeVisible();
  await userEvent.click(
    within(screen.getByRole('dialog', { name: 'Cambiar sección' })).getByRole(
      'button',
      { name: 'Cambiar sección' },
    ),
  );
  expect(await screen.findByText('¿Cambiar sección?')).toBeVisible();
  await userEvent.click(
    within(
      await screen.findByRole('dialog', { name: '¿Cambiar sección?' }),
    ).getByRole('button', {
      name: 'Cambiar sección',
    }),
  );

  await waitFor(() => expect(cambios).toEqual([{ seccionId: 's-b' }]));
});

test('descarga la plantilla de los documentos que la tienen', async () => {
  URL.createObjectURL = vi.fn(() => 'blob:plantilla');
  URL.revokeObjectURL = vi.fn();
  const clic = vi
    .spyOn(HTMLAnchorElement.prototype, 'click')
    .mockImplementation(() => undefined);
  await renderApp(<DetalleMatricula matricula={detalle()} />);
  const get = vi
    .spyOn(api, 'get')
    .mockResolvedValue(new Blob(['%PDF'], { type: 'application/pdf' }));

  expect(
    screen.queryByRole('button', {
      name: 'Descargar plantilla de DNI del estudiante',
    }),
  ).not.toBeInTheDocument();
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
});

const enValidacion = (extras: Partial<DetalleDto> = {}) =>
  detalle({
    estado: 'EN_VALIDACION',
    puedeValidar: true,
    documentos: [
      {
        id: 'd-1',
        nombre: 'Ficha de matrícula firmada',
        descripcion: null,
        tienePlantilla: false,
        archivo: {
          nombre: 'ficha.pdf',
          tipoMime: 'application/pdf',
          tamanoBytes: 2048,
          estado: 'EN_REVISION',
          observacion: null,
          subidoEn: '2026-11-01T10:00:00Z',
        },
      },
    ],
    ...extras,
  });

test('observa un documento en revisión con su motivo', async () => {
  let cuerpo: unknown;
  server.use(
    http.put(`${API}/documentos/d-1/observacion`, async ({ request }) => {
      cuerpo = await request.json();
      return HttpResponse.json(
        detalle({ estado: 'SECCION_RESERVADA', puedeValidar: false }),
      );
    }),
  );
  await renderApp(<DetalleMatricula matricula={enValidacion()} />);

  await userEvent.click(
    screen.getByRole('button', { name: 'Observar Ficha de matrícula firmada' }),
  );
  const dialogo = await screen.findByRole('dialog');
  const observar = within(dialogo).getByRole('button', { name: 'Observar' });
  expect(observar).toBeDisabled();
  await userEvent.type(
    within(dialogo).getByLabelText('Motivo de la observación'),
    'La firma no se ve',
  );
  await userEvent.click(observar);

  await waitFor(() =>
    expect(cuerpo).toEqual({ observacion: 'La firma no se ve' }),
  );
});

test('un documento aprobado u observado no se puede observar', async () => {
  await renderApp(<DetalleMatricula matricula={detalle()} />);
  expect(
    screen.queryByRole('button', { name: /^Observar / }),
  ).not.toBeInTheDocument();
});

test('valida la matrícula tras confirmar "¿Aprobar documentación?"', async () => {
  let llamadas = 0;
  server.use(
    http.post(`${API}/validar`, () => {
      llamadas += 1;
      return HttpResponse.json(
        detalle({ estado: 'PENDIENTE_PAGO', pasoActual: 3 }),
      );
    }),
  );
  await renderApp(<DetalleMatricula matricula={enValidacion()} />);

  await userEvent.click(screen.getByRole('button', { name: 'Validar' }));
  expect(await screen.findByText('¿Aprobar documentación?')).toBeVisible();
  await userEvent.click(screen.getByRole('button', { name: 'Aprobar' }));

  await waitFor(() => expect(llamadas).toBe(1));
});

test('Validar queda deshabilitado mientras haya un documento observado', async () => {
  await renderApp(
    <DetalleMatricula matricula={enValidacion({ puedeValidar: false })} />,
  );
  expect(screen.getByRole('button', { name: 'Validar' })).toBeDisabled();
});

test('un documento aprobado ya no se reemplaza ni muestra su plantilla', async () => {
  const base = enValidacion();
  const [documento] = base.documentos;
  await renderApp(
    <DetalleMatricula
      matricula={{
        ...base,
        documentos: [
          {
            ...documento,
            tienePlantilla: true,
            archivo: documento.archivo && {
              ...documento.archivo,
              estado: 'APROBADO',
            },
          },
        ],
      }}
    />,
  );
  expect(screen.getByText('Aprobado')).toBeVisible();
  expect(
    screen.getByRole('button', { name: 'Ver Ficha de matrícula firmada' }),
  ).toBeVisible();
  expect(
    screen.queryByRole('button', { name: /Reemplazar|plantilla/i }),
  ).not.toBeInTheDocument();
});

test('matriculado: el último paso del indicador figura completado', async () => {
  await renderApp(
    <DetalleMatricula
      matricula={detalle({
        estado: 'MATRICULADO',
        codigo: 'MAT-2027-0001',
        pasoActual: 4,
      })}
    />,
  );
  expect(document.querySelector('[aria-current="step"]')).toBeNull();
});
