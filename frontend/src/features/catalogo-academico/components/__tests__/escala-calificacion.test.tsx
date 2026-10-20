import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  renderApp,
  screen,
  userEvent,
  within,
} from '@/testing/test-utils';
import { SolicitudTablaConversion, TablaConversionDetalle } from '@/types/api';

import { EscalaCalificacion } from '../escala-calificacion';

// Figma > Personal Administrativo > CI-23 · Escala de calificación (HU-55)

const niveles = (a: number, b: number) => [
  {
    nivel: 'AD' as const,
    significado: 'Logro destacado',
    notaDesde: 18,
    notaHasta: 20,
  },
  {
    nivel: 'A' as const,
    significado: 'Logro esperado',
    notaDesde: a,
    notaHasta: 17,
  },
  {
    nivel: 'B' as const,
    significado: 'En proceso',
    notaDesde: 11,
    notaHasta: b,
  },
  {
    nivel: 'C' as const,
    significado: 'En inicio',
    notaDesde: 0,
    notaHasta: 10,
  },
];

const versiones: TablaConversionDetalle[] = [
  {
    id: 't27',
    vigenteDesde: '2027-03-01',
    estado: 'PROGRAMADA',
    niveles: niveles(15, 14),
  },
  {
    id: 't25',
    vigenteDesde: '2025-03-01',
    estado: 'VIGENTE',
    niveles: niveles(14, 13),
  },
];

let enviada: SolicitudTablaConversion | null = null;

beforeEach(() => {
  enviada = null;
  server.use(
    http.get(`${env.API_URL}/tablas-conversion`, () =>
      HttpResponse.json(versiones),
    ),
    http.post(`${env.API_URL}/tablas-conversion`, async ({ request }) => {
      enviada = (await request.json()) as SolicitudTablaConversion;
      return HttpResponse.json({}, { status: 201 });
    }),
  );
});

const renderizar = async () =>
  renderApp(<EscalaCalificacion />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

test('muestra la tabla vigente y las otras versiones', async () => {
  await renderizar();

  expect(
    await screen.findByText(
      'Vigente desde el 01/03/2025. Se usa en los cursos con escala cualitativa.',
    ),
  ).toBeInTheDocument();
  const tabla = within(screen.getByRole('table'));
  expect(tabla.getByText('Logro esperado')).toBeInTheDocument();
  expect(tabla.getByText('14')).toBeInTheDocument();
  expect(screen.getByText('Desde el 01/03/2027')).toBeInTheDocument();
  expect(screen.getByText('Programada')).toBeInTheDocument();
});

test('la nueva versión parte de la vigente y valida los rangos antes de enviar', async () => {
  await renderizar();
  await userEvent.click(
    await screen.findByRole('button', { name: /nueva versión/i }),
  );
  const modal = within(await screen.findByRole('dialog'));

  await userEvent.type(modal.getByLabelText(/vigente desde/i), '2027-03-01');
  const aDesde = modal.getByLabelText('A desde');
  await userEvent.clear(aDesde);
  await userEvent.type(aDesde, '15');
  await userEvent.click(modal.getByRole('button', { name: 'Guardar versión' }));

  expect(
    await modal.findByText('Falta cubrir de 14 a 14 antes de A.'),
  ).toBeInTheDocument();
  expect(enviada).toBeNull();

  const bHasta = modal.getByLabelText('B hasta');
  await userEvent.clear(bHasta);
  await userEvent.type(bHasta, '14');
  await userEvent.click(modal.getByRole('button', { name: 'Guardar versión' }));

  await vi.waitFor(() =>
    expect(enviada).toEqual({
      vigenteDesde: '2027-03-01',
      niveles: [
        { nivel: 'AD', notaDesde: 18, notaHasta: 20 },
        { nivel: 'A', notaDesde: 15, notaHasta: 17 },
        { nivel: 'B', notaDesde: 11, notaHasta: 14 },
        { nivel: 'C', notaDesde: 0, notaHasta: 10 },
      ],
    }),
  );
});
