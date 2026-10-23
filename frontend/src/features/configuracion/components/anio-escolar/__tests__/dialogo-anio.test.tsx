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
import { AnioEscolarDetalle } from '@/types/api';

import { DialogoAnio } from '../dialogo-anio';

// Figma > Personal Administrativo > "Modal - Nuevo año escolar" (HU-74). Editar año reutiliza el modal.

const API = env.API_URL;

const anio2027: AnioEscolarDetalle = {
  id: 'anio-2027',
  anio: 2027,
  fechaInicio: '2027-03-01',
  fechaFin: '2027-12-17',
  regimen: 'TRIMESTRAL',
  estado: 'PLANIFICADO',
  cantidadSecciones: 10,
  cantidadEstructuras: 2,
  periodos: [],
};

const administrativo = async () =>
  createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] });

const abrirNuevo = async (
  extras: Partial<React.ComponentProps<typeof DialogoAnio>> = {},
) => {
  const onCerrar = vi.fn();
  const onGuardado = vi.fn();
  await renderApp(
    <DialogoAnio
      abierto
      modo="nuevo"
      aniosRegistrados={[{ anio: 2026 }, { anio: 2027 }]}
      onCerrar={onCerrar}
      onGuardado={onGuardado}
      {...extras}
    />,
    { user: await administrativo() },
  );
  return { onCerrar, onGuardado };
};

test('el modal cerrado no muestra nada', async () => {
  await renderApp(
    <DialogoAnio abierto={false} modo="nuevo" onCerrar={vi.fn()} />,
    { user: await administrativo() },
  );

  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

test('Nuevo: abre con el año siguiente al último, sus fechas y Bimestral, sin "Reutilizar configuración"', async () => {
  await abrirNuevo();

  expect(
    screen.getByRole('heading', { name: 'Nuevo año escolar' }),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      'Se creará en estado Planificado. Los periodos se generan según el régimen elegido.',
    ),
  ).toBeInTheDocument();
  expect(screen.getByLabelText('Año *')).toHaveValue('2028');
  expect(screen.getByText('01/03/2028')).toBeInTheDocument();
  expect(screen.getByText('15/12/2028')).toBeInTheDocument();
  expect(screen.getByRole('radio', { name: /^Bimestral/ })).toBeChecked();
  expect(screen.getByRole('radio', { name: /^Trimestral/ })).not.toBeChecked();
  expect(screen.getByText('4 bimestres al año')).toBeInTheDocument();
  expect(screen.getByText('3 trimestres al año')).toBeInTheDocument();
  expect(
    screen.queryByText(/Reutilizar configuración/),
  ).not.toBeInTheDocument();
});

test('Nuevo: al cambiar el año las fechas se recalculan', async () => {
  await abrirNuevo();

  const anio = screen.getByLabelText('Año *');
  await userEvent.clear(anio);
  await userEvent.type(anio, '2029');

  expect(screen.getByText('01/03/2029')).toBeInTheDocument();
  expect(screen.getByText('15/12/2029')).toBeInTheDocument();
});

test('Nuevo: si el usuario ya cambió una fecha, cambiar el año no la pisa', async () => {
  await abrirNuevo();

  await userEvent.click(screen.getByText('01/03/2028'));
  await userEvent.click(screen.getByRole('button', { name: '10/03/2028' }));
  expect(screen.getByText('10/03/2028')).toBeInTheDocument();

  const anio = screen.getByLabelText('Año *');
  await userEvent.clear(anio);
  await userEvent.type(anio, '2029');

  expect(screen.getByText('10/03/2028')).toBeInTheDocument();
  expect(screen.getByText('15/12/2028')).toBeInTheDocument();
  expect(screen.queryByText('01/03/2029')).not.toBeInTheDocument();
});

test('Nuevo: crea el año con el régimen elegido, avisa y cierra', async () => {
  let cuerpo: unknown = null;
  server.use(
    http.post(`${API}/configuracion/anios-escolares`, async ({ request }) => {
      cuerpo = await request.json();
      return HttpResponse.json({ ...anio2027, id: 'anio-2028', anio: 2028 });
    }),
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([]),
    ),
  );
  const { onCerrar, onGuardado } = await abrirNuevo();

  await userEvent.click(screen.getByRole('radio', { name: /^Trimestral/ }));
  await userEvent.click(
    screen.getByRole('button', { name: 'Crear año escolar' }),
  );

  await waitFor(() => expect(onGuardado).toHaveBeenCalled());
  expect(cuerpo).toEqual({
    anio: 2028,
    fechaInicio: '2028-03-01',
    fechaFin: '2028-12-15',
    regimen: 'TRIMESTRAL',
  });
  expect(onGuardado.mock.calls[0][0].id).toBe('anio-2028');
  expect(onCerrar).toHaveBeenCalled();
  expect(await screen.findByText('Año escolar creado')).toBeInTheDocument();
});

