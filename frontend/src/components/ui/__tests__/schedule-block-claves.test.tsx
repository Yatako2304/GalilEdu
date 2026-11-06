import { vi } from 'vitest';

import { ScheduleBlock } from '@/components/ui/schedule-block';
import { rtlRender, screen } from '@/testing/test-utils';

// Dos cursos con el mismo nombre en una celda (por ejemplo, el mismo curso en dos secciones por un cruce guardado)
// no deben repetir la clave de React.

test('ScheduleBlock: dos cursos con el mismo nombre se dibujan los dos y no repiten la clave', () => {
  const error = vi.spyOn(console, 'error').mockImplementation(() => undefined);

  rtlRender(
    <ScheduleBlock
      type="assigned"
      courses={[
        { name: 'Álgebra', teacher: 'Prof. Ana' },
        { name: 'Álgebra', teacher: 'Prof. Luis' },
      ]}
      room="A-401 · A-402"
      section="1.° A · 1.° B"
    />,
  );

  expect(screen.getAllByText('Álgebra')).toHaveLength(2);
  expect(screen.getByText('Prof. Ana')).toBeInTheDocument();
  expect(screen.getByText('Prof. Luis')).toBeInTheDocument();
  expect(
    error.mock.calls.filter((llamada) =>
      String(llamada[0]).includes('same key'),
    ),
  ).toHaveLength(0);

  error.mockRestore();
});
