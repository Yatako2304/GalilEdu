import { render, screen, userEvent } from '@/testing/test-utils';

import { SegmentedControl } from '../segmented-control';

const opciones = [
  { value: 'PRESENTE', label: 'Presente', color: 'success' as const },
  { value: 'FALTA', label: 'Falta', color: 'error' as const },
];

test('es un grupo de opciones únicas que avisa la elegida', async () => {
  const onChange = vi.fn();
  render(
    <SegmentedControl
      label="Asistencia de Ana"
      options={opciones}
      value="PRESENTE"
      onChange={onChange}
    />,
  );

  const grupo = screen.getByRole('radiogroup', { name: 'Asistencia de Ana' });
  expect(grupo).toBeInTheDocument();
  expect(screen.getByRole('radio', { name: 'Presente' })).toBeChecked();
  await userEvent.click(screen.getByRole('radio', { name: 'Falta' }));
  expect(onChange).toHaveBeenCalledWith('FALTA');
});

test('sin marcar y con error, marca el grupo como inválido', () => {
  render(
    <SegmentedControl
      label="Asistencia de Ana"
      options={opciones}
      value={null}
      onChange={vi.fn()}
      invalid
    />,
  );

  expect(screen.getByRole('radiogroup')).toHaveAttribute(
    'aria-invalid',
    'true',
  );
  expect(screen.getByRole('radio', { name: 'Presente' })).not.toBeChecked();
});
