import * as React from 'react';

import { rtlRender, screen, userEvent } from '@/testing/test-utils';

import { Accordion } from '../accordion';
import { Calendar, DatePicker } from '../date-picker';
import { TagField } from '../form';
import { Rating } from '../rating';
import { Tabs } from '../tabs';

test('Tabs: el clic y las flechas cambian de pestaña y saltan las deshabilitadas', async () => {
  const cambiar = vi.fn();
  rtlRender(
    <Tabs
      aria-label="Secciones"
      value="a"
      onValueChange={cambiar}
      items={[
        { value: 'a', label: 'Uno' },
        { value: 'b', label: 'Dos', disabled: true },
        { value: 'c', label: 'Tres' },
      ]}
    />,
  );

  expect(screen.getByRole('tab', { name: 'Uno' })).toHaveAttribute(
    'aria-selected',
    'true',
  );
  await userEvent.click(screen.getByRole('tab', { name: 'Tres' }));
  expect(cambiar).toHaveBeenLastCalledWith('c');

  screen.getByRole('tab', { name: 'Uno' }).focus();
  await userEvent.keyboard('{ArrowRight}');
  expect(cambiar).toHaveBeenLastCalledWith('c');
});

test('Accordion: muestra el contenido solo cuando está abierto', async () => {
  rtlRender(
    <Accordion title="Unidad 1">
      <p>Contenido</p>
    </Accordion>,
  );

  const boton = screen.getByRole('button', { name: 'Unidad 1' });
  expect(screen.queryByText('Contenido')).not.toBeInTheDocument();
  await userEvent.click(boton);
  expect(boton).toHaveAttribute('aria-expanded', 'true');
  expect(screen.getByText('Contenido')).toBeInTheDocument();
});

test('TagField: Enter agrega una etiqueta sin repetir y la X la quita', async () => {
  const Demo = () => {
    const [valor, setValor] = React.useState(['Matemática']);
    return <TagField label="Cursos" value={valor} onChange={setValor} />;
  };
  rtlRender(<Demo />);

  const campo = screen.getByRole('textbox');
  await userEvent.type(campo, 'Arte{Enter}Matemática{Enter}');
  expect(screen.getByText('Arte')).toBeInTheDocument();
  expect(screen.getAllByText('Matemática')).toHaveLength(1);

  await userEvent.click(screen.getByRole('button', { name: 'Quitar Arte' }));
  expect(screen.queryByText('Arte')).not.toBeInTheDocument();
});

test('Calendar: elegir un día devuelve la fecha en formato YYYY-MM-DD y respeta los límites', async () => {
  const elegir = vi.fn();
  rtlRender(
    <Calendar
      value="2026-03-15"
      min="2026-03-05"
      max="2026-03-25"
      onChange={elegir}
    />,
  );

  expect(screen.getByText('Marzo 2026')).toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: '20/03/2026' }));
  expect(elegir).toHaveBeenCalledWith('2026-03-20');
  expect(screen.getByRole('button', { name: '2/03/2026' })).toBeDisabled();
});

test('DatePicker: abre el calendario y muestra la fecha elegida', async () => {
  const Demo = () => {
    const [fecha, setFecha] = React.useState<string | null>('2026-03-15');
    return <DatePicker label="Fecha" value={fecha} onChange={setFecha} />;
  };
  rtlRender(<Demo />);

  await userEvent.click(screen.getByText('15/03/2026'));
  await userEvent.click(screen.getByRole('button', { name: '20/03/2026' }));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(screen.getByText('20/03/2026')).toBeInTheDocument();
});

test('Rating: se lee como texto y, editable, devuelve la estrella elegida', async () => {
  const calificar = vi.fn();
  const { unmount } = rtlRender(<Rating value={3} />);
  expect(
    screen.getByRole('img', { name: 'Calificación: 3 de 5' }),
  ).toBeVisible();
  unmount();

  rtlRender(<Rating value={0} onChange={calificar} />);
  await userEvent.click(screen.getByRole('radio', { name: '4 de 5' }));
  expect(calificar).toHaveBeenCalledWith(4);
});
