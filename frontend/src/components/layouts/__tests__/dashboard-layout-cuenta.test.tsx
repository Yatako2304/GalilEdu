import { ROLES } from '@/config/roles';
import { createUser, renderApp, screen } from '@/testing/test-utils';

import { DashboardLayout } from '../dashboard-layout';

// «Mis datos»: la tarjeta del usuario lleva directo a la pantalla de datos personales (antes abría un
// desplegable con «Cambiar contraseña»; ese botón ahora vive dentro de «Mis datos»).
test('la tarjeta del usuario lleva a «Mis datos»', async () => {
  await renderApp(<DashboardLayout>contenido</DashboardLayout>, {
    user: await createUser({ roles: [ROLES.DOCENTE] }),
    // El rol activo sale de la URL (/app/docente).
    url: '/app/docente',
    path: '/app/docente',
  });

  const tarjetas = screen.getAllByRole('link', { name: /Ver mis datos/ });
  expect(tarjetas[0]).toHaveAttribute('href', '/app/docente/mis-datos');
});
