import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { server } from '@/testing/mocks/server';
import {
  createUser,
  renderApp,
  screen,
  userEvent,
  waitFor,
  within,
} from '@/testing/test-utils';
import {
  AnioEscolarDetalle,
  AnioEscolarResumen,
  EstadoAnioEscolar,
  RequisitosAnio,
} from '@/types/api';

import { AccionesAnio, motivoSinActivar } from '../acciones-anio';

// Figma > CI-06 (Editar, Activar año) y CI-12 (Cerrar año): acciones de la cabecera según el estado del año.

const API = env.API_URL;

const detalle = (
  anio: number,
  estado: EstadoAnioEscolar,
): AnioEscolarDetalle => ({
  id: `anio-${anio}`,
  anio,
  fechaInicio: `${anio}-03-01`,
  fechaFin: `${anio}-12-17`,
  regimen: 'TRIMESTRAL',
  estado,
  cantidadSecciones: 10,
  cantidadEstructuras: 2,
  periodos: [],
});

const resumen = (
  anio: number,
  estado: EstadoAnioEscolar,
): AnioEscolarResumen => ({
  id: `anio-${anio}`,
  anio,
  fechaInicio: `${anio}-03-01`,
  fechaFin: `${anio}-12-17`,
  regimen: 'TRIMESTRAL',
  cantidadPeriodos: 3,
  seccionesActivas: 10,
  estructurasHorarias: 2,
  estado,
});

const requisito = (codigo: string, texto: string, cumplido: boolean) =>
  ({
    codigo,
    texto,
    cumplido,
    detalle: '',
  }) as RequisitosAnio['obligatorios'][0];

const requisitosCompletos: RequisitosAnio = {
  puedeActivar: true,
  obligatorios: [
    requisito('PERIODOS', 'Periodos académicos definidos', true),
    requisito('SECCIONES', 'Secciones creadas', true),
    requisito('ESTRUCTURA_HORARIA', 'Estructura horaria registrada', true),
  ],
  recomendados: [],
};

/** Responde la lista de años y los requisitos del 2027. */
const servir = (
  anios: AnioEscolarResumen[],
  requisitos = requisitosCompletos,
) =>
  server.use(
    http.get(`${API}/configuracion/anios-escolares`, () =>
      HttpResponse.json(anios),
    ),
    http.get(`${API}/configuracion/anios-escolares/anio-2027/requisitos`, () =>
      HttpResponse.json(requisitos),
    ),
  );

