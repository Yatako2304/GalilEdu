import { render, screen, userEvent } from '@/testing/test-utils';

import { AvisoContrasena } from '../aviso-contrasena';

test('HU-4: muestra «Contraseña restablecida correctamente» y «Continuar» lo cierra', async () => {
  const onClose = vi.fn();
  render(<AvisoContrasena tipo="restablecida" onClose={onClose} />);

  expect(
    screen.getByRole('dialog', {
      name: 'Contraseña restablecida correctamente',
    }),
  ).toBeInTheDocument();
  expect(
    screen.getByText(/Debe volver a ingresar sus credenciales/),
  ).toBeInTheDocument();

  await userEvent.click(screen.getByRole('button', { name: 'Continuar' }));
  expect(onClose).toHaveBeenCalled();
});

test('HU-5: muestra «Contraseña actualizada correctamente» y que se cerró la sesión', () => {
  render(<AvisoContrasena tipo="actualizada" onClose={vi.fn()} />);

  expect(
    screen.getByRole('dialog', {
      name: 'Contraseña actualizada correctamente',
    }),
  ).toBeInTheDocument();
  expect(
    screen.getByText(/Se cerró la sesión en todos tus dispositivos/),
  ).toBeInTheDocument();
});

test('sin tipo no se ve', () => {
  render(<AvisoContrasena tipo={null} onClose={vi.fn()} />);
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});
