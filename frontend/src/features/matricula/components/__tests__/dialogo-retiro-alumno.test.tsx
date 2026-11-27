import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent, waitFor } from '@/testing/test-utils';

import { DialogoRetiroAlumno } from '../dialogo-retiro-alumno';

// Retiro del alumno del colegio (fila 36 del plan de pruebas): el mismo modal en Estudiantes, Matrícula y Pensiones.

const abrir = () =>
  renderApp(
    <DialogoRetiroAlumno
      abierto
      onOpenChange={() => undefined}
      estudianteId="sofia"
      estudiante="Sofía Paredes Rojas"
    />,
  );

test('pide el motivo y registra el retiro con la fecha y el motivo', async () => {
  let solicitud: Record<string, unknown> | undefined;
  server.use(
    http.post(
      `${env.API_URL}/matricula/matriculas/de-estudiante/:id/retiro`,
      async ({ request, params }) => {
        solicitud = {
          ...((await request.json()) as Record<string, unknown>),
          id: params.id,
        };
        return HttpResponse.json({
          matriculaId: 'm-1',
          codigo: 'MAT-2026-0001',
          fechaRetiro: '2026-10-15',
          cuotasAnuladas: 3,
        });
      },
    ),
  );

  await abrir();

  await userEvent.click(
    screen.getByRole('button', { name: 'Registrar retiro' }),
  );
  expect(
    await screen.findByText('Ingresa el motivo del retiro.'),
  ).toBeInTheDocument();
  expect(solicitud).toBeUndefined();

  const fecha = screen.getByLabelText('Fecha de retiro *');
  await userEvent.clear(fecha);
  await userEvent.type(fecha, '2026-10-15');
  await userEvent.type(
    screen.getByLabelText('Motivo del retiro *'),
    'Traslado a otra ciudad',
  );
  await userEvent.click(
    screen.getByRole('button', { name: 'Registrar retiro' }),
  );

  await waitFor(() =>
    expect(solicitud).toEqual({
      id: 'sofia',
      fechaRetiro: '2026-10-15',
      motivo: 'Traslado a otra ciudad',
    }),
  );
  expect(await screen.findByText('Retiro registrado')).toBeInTheDocument();
});