const mostrar = async (anio: AnioEscolarDetalle) =>
  renderApp(<AccionesAnio anio={anio} />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

describe('año PLANIFICADO', () => {
  test('muestra "Editar" (secundario) y "Activar año" (principal), en ese orden', async () => {
    servir([resumen(2027, 'PLANIFICADO')]);
    await mostrar(detalle(2027, 'PLANIFICADO'));

    // Al terminar de cargar, "Activar año" pasa de estar bloqueado a ser el botón que abre la confirmación.
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Activar año' }),
      ).not.toHaveAttribute('aria-disabled'),
    );
    const botones = screen.getAllByRole('button');
    expect(botones.map((b) => b.textContent)).toEqual([
      'Editar',
      'Activar año',
    ]);
    expect(botones[0]).toHaveClass('bg-white'); // outline: secundario
    expect(botones[1]).toHaveClass('bg-surface-accent-primary'); // primary: principal
  });

  test('Activar año pide confirmación con el texto de Figma y activa al confirmar', async () => {
    let activado = false;
    servir([resumen(2027, 'PLANIFICADO')]);
    server.use(
      http.post(
        `${API}/configuracion/anios-escolares/anio-2027/activar`,
        () => {
          activado = true;
          return HttpResponse.json(detalle(2027, 'ACTIVO'));
        },
      ),
    );
    await mostrar(detalle(2027, 'PLANIFICADO'));

    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Activar año' }),
      ).not.toHaveAttribute('aria-disabled'),
    );
    await userEvent.click(screen.getByRole('button', { name: 'Activar año' }));

    const dialogo = await screen.findByRole('dialog');
    expect(
      within(dialogo).getByText('¿Activar el año escolar 2027?'),
    ).toBeInTheDocument();
    expect(
      within(dialogo).getByText(
        'Pasará a ser el año en curso y ya no podrás editar sus fechas ni su régimen.',
      ),
    ).toBeInTheDocument();
    expect(activado).toBe(false);

    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Activar año' }),
    );

    await waitFor(() => expect(activado).toBe(true));
    expect(await screen.findByText('Año escolar activado')).toBeInTheDocument();
  });

  test('Cancelar en la confirmación no activa nada', async () => {
    let activado = false;
    servir([resumen(2027, 'PLANIFICADO')]);
    server.use(
      http.post(
        `${API}/configuracion/anios-escolares/anio-2027/activar`,
        () => {
          activado = true;
          return HttpResponse.json(detalle(2027, 'ACTIVO'));
        },
      ),
    );
    await mostrar(detalle(2027, 'PLANIFICADO'));

    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Activar año' }),
      ).not.toHaveAttribute('aria-disabled'),
    );
    await userEvent.click(screen.getByRole('button', { name: 'Activar año' }));
    await userEvent.click(
      within(await screen.findByRole('dialog')).getByRole('button', {
        name: 'Cancelar',
      }),
    );

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(activado).toBe(false);
  });

  test('con otro año ACTIVO, "Activar año" queda deshabilitado y el tooltip explica por qué', async () => {
    servir([resumen(2027, 'PLANIFICADO'), resumen(2026, 'ACTIVO')]);
    await mostrar(detalle(2027, 'PLANIFICADO'));

    const tooltip = await screen.findByRole('tooltip');
    expect(tooltip).toHaveTextContent(
      'El año 2026 sigue activo. Ciérralo antes de activar este.',
    );
    const activar = screen.getByRole('button', { name: 'Activar año' });
    expect(activar).toHaveAttribute('aria-disabled', 'true');
    expect(activar).toHaveAccessibleDescription(
      'El año 2026 sigue activo. Ciérralo antes de activar este.',
    );
    await userEvent.click(activar);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  test('con requisitos obligatorios pendientes, se deshabilita y nombra los que faltan', async () => {
    servir([resumen(2027, 'PLANIFICADO')], {
      puedeActivar: false,
      obligatorios: [
        requisito('PERIODOS', 'Periodos académicos definidos', true),
        requisito('SECCIONES', 'Secciones creadas', false),
        requisito('ESTRUCTURA_HORARIA', 'Estructura horaria registrada', false),
      ],
      recomendados: [],
    });
    await mostrar(detalle(2027, 'PLANIFICADO'));

    expect(
      await screen.findByText(
        'Faltan requisitos obligatorios: Secciones creadas, Estructura horaria registrada.',
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Activar año' })).toHaveAttribute(
      'aria-disabled',
      'true',
    );
  });

  test('"Editar" abre el modal con el año', async () => {
    servir([resumen(2027, 'PLANIFICADO')]);
    await mostrar(detalle(2027, 'PLANIFICADO'));

    await userEvent.click(
      await screen.findByRole('button', { name: 'Editar' }),
    );

    expect(
      await screen.findByRole('heading', { name: 'Editar año escolar 2027' }),
    ).toBeInTheDocument();
  });
});

describe('año ACTIVO', () => {
  test('solo ofrece "Cerrar año", sin pedir la lista ni los requisitos', async () => {
    // Sin handlers: cualquier petición haría fallar la prueba (onUnhandledRequest: 'error').
    await mostrar(detalle(2026, 'ACTIVO'));

    const botones = screen.getAllByRole('button');
    expect(botones.map((b) => b.textContent)).toEqual(['Cerrar año']);
  });

  test('Cerrar año pide confirmación y cierra al confirmar', async () => {
    let cerrado = false;
    server.use(
      http.get(`${API}/configuracion/anios-escolares`, () =>
        HttpResponse.json([resumen(2026, 'ACTIVO')]),
      ),
      http.get(`${API}/configuracion/anios-escolares/actual`, () =>
        HttpResponse.json({ id: 'anio-2026', anio: 2026 }),
      ),
      http.post(`${API}/configuracion/anios-escolares/anio-2026/cerrar`, () => {
        cerrado = true;
        return HttpResponse.json(detalle(2026, 'CERRADO'));
      }),
    );
    await mostrar(detalle(2026, 'ACTIVO'));

    await userEvent.click(screen.getByRole('button', { name: 'Cerrar año' }));
    const dialogo = await screen.findByRole('dialog');
    expect(
      within(dialogo).getByText('¿Cerrar el año escolar 2026?'),
    ).toBeInTheDocument();
    await userEvent.click(
      within(dialogo).getByRole('button', { name: 'Cerrar año' }),
    );

    await waitFor(() => expect(cerrado).toBe(true));
    expect(await screen.findByText('Año escolar cerrado')).toBeInTheDocument();
  });

  test('si el backend responde 409, se ve su mensaje y el modal se cierra', async () => {
    server.use(
      http.post(`${API}/configuracion/anios-escolares/anio-2026/cerrar`, () =>
        HttpResponse.json(
          {
            message: 'Los docentes pueden registrar notas hasta el 23/12/2026.',
          },
          { status: 409 },
        ),
      ),
    );
    await mostrar(detalle(2026, 'ACTIVO'));

    await userEvent.click(screen.getByRole('button', { name: 'Cerrar año' }));
    await userEvent.click(
      within(await screen.findByRole('dialog')).getByRole('button', {
        name: 'Cerrar año',
      }),
    );

    expect(
      await screen.findByText(
        'Los docentes pueden registrar notas hasta el 23/12/2026.',
      ),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(
      screen.getByRole('button', { name: 'Cerrar año' }),
    ).toBeInTheDocument();
  });
});

test('un año CERRADO no tiene acciones', async () => {
  await mostrar(detalle(2025, 'CERRADO'));

  expect(screen.queryByRole('button')).not.toBeInTheDocument();
});

describe('motivoSinActivar', () => {
  const requisitos = (puedeActivar: boolean) => ({
    puedeActivar,
    obligatorios: [{ cumplido: false, texto: 'Secciones creadas' }],
  });

  test('otro año activo manda sobre todo lo demás', () => {
    expect(
      motivoSinActivar({
        otroActivo: { anio: 2026 },
        cargando: true,
        fallo: true,
        requisitos: requisitos(false),
      }),
    ).toBe('El año 2026 sigue activo. Ciérralo antes de activar este.');
  });

  test('mientras carga o si falla, avisa y no deja activar', () => {
    expect(motivoSinActivar({ cargando: true, fallo: false })).toBe(
      'Verificando los requisitos…',
    );
    expect(motivoSinActivar({ cargando: false, fallo: true })).toBe(
      'No se pudieron verificar los requisitos.',
    );
  });

  test('sin requisitos pendientes devuelve null', () => {
    expect(
      motivoSinActivar({
        cargando: false,
        fallo: false,
        requisitos: { puedeActivar: true, obligatorios: [] },
      }),
    ).toBeNull();
  });

  test('con requisitos pendientes los lista', () => {
    expect(
      motivoSinActivar({
        cargando: false,
        fallo: false,
        requisitos: requisitos(false),
      }),
    ).toBe('Faltan requisitos obligatorios: Secciones creadas.');
  });
});
