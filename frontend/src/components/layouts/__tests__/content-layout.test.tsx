import { ROLES } from '@/config/roles';
import { createUser, renderApp, screen, within } from '@/testing/test-utils';

import { ContentLayout } from '../content-layout';

// Figma > Page Header: migas de pan sobre el título (CI-03, CI-06, CI-09 y CI-12 enlazan "Año escolar").

const renderizar = async (ui: React.ReactElement) =>
  renderApp(ui, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

test('sin migas muestra solo el título como página actual', async () => {
  await renderizar(
    <ContentLayout title="Año escolar">contenido</ContentLayout>,
  );

  const migas = screen.getByRole('navigation', { name: 'Ruta' });
  expect(within(migas).getByText('Año escolar')).toHaveAttribute(
    'aria-current',
    'page',
  );
  expect(within(migas).queryByRole('link')).not.toBeInTheDocument();
});

test('las migas de texto siguen sin enlace', async () => {
  await renderizar(
    <ContentLayout
      title="Competencias"
      breadcrumbs={['Configuración', 'Competencias', 'Plan curricular']}
    >
      contenido
    </ContentLayout>,
  );

  const migas = screen.getByRole('navigation', { name: 'Ruta' });
  expect(within(migas).queryByRole('link')).not.toBeInTheDocument();
  expect(within(migas).getByText('Configuración')).toBeInTheDocument();
  expect(within(migas).getByText('Plan curricular')).toHaveAttribute(
    'aria-current',
    'page',
  );
});

test('una miga con enlace lleva a su ruta, y la última nunca es enlace', async () => {
  await renderizar(
    <ContentLayout
      title="Año escolar 2026"
      breadcrumbs={[
        'Configuración',
        { label: 'Año escolar', to: '/app/administrativo/anio-escolar' },
        { label: '2026', to: '/app/administrativo/anio-escolar/x' },
      ]}
    >
      contenido
    </ContentLayout>,
  );

  const migas = screen.getByRole('navigation', { name: 'Ruta' });
  expect(
    within(migas).getByRole('link', { name: 'Año escolar' }),
  ).toHaveAttribute('href', '/app/administrativo/anio-escolar');
  expect(within(migas).getAllByRole('link')).toHaveLength(1);
  expect(within(migas).getByText('2026')).toHaveAttribute(
    'aria-current',
    'page',
  );
  expect(within(migas).getByText('Configuración')).not.toHaveAttribute(
    'aria-current',
  );
});

test('muestra el título con su etiqueta, el subtítulo y las acciones', async () => {
  await renderizar(
    <ContentLayout
      title="Año escolar 2026"
      titleTag={<span>Activo</span>}
      subtitle="Del 02/03/2026 al 18/12/2026"
      actions={<button>Cerrar año</button>}
    >
      contenido
    </ContentLayout>,
  );

  expect(
    screen.getByRole('heading', { name: 'Año escolar 2026' }),
  ).toBeInTheDocument();
  expect(screen.getByText('Activo')).toBeInTheDocument();
  expect(screen.getByText('Del 02/03/2026 al 18/12/2026')).toBeInTheDocument();
  expect(
    screen.getByRole('button', { name: 'Cerrar año' }),
  ).toBeInTheDocument();
});
