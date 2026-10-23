import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  renderApp,
  screen,
  userEvent,
  waitFor,
} from '@/testing/test-utils';
import { SolicitudTarifario, TarifarioVista } from '@/types/api';

import { useConfirmarTarifario } from '../mutaciones-tarifario';
import { usePeriodoMatricula } from '../periodos-matricula';
import { useTarifario } from '../tarifarios';

// Figma > Personal Administrativo > "P19 · Tarifario y calendario" > "Confirmar tarifario" (HU-34)

const API = `${env.API_URL}/configuracion/tarifarios`;
const API_PERIODOS = `${env.API_URL}/configuracion/periodos-matricula`;

const tarifario = (
  anio: number,
  extras: Partial<TarifarioVista> = {},
): TarifarioVista => ({
  anioEscolarId: `anio-${anio}`,
  anio,
  existe: true,
  estado: 'EN_CONFIGURACION',
  soloLectura: false,
  motivoSoloLectura: null,
  calendarioDesactualizado: false,
  conceptos: {
    montoCuotaIngreso: 500,
    montoMatricula: 450,
    vencimientoMatricula: `${anio}-02-28`,
    montoPension: 620,
    cantidadCuotas: 10,
  },
  morosidad: { diasGracia: 5, recargoPorcentaje: 2, topeRecargoPorcentaje: 10 },
  calendario: { cuotas: [], totalPensiones: 6200 },
  ...extras,
});

const solicitud: SolicitudTarifario = {
  montoCuotaIngreso: 500,
  montoMatricula: 450,
  montoPension: 700,
  diasGracia: 5,
  recargoPorcentaje: 2,
  topeRecargoPorcentaje: 10,
};

const administrativo = async () =>
  createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] });

const Confirmar = ({ alConfirmar }: { alConfirmar?: () => void }) => {
  const confirmar = useConfirmarTarifario(alConfirmar);
  const t2027 = useTarifario({ anioEscolarId: 'anio-2027' });
  const t2028 = useTarifario({ anioEscolarId: 'anio-2028' });
  return (
    <div>
      <p>
        2027: {t2027.data?.estado} · pensión{' '}
        {t2027.data?.conceptos.montoPension}
      </p>
      <p>2028: pensión {t2028.data?.conceptos.montoPension}</p>
      <button
        onClick={() =>
          confirmar.mutate({ anioEscolarId: 'anio-2027', data: solicitud })
        }
      >
        confirmar
      </button>
    </div>
  );
};

test('confirma con el cuerpo de la solicitud y deja en la consulta lo que devolvió el backend, sin volver a pedirlo', async () => {
  let cuerpo: unknown = null;
  let lecturas2027 = 0;
  server.use(
    http.get(API, ({ request }) => {
      const anio = new URL(request.url).searchParams.get('anioEscolarId');
      if (anio === 'anio-2027') lecturas2027++;
      return HttpResponse.json(tarifario(anio === 'anio-2027' ? 2027 : 2028));
    }),
    http.post(`${API}/anio-2027/confirmar`, async ({ request }) => {
      cuerpo = await request.json();
      return HttpResponse.json(
        tarifario(2027, {
          estado: 'CONFIRMADO',
          conceptos: { ...tarifario(2027).conceptos, montoPension: 700 },
        }),
      );
    }),
  );
  const alConfirmar = vi.fn();
  await renderApp(<Confirmar alConfirmar={alConfirmar} />, {
    user: await administrativo(),
  });
  await screen.findByText('2027: EN_CONFIGURACION · pensión 620');

  await userEvent.click(screen.getByRole('button', { name: 'confirmar' }));

  expect(
    await screen.findByText('2027: CONFIRMADO · pensión 700'),
  ).toBeInTheDocument();
  expect(cuerpo).toEqual(solicitud);
  expect(alConfirmar).toHaveBeenCalledTimes(1);
  expect(alConfirmar.mock.calls[0][0].estado).toBe('CONFIRMADO');
  expect(lecturas2027).toBe(1);
});

test('vuelve a pedir el tarifario de los otros años, que parten de los valores de este', async () => {
  let lecturas2028 = 0;
  server.use(
    http.get(API, ({ request }) => {
      const anio = new URL(request.url).searchParams.get('anioEscolarId');
      if (anio === 'anio-2028') lecturas2028++;
      return HttpResponse.json(tarifario(anio === 'anio-2027' ? 2027 : 2028));
    }),
    http.post(`${API}/anio-2027/confirmar`, () =>
      HttpResponse.json(tarifario(2027, { estado: 'CONFIRMADO' })),
    ),
  );
  await renderApp(<Confirmar />, { user: await administrativo() });
  await screen.findByText('2028: pensión 620');
  expect(lecturas2028).toBe(1);

  await userEvent.click(screen.getByRole('button', { name: 'confirmar' }));

  await waitFor(() => expect(lecturas2028).toBe(2));
});

test('refresca el detalle de un periodo de matrícula, que depende del tarifario confirmado', async () => {
  let lecturasDetalle = 0;
  const Detalle = () => {
    const periodo = usePeriodoMatricula('periodo-2027');
    return <p>Detalle: {periodo.data?.titulo}</p>;
  };
  const Ambos = () => (
    <>
      <Detalle />
      <Confirmar />
    </>
  );
  server.use(
    http.get(API, () => HttpResponse.json(tarifario(2027))),
    http.get(`${API_PERIODOS}/periodo-2027`, () => {
      lecturasDetalle++;
      return HttpResponse.json({ titulo: 'Matrícula 2027' });
    }),
    http.post(`${API}/anio-2027/confirmar`, () =>
      HttpResponse.json(tarifario(2027, { estado: 'CONFIRMADO' })),
    ),
  );
  await renderApp(<Ambos />, { user: await administrativo() });
  await screen.findByText('Detalle: Matrícula 2027');
  expect(lecturasDetalle).toBe(1);

  await userEvent.click(screen.getByRole('button', { name: 'confirmar' }));

  await waitFor(() => expect(lecturasDetalle).toBe(2));
});

test('si el backend rechaza (409 o 400), no avisa de éxito y su mensaje sale como notificación', async () => {
  const mensaje =
    'Ya se generaron compromisos de pago 2027: los montos y el calendario no se pueden modificar.';
  server.use(
    http.get(API, () => HttpResponse.json(tarifario(2027))),
    http.post(`${API}/anio-2027/confirmar`, () =>
      HttpResponse.json({ message: mensaje }, { status: 409 }),
    ),
  );
  const alConfirmar = vi.fn();
  await renderApp(<Confirmar alConfirmar={alConfirmar} />, {
    user: await administrativo(),
  });
  await screen.findByText('2027: EN_CONFIGURACION · pensión 620');

  await userEvent.click(screen.getByRole('button', { name: 'confirmar' }));

  expect(await screen.findByText(mensaje)).toBeInTheDocument();
  expect(alConfirmar).not.toHaveBeenCalled();
  expect(
    screen.getByText('2027: EN_CONFIGURACION · pensión 620'),
  ).toBeInTheDocument();
});
