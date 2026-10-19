import { createUser, renderApp, screen } from '@/testing/test-utils';

import { Authorization, ROLES } from '../authorization';

const protectedResource = 'Información confidencial';
const forbiddenMessage = 'No tiene permiso para ver este recurso';

test('muestra el recurso si el usuario tiene el rol permitido', async () => {
  const user = await createUser({ roles: [ROLES.ADMINISTRADOR] });

  await renderApp(
    <Authorization allowedRoles={[ROLES.ADMINISTRADOR]}>
      {protectedResource}
    </Authorization>,
    { user },
  );

  expect(screen.getByText(protectedResource)).toBeInTheDocument();
});

test('acepta usuarios con varios roles (docente + tutor)', async () => {
  const user = await createUser({ roles: [ROLES.DOCENTE, ROLES.TUTOR] });

  await renderApp(
    <Authorization allowedRoles={[ROLES.TUTOR]}>
      {protectedResource}
    </Authorization>,
    { user },
  );

  expect(screen.getByText(protectedResource)).toBeInTheDocument();
});

test('muestra el fallback si el rol no coincide', async () => {
  const user = await createUser({ roles: [ROLES.ESTUDIANTE] });

  await renderApp(
    <Authorization
      forbiddenFallback={<div>{forbiddenMessage}</div>}
      allowedRoles={[ROLES.ADMINISTRADOR]}
    >
      {protectedResource}
    </Authorization>,
    { user },
  );

  expect(screen.queryByText(protectedResource)).not.toBeInTheDocument();
  expect(screen.getByText(forbiddenMessage)).toBeInTheDocument();
});

test('respeta policyCheck', async () => {
  const user = await createUser({ roles: [ROLES.ESTUDIANTE] });

  await renderApp(
    <Authorization
      forbiddenFallback={<div>{forbiddenMessage}</div>}
      policyCheck={false}
    >
      {protectedResource}
    </Authorization>,
    { user },
  );

  expect(screen.queryByText(protectedResource)).not.toBeInTheDocument();
  expect(screen.getByText(forbiddenMessage)).toBeInTheDocument();
});
