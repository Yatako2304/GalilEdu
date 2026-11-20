import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent, waitFor } from '@/testing/test-utils';

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
            cantidadHermanosMatriculados: null,
            beneficiarios: [],
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

test('permite mantener el descuento hasta que el estudiante termine el colegio', async () => {
  let solicitud: Record<string, unknown> | undefined;
  server.use(
    http.get(`${env.API_URL}/pagos/compromisos/:id/descuentos-aplicables`, () =>
      HttpResponse.json({
        hermanosMatriculados: 1,
        actual: null,
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
            cantidadHermanosMatriculados: null,
            beneficiarios: [],
          },
        ],
      }),
    ),
    http.post(
      `${env.API_URL}/pagos/compromisos/compromiso-1/descuentos`,
      async ({ request }) => {
        solicitud = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json({
          descuento: {
            id: 'aplicado-1',
            descuentoId: 'descuento-1',
            nombre: 'Beca institucional',
            porcentaje: 50,
            vigenciaDesde: '2026-11-19',
            vigenciaHasta: null,
            hastaFinEscolaridad: true,
            motivo: 'Beca por rendimiento',
            estado: 'ACTIVO',
            aplicadoPor: 'Valeria Sánchez',
            aplicadoEn: '2026-11-19T12:00:00Z',
            motivoRevocacion: null,
            revocadoEn: null,
          },
          cuotasRecalculadas: 10,
        });
      },
    ),
  );

  await renderApp(
    <DialogoAplicarDescuento
      abierto
      onOpenChange={() => undefined}
      compromisoId="compromiso-1"
    />,
  );

  const hastaFinColegio = await screen.findByRole('checkbox', {
    name: /Hasta que el estudiante termine el colegio/,
  });
  await userEvent.click(hastaFinColegio);
  expect(screen.getByLabelText('Hasta')).toBeDisabled();
  await userEvent.type(
    screen.getByRole('textbox', { name: 'Motivo o sustento *' }),
    'Beca por rendimiento',
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Aplicar descuento' }),
  );

  await waitFor(() => expect(solicitud).toBeDefined());
  expect(solicitud).toMatchObject({
    descuentoId: 'descuento-1',
    motivo: 'Beca por rendimiento',
    vigenciaHasta: null,
    hastaFinEscolaridad: true,
  });
});

test('explica que el descuento por hermanos corresponde al hermano menor', async () => {
  server.use(
    http.get(`${env.API_URL}/pagos/compromisos/:id/descuentos-aplicables`, () =>
      HttpResponse.json({
        hermanosMatriculados: 2,
        actual: null,
        descuentos: [
          {
            id: 'hermanos',
            nombre: 'Descuento por hermanos',
            tipo: 'POR_HERMANOS',
            aplicacion: 'AUTOMATICA',
            porcentaje: 10,
            conceptos: ['PENSION'],
            condicion: null,
            aplicable: false,
            motivo:
              'Este descuento corresponde al hermano menor: Mateo Paredes.',
            cantidadHermanosMatriculados: 2,
            beneficiarios: [{ estudianteId: 'mateo', nombre: 'Mateo Paredes' }],
          },
        ],
      }),
    ),
  );

  await renderApp(
    <DialogoAplicarDescuento
      abierto
      onOpenChange={() => undefined}
      compromisoId="compromiso-sofia"
    />,
  );

  expect(await screen.findByText('Hermanos matriculados: 2')).toBeVisible();
  expect(
    screen.getByText(
      'Este descuento corresponde al hermano menor: Mateo Paredes.',
    ),
  ).toBeVisible();
  expect(
    screen.getByRole('radio', { name: /^Descuento por hermanos/ }),
  ).toBeDisabled();
});
