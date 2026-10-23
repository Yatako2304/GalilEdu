import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import { createUser, renderApp, screen } from '@/testing/test-utils';
import { TarifarioVista } from '@/types/api';

import { useTarifario } from '../tarifarios';

// Figma > Personal Administrativo > "P19 · Tarifario y calendario" (HU-34)

const API = `${env.API_URL}/configuracion/tarifarios`;

const tarifario2027: TarifarioVista = {
  anioEscolarId: 'anio-2027',
  anio: 2027,
  existe: true,
  estado: 'EN_CONFIGURACION',
  soloLectura: false,
  motivoSoloLectura: null,
  calendarioDesactualizado: false,
  conceptos: {
    montoCuotaIngreso: 500,
    montoMatricula: 450,
    vencimientoMatricula: '2027-02-28',
    montoPension: 620,
    cantidadCuotas: 10,
  },
  morosidad: { diasGracia: 5, recargoPorcentaje: 2, topeRecargoPorcentaje: 10 },
  calendario: {
    cuotas: [
      {
        numero: 1,
        nombre: 'Pensión de marzo',
        fechaVencimiento: '2027-03-31',
        monto: 620,
      },
    ],
    totalPensiones: 6200,
  },
};

const administrativo = async () =>
  createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] });

const Ver = ({ anioEscolarId }: { anioEscolarId: string | undefined }) => {
  const tarifario = useTarifario({ anioEscolarId });
  return (
    <p>
      {tarifario.data
        ? `${tarifario.data.anio} · ${tarifario.data.estado} · pensión ${tarifario.data.conceptos.montoPension}`
        : 'sin datos'}
    </p>
  );
};

test('pide el tarifario del año con su anioEscolarId', async () => {
  let consulta: URLSearchParams | null = null;
  server.use(
    http.get(API, ({ request }) => {
      consulta = new URL(request.url).searchParams;
      return HttpResponse.json(tarifario2027);
    }),
  );

  await renderApp(<Ver anioEscolarId="anio-2027" />, {
    user: await administrativo(),
  });

  expect(
    await screen.findByText('2027 · EN_CONFIGURACION · pensión 620'),
  ).toBeInTheDocument();
  expect(Object.fromEntries(consulta!.entries())).toEqual({
    anioEscolarId: 'anio-2027',
  });
});

test('sin año elegido no pide nada (sin handler, una petición haría fallar la prueba)', async () => {
  await renderApp(<Ver anioEscolarId={undefined} />, {
    user: await administrativo(),
  });

  expect(screen.getByText('sin datos')).toBeInTheDocument();
});

test('una propuesta sin tarifario del año anterior llega con los montos vacíos', async () => {
  server.use(
    http.get(API, () =>
      HttpResponse.json({
        ...tarifario2027,
        existe: false,
        conceptos: {
          ...tarifario2027.conceptos,
          montoCuotaIngreso: null,
          montoMatricula: null,
          montoPension: null,
        },
        morosidad: {
          diasGracia: null,
          recargoPorcentaje: null,
          topeRecargoPorcentaje: null,
        },
        calendario: { ...tarifario2027.calendario, totalPensiones: null },
      } satisfies TarifarioVista),
    ),
  );

  await renderApp(<Ver anioEscolarId="anio-2027" />, {
    user: await administrativo(),
  });

  expect(
    await screen.findByText('2027 · EN_CONFIGURACION · pensión null'),
  ).toBeInTheDocument();
});

test('un año inexistente (404) no entrega datos y el mensaje sale como notificación', async () => {
  server.use(
    http.get(API, () =>
      HttpResponse.json(
        { message: 'El año escolar no existe.' },
        { status: 404 },
      ),
    ),
  );

  await renderApp(<Ver anioEscolarId="anio-9999" />, {
    user: await administrativo(),
  });

  expect(
    await screen.findByText('El año escolar no existe.'),
  ).toBeInTheDocument();
  expect(screen.getByText('sin datos')).toBeInTheDocument();
});
