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
} from '@/testing/test-utils';
import { AnioEscolarResumen } from '@/types/api';

import { BotonNuevoAnio } from '../boton-nuevo-anio';

// Figma > CI-01 · Año escolar > botón "Nuevo año escolar" y su modal

const API = env.API_URL;

// Navegar de verdad en jsdom rompe el router (su Request no acepta el AbortSignal de jsdom): se comprueba el destino.
const { navegar } = vi.hoisted(() => ({ navegar: vi.fn() }));
vi.mock('react-router', async () => ({
  ...(await vi.importActual<typeof import('react-router')>('react-router')),
  useNavigate: () => navegar,
}));

beforeEach(() => navegar.mockClear());

const resumen = (anio: number): AnioEscolarResumen => ({
  id: `anio-${anio}`,
  anio,
  fechaInicio: `${anio}-03-01`,
  fechaFin: `${anio}-12-17`,
  regimen: 'BIMESTRAL',
  cantidadPeriodos: 4,
  seccionesActivas: 10,
  estructurasHorarias: 2,
  estado: 'PLANIFICADO',
});

const mostrar = async () =>
  renderApp(<BotonNuevoAnio />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

test('abre el modal con el año siguiente al último registrado', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([resumen(2026), resumen(2027)]),
    ),
  );
  await mostrar();

  const boton = screen.getByRole('button', { name: 'Nuevo año escolar' });
  await waitFor(() => expect(boton).toBeEnabled());
  await userEvent.click(boton);

  expect(
    await screen.findByRole('heading', { name: 'Nuevo año escolar' }),
  ).toBeInTheDocument();
  expect(screen.getByLabelText('Año *')).toHaveValue('2028');
});

test('al crear el año abre su página', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([resumen(2027)]),
    ),
    http.post(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json({
        ...resumen(2028),
        cantidadSecciones: 0,
        cantidadEstructuras: 0,
        periodos: [],
      }),
    ),
  );
  await mostrar();

  const boton = screen.getByRole('button', { name: 'Nuevo año escolar' });
  await waitFor(() => expect(boton).toBeEnabled());
  await userEvent.click(boton);
  await userEvent.click(
    await screen.findByRole('button', { name: 'Crear año escolar' }),
  );

  await waitFor(() =>
    expect(navegar).toHaveBeenCalledWith(
      '/app/administrativo/anio-escolar/anio-2028',
    ),
  );
});
