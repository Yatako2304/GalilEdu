import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent, waitFor } from '@/testing/test-utils';
import { CuotaPorPagar } from '@/types/api';

import { DialogoRegistrarPago } from '../dialogo-registrar-pago';

// Pago en caja (fila 29 del plan de pruebas): el Personal administrativo registra lo que se pagó en el colegio.

const cuota = (datos: Partial<CuotaPorPagar>): CuotaPorPagar => ({
  cuotaId: 'septiembre',
  concepto: 'PENSION',
  numero: 7,
  nombre: 'Pensión de septiembre',
  anio: 2026,
  fechaVencimiento: '2026-09-30',
  capital: 600,
  recargo: 12,
  total: 612,
  vencida: true,
  estado: 'PENDIENTE',
  seleccionable: true,
  requiere: [],
  ...datos,
});

const cuotas = [
  cuota({}),
  cuota({
    cuotaId: 'octubre',
    numero: 8,
    nombre: 'Pensión de octubre',
    fechaVencimiento: '2026-10-31',
    recargo: 0,
    total: 600,
    vencida: false,
    requiere: ['septiembre'],
  }),
];

const abrir = () =>
  renderApp(
    <DialogoRegistrarPago
      abierto
      onOpenChange={() => undefined}
      compromisoId="compromiso-1"
      estudiante="Sofía Paredes Rojas"
    />,
  );

test('al marcar una pensión marca las anteriores y registra el pago en efectivo con su recargo', async () => {
  let solicitud: Record<string, unknown> | undefined;
  server.use(
    http.get(`${env.API_URL}/pagos/compromisos/:id/cuotas-por-pagar`, () =>
      HttpResponse.json(cuotas),
    ),
    http.post(
      `${env.API_URL}/pagos/compromisos/:id/pagos`,
      async ({ request }) => {
        solicitud = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(
          {
            pagoId: 'pago-1',
            numeroConstancia: 'C-2026-00008',
            capital: 1200,
            recargo: 12,
            total: 1212,
          },
          { status: 201 },
        );
      },
    ),
  );

  await abrir();

  await userEvent.click(
    await screen.findByRole('checkbox', { name: /Pensión de octubre/ }),
  );
  expect(
    screen.getByRole('checkbox', { name: /Pensión de septiembre/ }),
  ).toBeChecked();
  expect(screen.getByText('S/ 1,212.00')).toBeInTheDocument();
  expect(
    screen.getByText('2 cuota(s) · incluye S/ 12.00 de recargo'),
  ).toBeInTheDocument();

  await userEvent.click(screen.getByRole('button', { name: 'Registrar pago' }));

  await waitFor(() =>
    expect(solicitud).toMatchObject({
      cuotaIds: ['septiembre', 'octubre'],
      medioPago: 'EFECTIVO',
      numeroOperacion: null,
      observacion: null,
    }),
  );
  expect(await screen.findByText('Pago registrado')).toBeInTheDocument();
});

test('pide una cuota y el número de operación cuando no es en efectivo', async () => {
  const registrar = vi.fn();
  server.use(
    http.get(`${env.API_URL}/pagos/compromisos/:id/cuotas-por-pagar`, () =>
      HttpResponse.json(cuotas),
    ),
    http.post(`${env.API_URL}/pagos/compromisos/:id/pagos`, () => {
      registrar();
      return HttpResponse.json({}, { status: 201 });
    }),
  );

  await abrir();
  await screen.findByRole('checkbox', { name: /Pensión de septiembre/ });

  await userEvent.click(screen.getByRole('button', { name: 'Registrar pago' }));
  expect(
    await screen.findByText('Selecciona al menos una cuota.'),
  ).toBeInTheDocument();

  await userEvent.click(
    screen.getByRole('checkbox', { name: /Pensión de septiembre/ }),
  );
  await userEvent.selectOptions(
    screen.getByLabelText('Medio de pago *'),
    'TRANSFERENCIA',
  );
  await userEvent.click(screen.getByRole('button', { name: 'Registrar pago' }));

  expect(
    await screen.findByText('Ingresa el número de operación.'),
  ).toBeInTheDocument();
  expect(registrar).not.toHaveBeenCalled();
});

test('una cuota con un pago en línea en proceso no se puede marcar', async () => {
  server.use(
    http.get(`${env.API_URL}/pagos/compromisos/:id/cuotas-por-pagar`, () =>
      HttpResponse.json([
        cuota({ estado: 'EN_PROCESO', seleccionable: false, vencida: false }),
      ]),
    ),
  );

  await abrir();

  expect(
    await screen.findByRole('checkbox', { name: /Pensión de septiembre/ }),
  ).toBeDisabled();
  expect(
    screen.getByText('Tiene un pago en línea en proceso.'),
  ).toBeInTheDocument();
});
