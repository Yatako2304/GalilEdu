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
import { SolicitudTarifario } from '@/types/api';

import { ConfiguracionPensiones } from '../configuracion-pensiones';

import { API_TARIFARIOS, anio, servirTarifarios, tarifario2027 } from './datos';

// Figma > Personal Administrativo > "P19 · Tarifario y calendario" > "Confirmar tarifario" y el modal (HU-34)

const RUTA = '/app/administrativo/pensiones/configuracion';

// Navegar de verdad en jsdom rompe el router de la prueba: `useSearchParams` se sustituye por uno con estado propio y
// se anota cómo se cambia.
const { cambios } = vi.hoisted(() => ({ cambios: [] as unknown[][] }));
vi.mock('react-router', async () => {
  const actual =
    await vi.importActual<typeof import('react-router')>('react-router');
  const React = await vi.importActual<typeof import('react')>('react');
  return {
    ...actual,
    useSearchParams: () => {
      const [parametros, cambiar] = React.useState(() => new URLSearchParams());
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
  cambios.length = 0;
});

const ANIOS = [anio(2026, 'ACTIVO'), anio(2027, 'PLANIFICADO')];

const tarifario2026 = () =>
  tarifario2027({
    anioEscolarId: 'anio-2026',
    anio: 2026,
    estado: 'CONFIRMADO',
  });

const mostrar = async () =>
  renderApp(<ConfiguracionPensiones />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
    path: RUTA,
    url: RUTA,
  });

const monto = (nombre: string) =>
  screen.getByRole('spinbutton', { name: nombre });

const escribir = async (campo: HTMLElement, valor: string) => {
  await userEvent.clear(campo);
  if (valor) await userEvent.type(campo, valor);
};

const confirmarTarifario = () =>
  userEvent.click(screen.getByRole('button', { name: 'Confirmar tarifario' }));

/** Abre P19 de 2027 con su tarifario en configuración, y espera a que cargue. */
const abrir = async (extras: Parameters<typeof tarifario2027>[0] = {}) => {
  servirTarifarios(ANIOS, {
    'anio-2027': tarifario2027(extras),
    'anio-2026': tarifario2026(),
  });
  await mostrar();
  await screen.findByText('En configuración');
};

/** Registra lo que llega a `POST …/confirmar` y responde con el tarifario ya confirmado. */
const servirConfirmacion = () => {
  const recibido: { cuerpo: SolicitudTarifario | null } = { cuerpo: null };
  server.use(
    http.post(`${API_TARIFARIOS}/anio-2027/confirmar`, async ({ request }) => {
      const cuerpo = (await request.json()) as SolicitudTarifario;
      recibido.cuerpo = cuerpo;
      return HttpResponse.json(
        tarifario2027({
          estado: 'CONFIRMADO',
          conceptos: {
            montoCuotaIngreso: cuerpo.montoCuotaIngreso,
            montoMatricula: cuerpo.montoMatricula,
            vencimientoMatricula: '2027-02-28',
            montoPension: cuerpo.montoPension,
            cantidadCuotas: 10,
          },
          morosidad: {
            diasGracia: cuerpo.diasGracia,
            recargoPorcentaje: cuerpo.recargoPorcentaje,
            topeRecargoPorcentaje: cuerpo.topeRecargoPorcentaje,
          },
        }),
      );
    }),
  );
  return recibido;
};

describe('edición', () => {
  test('el calendario sigue a la pensión que se escribe: cada cuota y el total', async () => {
    await abrir();

    await escribir(monto('Monto de la pensión mensual, en soles'), '700');

    expect(screen.getAllByText('S/ 700.00')).toHaveLength(10);
    expect(
      screen.getByRole('row', { name: /Total pensiones/ }),
    ).toHaveTextContent('Total pensionesS/ 7,000.00');
  });

  test('con la pensión vacía, el calendario queda sin monto', async () => {
    await abrir();

    await escribir(monto('Monto de la pensión mensual, en soles'), '');

    expect(
      screen.getByRole('row', { name: /Total pensiones/ }),
    ).toHaveTextContent('Total pensiones—');
  });

  test('los datos inválidos se avisan bajo cada campo y no se pide confirmación ni se envía nada', async () => {
    let enviado = false;
    await abrir();
    server.use(
      http.post(`${API_TARIFARIOS}/anio-2027/confirmar`, () => {
        enviado = true;
        return HttpResponse.json(tarifario2027());
      }),
    );
    await escribir(monto('Monto de la matrícula, en soles'), '');
    await escribir(monto('Monto de la pensión mensual, en soles'), '0');
    await escribir(screen.getByLabelText('Días de gracia *'), '99');
    await escribir(
      screen.getByLabelText('Recargo por cada 30 días (%) *'),
      '11',
    );

    await confirmarTarifario();

    expect(
      await screen.findByText('Ingresa el monto de la matrícula.'),
    ).toBeInTheDocument();
    expect(
      screen.getByText('El monto de la pensión debe ser mayor que cero.'),
    ).toBeInTheDocument();
    expect(
      screen.getByText('Los días de gracia deben estar entre 0 y 60.'),
    ).toBeInTheDocument();
    expect(
      screen.getByText('El recargo no puede ser mayor que el tope de recargo.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(enviado).toBe(false);
  });
});

describe('modal de confirmación', () => {
  test('resume lo que se va a confirmar, como en Figma', async () => {
    await abrir();
    await escribir(
      screen.getByLabelText('Recargo por cada 30 días (%) *'),
      '2',
    );

    await confirmarTarifario();

    const modal = await screen.findByRole('dialog');
    expect(
      within(modal).getByText('¿Confirmar el tarifario 2027?'),
    ).toBeInTheDocument();
    expect(
      within(modal).getByText(
        /Con el tarifario confirmado se podrán generar los compromisos de pago 2027\. Después de generar el primer compromiso, los montos y el calendario ya no se podrán editar\./,
      ),
    ).toBeInTheDocument();
    const filas = within(modal).getAllByRole('definition');
    expect(filas.map((f) => f.textContent)).toEqual([
      'S/ 500.00',
      'S/ 450.00',
      'S/ 620.00 × 10 cuotas',
      '5 días de gracia · 2 % c/30 días · tope 10 %',
    ]);
  });

  test('sin cuota de ingreso, la muestra con "—" y la envía como null; "1 día" va en singular', async () => {
    await abrir();
    const recibido = servirConfirmacion();
    await escribir(monto('Monto de la cuota de ingreso, en soles'), '');
    await escribir(screen.getByLabelText('Días de gracia *'), '1');

    await confirmarTarifario();

    const modal = await screen.findByRole('dialog');
    const filas = within(modal).getAllByRole('definition');
    expect(filas[0]).toHaveTextContent('—');
    expect(filas[3]).toHaveTextContent(
      '1 día de gracia · 2 % c/30 días · tope 10 %',
    );
    await userEvent.click(
      within(modal).getByRole('button', { name: 'Confirmar' }),
    );
    await waitFor(() => expect(recibido.cuerpo).not.toBeNull());
    expect(recibido.cuerpo).toMatchObject({
      montoCuotaIngreso: null,
      diasGracia: 1,
    });
  });

  test('"Cancelar" cierra el modal sin confirmar y deja lo escrito', async () => {
    let enviado = false;
    await abrir();
    server.use(
      http.post(`${API_TARIFARIOS}/anio-2027/confirmar`, () => {
        enviado = true;
        return HttpResponse.json(tarifario2027());
      }),
    );
    await escribir(monto('Monto de la pensión mensual, en soles'), '700');
    await confirmarTarifario();

    const modal = await screen.findByRole('dialog');
    await userEvent.click(
      within(modal).getByRole('button', { name: 'Cancelar' }),
    );

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(enviado).toBe(false);
    expect(monto('Monto de la pensión mensual, en soles')).toHaveValue(700);
  });

  test('"Confirmar" envía los montos y la morosidad, avisa "Tarifario confirmado" y deja el formulario con lo guardado', async () => {
    await abrir();
    const recibido = servirConfirmacion();
    await escribir(monto('Monto de la pensión mensual, en soles'), '700');
    await confirmarTarifario();

    const modal = await screen.findByRole('dialog');
    await userEvent.click(
      within(modal).getByRole('button', { name: 'Confirmar' }),
    );

    expect(await screen.findByText('Tarifario confirmado')).toBeInTheDocument();
    expect(
      screen.getByText(
        'Los montos y el calendario del año escolar 2027 quedaron confirmados.',
      ),
    ).toBeInTheDocument();
    expect(recibido.cuerpo).toEqual({
      montoCuotaIngreso: 500,
      montoMatricula: 450,
      montoPension: 700,
      diasGracia: 5,
      recargoPorcentaje: 2,
      topeRecargoPorcentaje: 10,
    });
    // La etiqueta y los campos pasan a lo confirmado; sigue pudiéndose volver a confirmar.
    expect(await screen.findByText('Confirmado')).toBeInTheDocument();
    expect(screen.queryByText('En configuración')).not.toBeInTheDocument();
    expect(monto('Monto de la pensión mensual, en soles')).toHaveValue(700);
    expect(
      screen.getByRole('row', { name: /Total pensiones/ }),
    ).toHaveTextContent('S/ 7,000.00');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Confirmar tarifario' }),
    ).toBeEnabled();
  });

  test('si el backend rechaza (409), muestra su mensaje tal cual, cierra el modal y deja lo escrito', async () => {
    const mensaje =
      'Ya se generaron compromisos de pago 2027: los montos y el calendario no se pueden modificar.';
    await abrir();
    server.use(
      http.post(`${API_TARIFARIOS}/anio-2027/confirmar`, () =>
        HttpResponse.json({ message: mensaje }, { status: 409 }),
      ),
    );
    await escribir(monto('Monto de la pensión mensual, en soles'), '700');
    await confirmarTarifario();

    const modal = await screen.findByRole('dialog');
    await userEvent.click(
      within(modal).getByRole('button', { name: 'Confirmar' }),
    );

    expect(await screen.findByText(mensaje)).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(screen.queryByText('Tarifario confirmado')).not.toBeInTheDocument();
    expect(monto('Monto de la pensión mensual, en soles')).toHaveValue(700);
    expect(screen.getByText('En configuración')).toBeInTheDocument();
  });
});

describe('cambios sin confirmar', () => {
  const avisarAlCerrar = () => {
    const evento = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(evento);
    return evento.defaultPrevented;
  };

  test('al cerrar o recargar la pestaña, el navegador avisa solo si hay cambios', async () => {
    await abrir();
    expect(avisarAlCerrar()).toBe(false);

    await escribir(monto('Monto de la matrícula, en soles'), '460');
    expect(avisarAlCerrar()).toBe(true);

    // Volver a lo que había guarda la pantalla otra vez sin cambios.
    await escribir(monto('Monto de la matrícula, en soles'), '450');
    expect(avisarAlCerrar()).toBe(false);
  });

  test('al confirmar ya no hay cambios pendientes', async () => {
    await abrir();
    servirConfirmacion();
    await escribir(monto('Monto de la matrícula, en soles'), '460');
    expect(avisarAlCerrar()).toBe(true);
    await confirmarTarifario();
    const modal = await screen.findByRole('dialog');
    await userEvent.click(
      within(modal).getByRole('button', { name: 'Confirmar' }),
    );
    await screen.findByText('Confirmado');

    expect(avisarAlCerrar()).toBe(false);
  });

  test('"Volver" con cambios pide confirmación; "Seguir editando" se queda con lo escrito', async () => {
    await abrir();
    await escribir(monto('Monto de la matrícula, en soles'), '460');

    await userEvent.click(screen.getByRole('link', { name: 'Volver' }));

    const aviso = await screen.findByRole('dialog');
    expect(
      within(aviso).getByText('¿Salir sin confirmar?'),
    ).toBeInTheDocument();
    expect(
      within(aviso).getByText(
        'Tienes cambios sin confirmar en el tarifario. Si sigues, se perderán.',
      ),
    ).toBeInTheDocument();
    await userEvent.click(
      within(aviso).getByRole('button', { name: 'Seguir editando' }),
    );

    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(monto('Monto de la matrícula, en soles')).toHaveValue(460);
  });

  test('"Salir sin confirmar" deja seguir con la navegación', async () => {
    await abrir();
    await escribir(monto('Monto de la matrícula, en soles'), '460');
    await userEvent.click(screen.getByRole('link', { name: 'Volver' }));
    const aviso = await screen.findByRole('dialog');

    await userEvent.click(
      within(aviso).getByRole('button', { name: 'Salir sin confirmar' }),
    );

    await waitFor(() =>
      expect(
        screen.queryByRole('link', { name: 'Volver' }),
      ).not.toBeInTheDocument(),
    );
  });

  test('al cambiar de año con cambios pide confirmación; "Seguir editando" se queda en el mismo año', async () => {
    const consultas: string[] = [];
    servirTarifarios(
      ANIOS,
      { 'anio-2027': tarifario2027(), 'anio-2026': tarifario2026() },
      { consultas },
    );
    await mostrar();
    await screen.findByText('En configuración');
    await escribir(monto('Monto de la matrícula, en soles'), '460');

    await userEvent.selectOptions(
      screen.getByLabelText('Año escolar'),
      'anio-2026',
    );

    const aviso = await screen.findByRole('dialog');
    expect(
      within(aviso).getByText('¿Cambiar de año sin confirmar?'),
    ).toBeInTheDocument();
    await userEvent.click(
      within(aviso).getByRole('button', { name: 'Seguir editando' }),
    );
    await waitFor(() =>
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument(),
    );
    expect(consultas).toEqual(['anio-2027']);
    expect(cambios).toEqual([]);
    expect(monto('Monto de la matrícula, en soles')).toHaveValue(460);
  });

  test('"Cambiar de año" descarta lo escrito y abre el otro año', async () => {
    const consultas: string[] = [];
    servirTarifarios(
      ANIOS,
      { 'anio-2027': tarifario2027(), 'anio-2026': tarifario2026() },
      { consultas },
    );
    await mostrar();
    await screen.findByText('En configuración');
    await escribir(monto('Monto de la matrícula, en soles'), '460');
    await userEvent.selectOptions(
      screen.getByLabelText('Año escolar'),
      'anio-2026',
    );
    const aviso = await screen.findByRole('dialog');

    await userEvent.click(
      within(aviso).getByRole('button', { name: 'Cambiar de año' }),
    );

    expect(await screen.findByText('Confirmado')).toBeInTheDocument();
    expect(consultas).toEqual(['anio-2027', 'anio-2026']);
    expect(cambios).toEqual([
      [{ anioEscolarId: 'anio-2026' }, { replace: true }],
    ]);
    expect(monto('Monto de la matrícula, en soles')).toHaveValue(450);
    expect(avisarAlCerrar()).toBe(false);
  });
});
