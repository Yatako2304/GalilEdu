import * as React from 'react';
import { z } from 'zod';

import {
  rtlRender,
  screen,
  userEvent,
  waitFor,
  within,
} from '@/testing/test-utils';

import { Accordion } from '../accordion';
import { Calendar, DatePicker } from '../date-picker';
import { Form, Input, Select, TagField, Textarea } from '../form';
import { ProgressBar } from '../progress-bar';
import { Rating } from '../rating';
import { StatCard } from '../stat-card';
import { Tabs } from '../tabs';
import { SelectableTag } from '../tag';

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

test('SelectableTag readOnly: es una pastilla informativa, no un botón, con el relleno claro de Figma', () => {
  rtlRender(
    <>
      <SelectableTag readOnly shape="circle" size="md" color="warning">
        Programado
      </SelectableTag>
      <SelectableTag readOnly shape="circle" size="md" color="success">
        Abierto
      </SelectableTag>
      <SelectableTag readOnly shape="circle" size="md" color="primary">
        Cerrado
      </SelectableTag>
    </>,
  );

  expect(screen.queryByRole('button')).not.toBeInTheDocument();
  expect(screen.getByText('Programado')).toHaveClass(
    'rounded-full',
    'border-stroke-warning',
    'text-content-warning',
    'bg-surface-warning-light',
  );
  expect(screen.getByText('Abierto')).toHaveClass(
    'border-stroke-success',
    'bg-surface-success-light',
  );
  expect(screen.getByText('Cerrado')).toHaveClass(
    'border-stroke-accent-primary',
    'bg-surface-accent-light',
  );
});

test('SelectableTag readOnly ignora "selected": sigue siendo informativa', () => {
  rtlRender(
    <SelectableTag readOnly selected color="warning">
      Programado
    </SelectableTag>,
  );

  expect(screen.getByText('Programado')).toHaveClass(
    'bg-surface-warning-light',
  );
  expect(screen.getByText('Programado')).not.toHaveClass('text-white');
});

test('SelectableTag sin readOnly sigue siendo un botón que se marca', async () => {
  const marcar = vi.fn();
  rtlRender(<SelectableTag onClick={marcar}>Filtro</SelectableTag>);

  await userEvent.click(screen.getByRole('button', { name: 'Filtro' }));

  expect(marcar).toHaveBeenCalledTimes(1);
  expect(screen.getByRole('button', { name: 'Filtro' })).toHaveAttribute(
    'aria-pressed',
    'false',
  );
});

test('Textarea con showCount: el contador sigue al valor de un campo controlado', () => {
  const { rerender } = rtlRender(
    <Textarea
      label="Descripción"
      maxLength={100}
      showCount
      value="Ficha"
      onChange={() => {}}
    />,
  );
  expect(screen.getByText('5/100')).toBeInTheDocument();

  rerender(
    <Textarea
      label="Descripción"
      maxLength={100}
      showCount
      value="Ficha de matrícula"
      onChange={() => {}}
    />,
  );
  expect(screen.getByText('18/100')).toBeInTheDocument();
});

test('Input con startAdornment: muestra el prefijo dentro del campo, sin ensuciar su nombre ni su valor', async () => {
  rtlRender(
    <>
      <Input label="Monto" type="number" startAdornment="S/" />
      <Input label="Sin prefijo" />
    </>,
  );

  // El prefijo es decorativo: no entra en el nombre accesible, que sigue siendo la etiqueta.
  const monto = screen.getByRole('spinbutton', { name: 'Monto' });
  await userEvent.type(monto, '450');

  expect(screen.getByText('S/')).toBeInTheDocument();
  expect(monto).toHaveValue(450);
  expect(monto).toHaveClass('pl-10');
  expect(screen.getByText('S/')).toHaveClass('pointer-events-none');
  expect(screen.getByRole('textbox', { name: 'Sin prefijo' })).not.toHaveClass(
    'pl-10',
  );
});

test('Input con startAdornment y endAdornment a la vez deja lugar a los dos lados', () => {
  rtlRender(
    <Input
      label="Clave"
      startAdornment="S/"
      endAdornment={<button type="button">Ver</button>}
    />,
  );

  expect(screen.getByRole('textbox', { name: /Clave/ })).toHaveClass(
    'pl-10',
    'pr-10',
  );
  expect(screen.getByRole('button', { name: 'Ver' })).toBeInTheDocument();
});

test('Textarea con showCount: en un campo no controlado cuenta lo que se escribe y respeta maxLength', async () => {
  rtlRender(<Textarea label="Descripción" maxLength={5} showCount />);

  expect(screen.getByText('0/5')).toBeInTheDocument();
  await userEvent.type(screen.getByLabelText('Descripción'), 'abcdefgh');

  expect(screen.getByText('5/5')).toBeInTheDocument();
  expect(screen.getByLabelText('Descripción')).toHaveValue('abcde');
});

test('Textarea: sin showCount o sin maxLength no muestra contador, y llama a su onChange', async () => {
  const cambiar = vi.fn();
  rtlRender(
    <>
      <Textarea label="Uno" maxLength={10} onChange={cambiar} />
      <Textarea label="Dos" showCount />
    </>,
  );

  await userEvent.type(screen.getByLabelText('Uno'), 'hola');

  expect(cambiar).toHaveBeenCalledTimes(4);
  expect(screen.queryByText(/\/10$/)).not.toBeInTheDocument();
  expect(screen.queryByText(/^\d+\/\d+$/)).not.toBeInTheDocument();
});

test('Textarea con react-hook-form: el contador cuenta y el valor llega al formulario', async () => {
  const enviar = vi.fn();
  rtlRender(
    <Form onSubmit={enviar} schema={z.object({ descripcion: z.string() })}>
      {({ register }) => (
        <>
          <Textarea
            label="Descripción"
            maxLength={100}
            showCount
            registration={register('descripcion')}
          />
          <button type="submit">Guardar</button>
        </>
      )}
    </Form>,
  );

  await userEvent.type(screen.getByLabelText('Descripción'), 'Con firma');
  expect(screen.getByText('9/100')).toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: 'Guardar' }));

  await waitFor(() =>
    expect(enviar).toHaveBeenCalledWith(
      { descripcion: 'Con firma' },
      expect.anything(),
    ),
  );
});

test('StatCard: iconColor cambia el color del cuadro del ícono y, sin él, conserva el de siempre', () => {
  rtlRender(
    <>
      <StatCard
        icon={<i>Ícono con color</i>}
        iconColor="turquoise"
        value="60"
        label="Con color"
      />
      <StatCard icon={<i>Ícono sin color</i>} value="3" label="Sin color" />
    </>,
  );

  const conColor = screen.getByText('Ícono con color').parentElement;
  expect(conColor).toHaveClass('bg-turquoise-50', 'text-turquoise-600');
  expect(screen.getByText('Ícono sin color').parentElement).not.toHaveClass(
    'bg-turquoise-50',
  );
});
