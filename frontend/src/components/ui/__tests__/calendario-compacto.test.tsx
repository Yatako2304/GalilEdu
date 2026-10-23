import { renderApp, screen, userEvent } from '@/testing/test-utils';

import { Calendar, DatePicker } from '../date-picker';

// Date picker compacto (size="sm"): no está en Figma; el tamaño por defecto es el de Figma y no debe cambiar.

// El título está en la cabecera, que es hija directa de la caja del calendario.
const cajaDelCalendario = (titulo: HTMLElement) =>
  titulo.parentElement?.parentElement;

const diaDelCalendario = (fecha: string) =>
  screen.getByRole('button', { name: fecha });

test('por defecto el calendario es el de Figma: título grande, celdas de 32 px y relleno de 24 px', async () => {
  await renderApp(<Calendar value="2027-03-10" onChange={() => undefined} />);

  const titulo = screen.getByRole('button', { name: /Marzo 2027/ });
  expect(titulo).toHaveClass('text-h6');
  expect(diaDelCalendario('10/03/2027')).toHaveClass('size-8');
  expect(cajaDelCalendario(titulo)).toHaveClass('p-6');
});

test('con size="sm" el título es más pequeño y las celdas y el relleno se reducen', async () => {
  await renderApp(
    <Calendar size="sm" value="2027-03-10" onChange={() => undefined} />,
  );

  const titulo = screen.getByRole('button', { name: /Marzo 2027/ });
  expect(titulo).toHaveClass('text-label-1');
  expect(titulo).not.toHaveClass('text-h6');
  expect(diaDelCalendario('10/03/2027')).toHaveClass('size-7');
  expect(cajaDelCalendario(titulo)).toHaveClass('p-4');
});

test('el calendario compacto sigue eligiendo días, meses y años', async () => {
  const cambios: string[] = [];
  await renderApp(
    <Calendar
      size="sm"
      value="2027-03-10"
      onChange={(fecha) => cambios.push(fecha)}
    />,
  );

  await userEvent.click(diaDelCalendario('15/03/2027'));
  expect(cambios).toEqual(['2027-03-15']);

  await userEvent.click(screen.getByRole('button', { name: /Marzo 2027/ }));
  await userEvent.click(screen.getByRole('button', { name: /2027/ }));
  await userEvent.click(screen.getByRole('button', { name: '2025' }));
  await userEvent.click(screen.getByRole('button', { name: 'JUN' }));
  await userEvent.click(diaDelCalendario('10/06/2025'));

  expect(cambios).toEqual(['2027-03-15', '2025-06-10']);
});

test('DatePicker con size="sm" abre el calendario compacto; sin size, el de Figma', async () => {
  await renderApp(
    <>
      <DatePicker
        label="Compacto"
        size="sm"
        value="2027-03-10"
        onChange={() => undefined}
      />
      <DatePicker
        label="Normal"
        value="2027-03-10"
        onChange={() => undefined}
      />
    </>,
  );

  // El campo muestra la fecha como texto: el primero es el compacto y el segundo el normal.
  const [compacto, normal] = screen.getAllByText('10/03/2027');
  await userEvent.click(compacto);
  expect(screen.getByRole('button', { name: /Marzo 2027/ })).toHaveClass(
    'text-label-1',
  );

  // Al abrir el otro campo, el primero se cierra: queda un solo calendario abierto.
  await userEvent.click(normal);
  expect(screen.getByRole('button', { name: /Marzo 2027/ })).toHaveClass(
    'text-h6',
  );
});

test('la sombra del calendario es la negra de Figma (`shadow-card-elevation`), no la blanca', async () => {
  await renderApp(<Calendar value="2027-03-10" onChange={() => undefined} />);

  const caja = cajaDelCalendario(
    screen.getByRole('button', { name: /Marzo 2027/ }),
  );
  expect(caja).toHaveClass('shadow-card-elevation');
  expect(caja).not.toHaveClass('shadow-card');
});

test('el calendario desplegable no se limita al ancho del campo', async () => {
  await renderApp(
    <DatePicker label="Fecha" value="2027-03-10" onChange={() => undefined} />,
  );

  await userEvent.click(screen.getByText('10/03/2027'));

  expect(screen.getByRole('dialog', { name: 'Elegir fecha' })).toHaveClass(
    'w-max',
  );
});
