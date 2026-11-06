import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';

import { api } from '../api-client';

const ubicacionOriginal = window.location;
const pantalla = 'http://localhost:3000/app/apoderado/horarios';

beforeEach(() => {
  // jsdom no navega: se reemplaza location para ver a dónde mandaría la app.
  const url = new URL(pantalla);
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: {
      href: url.href,
      origin: url.origin,
      protocol: url.protocol,
      host: url.host,
      hostname: url.hostname,
      port: url.port,
      pathname: url.pathname,
      search: '',
      hash: '',
    },
  });
});

afterEach(() => {
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: ubicacionOriginal,
  });
});

const recursoCon401 = http.get(`${env.API_URL}/recurso`, () =>
  HttpResponse.json({ message: 'No autorizado' }, { status: 401 }),
);

test('un 401 de un endpoint con la sesión vigente no manda al login (evita el bucle)', async () => {
  server.use(
    recursoCon401,
    http.get(`${env.API_URL}/auth/me`, () =>
      HttpResponse.json({ data: { id: '1' } }),
    ),
  );

  await expect(api.get('/recurso')).rejects.toBeTruthy();
  expect(window.location.href).toBe(pantalla);
});

test('un 401 con la sesión vencida manda al login y vuelve a la misma pantalla', async () => {
  server.use(
    recursoCon401,
    http.get(`${env.API_URL}/auth/me`, () =>
      HttpResponse.json({ message: 'No autorizado' }, { status: 401 }),
    ),
  );

  await expect(api.get('/recurso')).rejects.toBeTruthy();
  expect(window.location.href).toBe(
    `/auth/login?redirectTo=${encodeURIComponent('/app/apoderado/horarios')}`,
  );
});

test('acepta cualquier tipo de contenido cuando descarga un archivo', async () => {
  let accept = '';
  server.use(
    http.get(`${env.API_URL}/archivo`, ({ request }) => {
      accept = request.headers.get('Accept') ?? '';
      return new HttpResponse('contenido', {
        headers: { 'Content-Type': 'text/csv' },
      });
    }),
  );

  await api.get('/archivo', { responseType: 'blob' });

  expect(accept).toBe('*/*');
});

test('mantiene JSON como contenido esperado en las consultas normales', async () => {
  let accept = '';
  server.use(
    http.get(`${env.API_URL}/consulta`, ({ request }) => {
      accept = request.headers.get('Accept') ?? '';
      return HttpResponse.json({ data: [] });
    }),
  );

  await api.get('/consulta');

  expect(accept).toBe('application/json');
});
