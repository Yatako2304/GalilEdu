import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent } from '@/testing/test-utils';

import { PagoMatriculaCard } from '../pago-matricula-card';

const cuota = {
  cuotaId: 'cuota-1',
  compromisoId: 'compromiso-1',
  estudianteId: 'estudiante-1',
  anio: 2027,
  concepto: 'Matrícula 2027',
  montoBase: 450,
  montoDescuento: 0,
  monto: 450,
  montoPagado: 0,
  fechaVencimiento: '2027-02-28',
  estado: 'PENDIENTE' as const,
  fechaPago: null,
  responsable: 'Lucía Huamán Paz',
};

test('muestra la cuota generada con los datos del backend', async () => {
  server.use(
    http.get(`${env.API_URL}/pagos/matriculas/:id/cuota-matricula`, () =>
      HttpResponse.json(cuota),
    ),
  );
  await renderApp(<PagoMatriculaCard matriculaId="matricula-1" />);
  expect(await screen.findByText('Matrícula 2027')).toBeInTheDocument();
  expect(screen.getByText('S/ 450.00')).toBeInTheDocument();
  expect(screen.getByText('Pendiente de pago')).toBeInTheDocument();
});

test('genera la cuota cuando todavía no existe', async () => {
  let generada = false;
  server.use(
    http.get(`${env.API_URL}/pagos/matriculas/:id/cuota-matricula`, () =>
      generada
        ? HttpResponse.json(cuota)
        : HttpResponse.json({ message: 'No existe' }, { status: 404 }),
    ),
    http.post(`${env.API_URL}/pagos/matriculas/:id/cuota-matricula`, () => {
      generada = true;
      return HttpResponse.json(cuota);
    }),
  );
  await renderApp(<PagoMatriculaCard matriculaId="matricula-1" />);
  await userEvent.click(
    await screen.findByRole('button', { name: 'Generar cuota de matrícula' }),
  );
  expect(await screen.findByText('S/ 450.00')).toBeInTheDocument();
});
