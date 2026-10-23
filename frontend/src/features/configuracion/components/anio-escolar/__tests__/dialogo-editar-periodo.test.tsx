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
import { AnioEscolarDetalle, PeriodoDetalle } from '@/types/api';

import { DialogoEditarPeriodo } from '../dialogo-editar-periodo';

import { anio2026, anioCorto } from './datos';

// Figma > Personal Administrativo > "Modal - Editar periodo" (HU-74)
//
// Con `anioCorto` (periodos del 10 al 18 y del 20 al 31 de marzo de 2027) todas las fechas que se prueban están en
// el mismo mes del calendario: el selector abre en el mes del valor actual.

const API = env.API_URL;

const abrir = async (
  periodo: PeriodoDetalle,
  anio: AnioEscolarDetalle = anioCorto,
) => {
  const onCerrar = vi.fn();
  await renderApp(
    <DialogoEditarPeriodo periodo={periodo} anio={anio} onCerrar={onCerrar} />,
    { user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }) },
  );
  return onCerrar;
};

const elegirDia = async (valorActual: string, dia: string) => {
  await userEvent.click(screen.getByText(valorActual));
  await userEvent.click(screen.getByRole('button', { name: dia }));
};

const guardar = () =>
  userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }));

test('abre con los datos del periodo, el año y la alerta de Figma', async () => {
  await abrir(anioCorto.periodos[0]);

  expect(
    screen.getByRole('heading', { name: 'Editar I Bimestre' }),
  ).toBeInTheDocument();
  expect(
    screen.getByText('Año escolar 2027 · del 05/03/2027 al 17/12/2027'),
  ).toBeInTheDocument();
  expect(screen.getByLabelText('Nombre del periodo *')).toHaveValue(
    'I Bimestre',
  );
  expect(screen.getByText('10/03/2027')).toBeInTheDocument();
  expect(screen.getByText('18/03/2027')).toBeInTheDocument();
  expect(screen.getByText('25/03/2027')).toBeInTheDocument();
  expect(
    screen.getByText('Debe ser igual o posterior al fin del periodo.'),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      'Los docentes verán las nuevas fechas en el registro de notas.',
    ),
  ).toBeInTheDocument();
  // Un periodo planificado se puede editar entero.
  expect(screen.getByLabelText('Nombre del periodo *')).toBeEnabled();
  expect(screen.queryByText(/ya empezó/)).not.toBeInTheDocument();
});

test('guarda los cambios, avisa y cierra', async () => {
  let cuerpo: unknown = null;
  server.use(
    http.put(
      `${API}/configuracion/periodos-academicos/p1`,
      async ({ request }) => {
        cuerpo = await request.json();
        return HttpResponse.json({
          ...anioCorto.periodos[0],
          nombre: 'Primer bimestre',
        });
      },
    ),
  );
  const onCerrar = await abrir(anioCorto.periodos[0]);

  const nombre = screen.getByLabelText('Nombre del periodo *');
  await userEvent.clear(nombre);
  await userEvent.type(nombre, 'Primer bimestre');
  await elegirDia('25/03/2027', '30/03/2027');
  await guardar();

  await waitFor(() => expect(onCerrar).toHaveBeenCalled());
  expect(cuerpo).toEqual({
    nombre: 'Primer bimestre',
    fechaInicio: '2027-03-10',
    fechaFin: '2027-03-18',
    fechaLimiteNotas: '2027-03-30',
  });
  expect(await screen.findByText('Periodo actualizado')).toBeInTheDocument();
});

