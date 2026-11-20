import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen } from '@/testing/test-utils';

import { DescuentoCompromiso } from '../descuento-compromiso';

test('muestra la vigencia hasta que el estudiante termine el colegio', async () => {
  server.use(
    http.get(`${env.API_URL}/pagos/compromisos/:id/descuento`, () =>
      HttpResponse.json({
        activo: {
          id: 'aplicado-1',
          descuentoId: 'beca-1',
          nombre: 'Beca institucional',
          porcentaje: 25,
          vigenciaDesde: '2026-03-01',
          vigenciaHasta: null,
          hastaFinEscolaridad: true,
          motivo: 'Beca por rendimiento',
          estado: 'ACTIVO',
          aplicadoPor: 'Valeria Sánchez',
          aplicadoEn: '2026-03-01T15:00:00Z',
          motivoRevocacion: null,
          revocadoEn: null,
        },
        cuotasConDescuento: 10,
        historial: [],
      }),
    ),
  );

  await renderApp(<DescuentoCompromiso compromisoId="compromiso-1" />);

  expect(screen.getByText(/Hasta que termine el colegio/)).toBeInTheDocument();
});
