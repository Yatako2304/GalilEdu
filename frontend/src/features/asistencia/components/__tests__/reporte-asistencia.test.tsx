import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import {
  renderApp,
  screen,
  userEvent,
  waitFor,
  within,
} from '@/testing/test-utils';
import { ReporteAsistencia as Reporte } from '@/types/api';

import { ReporteAsistencia } from '../reporte-asistencia';

const seccion = {
  id: 'seccion-1',
  anio: 2026,
  grado: '1.° de secundaria',
  nombre: 'A',
  cargo: 'TUTOR' as const,
};

const reporte = (tipo: string | null, valor: string | null): Reporte => {
  const porPeriodo = tipo === 'PERIODO';
  return {
    seccion,
    tipo: porPeriodo ? 'PERIODO' : 'MES',
    valor: valor ?? '2026-11',
    etiqueta: porPeriodo ? 'IV Bimestre' : 'Noviembre 2026',
    desde: porPeriodo ? '2026-10-19' : '2026-11-01',
    hasta: porPeriodo ? '2026-12-18' : '2026-11-30',
    diasRegistrados: valor === '2026-10' ? 0 : 12,
    opciones: {
      meses: [
        { valor: '2026-11', etiqueta: 'Noviembre 2026' },
        { valor: '2026-10', etiqueta: 'Octubre 2026' },
      ],
      periodos: [
        { valor: '3', etiqueta: 'III Bimestre' },
        { valor: '4', etiqueta: 'IV Bimestre' },
      ],
    },
    filas: [
      {
        matriculaId: 'm1',
        nombres: 'Ana Lucía',
        apellidos: 'García López',
        documento: '99006101',
        faltas: 2,
        faltasJustificadas: 1,
        tardanzas: 3,
        tardanzasJustificadas: 0,
      },
    ],
    totales: {
      faltas: 2,
      faltasJustificadas: 1,
      tardanzas: 3,
      tardanzasJustificadas: 0,
    },
  };
};

const usarReporte = () => {
  const pedidos: { tipo: string | null; valor: string | null }[] = [];
  server.use(
    http.get(`${env.API_URL}/asistencia/secciones`, () =>
      HttpResponse.json([seccion]),
    ),
    http.get(
      `${env.API_URL}/asistencia/secciones/:id/reporte`,
      ({ request }) => {
        const url = new URL(request.url);
        const pedido = {
          tipo: url.searchParams.get('tipo'),
          valor: url.searchParams.get('valor'),
        };
        pedidos.push(pedido);
        return HttpResponse.json(reporte(pedido.tipo, pedido.valor));
      },
    ),
  );
  return pedidos;
};

test('muestra el mes actual por apellidos con las justificadas aparte', async () => {
  const pedidos = usarReporte();
  await renderApp(<ReporteAsistencia />);

  expect(
    await screen.findByText('García López, Ana Lucía'),
  ).toBeInTheDocument();
  expect(pedidos[0]).toEqual({ tipo: null, valor: null });
  expect(
    screen.getByText(/Del 01\/11\/2026 al 30\/11\/2026 · 12 días/),
  ).toBeInTheDocument();
  const fila = screen.getByText('García López, Ana Lucía').closest('tr')!;
  expect(
    within(fila)
      .getAllByRole('cell')
      .slice(2)
      .map((c) => c.textContent),
  ).toEqual(['2', '1', '3', '0']);
  expect(screen.getByText('Total de la sección')).toBeInTheDocument();
});

test('"Por bimestre" pide el periodo más reciente y el selector cambia el mes', async () => {
  const pedidos = usarReporte();
  await renderApp(<ReporteAsistencia />);
  await screen.findByText('García López, Ana Lucía');

  await userEvent.click(screen.getByRole('tab', { name: 'Por bimestre' }));
  await waitFor(() =>
    expect(pedidos.at(-1)).toEqual({ tipo: 'PERIODO', valor: '4' }),
  );
  expect(await screen.findByText(/Del 19\/10\/2026/)).toBeInTheDocument();

  await userEvent.click(screen.getByRole('tab', { name: 'Por mes' }));
  await waitFor(() =>
    expect(pedidos.at(-1)).toEqual({ tipo: 'MES', valor: '2026-11' }),
  );
  await userEvent.selectOptions(screen.getByLabelText('Mes'), '2026-10');
  await waitFor(() =>
    expect(pedidos.at(-1)).toEqual({ tipo: 'MES', valor: '2026-10' }),
  );
  expect(
    await screen.findByText('No hay asistencia registrada en noviembre 2026.'),
  ).toBeInTheDocument();
});