describe('validación en el formulario, bajo cada campo', () => {
  let enviado = false;
  beforeEach(() => {
    enviado = false;
    server.use(
      http.put(`${API}/configuracion/periodos-academicos/p1`, () => {
        enviado = true;
        return HttpResponse.json(anioCorto.periodos[0]);
      }),
    );
  });

  test('el nombre es obligatorio', async () => {
    await abrir(anioCorto.periodos[0]);

    await userEvent.clear(screen.getByLabelText('Nombre del periodo *'));
    await guardar();

    expect(
      await screen.findByText('Ingresa el nombre del periodo.'),
    ).toBeInTheDocument();
    expect(enviado).toBe(false);
  });

  test('un cruce con otro periodo se avisa bajo la fecha de fin', async () => {
    await abrir(anioCorto.periodos[0]);

    await elegirDia('18/03/2027', '25/03/2027');
    await guardar();

    expect(
      await screen.findByText(
        'Se cruza con el II Bimestre, que empieza el 20/03/2027.',
      ),
    ).toBeInTheDocument();
    expect(enviado).toBe(false);
  });

  test('el fin debe ser posterior al inicio', async () => {
    await abrir(anioCorto.periodos[0]);

    await elegirDia('18/03/2027', '10/03/2027');
    await guardar();

    expect(
      await screen.findByText(
        'La fecha de fin debe ser posterior a la fecha de inicio.',
      ),
    ).toBeInTheDocument();
    expect(enviado).toBe(false);
  });

  test('las fechas deben estar dentro del año escolar', async () => {
    await abrir(anioCorto.periodos[0]);

    await elegirDia('10/03/2027', '2/03/2027');
    await guardar();

    expect(
      await screen.findByText(
        'Las fechas del periodo deben estar dentro del año escolar (05/03/2027 al 17/12/2027).',
      ),
    ).toBeInTheDocument();
    expect(enviado).toBe(false);
  });

  test('la fecha límite no puede ser anterior al fin', async () => {
    await abrir(anioCorto.periodos[0]);

    await elegirDia('25/03/2027', '12/03/2027');
    await guardar();

    expect(
      await screen.findByText(
        'La fecha límite de notas no puede ser anterior al fin del periodo.',
      ),
    ).toBeInTheDocument();
    expect(enviado).toBe(false);
  });
});

test('un periodo en curso no deja cambiar la fecha de inicio', async () => {
  const enCurso = anio2026.periodos[2];
  await abrir(enCurso, anio2026);

  expect(
    screen.getByText('El periodo ya empezó: no se puede cambiar.'),
  ).toBeInTheDocument();
  // El resto sí se edita: el nombre, el fin y el límite.
  expect(screen.getByLabelText('Nombre del periodo *')).toBeEnabled();
  expect(
    screen.getByRole('button', { name: /Fecha de inicio/ }),
  ).toBeDisabled();
  expect(screen.getByRole('button', { name: /Fecha de fin/ })).toBeEnabled();
  expect(screen.getByRole('button', { name: /Fecha límite/ })).toBeEnabled();
});

test('un periodo terminado solo deja cambiar la fecha límite de notas', async () => {
  let cuerpo: unknown = null;
  server.use(
    http.put(
      `${API}/configuracion/periodos-academicos/p1`,
      async ({ request }) => {
        cuerpo = await request.json();
        return HttpResponse.json(anio2026.periodos[0]);
      },
    ),
  );
  const cerrado = anio2026.periodos[0];
  const onCerrar = await abrir(cerrado, anio2026);

  expect(
    screen.getByText(
      'El periodo ya terminó: solo se puede cambiar la fecha límite de notas.',
    ),
  ).toBeInTheDocument();
  expect(screen.getByLabelText('Nombre del periodo *')).toBeDisabled();
  expect(
    screen.getByRole('button', { name: /Fecha de inicio/ }),
  ).toBeDisabled();
  expect(screen.getByRole('button', { name: /Fecha de fin/ })).toBeDisabled();
  expect(screen.getByRole('button', { name: /Fecha límite/ })).toBeEnabled();

  await elegirDia('15/05/2026', '22/05/2026');
  await guardar();

  await waitFor(() => expect(onCerrar).toHaveBeenCalled());
  // Lo que no se edita viaja igual: el backend lo compara con lo guardado.
  expect(cuerpo).toEqual({
    nombre: 'I Bimestre',
    fechaInicio: '2026-03-02',
    fechaFin: '2026-05-08',
    fechaLimiteNotas: '2026-05-22',
  });
});

test('si el backend rechaza, el modal sigue abierto y el aviso sale como notificación', async () => {
  server.use(
    http.put(`${API}/configuracion/periodos-academicos/p1`, () =>
      HttpResponse.json(
        { message: 'El periodo ya empezó: no se puede cambiar.' },
        { status: 409 },
      ),
    ),
  );
  const onCerrar = await abrir(anioCorto.periodos[0]);

  await guardar();

  expect(
    await screen.findByText('El periodo ya empezó: no se puede cambiar.'),
  ).toBeInTheDocument();
  expect(onCerrar).not.toHaveBeenCalled();
  expect(screen.getByRole('dialog')).toBeInTheDocument();
});

test('Cancelar cierra sin enviar nada', async () => {
  const onCerrar = await abrir(anioCorto.periodos[0]);

  await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

  expect(onCerrar).toHaveBeenCalled();
});
