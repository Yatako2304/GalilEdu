import { HttpResponse, http } from 'msw';

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

import { ConfiguracionPensiones } from '../configuracion-pensiones';

import {
  API_ANIOS,
  anio,
  propuestaVacia,
  servirTarifarios,
  tarifario2027,
} from './datos';

// Figma > Personal Administrativo > "P19 · Tarifario y calendario" (HU-34), en solo lectura

const RUTA = '/app/administrativo/pensiones/configuracion';

// Navegar de verdad en jsdom rompe el router de la prueba: `useSearchParams` se sustituye por uno con estado propio,
// que parte de `?anioEscolarId=` (`busqueda`) y anota cómo se cambia.
const { busqueda, cambios } = vi.hoisted(() => ({
  busqueda: { inicial: '' },
  cambios: [] as unknown[][],
}));
vi.mock('react-router', async () => {
  const actual =
    await vi.importActual<typeof import('react-router')>('react-router');
  const React = await vi.importActual<typeof import('react')>('react');
  return {
    ...actual,
    useSearchParams: () => {
      const [parametros, cambiar] = React.useState(
        () => new URLSearchParams(busqueda.inicial),
      );
      return [
        parametros,
        (siguiente: Record<string, string>, opciones: unknown) => {
          cambios.push([siguiente, opciones]);
          cambiar(new URLSearchParams(siguiente));
        },
      ] as const;
    },
  };
});

beforeEach(() => {
  busqueda.inicial = '';
  cambios.length = 0;
});

const ANIOS = [
  anio(2025, 'CERRADO'),
  anio(2026, 'ACTIVO'),
  anio(2027, 'PLANIFICADO'),
];

const mostrar = async (busquedaInicial = '') => {
  busqueda.inicial = busquedaInicial;
  return renderApp(<ConfiguracionPensiones />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
    path: RUTA,
    url: RUTA,
  });
};

const tarifario2026 = () =>
  tarifario2027({
    anioEscolarId: 'anio-2026',
    anio: 2026,
    estado: 'CONFIRMADO',
    conceptos: {
      montoCuotaIngreso: 500,
      montoMatricula: 420,
      vencimientoMatricula: '2026-02-28',
      montoPension: 600,
      cantidadCuotas: 10,
    },
  });

const monto = (nombre: string) =>
  screen.getByRole('spinbutton', { name: nombre });

describe('año que abre la pantalla', () => {
  test('sin año en la dirección, el PLANIFICADO más reciente', async () => {
    const consultas: string[] = [];
    servirTarifarios(
      ANIOS,
      { 'anio-2027': tarifario2027(), 'anio-2026': tarifario2026() },
      { consultas },
    );
    await mostrar();

    expect(await screen.findByText('En configuración')).toBeInTheDocument();
    expect(consultas).toEqual(['anio-2027']);
    expect(screen.getByLabelText('Año escolar')).toHaveDisplayValue(
      'Año escolar 2027',
    );
  });

  test('con ?anioEscolarId= abre ese año', async () => {
    const consultas: string[] = [];
    servirTarifarios(
      ANIOS,
      { 'anio-2027': tarifario2027(), 'anio-2026': tarifario2026() },
      { consultas },
    );
    await mostrar('?anioEscolarId=anio-2026');

    expect(await screen.findByText('Confirmado')).toBeInTheDocument();
    expect(consultas).toEqual(['anio-2026']);
    expect(monto('Monto de la matrícula, en soles')).toHaveValue(420);
  });

  test('al cambiar de año en el selector pide el de ese año', async () => {
    const consultas: string[] = [];
    servirTarifarios(
      ANIOS,
      { 'anio-2027': tarifario2027(), 'anio-2026': tarifario2026() },
      { consultas },
    );
    await mostrar();
    await screen.findByText('En configuración');
    expect(
      within(screen.getByLabelText('Año escolar'))
        .getAllByRole('option', { hidden: true })
        .map((o) => o.textContent),
    ).toEqual(['Año escolar 2027', 'Año escolar 2026', 'Año escolar 2025']);

    await userEvent.selectOptions(
      screen.getByLabelText('Año escolar'),
      'anio-2026',
    );

    expect(await screen.findByText('Confirmado')).toBeInTheDocument();
    expect(consultas).toEqual(['anio-2027', 'anio-2026']);
    // El año elegido queda en la dirección, para poder compartirla o recargarla.
    expect(cambios).toEqual([
      [{ anioEscolarId: 'anio-2026' }, { replace: true }],
    ]);
    expect(monto('Monto de la pensión mensual, en soles')).toHaveValue(600);
  });
});

