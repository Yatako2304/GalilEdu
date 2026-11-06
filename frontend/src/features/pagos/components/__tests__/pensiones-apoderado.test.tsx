import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent } from '@/testing/test-utils';

import { PensionesApoderado } from '../pensiones-apoderado';

const hijos = [
  {
    estudianteId: 'sofia',
    codigo: '74125896',
    nombres: 'Sofía',
    apellidos: 'Paredes',
    grado: '1.° de secundaria',
    seccion: 'A',
    saldo: 600,
    cuotasVencidas: 1,
  },
  {
    estudianteId: 'mateo',
    codigo: '75125896',
    nombres: 'Mateo',
    apellidos: 'Paredes',
    grado: '2.° de primaria',
    seccion: null,
    saldo: 0,
    cuotasVencidas: 0,
  },
];

const detalle = {
  id: 'compromiso-1',
  anio: 2026,
  estado: 'VIGENTE',
  estudiante: {
    id: 'sofia',
    nombre: 'Sofía Paredes',
    numeroDocumento: '74125896',
    apoderado: 'Luis Paredes',
    seccion: '1.° A',
  },
  resumen: {
    totalAnual: 6420,
    cuotasActivas: 11,
    capitalPagado: 5820,
    pensionesPagadas: 8,
    pensionesTotal: 10,
    descuentoPorcentaje: null,
    descuentoNombre: null,
    pensionMensual: 600,
    saldoPendiente: 600,
    cuotasVencidas: 1,
  },
  cuotas: [
    {
      id: 'cuota-1',
      concepto: 'PENSION',
      numero: 7,
      nombre: 'Pensión de septiembre',
      fechaVencimiento: '2026-09-05',
      montoBase: 600,
      montoDescuento: 0,
      porcentajeDescuento: null,
      montoFinal: 600,
      montoPagado: 0,
      estado: 'PENDIENTE',
      vencida: true,
      fechaPago: null,
      diasAtraso: 13,
      recargo: 12,
    },
  ],
};

test('muestra la deuda y cambia a un hijo sin compromiso', async () => {
  server.use(
    http.get(`${env.API_URL}/pagos/apoderado/estudiantes`, () =>
      HttpResponse.json(hijos),
    ),
    http.get(
      `${env.API_URL}/pagos/apoderado/estudiantes/:id/compromisos`,
      ({ params }) =>
        HttpResponse.json(
          params.id === 'sofia'
            ? [{ id: 'compromiso-1', anio: 2026, estado: 'VIGENTE' }]
            : [],
        ),
    ),
    http.get(`${env.API_URL}/pagos/apoderado/compromisos/:id`, () =>
      HttpResponse.json(detalle),
    ),
  );

  await renderApp(<PensionesApoderado />);

  expect(await screen.findByText('Pensión de septiembre')).toBeInTheDocument();
  expect(screen.getByText('Vencida hace 13 días')).toBeInTheDocument();
  expect(screen.getByText('+ S/ 12.00')).toBeInTheDocument();

  await userEvent.click(
    screen.getByRole('button', { name: 'Seleccionar estudiante' }),
  );
  await userEvent.click(
    screen.getByRole('menuitem', { name: /Mateo Paredes/ }),
  );
  expect(
    await screen.findByText('Aún no tiene compromiso de pago.'),
  ).toBeInTheDocument();
});
