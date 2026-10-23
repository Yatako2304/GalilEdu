import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import { createUser, renderApp, screen, userEvent } from '@/testing/test-utils';
import { AnioEscolarDetalle } from '@/types/api';

import { ContenidoAnio } from '../contenido-anio';

import { anio2026, anio2027 } from './datos';

// Figma > CI-03, CI-06, CI-09 y CI-12: requisitos (solo con el año PLANIFICADO), pestañas y su contenido.

const API = env.API_URL;

const servirRequisitos = () =>
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([]),
    ),
    http.get(`${API}/configuracion/anios-escolares/anio-2027/requisitos`, () =>
      HttpResponse.json({
        puedeActivar: true,
        obligatorios: [
          {
            codigo: 'SECCIONES',
            texto: 'Secciones creadas',
            cumplido: true,
            detalle: '23 secciones activas en 11 grados',
          },
          {
            codigo: 'ESTRUCTURA_HORARIA',
            texto: 'Estructura horaria registrada',
            cumplido: true,
            detalle: 'EH-PRI-27 · EH-SEC-27',
          },
        ],
        recomendados: [],
      }),
    ),
  );

const servirSecciones = (anioId: string) =>
  server.use(
    http.get(`${API}/configuracion/anios-escolares/${anioId}/secciones`, () =>
      HttpResponse.json([]),
    ),
  );

const mostrar = async (anio: AnioEscolarDetalle) =>
  renderApp(<ContenidoAnio anio={anio} />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

test('muestra las tres pestañas con sus contadores y abre en Periodos', async () => {
  await mostrar(anio2026);

  expect(screen.getAllByRole('tab').map((p) => p.textContent)).toEqual([
    'Periodos (4)',
    'Secciones (23)',
    'Estructura horaria (2)',
  ]);
  expect(screen.getByRole('tab', { name: 'Periodos (4)' })).toHaveAttribute(
    'aria-selected',
    'true',
  );
  expect(
    screen.getByRole('heading', { name: 'Periodos académicos' }),
  ).toBeInTheDocument();
});

test('cada pestaña muestra el contenido de su pantalla de Figma', async () => {
  servirSecciones('anio-2026');
  await mostrar(anio2026);

  await userEvent.click(screen.getByRole('tab', { name: 'Secciones (23)' }));
  expect(screen.getByRole('tab', { name: 'Secciones (23)' })).toHaveAttribute(
    'aria-selected',
    'true',
  );
  expect(
    await screen.findByRole('heading', { name: 'Secciones por grado' }),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole('heading', { name: 'Periodos académicos' }),
  ).not.toBeInTheDocument();

  await userEvent.click(
    screen.getByRole('tab', { name: 'Estructura horaria (2)' }),
  );
  expect(screen.getByText('HU-44')).toBeInTheDocument();
  expect(
    screen.queryByRole('heading', { name: 'Secciones por grado' }),
  ).not.toBeInTheDocument();
  expect(
    screen.getByRole('link', { name: /Ver diseño en Figma/ }),
  ).toHaveAttribute('href', expect.stringContaining('850-10152'));

  await userEvent.click(screen.getByRole('tab', { name: 'Periodos (4)' }));
  expect(
    screen.getByRole('heading', { name: 'Periodos académicos' }),
  ).toBeInTheDocument();
});

test('año ACTIVO: sin la tarjeta de requisitos, y sin pedirlos', async () => {
  // Sin handlers: pedir los requisitos de un año activo haría fallar la prueba.
  await mostrar(anio2026);

  expect(
    screen.queryByRole('heading', { name: 'Antes de activar el año' }),
  ).not.toBeInTheDocument();
});

test('año PLANIFICADO: muestra los requisitos sobre las pestañas y "Revisar" cambia de pestaña', async () => {
  servirRequisitos();
  servirSecciones('anio-2027');
  await mostrar(anio2027);

  expect(
    await screen.findByRole('heading', { name: 'Antes de activar el año' }),
  ).toBeInTheDocument();
  expect(screen.getByRole('tab', { name: 'Periodos (3)' })).toHaveAttribute(
    'aria-selected',
    'true',
  );

  await userEvent.click(
    screen.getByRole('button', { name: 'Revisar Secciones creadas' }),
  );
  expect(screen.getByRole('tab', { name: 'Secciones (23)' })).toHaveAttribute(
    'aria-selected',
    'true',
  );
  expect(
    await screen.findByRole('heading', { name: 'Secciones por grado' }),
  ).toBeInTheDocument();

  await userEvent.click(
    screen.getByRole('button', {
      name: 'Revisar Estructura horaria registrada',
    }),
  );
  expect(
    screen.getByRole('tab', { name: 'Estructura horaria (2)' }),
  ).toHaveAttribute('aria-selected', 'true');
  expect(screen.getByText('HU-44')).toBeInTheDocument();
});