describe('un tarifario en configuración (Figma 2027)', () => {
  beforeEach(() => {
    servirTarifarios(ANIOS, { 'anio-2027': tarifario2027() });
  });

  test('muestra los conceptos y montos con su nombre accesible en soles', async () => {
    await mostrar();
    await screen.findByText('En configuración');

    expect(monto('Monto de la cuota de ingreso, en soles')).toHaveValue(500);
    expect(monto('Monto de la matrícula, en soles')).toHaveValue(450);
    expect(monto('Monto de la pensión mensual, en soles')).toHaveValue(620);
    const tabla = within(
      screen
        .getByRole('heading', { name: 'Conceptos y montos' })
        .closest('div')!,
    );
    expect(tabla.getByText('Pago único al incorporarse')).toBeInTheDocument();
    expect(tabla.getByText('Solo alumnos nuevos')).toBeInTheDocument();
    expect(tabla.getByText('Antes de la matrícula')).toBeInTheDocument();
    expect(tabla.getByText('Matrícula 2027')).toBeInTheDocument();
    expect(tabla.getByText('28/02/2027')).toBeInTheDocument();
    // La cantidad y los meses salen del calendario del backend.
    expect(tabla.getByText('10 cuotas (marzo–diciembre)')).toBeInTheDocument();
    expect(
      tabla.getByText('Último día de cada mes · diciembre: 15/12'),
    ).toBeInTheDocument();
  });

  test('muestra la política de morosidad', async () => {
    await mostrar();
    await screen.findByText('En configuración');

    expect(screen.getByLabelText('Días de gracia *')).toHaveValue(5);
    expect(screen.getByLabelText('Recargo por cada 30 días (%) *')).toHaveValue(
      2,
    );
    expect(screen.getByLabelText('Tope de recargo (%) *')).toHaveValue(10);
    expect(
      screen.getByText(
        'El recargo se calcula sobre el capital pendiente de la cuota a partir del día siguiente al periodo de gracia y no supera el tope.',
      ),
    ).toBeInTheDocument();
  });

  test('el calendario lista las cuotas con su vencimiento, su monto y el total', async () => {
    await mostrar();
    await screen.findByText('En configuración');

    const filas = screen.getAllByRole('row');
    const calendario = filas.filter((f) =>
      /Pensión de/.test(f.textContent ?? ''),
    );
    expect(calendario).toHaveLength(10);
    expect(calendario[0]).toHaveTextContent(
      '1Pensión de marzo31/03/2027S/ 620.00',
    );
    expect(calendario[9]).toHaveTextContent(
      '10Pensión de diciembre15/12/2027S/ 620.00',
    );
    expect(
      screen.getByRole('row', { name: /Total pensiones/ }),
    ).toHaveTextContent('Total pensionesS/ 6,200.00');
  });

  test('los campos se pueden editar; la pestaña de descuentos no está disponible y hay "Volver" y "Confirmar tarifario"', async () => {
    await mostrar();
    await screen.findByText('En configuración');

    expect(monto('Monto de la matrícula, en soles')).toBeEnabled();
    expect(screen.getByLabelText('Días de gracia *')).toBeEnabled();
    expect(
      screen.getByRole('tab', { name: 'Tarifario y calendario' }),
    ).toBeInTheDocument();
    const descuentos = screen.getByRole('tab', {
      name: 'Catálogo de descuentos',
    });
    expect(descuentos).toBeDisabled();
    expect(descuentos).toHaveAttribute('title', 'Disponible pronto');
    expect(screen.getByRole('link', { name: 'Volver' })).toHaveAttribute(
      'href',
      '/app/administrativo/pensiones',
    );
    expect(
      screen.getByRole('button', { name: 'Confirmar tarifario' }),
    ).toBeEnabled();
  });
});

