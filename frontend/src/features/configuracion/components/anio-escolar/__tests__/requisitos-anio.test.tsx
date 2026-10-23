import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import { createUser, renderApp, screen, userEvent } from '@/testing/test-utils';
import {
  AnioEscolarResumen,
  CodigoRequisito,
  Requisito,
  RequisitosAnio,
} from '@/types/api';

import {
  destinoDeRevision,
  RequisitosAnio as TarjetaRequisitos,
} from '../requisitos-anio';

// Figma > Personal Administrativo > CI-06 · "Card - Requisitos": "Antes de activar el año"

const API = env.API_URL;

const requisito = (
  codigo: CodigoRequisito,
  texto: string,
  cumplido: boolean,
  detalle: string,
): Requisito => ({ codigo, texto, cumplido, detalle });

const requisitos: RequisitosAnio = {
  puedeActivar: true,
  obligatorios: [
    requisito(
      'PERIODOS',
      'Periodos académicos definidos',
      true,
      '3 trimestres del 01/03/2027 al 17/12/2027',
    ),
    requisito(
      'SECCIONES',
      'Secciones creadas',
      true,
      '23 secciones activas en 11 grados',
    ),
    requisito(
      'ESTRUCTURA_HORARIA',
      'Estructura horaria registrada',
      true,
      'EH-PRI-27 · EH-SEC-27',
    ),
  ],
  recomendados: [
    requisito(
      'TUTORES',
      'Tutores asignados (recomendado)',
      false,
      '10 de 23 secciones aún no tienen tutor',
    ),
    requisito(
      'PLAN_EVALUACION',
      'Plan de evaluación por grado (recomendado)',
      false,
      '8 de 11 grados con cursos asignados',
    ),
  ],
};

const resumen = (estado: AnioEscolarResumen['estado']): AnioEscolarResumen => ({
  id: 'anio-2026',
  anio: 2026,
  fechaInicio: '2026-03-02',
  fechaFin: '2026-12-18',
  regimen: 'BIMESTRAL',
  cantidadPeriodos: 4,
  seccionesActivas: 23,
  estructurasHorarias: 2,
  estado,
});

const servir = (
  anios: AnioEscolarResumen[],
  datos: RequisitosAnio = requisitos,
) =>
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json(anios),
    ),
    http.get(`${API}/configuracion/anios-escolares/anio-2027/requisitos`, () =>
      HttpResponse.json(datos),
    ),
  );

const mostrar = async (onRevisar = vi.fn()) => {
  await renderApp(
    <TarjetaRequisitos anio={{ id: 'anio-2027' }} onRevisar={onRevisar} />,
    { user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }) },
  );
  return onRevisar;
};

test('muestra los cinco requisitos con su título y detalle, cumplidos o pendientes', async () => {
  servir([]);
  await mostrar();

  expect(
    await screen.findByRole('heading', { name: 'Antes de activar el año' }),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      'Los tres primeros requisitos son obligatorios. Los demás se pueden completar después de activarlo.',
    ),
  ).toBeInTheDocument();
  expect(screen.getAllByRole('listitem')).toHaveLength(5);
  for (const r of [...requisitos.obligatorios, ...requisitos.recomendados]) {
    expect(screen.getByText(r.texto)).toBeInTheDocument();
    expect(screen.getByText(r.detalle)).toBeInTheDocument();
  }
  expect(screen.getAllByRole('img', { name: 'Cumplido' })).toHaveLength(3);
  expect(screen.getAllByRole('img', { name: 'Pendiente' })).toHaveLength(2);
});

test('"Revisar" lleva a la pestaña de cada requisito', async () => {
  servir([]);
  const onRevisar = await mostrar();
  await screen.findByText('Secciones creadas');

  await userEvent.click(
    screen.getByRole('button', {
      name: 'Revisar Periodos académicos definidos',
    }),
  );
  expect(onRevisar).toHaveBeenLastCalledWith('periodos');

  await userEvent.click(
    screen.getByRole('button', { name: 'Revisar Secciones creadas' }),
  );
  expect(onRevisar).toHaveBeenLastCalledWith('secciones');

  await userEvent.click(
    screen.getByRole('button', {
      name: 'Revisar Tutores asignados (recomendado)',
    }),
  );
  expect(onRevisar).toHaveBeenLastCalledWith('secciones');

  await userEvent.click(
    screen.getByRole('button', {
      name: 'Revisar Estructura horaria registrada',
    }),
  );
  expect(onRevisar).toHaveBeenLastCalledWith('estructura-horaria');
  expect(onRevisar).toHaveBeenCalledTimes(4);
});

test('el plan de evaluación se revisa en Competencias, donde se configura', async () => {
  servir([]);
  const onRevisar = await mostrar();

  const enlace = await screen.findByRole('link', {
    name: 'Revisar Plan de evaluación por grado (recomendado)',
  });

  expect(enlace).toHaveAttribute('href', '/app/administrativo/competencias');
  // Es un enlace de verdad, no un cambio de pestaña.
  expect(onRevisar).not.toHaveBeenCalled();
});

test('con otro año ACTIVO muestra "El año 2026 sigue activo"', async () => {
  servir([resumen('ACTIVO')]);
  await mostrar();

  expect(
    await screen.findByText('El año 2026 sigue activo'),
  ).toBeInTheDocument();
});

test('sin otro año activo no muestra ese aviso', async () => {
  servir([resumen('CERRADO')]);
  await mostrar();

  await screen.findByText('Secciones creadas');
  expect(screen.queryByText(/sigue activo/)).not.toBeInTheDocument();
});

test('si los requisitos no cargan, la tarjeta no aparece', async () => {
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([]),
    ),
    http.get(`${API}/configuracion/anios-escolares/anio-2027/requisitos`, () =>
      HttpResponse.json(
        { message: 'El año escolar no existe.' },
        { status: 404 },
      ),
    ),
  );
  await mostrar();

  expect(
    await screen.findByText('El año escolar no existe.'),
  ).toBeInTheDocument();
  expect(
    screen.queryByRole('heading', { name: 'Antes de activar el año' }),
  ).not.toBeInTheDocument();
});

test('destinoDeRevision: cuatro requisitos cambian de pestaña y el plan va a Competencias', () => {
  expect(destinoDeRevision('PERIODOS')).toEqual({ pestana: 'periodos' });
  expect(destinoDeRevision('SECCIONES')).toEqual({ pestana: 'secciones' });
  expect(destinoDeRevision('TUTORES')).toEqual({ pestana: 'secciones' });
  expect(destinoDeRevision('ESTRUCTURA_HORARIA')).toEqual({
    pestana: 'estructura-horaria',
  });
  expect(destinoDeRevision('PLAN_EVALUACION')).toEqual({
    ruta: '/app/administrativo/competencias',
  });
});
