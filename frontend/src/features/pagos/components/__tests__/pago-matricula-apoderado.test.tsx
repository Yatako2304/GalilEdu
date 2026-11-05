import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent, waitFor } from '@/testing/test-utils';
import { PagoMatricula } from '@/types/api';

import { PagoMatriculaApoderado } from '../pago-matricula-apoderado';

// HU-31 · Derivación a pagos (CAHU31-1, CAHU31-2, CAHU31-3 y CAHU31-6).

const pago: PagoMatricula = {
  cuotaId: 'cuota-matricula',
  concepto: 'Matrícula 2027',
  monto: 850,
  fechaVencimiento: '2027-01-31',
  estado: 'PENDIENTE',
  responsable: 'Luis Paredes',
  puedePagar: true,
  motivo: null,
};

afterEach(() => {
  vi.unstubAllGlobals();
});

test('paga solo la cuota de matrícula con el pago en línea', async () => {
  let enviadas: string[] = [];
  server.use(
    http.post(
      `${env.API_URL}/pagos/pago-en-linea/ordenes`,
      async ({ request }) => {
        enviadas = ((await request.json()) as { cuotaIds: string[] }).cuotaIds;
        return HttpResponse.json(
          {
            codigo: 'OP-2026-000002',
            total: 850,
            expiraEn: '2026-10-20T10:30:00-05:00',
            urlPago: '/pasarela?orden=OP-2026-000002',
          },
          { status: 201 },
        );
      },
    ),
  );
  const asignar = vi.fn();
  vi.stubGlobal('location', { ...window.location, assign: asignar });

  await renderApp(<PagoMatriculaApoderado pago={pago} />);
  expect(screen.getByText('Matrícula 2027')).toBeInTheDocument();
  expect(screen.getByText('S/ 850.00')).toBeInTheDocument();
  expect(screen.getByText('Pendiente de pago')).toBeInTheDocument();
  expect(screen.getByText('Luis Paredes')).toBeInTheDocument();

  await userEvent.click(
    screen.getByRole('button', { name: 'Pagar matrícula' }),
  );
  await waitFor(() =>
    expect(asignar).toHaveBeenCalledWith('/pasarela?orden=OP-2026-000002'),
  );
  expect(enviadas).toEqual(['cuota-matricula']);
});

test('no deja pagar y explica el motivo', async () => {
  await renderApp(
    <PagoMatriculaApoderado
      pago={{
        ...pago,
        puedePagar: false,
        motivo: 'Primero paga las pensiones vencidas de Mateo Paredes.',
      }}
    />,
  );
  expect(
    screen.getByRole('button', { name: 'Pagar matrícula' }),
  ).toBeDisabled();
  expect(
    screen.getByText('Primero paga las pensiones vencidas de Mateo Paredes.'),
  ).toBeInTheDocument();
});

test('con la matrícula pagada no muestra el botón', async () => {
  await renderApp(
    <PagoMatriculaApoderado
      pago={{
        ...pago,
        estado: 'PAGADA',
        puedePagar: false,
        motivo: 'La matrícula ya está confirmada.',
      }}
    />,
  );
  expect(screen.getByText('Pagada')).toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'Pagar matrícula' }),
  ).not.toBeInTheDocument();
});
