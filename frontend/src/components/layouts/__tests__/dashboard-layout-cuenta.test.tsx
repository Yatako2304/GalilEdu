import { ROLES } from '@/config/roles';
import { createUser, renderApp, screen, userEvent } from '@/testing/test-utils';

import { DashboardLayout } from '../dashboard-layout';

// HU-5 · Figma > Cambiar contraseña: la tarjeta del usuario abre el menú con «Cambiar contraseña».
test('la tarjeta del usuario abre «Cambiar contraseña»', async () => {
  await renderApp(<DashboardLayout>contenido</DashboardLayout>, {
    user: await createUser({ roles: [ROLES.DOCENTE] }),
    // El rol activo sale de la URL (/app/docente).
    url: '/app/docente',
    path: '/app/docente',
  });

  const tarjetas = screen.getAllByRole('button', {
    name: /Opciones de tu cuenta/,
  });
  await userEvent.click(tarjetas[0]);

  expect(
    await screen.findByRole('menuitem', { name: 'Cambiar contraseña' }),
  ).toBeInTheDocument();
});
