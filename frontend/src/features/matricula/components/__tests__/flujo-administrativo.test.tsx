import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import { renderApp, screen, userEvent } from '@/testing/test-utils';
import { MatriculaAdministrativa } from '@/types/api';

import { DetalleMatricula } from '../detalle-matricula';
import { MatriculasPeriodo } from '../matriculas-periodo';

const matriculas: MatriculaAdministrativa[] = [
  {
    id: 'matricula-proceso',
    estudianteId: 'estudiante-camila',
    nombre: 'Camila Torres Huamán',
    numeroDocumento: '77894512',
    tipo: 'CONTINUADOR',
    grado: '3.° de secundaria',
    anio: 2027,
    seccion: null,
    tutor: null,
    estado: 'SECCION_RESERVADA',
    codigo: null,
    apoderado: 'Lucía Huamán Paz',
    contacto: '945210778 · lhuaman@correo.com',
    contactoEmergencia: 'Carlos Torres · 999999999',
    informacionMedica: 'Alergias',
  },
  {
    id: 'matricula-pago',
    estudianteId: 'estudiante-mateo',
    nombre: 'Mateo Paredes',
    numeroDocumento: '74125836',
    tipo: 'CONTINUADOR',
    grado: '1.° de primaria',
    anio: 2027,
    seccion: null,
    tutor: null,
    estado: 'PENDIENTE_PAGO',
    codigo: null,
    apoderado: 'Valeria Paredes',
    contacto: '987654321 · vp@example.com',
    contactoEmergencia: null,
    informacionMedica: 'Sin observaciones',
  },
  {
    id: 'matricula-finalizada',
    estudianteId: 'estudiante-sofia',
    nombre: 'Sofía Paredes',
    numeroDocumento: '72845126',
    tipo: 'CONTINUADOR',
    grado: '4.° de primaria',
    anio: 2026,
    seccion: '4.° A',
    tutor: 'María Rojas',
    estado: 'MATRICULADO',
    codigo: 'MAT-2026-0001',
    apoderado: 'Valeria Paredes',
    contacto: '987654321 · vp@example.com',
    contactoEmergencia: null,
    informacionMedica: 'Sin observaciones',
  },
];

const servirMatriculas = () =>
  server.use(
    http.get(
      `${env.API_URL}/matricula/administrativo/periodos/:periodoId/matriculas`,
      () => HttpResponse.json(matriculas),
    ),
  );

test('carga desde la API y permite buscar una matrícula del despliegue', async () => {
  servirMatriculas();
  await renderApp(<MatriculasPeriodo periodoId="periodo-1" />);

  expect(await screen.findByText('Mateo Paredes')).toBeInTheDocument();
  expect(screen.getByText('Sofía Paredes')).toBeInTheDocument();

  await userEvent.type(
    screen.getByRole('textbox', { name: 'Buscar por nombre, DNI o código' }),
    'Mateo',
  );

  expect(screen.getByText('Mateo Paredes')).toBeInTheDocument();
  expect(screen.queryByText('Sofía Paredes')).not.toBeInTheDocument();
});

test('mantiene desactivadas las acciones fuera del flujo implementado', async () => {
  await renderApp(
    <DetalleMatricula matricula={matriculas[0]} periodoId="periodo-2027" />,
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

test('permite probar HU-36 con el id real aunque la matrícula siga en proceso', async () => {
  await renderApp(
    <DetalleMatricula
      matricula={matriculas[0]}
      periodoId="periodo-2027"
      pagoMatricula={<div>Cuota conectada a matricula-proceso</div>}
    />,
  );

  expect(
    screen.getByText('Cuota conectada a matricula-proceso'),
  ).toBeInTheDocument();
});

test('muestra la sección cuando la matrícula está finalizada', async () => {
  await renderApp(
    <DetalleMatricula matricula={matriculas[2]} periodoId="periodo-2026" />,
  );

  expect(screen.getByText('4.° A · Tutor: María Rojas')).toBeInTheDocument();
});

test('muestra la tarjeta conectada desde el detalle pendiente de pago', async () => {
  await renderApp(
    <DetalleMatricula
      matricula={matriculas[1]}
      periodoId="periodo-2027"
      pagoMatricula={<div>Tarjeta conectada con HU-36</div>}
    />,
  );

  expect(screen.getByText('Tarjeta conectada con HU-36')).toBeInTheDocument();
});
