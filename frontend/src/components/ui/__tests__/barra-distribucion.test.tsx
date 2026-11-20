import { render, screen } from '@testing-library/react';

import { BarraDistribucion, porcentajeDe } from '../barra-distribucion';

test('describe el porcentaje y la cantidad de cada nivel, de AD a C', () => {
  render(<BarraDistribucion conteo={{ ad: 1, a: 11, b: 5, c: 5 }} />);

  expect(
    screen.getByRole('img', {
      name: 'AD: 5 % (1), A: 50 % (11), B: 23 % (5), C: 23 % (5)',
    }),
  ).toBeInTheDocument();
});

test('sin calificaciones queda solo la pista gris', () => {
  render(<BarraDistribucion conteo={{ ad: 0, a: 0, b: 0, c: 0 }} />);

  expect(
    screen.getByRole('img', { name: 'Sin calificaciones' }),
  ).toBeEmptyDOMElement();
});

test('porcentajeDe redondea al entero y no divide entre cero', () => {
  expect(porcentajeDe(1, 22)).toBe(5);
  expect(porcentajeDe(1, 8)).toBe(13);
  expect(porcentajeDe(0, 0)).toBeNull();
});
