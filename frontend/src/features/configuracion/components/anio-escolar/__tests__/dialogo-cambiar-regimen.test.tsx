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

import { DialogoCambiarRegimen } from '../dialogo-cambiar-regimen';

import { anio2027 } from './datos';

// Figma > Personal Administrativo > "Modal - Cambiar régimen de periodos" (HU-74)

const API = env.API_URL;

const abrir = async () => {
  const onCerrar = vi.fn();
  await renderApp(
    <DialogoCambiarRegimen abierto anio={anio2027} onCerrar={onCerrar} />,
    { user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }) },
  );
  return onCerrar;
};

test('abre con el otro régimen marcado y señala el actual, como en Figma', async () => {
  await abrir();

  expect(
    screen.getByRole('heading', { name: 'Cambiar régimen de periodos' }),
  ).toBeInTheDocument();
  expect(screen.getByText('Año escolar 2027')).toBeInTheDocument();
  expect(screen.getByRole('radio', { name: /^Bimestral/ })).toBeChecked();
  expect(screen.getByRole('radio', { name: /^Trimestral/ })).not.toBeChecked();
  expect(screen.getByText('4 bimestres al año')).toBeInTheDocument();
  expect(screen.getByText('3 trimestres al año (actual)')).toBeInTheDocument();
  expect(
    screen.getByText(
      'Se volverán a generar los periodos con fechas sugeridas.',
    ),
  ).toBeInTheDocument();
  expect(
    screen.getByText('Luego podrás ajustar las fechas de cada periodo.'),
  ).toBeInTheDocument();
});

test('cambia el régimen, avisa y cierra', async () => {
  let cuerpo: unknown = null;
  server.use(
    http.put(
      `${API}/configuracion/anios-escolares/anio-2027/regimen`,
      async ({ request }) => {
        cuerpo = await request.json();
        return HttpResponse.json({ ...anio2027, regimen: 'BIMESTRAL' });
      },
    ),
  );
  const onCerrar = await abrir();

  await userEvent.click(
    screen.getByRole('button', { name: 'Cambiar régimen' }),
  );

  await waitFor(() => expect(onCerrar).toHaveBeenCalled());
  expect(cuerpo).toEqual({ regimen: 'BIMESTRAL' });
  expect(await screen.findByText('Régimen actualizado')).toBeInTheDocument();
  expect(
    screen.getByText('Año escolar 2027 · Régimen bimestral'),
  ).toBeInTheDocument();
});

test('con el régimen actual elegido no se puede enviar', async () => {
  await abrir();

  await userEvent.click(screen.getByRole('radio', { name: /^Trimestral/ }));

  expect(
    screen.getByRole('button', { name: 'Cambiar régimen' }),
  ).toBeDisabled();
});

test('si el backend rechaza, el modal sigue abierto', async () => {
  server.use(
    http.put(`${API}/configuracion/anios-escolares/anio-2027/regimen`, () =>
      HttpResponse.json(
        { message: 'Solo se puede modificar un año planificado.' },
        { status: 409 },
      ),
    ),
  );
  const onCerrar = await abrir();

  await userEvent.click(
    screen.getByRole('button', { name: 'Cambiar régimen' }),
  );

  expect(
    await screen.findByText('Solo se puede modificar un año planificado.'),
  ).toBeInTheDocument();
  expect(onCerrar).not.toHaveBeenCalled();
});

test('Cancelar cierra sin enviar nada', async () => {
  const onCerrar = await abrir();

  await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

  expect(onCerrar).toHaveBeenCalled();
});
