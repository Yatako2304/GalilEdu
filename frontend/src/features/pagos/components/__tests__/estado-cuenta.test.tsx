import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent } from '@/testing/test-utils';
import { AlumnoEstadoCuenta } from '@/types/api';

import { EstadoCuenta } from '../estado-cuenta';

// P2 · Estado de cuenta: salen todos los alumnos activos, cada uno con su compromiso más reciente (de cualquier año).

const alumno = (datos: Partial<AlumnoEstadoCuenta>): AlumnoEstadoCuenta => ({
  estudianteId: 'sofia',
  compromisoId: 'compromiso-2026',
  nombre: 'Sofía Paredes Rojas',
  numeroDocumento: '74125896',
  apoderado: 'Luis Paredes Silva',
  seccion: '4.° B',
  antiguedad: 1,
  estadoCompromiso: 'VIGENTE',
  totalAnual: 6420,
  capitalPagado: 6420,
  deudaVencida: 0,
  cuotasVencidas: 0,
  anioCompromiso: 2026,
  retirado: false,
  ...datos,
});

test('lista a los alumnos con compromiso de distintos años y a los que aún no tienen compromiso', async () => {
  server.use(
    http.get(`${env.API_URL}/pagos/estado-cuenta`, () =>
      HttpResponse.json({
        anio: null,
        comprometido: 12620,
        cobrado: 6420,
        porcentajeCobrado: 51,
        deudaVencida: 0,
        cuotasVencidas: 0,
        alumnosConDeuda: 0,
        alumnos: [
          alumno({
            estudianteId: 'diego',
            compromisoId: 'compromiso-2027',
            nombre: 'Diego Alonso Mendoza Quispe',
            numeroDocumento: '76543210',
            totalAnual: 6200,
            capitalPagado: 0,
            anioCompromiso: 2027,
            retirado: true,
          }),
          alumno({}),
          alumno({
            estudianteId: 'valeria',
            compromisoId: null,
            estadoCompromiso: null,
            nombre: 'Valeria Sánchez Ferrara',
            numeroDocumento: '70000003',
            totalAnual: 0,
            capitalPagado: 0,
            anioCompromiso: null,
          }),
        ],
      }),
    ),
  );

  await renderApp(<EstadoCuenta />);

  expect(await screen.findByText('Todos los alumnos (3)')).toBeInTheDocument();
  expect(screen.getByText('Diego Alonso Mendoza Quispe')).toBeInTheDocument();
  expect(screen.getByText('Valeria Sánchez Ferrara')).toBeInTheDocument();
  // Columna «Compromiso»: el año de cada uno, o «Sin compromiso».
  expect(
    screen.getByRole('columnheader', { name: 'Compromiso' }),
  ).toBeInTheDocument();
  expect(screen.getByText('2027')).toBeInTheDocument();
  expect(screen.getByText('2026')).toBeInTheDocument();
  expect(screen.getByText('Sin compromiso')).toBeInTheDocument();
  // Un retirado sale igual (puede haber dejado deuda), marcado.
  expect(screen.getByText('Retirado')).toBeInTheDocument();
  // Sin año pedido, los totales no dicen un año (mezclan compromisos de varios años).
  expect(screen.getByText('Comprometido')).toBeInTheDocument();
  expect(screen.getByText('Cobrado · 51 %')).toBeInTheDocument();
});

test('muestra 10 alumnos por página', async () => {
  const alumnos = Array.from({ length: 12 }, (_, i) =>
    alumno({
      estudianteId: `alumno-${i + 1}`,
      compromisoId: `compromiso-${i + 1}`,
      nombre: `Alumno ${String(i + 1).padStart(2, '0')}`,
    }),
  );
  server.use(
    http.get(`${env.API_URL}/pagos/estado-cuenta`, () =>
      HttpResponse.json({
        anio: null,
        comprometido: 0,
        cobrado: 0,
        porcentajeCobrado: 0,
        deudaVencida: 0,
        cuotasVencidas: 0,
        alumnosConDeuda: 0,
        alumnos,
      }),
    ),
  );

  await renderApp(<EstadoCuenta />);

  expect(await screen.findByText('Alumno 10')).toBeInTheDocument();
  expect(screen.queryByText('Alumno 11')).not.toBeInTheDocument();
  expect(screen.getByText('1-10')).toBeInTheDocument();

  await userEvent.click(
    screen.getByRole('button', { name: 'Página siguiente' }),
  );

  expect(await screen.findByText('Alumno 12')).toBeInTheDocument();
  expect(screen.queryByText('Alumno 01')).not.toBeInTheDocument();
});
