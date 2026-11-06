import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent } from '@/testing/test-utils';

import { DialogoAplicarDescuento } from '../dialogo-aplicar-descuento';

test('avisa el reemplazo del descuento vigente y exige el motivo', async () => {
  server.use(
    http.get(`${env.API_URL}/pagos/compromisos/:id/descuentos-aplicables`, () =>
      HttpResponse.json({
        hermanosMatriculados: 1,
        actual: {
          id: 'aplicado-1',
          nombre: 'Acuerdo particular',
          porcentaje: 20,
        },
        descuentos: [
          {
            id: 'descuento-1',
            nombre: 'Beca institucional',
            tipo: 'PORCENTAJE_UNICO',
            aplicacion: 'MANUAL',
            porcentaje: 50,
            conceptos: ['PENSION'],
            condicion: null,
            aplicable: true,
            motivo: null,
          },
        ],
      }),
    ),
  );

  await renderApp(
    <DialogoAplicarDescuento
      abierto
      onOpenChange={() => undefined}
      compromisoId="compromiso-1"
    />,
  );

  expect(
    await screen.findByText(
      'El estudiante ya tiene «Acuerdo particular» (20 %).',
    ),
  ).toBeInTheDocument();
  expect(screen.getByText('Hermanos matriculados: 1')).toBeInTheDocument();

  await userEvent.click(
    screen.getByRole('button', { name: 'Aplicar descuento' }),
  );
  expect(
    await screen.findByText('Ingresa el motivo del descuento.'),
  ).toBeInTheDocument();
});
