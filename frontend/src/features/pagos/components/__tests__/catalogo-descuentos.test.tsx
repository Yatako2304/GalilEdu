import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent, waitFor } from '@/testing/test-utils';

import { CatalogoDescuentos } from '../catalogo-descuentos';
import { descuentoSchema } from '../esquema-descuento';

const descuentos = [
  {
    id: 'descuento-1',
    nombre: 'Beca institucional',
    descripcion: 'Beneficio por rendimiento académico.',
    tipo: 'PORCENTAJE_UNICO',
    aplicacion: 'MANUAL',
    porcentaje: 50,
    tramos: [],
    conceptos: ['PENSION', 'MATRICULA'],
    condicion: null,
    estado: 'ACTIVO',
  },
  {
    id: 'descuento-2',
    nombre: 'Hermanos matriculados',
    descripcion: null,
    tipo: 'POR_HERMANOS',
    aplicacion: 'AUTOMATICO',
    porcentaje: null,
    tramos: [
      { hermanosDesde: 2, hermanosHasta: 2, porcentaje: 10 },
      { hermanosDesde: 3, hermanosHasta: null, porcentaje: 15 },
    ],
    conceptos: ['PENSION'],
    condicion: 'Tener al menos dos hermanos matriculados.',
    estado: 'ACTIVO',
  },
] as const;

beforeEach(() => {
  server.use(
    http.get(`${env.API_URL}/pagos/descuentos`, () =>
      HttpResponse.json(descuentos),
    ),
    http.get(`${env.API_URL}/pagos/descuentos/configuracion`, () =>
      HttpResponse.json({ porcentajeMaximo: 50 }),
    ),
  );
});

test('muestra el catálogo y el formato de los porcentajes', async () => {
  await renderApp(
    <CatalogoDescuentos
      nuevoAbierto={false}
      onNuevoAbiertoChange={() => undefined}
    />,
  );

  expect(await screen.findByText('Beca institucional')).toBeInTheDocument();
  expect(screen.getByText('50 %')).toBeInTheDocument();
  expect(screen.getByText('2 hermanos: 10 % · 3+: 15 %')).toBeInTheDocument();
  expect(screen.getByText('Pensiones · Matrícula')).toBeInTheDocument();
});

test('el formulario por hermanos empieza con dos tramos y permite agregar otro', async () => {
  await renderApp(
    <CatalogoDescuentos nuevoAbierto onNuevoAbiertoChange={() => undefined} />,
  );

  expect(
    await screen.findByRole('heading', { name: 'Nuevo descuento' }),
  ).toBeInTheDocument();
  await userEvent.selectOptions(
    screen.getByLabelText('Tipo *'),
    'POR_HERMANOS',
  );
  expect(screen.getAllByLabelText('Desde *')).toHaveLength(2);
  await userEvent.click(screen.getByRole('button', { name: 'Agregar tramo' }));
  expect(screen.getAllByLabelText('Desde *')).toHaveLength(3);
});

test('permite quitar un tramo mientras conserva al menos una fila editable', async () => {
  await renderApp(
    <CatalogoDescuentos nuevoAbierto onNuevoAbiertoChange={() => undefined} />,
  );

  await userEvent.selectOptions(
    await screen.findByLabelText('Tipo *'),
    'POR_HERMANOS',
  );
  await userEvent.click(screen.getByRole('button', { name: 'Quitar tramo 2' }));

  expect(screen.getAllByLabelText('Desde *')).toHaveLength(1);
  expect(screen.getByRole('button', { name: 'Quitar tramo 1' })).toBeDisabled();
});

test('avisa si Desde es mayor que Hasta y retira el aviso al corregirlo', async () => {
  await renderApp(
    <CatalogoDescuentos nuevoAbierto onNuevoAbiertoChange={() => undefined} />,
  );

  await userEvent.selectOptions(
    await screen.findByLabelText('Tipo *'),
    'POR_HERMANOS',
  );
  const desde = screen.getAllByLabelText('Desde *')[0];
  const hasta = screen.getAllByLabelText('Hasta')[0];

  await userEvent.clear(desde);
  await userEvent.type(desde, '4');
  expect(
    await screen.findByText('El inicio no puede ser mayor que el final.'),
  ).toBeInTheDocument();

  await userEvent.clear(hasta);
  await userEvent.type(hasta, '5');
  await waitFor(() =>
    expect(
      screen.queryByText('El inicio no puede ser mayor que el final.'),
    ).not.toBeInTheDocument(),
  );
});

test('rechaza tramos superpuestos y acepta rangos separados en cualquier orden', () => {
  const valores = {
    nombre: 'Hermanos',
    descripcion: '',
    tipo: 'POR_HERMANOS' as const,
    aplicacion: 'AUTOMATICO' as const,
    porcentaje: '',
    conceptos: ['PENSION'],
    condicion: '',
    tramos: [
      { hermanosDesde: '2', hermanosHasta: '4', porcentaje: '10' },
      { hermanosDesde: '4', hermanosHasta: '6', porcentaje: '15' },
    ],
  };

  const superpuestos = descuentoSchema.safeParse(valores);
  expect(superpuestos.success).toBe(false);
  if (!superpuestos.success) {
    expect(superpuestos.error.issues).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          path: ['tramos', 1, 'hermanosDesde'],
          message: 'Este tramo se superpone con el tramo 1.',
        }),
      ]),
    );
  }

  expect(
    descuentoSchema.safeParse({
      ...valores,
      tramos: [
        { hermanosDesde: '5', hermanosHasta: '7', porcentaje: '15' },
        { hermanosDesde: '2', hermanosHasta: '4', porcentaje: '10' },
      ],
    }).success,
  ).toBe(true);
});
