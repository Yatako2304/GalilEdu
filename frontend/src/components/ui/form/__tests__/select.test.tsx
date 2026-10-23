import { render, screen, userEvent, within } from '@/testing/test-utils';

import { Select } from '../select';

const opciones = [
  { label: 'Inglés', value: 'Inglés' },
  { label: 'Quechua', value: 'Quechua' },
];

// El <select> nativo oculto también tiene rol "combobox": se usa el botón que dibuja Figma.
const abrir = async () => {
  const boton = screen
    .getAllByRole('combobox')
    .find((el) => el.tagName === 'BUTTON')!;
  await userEvent.click(boton);
};

const listaAbierta = () => within(screen.getByRole('listbox'));

test('un Select vaciable permite volver a "Selecciona" después de elegir una opción', async () => {
  const alCambiar = vi.fn();
  render(
    <Select
      label="Segunda lengua"
      options={opciones}
      placeholder="Selecciona"
      vaciable
      onChange={(e) => alCambiar(e.target.value)}
    />,
  );

  await abrir();
  await userEvent.click(listaAbierta().getByRole('option', { name: 'Inglés' }));
  expect(alCambiar).toHaveBeenLastCalledWith('Inglés');

  await abrir();
  await userEvent.click(
    listaAbierta().getByRole('option', { name: 'Selecciona' }),
  );
  expect(alCambiar).toHaveBeenLastCalledWith('');
});

test('un Select sin "vaciable" no ofrece "Selecciona" como opción', async () => {
  render(<Select label="Lengua" options={opciones} placeholder="Selecciona" />);

  await abrir();
  expect(listaAbierta().getAllByRole('option')).toHaveLength(2);
  expect(
    listaAbierta().queryByRole('option', { name: 'Selecciona' }),
  ).not.toBeInTheDocument();
});
