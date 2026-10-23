import { rtlRender, screen, userEvent, within } from '@/testing/test-utils';

import { Select } from '../select';

const OPCIONES = [
  { label: 'Ana', value: 'a' },
  { label: 'Beto', value: 'b', disabled: true },
  { label: 'Carla', value: 'c' },
];

const abrir = async () => {
  await userEvent.click(screen.getByRole('combobox', { expanded: false }));
  return within(screen.getByRole('listbox')).getAllByRole('option');
};

test('una opción deshabilitada se ve marcada y no se puede elegir', async () => {
  const cambiar = vi.fn();
  rtlRender(
    <Select
      label="Docente"
      placeholder="Elige"
      options={OPCIONES}
      onChange={(e) => cambiar(e.target.value)}
    />,
  );

  const opciones = await abrir();
  expect(opciones[1]).toHaveAttribute('aria-disabled', 'true');
  expect(opciones[1]).toHaveClass('cursor-not-allowed');

  await userEvent.click(opciones[1]);

  expect(cambiar).not.toHaveBeenCalled();
  expect(screen.getByRole('listbox')).toBeInTheDocument();
});

test('las demás opciones se siguen pudiendo elegir', async () => {
  const cambiar = vi.fn();
  rtlRender(
    <Select
      label="Docente"
      placeholder="Elige"
      options={OPCIONES}
      onChange={(e) => cambiar(e.target.value)}
    />,
  );

  await userEvent.click((await abrir())[2]);

  expect(cambiar).toHaveBeenCalledWith('c');
});

test('el teclado salta las opciones deshabilitadas', async () => {
  const cambiar = vi.fn();
  rtlRender(
    <Select
      label="Docente"
      placeholder="Elige"
      options={OPCIONES}
      onChange={(e) => cambiar(e.target.value)}
    />,
  );

  screen.getByRole('combobox', { expanded: false }).focus();
  await userEvent.keyboard('{ArrowDown}'); // abre con Ana activa
  await userEvent.keyboard('{ArrowDown}'); // salta a Beto (deshabilitada) → Carla
  await userEvent.keyboard('{Enter}');

  expect(cambiar).toHaveBeenCalledWith('c');
});
