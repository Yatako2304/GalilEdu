import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent, waitFor } from '@/testing/test-utils';

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
    http.get(`${env.API_URL}/pagos/pago-en-linea/cuotas`, () =>
      HttpResponse.json({ estudiantes: [] }),
    ),
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

// HU-41 · Las casillas del cronograma arman el pago en línea (CAHU41-1 a CAHU41-5).
const cuotaPorPagar = (id: string, nombre: string, extra = {}) => ({
  cuotaId: id,
  concepto: 'PENSION',
  numero: null,
  nombre,
  anio: 2026,
  fechaVencimiento: '2026-10-05',
  capital: 600,
  recargo: 0,
  total: 600,
  vencida: false,
  estado: 'PENDIENTE',
  seleccionable: true,
  requiere: [],
  ...extra,
});

const pensionOctubre = {
  ...detalle.cuotas[0],
  id: 'cuota-2',
  numero: 8,
  nombre: 'Pensión de octubre',
  fechaVencimiento: '2026-10-05',
  vencida: false,
  diasAtraso: 0,
  recargo: 0,
};

const pensionAgosto = {
  ...detalle.cuotas[0],
  id: 'cuota-0',
  numero: 6,
  nombre: 'Pensión de agosto',
  fechaVencimiento: '2026-08-05',
  estado: 'PAGADA',
  montoPagado: 600,
  vencida: false,
  fechaPago: '2026-08-01',
  diasAtraso: 0,
  recargo: 0,
};

test('marca las cuotas en orden y paga las seleccionadas', async () => {
  let enviadas: string[] = [];
  server.use(
    http.get(`${env.API_URL}/pagos/apoderado/estudiantes`, () =>
      HttpResponse.json(hijos),
    ),
    http.get(`${env.API_URL}/pagos/apoderado/estudiantes/:id/compromisos`, () =>
      HttpResponse.json([
        { id: 'compromiso-1', anio: 2026, estado: 'VIGENTE' },
      ]),
    ),
    http.get(`${env.API_URL}/pagos/apoderado/compromisos/:id`, () =>
      HttpResponse.json({
        ...detalle,
        cuotas: [pensionAgosto, ...detalle.cuotas, pensionOctubre],
      }),
    ),
    http.get(`${env.API_URL}/pagos/pago-en-linea/cuotas`, () =>
      HttpResponse.json({
        estudiantes: [
          {
            estudianteId: 'sofia',
            nombre: 'Sofía Paredes',
            cuotas: [
              cuotaPorPagar('cuota-1', 'Pensión de septiembre', {
                fechaVencimiento: '2026-09-05',
                recargo: 12,
                total: 612,
                vencida: true,
              }),
              cuotaPorPagar('cuota-2', 'Pensión de octubre', {
                requiere: ['cuota-1'],
              }),
            ],
          },
        ],
      }),
    ),
    http.post(
      `${env.API_URL}/pagos/pago-en-linea/ordenes`,
      async ({ request }) => {
        enviadas = ((await request.json()) as { cuotaIds: string[] }).cuotaIds;
        return HttpResponse.json(
          {
            codigo: 'OP-2026-000001',
            total: 1212,
            expiraEn: '2026-10-20T10:30:00-05:00',
            urlPago:
              '/app/apoderado/pensiones/pasarela-simulada?orden=OP-2026-000001',
          },
          { status: 201 },
        );
      },
    ),
  );
  const asignar = vi.fn();
  vi.stubGlobal('location', { ...window.location, assign: asignar });

  await renderApp(<PensionesApoderado />);
  expect(await screen.findByText('Pensión de octubre')).toBeInTheDocument();

  // Una cuota ya pagada no se puede marcar.
  expect(screen.getByLabelText('Seleccionar Pensión de agosto')).toBeDisabled();
  await waitFor(() =>
    expect(
      screen.getByLabelText('Seleccionar Pensión de octubre'),
    ).toBeEnabled(),
  );

  // Al marcar octubre se marca también septiembre, que vence antes (CAHU41-3).
  await userEvent.click(
    screen.getByLabelText('Seleccionar Pensión de octubre'),
  );
  expect(
    screen.getByLabelText('Seleccionar Pensión de septiembre'),
  ).toBeChecked();
  expect(
    screen.getByText('2 cuotas seleccionadas · Total: S/ 1,212.00'),
  ).toBeInTheDocument();

  await userEvent.click(
    screen.getByRole('button', { name: /Pagar seleccionadas/ }),
  );
  await waitFor(() => expect(asignar).toHaveBeenCalled());
  expect(enviadas.sort()).toEqual(['cuota-1', 'cuota-2']);
  vi.unstubAllGlobals();
});

test('sin cuotas por pagar, "Pagar seleccionadas" queda deshabilitado', async () => {
  server.use(
    http.get(`${env.API_URL}/pagos/apoderado/estudiantes`, () =>
      HttpResponse.json(hijos),
    ),
    http.get(`${env.API_URL}/pagos/apoderado/estudiantes/:id/compromisos`, () =>
      HttpResponse.json([
        { id: 'compromiso-1', anio: 2026, estado: 'VIGENTE' },
      ]),
    ),
    http.get(`${env.API_URL}/pagos/apoderado/compromisos/:id`, () =>
      HttpResponse.json(detalle),
    ),
    http.get(`${env.API_URL}/pagos/pago-en-linea/cuotas`, () =>
      HttpResponse.json({ estudiantes: [] }),
    ),
  );
  await renderApp(<PensionesApoderado />);
  expect(await screen.findByText('Pensión de septiembre')).toBeInTheDocument();
  expect(
    screen.getByLabelText('Seleccionar Pensión de septiembre'),
  ).toBeDisabled();
  expect(
    screen.getByRole('button', { name: /Pagar seleccionadas/ }),
  ).toBeDisabled();
});

