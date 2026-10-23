import { HttpResponse, http } from 'msw';

import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import { createUser, renderApp, screen, userEvent } from '@/testing/test-utils';
import { FormularioPeriodoMatricula } from '@/types/api';

import { DialogoResumenConfiguracion } from '../dialogo-resumen-configuracion';

import { API, detalle2027, vigente2027 } from './datos';

// Figma > Personal Administrativo > "Modal - Configuración · Matrícula 2027" (HU-23)

const periodo = vigente2027();

const abrir = async (
  detalle: FormularioPeriodoMatricula = detalle2027(),
  vigente = periodo,
) => {
  server.use(
    http.get(`${API}/${vigente.id}`, () => HttpResponse.json(detalle)),
  );
  const onCerrar = vi.fn();
  await renderApp(
    <DialogoResumenConfiguracion
      abierto
      onCerrar={onCerrar}
      periodo={vigente}
    />,
    { user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }) },
  );
  return onCerrar;
};

test('un periodo abierto: sus datos, sus reglas y el aviso de solo lectura, como en Figma', async () => {
  await abrir();

  expect(
    await screen.findByRole('heading', {
      name: 'Configuración · Matrícula 2027',
    }),
  ).toBeInTheDocument();
  expect(
    screen.getByText(
      'Periodo habilitado: la configuración es de solo lectura.',
    ),
  ).toBeInTheDocument();
  const filas: [string, string][] = [
    ['Fechas de matrícula regular', '15/09/2026 – 31/10/2026'],
    ['Vacantes totales', '300'],
    ['Secciones', '10 (1.° a 5.°, A y B)'],
    ['Monto de matrícula', 'S/ 450.00'],
    ['Documentos requeridos', '4'],
    ['Bloquear por morosidad', 'Sí'],
    ['Matrícula extemporánea', 'Permitida tras el cierre'],
  ];
  for (const [etiqueta, valor] of filas) {
    expect(await screen.findByText(etiqueta)).toBeInTheDocument();
    expect(screen.getByText(etiqueta).nextSibling).toHaveTextContent(
      new RegExp(`^${valor.replace(/[().]/g, '\\$&')}$`),
    );
  }
  expect(
    screen.getByText(
      'Una vez habilitado, el periodo no se puede editar; solo deshabilitar.',
    ),
  ).toBeInTheDocument();
});

test('las reglas salen del periodo vigente: sin bloqueo por morosidad ni matrícula extemporánea', async () => {
  await abrir(
    detalle2027(),
    vigente2027({ bloquearPorMorosidad: false, permitirExtemporanea: false }),
  );

  expect(await screen.findByText('Bloquear por morosidad')).toBeInTheDocument();
  expect(
    screen.getByText('Bloquear por morosidad').nextSibling,
  ).toHaveTextContent('No');
  expect(
    screen.getByText('Matrícula extemporánea').nextSibling,
  ).toHaveTextContent('No permitida');
});

test('un periodo programado todavía se puede editar: no dice que sea de solo lectura', async () => {
  await abrir(detalle2027({ estado: 'PROGRAMADO', editable: true }), {
    ...periodo,
    estado: 'PROGRAMADO',
  });

  expect(
    await screen.findByText(
      'Periodo programado: aún puedes editar su configuración.',
    ),
  ).toBeInTheDocument();
  expect(screen.queryByText(/solo deshabilitar/)).not.toBeInTheDocument();
});

test('sin tarifario confirmado no inventa un monto', async () => {
  await abrir(
    detalle2027({ montoMatricula: null, tarifarioConfirmado: false }),
  );

  expect(
    await screen.findByText('Sin tarifario confirmado'),
  ).toBeInTheDocument();
});

test('si la configuración no carga, avisa en el modal', async () => {
  server.use(
    http.get(`${API}/${periodo.id}`, () =>
      HttpResponse.json({ message: 'Error inesperado.' }, { status: 500 }),
    ),
  );
  await renderApp(
    <DialogoResumenConfiguracion
      abierto
      onCerrar={vi.fn()}
      periodo={periodo}
    />,
    { user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }) },
  );

  expect(
    await screen.findByText('No pudimos cargar la configuración.'),
  ).toBeInTheDocument();
});

test('"Cerrar" cierra el modal', async () => {
  const onCerrar = await abrir();
  await screen.findByText('Vacantes totales');

  await userEvent.click(screen.getByRole('button', { name: 'Cerrar' }));

  expect(onCerrar).toHaveBeenCalled();
});

test('cerrado no pide la configuración', async () => {
  let consultas = 0;
  server.use(
    http.get(`${API}/${periodo.id}`, () => {
      consultas++;
      return HttpResponse.json(detalle2027());
    }),
  );
  await renderApp(
    <DialogoResumenConfiguracion
      abierto={false}
      onCerrar={vi.fn()}
      periodo={periodo}
    />,
    { user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }) },
  );

  await new Promise((resolver) => setTimeout(resolver, 100));

  expect(consultas).toBe(0);
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});
