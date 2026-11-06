import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen } from '@/testing/test-utils';

import { MorosidadCompromiso } from '../morosidad-compromiso';

const morosidad = {
  compromisoId: 'compromiso-1',
  fecha: '2026-10-02',
  anio: 2026,
  estudianteId: 'estudiante-1',
  estudiante: 'Sofía Paredes',
  diasGracia: 5,
  recargoPorcentaje: 2,
  topeRecargoPorcentaje: 10,
  capital: 600,
  recargo: 24,
  total: 624,
  cuotas: [
    {
      cuotaId: 'cuota-1',
      concepto: 'PENSION',
      numero: 5,
      fechaVencimiento: '2026-07-31',
      diasAtraso: 63,
      capital: 600,
      recargo: 24,
      total: 624,
    },
  ],
};

test('muestra el monto pendiente y el recargo por separado con sus totales', async () => {
  server.use(
    http.get(`${env.API_URL}/pagos/compromisos/:id/morosidad`, () =>
      HttpResponse.json(morosidad),
    ),
  );

  await renderApp(<MorosidadCompromiso compromisoId="compromiso-1" />);

  expect(await screen.findByText('Pensión de julio')).toBeInTheDocument();
  expect(screen.getAllByText('+ S/ 24.00')).toHaveLength(2);
  expect(screen.getAllByText('S/ 624.00')).toHaveLength(2);
  expect(
    screen.getByRole('columnheader', { name: 'Monto pendiente' }),
  ).toBeInTheDocument();
  expect(screen.queryByText('Capital')).not.toBeInTheDocument();
  expect(
    screen.getByText('Gracia 5 días · Recargo 2 % cada 30 días · Tope 10 %'),
  ).toBeInTheDocument();
});

test('muestra el estado vacío cuando no hay cuotas vencidas', async () => {
  server.use(
    http.get(`${env.API_URL}/pagos/compromisos/:id/morosidad`, () =>
      HttpResponse.json({
        ...morosidad,
        capital: 0,
        recargo: 0,
        total: 0,
        cuotas: [],
      }),
    ),
  );

  await renderApp(<MorosidadCompromiso compromisoId="compromiso-1" />);

  expect(await screen.findByText('Sin cuotas vencidas')).toBeInTheDocument();
});