// HU-41 · Si el apoderado cerró la pasarela, puede retomar o cancelar el pago (no espera a que la orden venza).
test('con un pago abierto ofrece continuarlo o cancelarlo', async () => {
  let cancelada = false;
  const ordenAbierta = {
    codigo: 'OP-2026-000016',
    total: 1091.4,
    expiraEn: '2026-11-18T11:41:00-05:00',
    urlPago: 'https://checkout.stripe.com/c/pay/cs_test_123',
  };
  server.use(
    http.get(`${env.API_URL}/pagos/apoderado/estudiantes`, () =>
      HttpResponse.json(hijos),
    ),
    http.get(`${env.API_URL}/pagos/apoderado/estudiantes/:id/compromisos`, () =>
      HttpResponse.json([
        { id: 'compromiso-1', anio: 2026, estado: 'VIGENTE' },
      ]),
    ),
    http.get(`${env.API_URL}/pagos/apoderado/compromisos/:id`, () =>
      HttpResponse.json(detalle),
    ),
    http.get(`${env.API_URL}/pagos/pago-en-linea/cuotas`, () =>
      HttpResponse.json({
        estudiantes: [],
        ordenEnProceso: cancelada ? null : ordenAbierta,
      }),
    ),
    http.post(
      `${env.API_URL}/pagos/pago-en-linea/ordenes/:codigo/cancelar`,
      () => {
        cancelada = true;
        return HttpResponse.json({
          codigo: 'OP-2026-000016',
          estado: 'RECHAZADA',
          total: 1091.4,
          montoConfirmado: null,
          motivo: 'Cancelaste el pago.',
          creadoEn: '2026-11-18T11:11:00-05:00',
          expiraEn: '2026-11-18T11:41:00-05:00',
          urlPago: ordenAbierta.urlPago,
          cuotas: [],
          constancias: [],
        });
      },
    ),
  );
  const asignar = vi.fn();
  vi.stubGlobal('location', { ...window.location, assign: asignar });

  await renderApp(<PensionesApoderado />);
  const aviso = await screen.findByRole('alert');
  expect(aviso).toHaveTextContent('Tienes un pago en proceso por S/ 1,091.40');
  expect(aviso).toHaveTextContent('Orden OP-2026-000016');
  expect(aviso).toHaveTextContent('11:41');

  await userEvent.click(screen.getByRole('button', { name: 'Continuar pago' }));
  expect(asignar).toHaveBeenCalledWith(ordenAbierta.urlPago);

  await userEvent.click(screen.getByRole('button', { name: 'Cancelar pago' }));
  await waitFor(() =>
    expect(
      screen.queryByText(/Tienes un pago en proceso/),
    ).not.toBeInTheDocument(),
  );
  vi.unstubAllGlobals();
});

// HU-42 · RF-108: el apoderado descarga la constancia desde la pestaña Pagos.
test('descarga la constancia del pago en PDF desde la pestaña Pagos', async () => {
  const pedidas: string[] = [];
  server.use(
    http.get(`${env.API_URL}/pagos/pago-en-linea/cuotas`, () =>
      HttpResponse.json({ estudiantes: [] }),
    ),
    http.get(`${env.API_URL}/pagos/apoderado/estudiantes`, () =>
      HttpResponse.json(hijos),
    ),
    http.get(`${env.API_URL}/pagos/apoderado/estudiantes/:id/compromisos`, () =>
      HttpResponse.json([
        { id: 'compromiso-1', anio: 2026, estado: 'VIGENTE' },
      ]),
    ),
    http.get(`${env.API_URL}/pagos/apoderado/compromisos/:id`, () =>
      HttpResponse.json(detalle),
    ),
    http.get(`${env.API_URL}/pagos/apoderado/compromisos/:id/pagos`, () =>
      HttpResponse.json([
        {
          id: 'pago-7',
          numeroConstancia: 'C-2026-00007',
          fechaPago: '2026-08-25T10:00:00-05:00',
          capital: 600,
          recargo: 0,
          monto: 600,
          estado: 'CONFIRMADO',
          cuotas: ['Pensión de agosto'],
        },
      ]),
    ),
    http.get(
      `${env.API_URL}/pagos/apoderado/pagos/:id/constancia`,
      ({ params }) => {
        pedidas.push(params.id as string);
        return new HttpResponse(
          new Blob(['%PDF-'], { type: 'application/pdf' }),
          {
            headers: { 'Content-Type': 'application/pdf' },
          },
        );
      },
    ),
  );
  const crearUrl = vi.fn(() => 'blob:constancia');
  const liberarUrl = vi.fn();
  const { createObjectURL, revokeObjectURL } = URL;
  URL.createObjectURL = crearUrl;
  URL.revokeObjectURL = liberarUrl;
  const clic = vi
    .spyOn(HTMLAnchorElement.prototype, 'click')
    .mockImplementation(function (this: HTMLAnchorElement) {
      expect(this.download).toBe('constancia-C-2026-00007.pdf');
    });

  await renderApp(<PensionesApoderado />);
  await screen.findByText('Pensión de septiembre');
  await userEvent.click(screen.getByRole('tab', { name: 'Pagos' }));
  await userEvent.click(
    await screen.findByRole('button', {
      name: 'Descargar constancia C-2026-00007',
    }),
  );

  await waitFor(() => expect(clic).toHaveBeenCalledTimes(1));
  expect(pedidas).toEqual(['pago-7']);
  expect(crearUrl).toHaveBeenCalledTimes(1);
  expect(liberarUrl).toHaveBeenCalledWith('blob:constancia');
  clic.mockRestore();
  URL.createObjectURL = createObjectURL;
  URL.revokeObjectURL = revokeObjectURL;
});
