import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { api } from '@/lib/api-client';
import { server } from '@/testing/mocks/server';

import {
  descargarPlantilla,
  emparejarPlantillas,
  quitarPlantilla,
  subirPlantilla,
  subirPlantillas,
} from '../plantillas';

import '../../__tests__/polyfill-blob';

// Plantillas de los documentos requeridos (HU-23): se suben después de guardar el periodo.

const API = `${env.API_URL}/configuracion/periodos-matricula/periodo-1/documentos`;

const csv = (nombre: string) =>
  new File(['nombre,dni\nAna,1\n'], nombre, { type: 'text/csv' });

const documentoVista = (
  id: string,
  nombre: string,
  tienePlantilla: boolean,
) => ({
  id,
  nombre,
  descripcion: null,
  orden: 1,
  tienePlantilla,
  plantillaNombreArchivo: tienePlantilla ? 'ficha.csv' : null,
  tieneEntregas: false,
});

describe('emparejarPlantillas', () => {
  const guardados = [
    { id: 'doc-1', nombre: 'Ficha de matrícula' },
    { id: 'doc-2', nombre: 'DNI del estudiante' },
  ];

  test('une cada archivo con su documento por nombre, no por posición', () => {
    const ficha = csv('ficha.csv');
    const dni = csv('dni.csv');

    const resultado = emparejarPlantillas(
      [
        { nombre: 'DNI del estudiante', archivo: dni },
        { nombre: 'Ficha de matrícula', archivo: ficha },
      ],
      guardados,
    );

    expect(resultado).toEqual([
      { documentoId: 'doc-2', nombre: 'DNI del estudiante', archivo: dni },
      { documentoId: 'doc-1', nombre: 'Ficha de matrícula', archivo: ficha },
    ]);
  });

  test('ignora mayúsculas y espacios en los bordes, como el backend', () => {
    const ficha = csv('ficha.csv');

    const resultado = emparejarPlantillas(
      [{ nombre: '  ficha DE matrícula ', archivo: ficha }],
      guardados,
    );

    expect(resultado.map((r) => r.documentoId)).toEqual(['doc-1']);
  });

  test('descarta un archivo cuyo documento no quedó guardado', () => {
    expect(
      emparejarPlantillas(
        [{ nombre: 'Otro documento', archivo: csv('otro.csv') }],
        guardados,
      ),
    ).toEqual([]);
  });
});

// Bajo jsdom, MSW recibe el FormData como "[object FormData]" en Node 24 (y en Node 20 solo con el parche de Blob),
// así que su cuerpo multipart no se puede leer ahí en las dos versiones. Lo que se envía se comprueba en la llamada a
// axios; el navegador arma el multipart con ese FormData.
test('subirPlantilla envía el archivo en la parte "archivo"', async () => {
  const respuesta = documentoVista('doc-1', 'Ficha', true);
  const post = vi.spyOn(api, 'post').mockResolvedValueOnce(respuesta);

  const documento = await subirPlantilla(
    'periodo-1',
    'doc-1',
    csv('ficha.csv'),
  );

  expect(post).toHaveBeenCalledTimes(1);
  const [ruta, datos] = post.mock.calls[0];
  expect(ruta).toBe(
    '/configuracion/periodos-matricula/periodo-1/documentos/doc-1/plantilla',
  );
  expect(datos).toBeInstanceOf(FormData);
  const archivo = (datos as FormData).get('archivo') as File;
  expect(archivo.name).toBe('ficha.csv');
  expect(archivo.type).toBe('text/csv');
  expect(await archivo.text()).toBe('nombre,dni\nAna,1\n');
  expect(documento).toEqual(respuesta);
  post.mockRestore();
});

test('quitarPlantilla pide el DELETE del documento', async () => {
  let metodo = '';
  server.use(
    http.delete(`${API}/doc-1/plantilla`, ({ request }) => {
      metodo = request.method;
      return HttpResponse.json(documentoVista('doc-1', 'Ficha', false));
    }),
  );

  const documento = await quitarPlantilla('periodo-1', 'doc-1');

  expect(metodo).toBe('DELETE');
  expect(documento.tienePlantilla).toBe(false);
});

describe('subirPlantillas', () => {
  test('las sube en orden y devuelve sin fallos', async () => {
    const orden: string[] = [];
    server.use(
      http.post(`${API}/:documentoId/plantilla`, ({ params }) => {
        orden.push(String(params.documentoId));
        return HttpResponse.json(documentoVista('x', 'X', true));
      }),
    );

    const resultado = await subirPlantillas('periodo-1', [
      { documentoId: 'doc-1', nombre: 'Ficha', archivo: csv('a.csv') },
      { documentoId: 'doc-2', nombre: 'DNI', archivo: csv('b.csv') },
    ]);

    expect(orden).toEqual(['doc-1', 'doc-2']);
    expect(resultado).toEqual({ fallidas: [] });
  });

  test('si una falla sigue con las demás y dice cuál falló', async () => {
    const orden: string[] = [];
    server.use(
      http.post(`${API}/:documentoId/plantilla`, ({ params }) => {
        orden.push(String(params.documentoId));
        return params.documentoId === 'doc-1'
          ? HttpResponse.json(
              { message: 'La plantilla debe ser un archivo XLSX, XLS o CSV.' },
              { status: 400 },
            )
          : HttpResponse.json(documentoVista('doc-2', 'DNI', true));
      }),
    );

    const resultado = await subirPlantillas('periodo-1', [
      { documentoId: 'doc-1', nombre: 'Ficha', archivo: csv('a.csv') },
      { documentoId: 'doc-2', nombre: 'DNI', archivo: csv('b.csv') },
    ]);

    expect(orden).toEqual(['doc-1', 'doc-2']);
    expect(resultado).toEqual({ fallidas: ['Ficha'] });
  });

  test('sin pendientes no hace ninguna petición', async () => {
    // Sin handlers: cualquier petición haría fallar la prueba (onUnhandledRequest: 'error').
    expect(await subirPlantillas('periodo-1', [])).toEqual({ fallidas: [] });
  });
});

test('descargarPlantilla guarda el archivo con el nombre que se subió', async () => {
  server.use(
    http.get(
      `${API}/doc-1/plantilla`,
      () =>
        new HttpResponse('nombre,dni\n', {
          headers: { 'Content-Type': 'text/csv' },
        }),
    ),
  );
  const crear = vi.fn(() => 'blob:plantilla');
  const liberar = vi.fn();
  // jsdom no trae estos dos métodos: se agregan y se quitan al terminar.
  const original = {
    crear: URL.createObjectURL,
    liberar: URL.revokeObjectURL,
  };
  URL.createObjectURL = crear;
  URL.revokeObjectURL = liberar;
  const enlaces: HTMLAnchorElement[] = [];
  const clic = vi
    .spyOn(HTMLAnchorElement.prototype, 'click')
    .mockImplementation(function (this: HTMLAnchorElement) {
      enlaces.push(this);
    });

  try {
    await descargarPlantilla('periodo-1', 'doc-1', 'ficha.csv');

    expect(crear).toHaveBeenCalledTimes(1);
    expect(enlaces).toHaveLength(1);
    expect(enlaces[0].download).toBe('ficha.csv');
    expect(enlaces[0].href).toBe('blob:plantilla');
    expect(liberar).toHaveBeenCalledWith('blob:plantilla');
  } finally {
    clic.mockRestore();
    URL.createObjectURL = original.crear;
    URL.revokeObjectURL = original.liberar;
  }
});
