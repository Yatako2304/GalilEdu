import { renderApp, screen } from '@/testing/test-utils';
import { CompromisoDetalle } from '@/types/api';

import { TablaCuotas } from '../cronograma-cuotas';
import { TablaPagos } from '../tabla-pagos';

const compromiso: CompromisoDetalle = {
  id: 'compromiso-1',
  anio: 2026,
  estado: 'VIGENTE',
  estudiante: {
    id: 'estudiante-1',
    nombre: 'Sofía Paredes',
    numeroDocumento: '74125896',
    apoderado: 'Luis Paredes',
    seccion: '4.° A',
  },
  resumen: {
    totalAnual: 600,
    cuotasActivas: 1,
    capitalPagado: 0,
    pensionesPagadas: 0,
    pensionesTotal: 10,
    descuentoPorcentaje: null,
    descuentoNombre: null,
    pensionMensual: 600,
    saldoPendiente: 600,
    cuotasVencidas: 1,
  },
  cuotas: [
    {
      id: 'cuota-7',
      concepto: 'PENSION',
      numero: 7,
      nombre: 'Pensión de septiembre',
      fechaVencimiento: '2026-09-30',
      montoBase: 600,
      montoDescuento: 0,
      porcentajeDescuento: null,
      montoFinal: 600,
      montoPagado: 0,
      estado: 'PENDIENTE',
      vencida: true,
      fechaPago: null,
      diasAtraso: 36,
      recargo: 12,
    },
  ],
};

test('el cronograma muestra el recargo de la cuota vencida y términos claros', async () => {
  await renderApp(<TablaCuotas compromiso={compromiso} />);

  expect(
    screen.getByRole('columnheader', { name: 'Recargo por mora' }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('columnheader', { name: 'Monto pagado' }),
  ).toBeInTheDocument();
  expect(screen.getAllByText('+ S/ 12.00')).toHaveLength(2);
  expect(screen.queryByText('Capital pagado')).not.toBeInTheDocument();
});

test('el historial distingue el monto de cuotas, el recargo y el total pagado', async () => {
  await renderApp(
    <TablaPagos
      isLoading={false}
      pagos={[
        {
          id: 'pago-1',
          numeroConstancia: 'C-2026-00001',
          fechaPago: '2026-10-05T10:00:00-05:00',
          capital: 600,
          recargo: 12,
          monto: 612,
          estado: 'CONFIRMADO',
          cuotas: ['Pensión de septiembre'],
        },
      ]}
    />,
  );

  expect(
    screen.getByRole('columnheader', { name: 'Monto de cuotas' }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('columnheader', { name: 'Total pagado' }),
  ).toBeInTheDocument();
  expect(screen.queryByText('Capital')).not.toBeInTheDocument();
});
