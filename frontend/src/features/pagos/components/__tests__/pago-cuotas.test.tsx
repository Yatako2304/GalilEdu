import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent, waitFor } from '@/testing/test-utils';
import { CuotaPorPagar, CuotasPorPagar } from '@/types/api';

import { PagoCuotas } from '../pago-cuotas';
import { alternarCuota, unirNombres } from '../seleccion-cuotas';

// HU-41 · Selección de cuotas para el pago en línea (CAHU41-1 a CAHU41-5).

const cuota = (
  id: string,
  nombre: string,
  extra: Partial<CuotaPorPagar> = {},
): CuotaPorPagar => ({
  cuotaId: id,
  concepto: 'PENSION',
  numero: null,
  nombre,
  anio: 2026,
  fechaVencimiento: '2026-09-30',
  capital: 600,
  recargo: 0,
  total: 600,
  vencida: false,
  estado: 'PENDIENTE',
  seleccionable: true,
  requiere: [],
  ...extra,
});

const septiembre = cuota('sep', 'Pensión de septiembre', {
  recargo: 12,
  total: 612,
  vencida: true,
});
const octubre = cuota('oct', 'Pensión de octubre', {
  fechaVencimiento: '2026-10-31',
  requiere: ['sep'],
});
const noviembre = cuota('nov', 'Pensión de noviembre', {
  fechaVencimiento: '2026-11-30',
  requiere: ['sep', 'oct'],
});

describe('alternarCuota (las pensiones se pagan en orden)', () => {
  const todas = [septiembre, octubre, noviembre];

  test('marcar una pensión marca también las anteriores pendientes', () => {
    expect([...alternarCuota(new Set(), noviembre, todas)].sort()).toEqual([
      'nov',
      'oct',
      'sep',
    ]);
  });

  test('desmarcar una pensión desmarca las que dependen de ella', () => {
    const marcadas = new Set(['sep', 'oct', 'nov']);
    expect([...alternarCuota(marcadas, octubre, todas)]).toEqual(['sep']);
  });

  test('una cuota que no se puede pagar no se marca', () => {
    const enProceso = cuota('x', 'Pensión de agosto', {
      seleccionable: false,
      estado: 'EN_PROCESO',
    });
    expect(alternarCuota(new Set(), enProceso, [enProceso]).size).toBe(0);
  });
});

test('une los nombres como en Figma', () => {
  expect(unirNombres(['Pensión de agosto', 'Pensión de septiembre'])).toBe(
    'Pensión de agosto y Pensión de septiembre',
  );
  expect(unirNombres(['A', 'B', 'C'])).toBe('A, B y C');
});

const respuesta: CuotasPorPagar = {
  estudiantes: [
    {
      estudianteId: 'sofia',
      nombre: 'Sofía Paredes Rojas',
      cuotas: [septiembre, octubre],
    },
  ],
};

test('muestra el total con recargo y paga las cuotas marcadas', async () => {
  let enviadas: string[] = [];
  server.use(
    http.get(`${env.API_URL}/pagos/pago-en-linea/cuotas`, () =>
      HttpResponse.json(respuesta),
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

  await renderApp(<PagoCuotas />);
  expect(await screen.findByText('Sofía Paredes Rojas')).toBeInTheDocument();
  expect(screen.getByText('S/ 12.00')).toBeInTheDocument();

  // Al marcar octubre se marca también septiembre (CAHU41-3).
  await userEvent.click(
    screen.getByLabelText('Pagar Pensión de octubre de Sofía Paredes Rojas'),
  );
  expect(
    screen.getByText('2 cuotas seleccionadas · Total: S/ 1,212.00'),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      'Pensión de septiembre y Pensión de octubre · incluye S/ 12.00 de recargo por mora',
    ),
  ).toBeInTheDocument();

  await userEvent.click(
    screen.getByRole('button', { name: /Pagar seleccionadas/ }),
  );
  await waitFor(() => expect(asignar).toHaveBeenCalled());
  expect(enviadas.sort()).toEqual(['oct', 'sep']);
  vi.unstubAllGlobals();
});

test('sin cuotas pendientes lo dice y no muestra la barra', async () => {
  server.use(
    http.get(`${env.API_URL}/pagos/pago-en-linea/cuotas`, () =>
      HttpResponse.json({ estudiantes: [] }),
    ),
  );
  await renderApp(<PagoCuotas />);
  expect(
    await screen.findByText('No tienes cuotas pendientes.'),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: /Pagar seleccionadas/ }),
  ).not.toBeInTheDocument();
});