test('Nuevo: un año fuera de rango se rechaza bajo el campo y no se envía', async () => {
  let enviado = false;
  server.use(
    http.post(`${API}/configuracion/anios-escolares`, () => {
      enviado = true;
      return HttpResponse.json(anio2027);
    }),
  );
  const { onGuardado } = await abrirNuevo();

  const anio = screen.getByLabelText('Año *');
  await userEvent.clear(anio);
  await userEvent.type(anio, '1999');
  await userEvent.click(
    screen.getByRole('button', { name: 'Crear año escolar' }),
  );

  expect(
    await screen.findByText('El año escolar debe estar entre 2000 y 2100.'),
  ).toBeInTheDocument();
  expect(enviado).toBe(false);
  expect(onGuardado).not.toHaveBeenCalled();
});

test('Nuevo: si el backend rechaza (año repetido), el modal sigue abierto y el aviso sale como notificación', async () => {
  server.use(
    http.post(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json(
        { message: 'Ya existe un año escolar 2028.' },
        { status: 409 },
      ),
    ),
  );
  const { onCerrar } = await abrirNuevo();

  await userEvent.click(
    screen.getByRole('button', { name: 'Crear año escolar' }),
  );

  expect(
    await screen.findByText('Ya existe un año escolar 2028.'),
  ).toBeInTheDocument();
  expect(onCerrar).not.toHaveBeenCalled();
  expect(screen.getByRole('dialog')).toBeInTheDocument();
});

test('Cancelar cierra el modal sin enviar nada', async () => {
  const { onCerrar } = await abrirNuevo();

  await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

  expect(onCerrar).toHaveBeenCalled();
});

test('Editar: título con el año, año fijo, sin régimen y con el aviso de periodos', async () => {
  await renderApp(
    <DialogoAnio abierto modo="editar" anio={anio2027} onCerrar={vi.fn()} />,
    { user: await administrativo() },
  );

  expect(
    screen.getByRole('heading', { name: 'Editar año escolar 2027' }),
  ).toBeInTheDocument();
  expect(screen.getByLabelText('Año *')).toBeDisabled();
  expect(screen.getByLabelText('Año *')).toHaveValue('2027');
  expect(screen.getByText('01/03/2027')).toBeInTheDocument();
  expect(screen.getByText('17/12/2027')).toBeInTheDocument();
  expect(screen.queryByText('Régimen de periodos *')).not.toBeInTheDocument();
  expect(screen.queryByRole('radio')).not.toBeInTheDocument();
  expect(
    screen.getByText('Los periodos se regeneran con fechas sugeridas.'),
  ).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'Guardar cambios' }),
  ).toBeInTheDocument();
});

test('Editar: guarda solo las fechas', async () => {
  let cuerpo: unknown = null;
  server.use(
    http.put(
      `${API}/configuracion/anios-escolares/anio-2027`,
      async ({ request }) => {
        cuerpo = await request.json();
        return HttpResponse.json({ ...anio2027, fechaInicio: '2027-03-10' });
      },
    ),
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json([]),
    ),
  );
  const onCerrar = vi.fn();
  const onGuardado = vi.fn();
  await renderApp(
    <DialogoAnio
      abierto
      modo="editar"
      anio={anio2027}
      onCerrar={onCerrar}
      onGuardado={onGuardado}
    />,
    { user: await administrativo() },
  );

  await userEvent.click(screen.getByText('01/03/2027'));
  await userEvent.click(screen.getByRole('button', { name: '10/03/2027' }));
  await userEvent.click(
    screen.getByRole('button', { name: 'Guardar cambios' }),
  );

  await waitFor(() => expect(onGuardado).toHaveBeenCalled());
  expect(cuerpo).toEqual({
    fechaInicio: '2027-03-10',
    fechaFin: '2027-12-17',
  });
  expect(onCerrar).toHaveBeenCalled();
  expect(
    await screen.findByText('Año escolar actualizado'),
  ).toBeInTheDocument();
});
