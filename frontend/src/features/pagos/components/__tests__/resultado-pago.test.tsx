import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen } from '@/testing/test-utils';

import { ResultadoPago } from '../resultado-pago';

const orden = (cuotas: { matricula: boolean; estudianteId: string }[]) => ({
  codigo: 'OP-1',
  estado: 'APROBADA',
  total: 450,
  montoConfirmado: 450,
  motivo: null,
  creadoEn: '2027-01-01T10:00:00Z',
  expiraEn: '2027-01-01T10:30:00Z',
  urlPago: null,
  cuotas: cuotas.map((c, i) => ({
    cuotaId: `c${i}`,
    estudianteId: c.estudianteId,
    estudiante: 'Mateo',
    nombre: c.matricula ? 'Matrícula 2027' : 'Pensión de marzo',
    matricula: c.matricula,
    capital: 450,
    recargo: 0,
  })),
  constancias: [],
});

const responder = (cuerpo: ReturnType<typeof orden>) =>
  server.use(
    http.get(`${env.API_URL}/pagos/pago-en-linea/ordenes/OP-1`, () =>
      HttpResponse.json(cuerpo),
    ),
  );

test('tras pagar la matrícula ofrece ver la matrícula de ese hijo', async () => {
  responder(
    orden([
      { matricula: true, estudianteId: 'mateo' },
      { matricula: false, estudianteId: 'sofia' },
    ]),
  );
  await renderApp(<ResultadoPago codigo="OP-1" cancelado={false} />);

  const enlace = await screen.findByRole('link', { name: 'Ver matrícula' });
  expect(enlace).toHaveAttribute(
    'href',
    '/app/apoderado/matricula?estudiante=mateo',
  );
});

test('un pago de pensiones no ofrece ver la matrícula', async () => {
  responder(orden([{ matricula: false, estudianteId: 'mateo' }]));
  await renderApp(<ResultadoPago codigo="OP-1" cancelado={false} />);

  await screen.findByText('Pago realizado');
  expect(
    screen.queryByRole('link', { name: 'Ver matrícula' }),
  ).not.toBeInTheDocument();
});

test('volver a pensiones abre las pensiones del estudiante que pagó', async () => {
  responder(orden([{ matricula: false, estudianteId: 'mateo' }]));
  await renderApp(<ResultadoPago codigo="OP-1" cancelado={false} />);

  expect(
    await screen.findByRole('link', { name: 'Volver a Pensiones' }),
  ).toHaveAttribute('href', '/app/apoderado/pensiones?estudiante=mateo');
});
