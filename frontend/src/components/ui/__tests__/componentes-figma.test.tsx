import * as React from 'react';
import { z } from 'zod';

import { rtlRender, screen, userEvent, within } from '@/testing/test-utils';

import { Accordion } from '../accordion';
import { Calendar, DatePicker } from '../date-picker';
import { Form, Input, Select, TagField } from '../form';
import { ProgressBar } from '../progress-bar';
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

test('Select: la lista de Figma elige la opción y avisa al formulario como el <select> nativo', async () => {
  const cambiar = vi.fn();
  rtlRender(
    <Select
      label="Grado de instrucción"
      placeholder="Selecciona"
      defaultValue=""
      onChange={(e) => cambiar(e.target.value)}
      options={[
        { label: 'Primaria', value: 'PRIMARIA' },
        { label: 'Secundaria', value: 'SECUNDARIA' },
      ]}
    />,
  );

  await userEvent.click(screen.getByRole('combobox', { expanded: false }));
  await userEvent.click(
    within(screen.getByRole('listbox')).getByRole('option', {
      name: 'Secundaria',
    }),
  );

  expect(cambiar).toHaveBeenCalledWith('SECUNDARIA');
  expect(screen.queryByRole('listbox')).not.toBeInTheDocument();
  expect(screen.getByLabelText('Grado de instrucción')).toHaveValue(
    'SECUNDARIA',
  );
});

test('Input type="date": usa el calendario de Figma y el formulario recibe la fecha', async () => {
  const enviar = vi.fn();
  rtlRender(
    <Form
      id="f"
      schema={z.object({ fecha: z.string().min(1) })}
      onSubmit={(valores) => enviar(valores)}
    >
      {({ register }) => (
        <>
          <Input type="date" label="Fecha" registration={register('fecha')} />
          <button type="submit">Guardar</button>
        </>
      )}
    </Form>,
  );

  await userEvent.click(screen.getByRole('button', { name: 'DD/MM/AAAA' }));
  await userEvent.click(screen.getByRole('button', { name: 'Mes siguiente' }));
  const dia = within(screen.getByRole('dialog')).getAllByRole('button', {
    name: /^15\//,
  })[0];
  await userEvent.click(dia);
  await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

  expect(enviar).toHaveBeenCalledTimes(1);
  expect(enviar.mock.calls[0][0].fecha).toMatch(/^\d{4}-\d{2}-15$/);
});

test('ProgressBar: muestra el porcentaje, expone el valor y lo limita entre 0 y 100', () => {
  const { rerender } = rtlRender(
    <ProgressBar value={74} label="Avance del año escolar" />,
  );

  const barra = screen.getByRole('progressbar', {
    name: 'Avance del año escolar',
  });
  // aria-valuenow es un atributo ARIA de un div: la regla lo confunde con el `value` de un campo.
  // eslint-disable-next-line jest-dom/prefer-to-have-value
  expect(barra).toHaveAttribute('aria-valuenow', '74');
  expect(screen.getByText('74 %')).toBeInTheDocument();
  expect(barra.firstElementChild).toHaveStyle({ width: '74%' });

  rerender(<ProgressBar value={140} label="Avance del año escolar" />);
  // eslint-disable-next-line jest-dom/prefer-to-have-value
  expect(screen.getByRole('progressbar')).toHaveAttribute(
    'aria-valuenow',
    '100',
  );

  rerender(<ProgressBar value={-5} label="Avance del año escolar" />);
  // eslint-disable-next-line jest-dom/prefer-to-have-value
  expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '0');
});

test('ProgressBar: sin etiqueta visible conserva el nombre accesible', () => {
  rtlRender(<ProgressBar value={30} label="Avance" showLabel={false} />);

  expect(
    screen.getByRole('progressbar', { name: 'Avance' }),
  ).toBeInTheDocument();
  expect(screen.queryByText('30 %')).not.toBeInTheDocument();
});
