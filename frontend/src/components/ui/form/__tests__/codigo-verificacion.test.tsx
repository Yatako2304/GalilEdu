import * as React from 'react';

import { render, screen, userEvent } from '@/testing/test-utils';

import { CodigoVerificacion } from '../codigo-verificacion';

const Prueba = ({ inicial = '' }: { inicial?: string }) => {
  const [valor, setValor] = React.useState(inicial);
  return (
    <>
      <CodigoVerificacion value={valor} onChange={setValor} />
      <output>{valor}</output>
    </>
  );
};

test('escribir avanza de casilla y solo acepta números', async () => {
  render(<Prueba />);
  await userEvent.click(screen.getByLabelText('Dígito 1 de 6'));
  await userEvent.keyboard('1a2');

  expect(screen.getByRole('status')).toHaveTextContent('12');
  expect(screen.getByLabelText('Dígito 3 de 6')).toHaveFocus();
});

test('pegar el código completo llena todas las casillas', async () => {
  render(<Prueba />);
  await userEvent.click(screen.getByLabelText('Dígito 1 de 6'));
  await userEvent.paste('48 29-13');

  expect(screen.getByRole('status')).toHaveTextContent('482913');
  expect(screen.getByLabelText('Dígito 6 de 6')).toHaveValue('3');
});

test('Backspace borra y retrocede', async () => {
  render(<Prueba inicial="123" />);
  await userEvent.click(screen.getByLabelText('Dígito 4 de 6'));
  await userEvent.keyboard('{Backspace}');

  expect(screen.getByRole('status')).toHaveTextContent(/^12$/);
  expect(screen.getByLabelText('Dígito 3 de 6')).toHaveFocus();
});

test('con error pinta las casillas y muestra el mensaje', () => {
  render(
    <CodigoVerificacion
      value="111111"
      onChange={vi.fn()}
      error="Código de verificación incorrecto."
    />,
  );
  expect(screen.getByLabelText('Dígito 1 de 6')).toHaveAttribute(
    'aria-invalid',
    'true',
  );
  expect(screen.getByRole('alert')).toHaveTextContent(
    'Código de verificación incorrecto.',
  );
});
