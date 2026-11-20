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
import { FilaCalificacion } from '@/types/api';

import { DialogoModificarCalificacion } from '../dialogo-modificar-calificacion';

// HU-60: modificar una nota ya registrada con motivo y ver su historial.

const API = env.API_URL;
const RUTA = `${API}/calificaciones/docente/items/item-1/calificaciones/mat-ana`;

const fila: FilaCalificacion = {
  id: 'cal-1',
  matriculaId: 'mat-ana',
  estudianteId: 'est-ana',
  estudiante: 'Ana Lucía García López',
  notaNumerica: 12,
  nivelLogro: null,
  evidencia: null,
  estado: 'CALIFICADO',
  modificaciones: 1,
  noEvaluado: false,
  motivoNoEvaluado: null,
};

const vacia: FilaCalificacion = {
  ...fila,
  id: null,
  notaNumerica: null,
  estado: 'PENDIENTE',
  modificaciones: 0,
};

const preparar = () => {
  const enviadas: unknown[] = [];
  server.use(
    http.get(`${RUTA}/historial`, () =>
      HttpResponse.json([
        {
          notaAnterior: 10,
          nivelAnterior: null,
          noEvaluadoAnterior: false,
          notaNueva: 12,
          nivelNuevo: null,
          noEvaluadoNuevo: false,
          motivo: 'Corrección durante el registro de notas',
          modificadoPor: 'María Fernández Ruiz',
          modificadoEn: '2026-10-10T15:00:00Z',
        },
      ]),
    ),
    http.put(RUTA, async ({ request }) => {
      enviadas.push(await request.json());
      return HttpResponse.json({
        ...fila,
        notaNumerica: 15,
        modificaciones: 2,
      });
    }),
  );
  return enviadas;
};

const renderDialogo = async (
  onCerrar: () => void = vi.fn(),
  { celda = fila, enPlazo = false } = {},
) =>
  renderApp(
    <DialogoModificarCalificacion
      itemId="item-1"
      item="Examen bimestral"
      escala="VIGESIMAL"
      fila={celda}
      enPlazo={enPlazo}
      onCerrar={onCerrar}
    />,
    { user: await createUser({ roles: [ROLES.DOCENTE] }) },
  );

test('muestra la nota actual y el historial de cambios', async () => {
  preparar();
  await renderDialogo();

  expect(
    await screen.findByText('Corrección durante el registro de notas'),
  ).toBeInTheDocument();
  expect(screen.getByLabelText('Valor actual')).toHaveValue('12');
  expect(screen.getByText('10 → 12')).toBeInTheDocument();
});

test('exige un motivo suficiente antes de enviar', async () => {
  const enviadas = preparar();
  await renderDialogo();

  await userEvent.type(
    await screen.findByLabelText(/nueva calificación/i),
    '15',
  );
  await userEvent.type(
    screen.getByRole('textbox', { name: /motivo/i }),
    'Error',
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Guardar modificación' }),
  );

  expect(screen.getByRole('alert')).toHaveTextContent(/al menos 10 caracteres/);
  expect(enviadas).toHaveLength(0);
});

test('envía la nueva nota con su motivo y cierra', async () => {
  const enviadas = preparar();
  const onCerrar = vi.fn();
  await renderDialogo(onCerrar);

  await userEvent.type(
    await screen.findByLabelText(/nueva calificación/i),
    '15',
  );
  await userEvent.type(
    screen.getByRole('textbox', { name: /motivo/i }),
    'Error al transcribir el examen',
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Guardar modificación' }),
  );

  await waitFor(() => expect(onCerrar).toHaveBeenCalled());
  expect(enviadas).toEqual([
    {
      notaNumerica: 15,
      nivelLogro: null,
      noEvaluado: false,
      motivo: 'Error al transcribir el examen',
    },
  ]);
});

test('«No evaluado» no pide nota, pero sí motivo', async () => {
  const enviadas = preparar();
  const onCerrar = vi.fn();
  await renderDialogo(onCerrar, { celda: vacia });

  expect(
    await screen.findByRole('heading', { name: 'Registrar calificación' }),
  ).toBeVisible();
  await userEvent.click(screen.getByRole('radio', { name: /no evaluado/i }));
  expect(
    screen.queryByLabelText(/nueva calificación/i),
  ).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: 'Registrar' }));
  expect(screen.getByRole('alert')).toHaveTextContent(/motivo/);

  await userEvent.type(
    screen.getByRole('textbox', { name: /motivo/i }),
    'Se matriculó después del examen',
  );
  await userEvent.click(screen.getByRole('button', { name: 'Registrar' }));
  await waitFor(() => expect(onCerrar).toHaveBeenCalled());
  expect(enviadas).toEqual([
    {
      notaNumerica: null,
      nivelLogro: null,
      noEvaluado: true,
      motivo: 'Se matriculó después del examen',
    },
  ]);
});

test('dentro del plazo, registrar una nota nueva no pide motivo', async () => {
  const enviadas = preparar();
  const onCerrar = vi.fn();
  await renderDialogo(onCerrar, { celda: vacia, enPlazo: true });

  await userEvent.type(
    await screen.findByLabelText(/nueva calificación/i),
    '14',
  );
  await userEvent.click(screen.getByRole('button', { name: 'Registrar' }));

  await waitFor(() => expect(onCerrar).toHaveBeenCalled());
  expect(enviadas).toEqual([
    { notaNumerica: 14, nivelLogro: null, noEvaluado: false },
  ]);
});