describe('otros estados del tarifario', () => {
  test('una propuesta sin datos del año anterior deja los campos vacíos y el calendario sin montos', async () => {
    servirTarifarios(ANIOS, { 'anio-2027': propuestaVacia() });
    await mostrar();
    await screen.findByText('En configuración');

    expect(monto('Monto de la matrícula, en soles')).toHaveValue(null);
    expect(monto('Monto de la pensión mensual, en soles')).toHaveValue(null);
    expect(screen.getByLabelText('Días de gracia *')).toHaveValue(null);
    expect(
      screen.getByRole('row', { name: /Total pensiones/ }),
    ).toHaveTextContent('Total pensiones—');
    expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(11);
  });

  test('de solo lectura: muestra el motivo del backend y no ofrece confirmar', async () => {
    const motivo =
      'Ya se generaron compromisos de pago 2026: los montos y el calendario no se pueden modificar.';
    servirTarifarios(ANIOS, {
      'anio-2027': tarifario2027(),
      'anio-2026': {
        ...tarifario2026(),
        soloLectura: true,
        motivoSoloLectura: motivo,
      },
    });
    await mostrar('?anioEscolarId=anio-2026');

    expect(await screen.findByText(motivo)).toBeInTheDocument();
    expect(monto('Monto de la matrícula, en soles')).toBeDisabled();
    expect(screen.getByLabelText('Tope de recargo (%) *')).toBeDisabled();
    expect(
      screen.queryByRole('button', { name: 'Confirmar tarifario' }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Volver' })).toBeInTheDocument();
  });

  test('calendario desactualizado y se puede confirmar: avisa que hay que volver a confirmar', async () => {
    servirTarifarios(ANIOS, {
      'anio-2027': tarifario2027({ calendarioDesactualizado: true }),
    });
    await mostrar();

    expect(
      await screen.findByText(
        'Las fechas del año escolar cambiaron después de confirmar este tarifario. Vuelve a confirmarlo para actualizar el calendario.',
      ),
    ).toBeInTheDocument();
  });

  test('calendario desactualizado y de solo lectura: avisa que no se puede regenerar', async () => {
    servirTarifarios(ANIOS, {
      'anio-2027': tarifario2027({
        calendarioDesactualizado: true,
        soloLectura: true,
        motivoSoloLectura: 'Ya se generaron compromisos de pago 2027.',
      }),
    });
    await mostrar();

    expect(
      await screen.findByText(
        'Las fechas del año escolar ya no coinciden con este calendario, pero no se puede regenerar porque ya hay compromisos de pago.',
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText('Ya se generaron compromisos de pago 2027.'),
    ).toBeInTheDocument();
  });

  test('un tarifario que no carga lo avisa en la pantalla', async () => {
    servirTarifarios(ANIOS, {});
    await mostrar();

    expect(
      await screen.findByText('No pudimos cargar el tarifario del año.'),
    ).toBeInTheDocument();
    expect(screen.queryByText('Conceptos y montos')).not.toBeInTheDocument();
  });

  test('sin años escolares lo dice y no pide ningún tarifario', async () => {
    servirTarifarios([], {});
    await mostrar();

    expect(
      await screen.findByText(
        'Aún no hay años escolares para configurar su tarifario.',
      ),
    ).toBeInTheDocument();
    expect(screen.queryByLabelText('Año escolar')).not.toBeInTheDocument();
  });

  test('si los años no cargan, lo avisa en la pantalla', async () => {
    server.use(
      http.get(API_ANIOS, () =>
        HttpResponse.json({ message: 'Error inesperado.' }, { status: 500 }),
      ),
    );
    await mostrar();

    expect(
      await screen.findByText('No pudimos cargar los años escolares.'),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByRole('progressbar')).not.toBeInTheDocument(),
    );
  });
});
