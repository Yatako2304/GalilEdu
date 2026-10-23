import { rtlRender, screen, within } from '@/testing/test-utils';
import { AnioEscolarDetalle } from '@/types/api';

import { LineaDeTiempo, porcentajeDelAnio } from '../linea-de-tiempo';

import { anio2026, periodo } from './datos';

// Figma > CI-03 y CI-06 > "Línea de tiempo"

// Un año de 10 días (01 al 10 de enero) para que los porcentajes salgan redondos.
const anioDeDiezDias: AnioEscolarDetalle = {
  ...anio2026,
  fechaInicio: '2026-01-01',
  fechaFin: '2026-01-10',
  periodos: [
    periodo(
      1,
      'I Bimestre',
      '2026-01-01',
      '2026-01-04',
      '2026-01-06',
      'CERRADO',
    ),
    periodo(
      2,
      'II Bimestre',
      '2026-01-06',
      '2026-01-10',
      '2026-01-12',
      'EN_CURSO',
    ),
  ],
};

test('porcentajeDelAnio: cada día ocupa el mismo ancho y el fin llega hasta el final de su día', () => {
  expect(porcentajeDelAnio(anioDeDiezDias, '2026-01-01')).toBe(0);
  expect(porcentajeDelAnio(anioDeDiezDias, '2026-01-06')).toBe(50);
  expect(porcentajeDelAnio(anioDeDiezDias, '2026-01-04', true)).toBe(40);
  expect(porcentajeDelAnio(anioDeDiezDias, '2026-01-10', true)).toBe(100);
});

test('coloca cada periodo según sus fechas y muestra el inicio y el fin del año', () => {
  rtlRender(<LineaDeTiempo anio={anioDeDiezDias} hoy="2026-02-01" />);

  const linea = screen.getByRole('list', {
    name: 'Línea de tiempo de los periodos',
  });
  const [primero, segundo] = within(linea).getAllByRole('listitem');
  expect(primero).toHaveTextContent('I Bimestre');
  expect(primero).toHaveStyle({ left: '0%', width: '40%' });
  expect(segundo).toHaveTextContent('II Bimestre');
  expect(segundo).toHaveStyle({ left: '50%', width: '50%' });
  expect(screen.getByText('Inicio 01/01/2026')).toBeInTheDocument();
  expect(screen.getByText('Fin 10/01/2026')).toBeInTheDocument();
});

test('cada segmento indica su estado, para quien no ve el color', () => {
  rtlRender(<LineaDeTiempo anio={anioDeDiezDias} hoy="2026-02-01" />);

  expect(screen.getByTitle('I Bimestre · Cerrado')).toBeInTheDocument();
  expect(screen.getByTitle('II Bimestre · En curso')).toBeInTheDocument();
});

test('con el año ACTIVO marca "Hoy" en el centro del día', () => {
  rtlRender(<LineaDeTiempo anio={anioDeDiezDias} hoy="2026-01-06" />);

  expect(screen.getByLabelText('Hoy')).toHaveStyle({ left: '55%' });
  expect(screen.getByText('Hoy')).toBeInTheDocument();
});

test('no marca "Hoy" si el día cae fuera del año', () => {
  rtlRender(<LineaDeTiempo anio={anioDeDiezDias} hoy="2025-12-31" />);

  expect(screen.queryByLabelText('Hoy')).not.toBeInTheDocument();
});

test('no marca "Hoy" si el año no está activo', () => {
  rtlRender(
    <LineaDeTiempo
      anio={{ ...anioDeDiezDias, estado: 'PLANIFICADO' }}
      hoy="2026-01-06"
    />,
  );

  expect(screen.queryByLabelText('Hoy')).not.toBeInTheDocument();
});
