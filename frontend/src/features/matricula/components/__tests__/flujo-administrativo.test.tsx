import { renderApp, screen, userEvent } from '@/testing/test-utils';

import { MATRICULAS_FLUJO } from '../datos-flujo-matricula';
import { DetalleMatricula } from '../detalle-matricula';
import { MatriculasPeriodo } from '../matriculas-periodo';

test('permite buscar una matrícula en el listado de apoyo', async () => {
  await renderApp(<MatriculasPeriodo />);

  expect(screen.getByText('Mateo Paredes')).toBeInTheDocument();
  expect(screen.getByText('Sofía Paredes')).toBeInTheDocument();

  await userEvent.type(
    screen.getByRole('textbox', { name: 'Buscar por nombre, DNI o código' }),
    'Mateo',
  );

  expect(screen.getByText('Mateo Paredes')).toBeInTheDocument();
  expect(screen.queryByText('Sofía Paredes')).not.toBeInTheDocument();
});

test('mantiene desactivadas las acciones fuera del flujo implementado', async () => {
  const matricula = MATRICULAS_FLUJO.find(
    (item) => item.id === 'maqueta-seccion-reservada',
  );
  expect(matricula).toBeDefined();

  await renderApp(
    <DetalleMatricula matricula={matricula!} periodoId="periodo-2027" />,
  );

  expect(
    screen.getByRole('button', { name: 'Cambiar sección' }),
  ).toBeDisabled();
  expect(screen.getByRole('button', { name: 'Validar' })).toBeDisabled();
  expect(
    screen.getByRole('button', { name: 'Ver Ficha de matrícula firmada' }),
  ).toBeDisabled();
  expect(screen.getByText('Por asignar')).toBeInTheDocument();
});

test('muestra la sección únicamente cuando la matrícula está finalizada', async () => {
  const matricula = MATRICULAS_FLUJO.find(
    (item) => item.id === '50000000-0000-0000-0000-000000000001',
  );
  expect(matricula).toBeDefined();

  await renderApp(
    <DetalleMatricula matricula={matricula!} periodoId="periodo-2026" />,
  );

  expect(screen.getByText('4.° A · Tutor: María Rojas')).toBeInTheDocument();
});

test('muestra la tarjeta conectada desde el detalle pendiente de pago', async () => {
  const matricula = MATRICULAS_FLUJO.find(
    (item) => item.id === '50000000-0000-0000-0000-000000000002',
  );
  expect(matricula).toBeDefined();

  await renderApp(
    <DetalleMatricula
      matricula={matricula!}
      periodoId="periodo-2027"
      pagoMatricula={<div>Tarjeta conectada con HU-36</div>}
    />,
  );

  expect(screen.getByText('Tarjeta conectada con HU-36')).toBeInTheDocument();
});
