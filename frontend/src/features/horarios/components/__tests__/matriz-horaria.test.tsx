import { horarioSeccion5A } from '@/testing/mocks/fixtures/horarios';
import { rtlRender, screen, within } from '@/testing/test-utils';

import { MatrizHoraria, formatHora } from '../matriz-horaria';

test('formatHora usa el formato de 12 horas de Figma', () => {
  expect(formatHora('08:00')).toBe('8:00am');
  expect(formatHora('12:45')).toBe('12:45pm');
  expect(formatHora('13:30')).toBe('1:30pm');
});

test('pinta los días, bloques, recreos y subgrupos', () => {
  rtlRender(<MatrizHoraria horario={horarioSeccion5A} />);

  const table = screen.getByRole('table', { name: /matriz horaria/i });

  for (const dia of ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes']) {
    expect(
      within(table).getByRole('columnheader', { name: dia }),
    ).toBeInTheDocument();
  }

  // 2 recreos x 5 días
  expect(within(table).getAllByText('Recreo')).toHaveLength(10);
  expect(within(table).getByText('Movimiento creativo')).toBeInTheDocument();
  expect(within(table).getByText('Laboratorio de arte')).toBeInTheDocument();
});
