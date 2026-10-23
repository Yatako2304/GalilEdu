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
import { FormularioPeriodoMatricula, SeccionVacante } from '@/types/api';

import { FormularioPeriodo } from '../formulario-periodo';

import { API } from './datos';

// Figma > Personal Administrativo > pantalla 13 > "Nuevo periodo de matrícula" (HU-23)

// Navegar de verdad en jsdom rompe el router de la prueba: se sustituye `useNavigate` y se comprueba el destino.
const { navegar, enlaces } = vi.hoisted(() => ({
  navegar: vi.fn(),
  enlaces: {
    MATRICULAS_DEL_PERIODO_DISPONIBLE: false,
    TARIFARIO_DISPONIBLE: false,
    TEXTO_DISPONIBLE_PRONTO: 'Disponible pronto',
  },
}));
vi.mock('react-router', async () => ({
  ...(await vi.importActual<typeof import('react-router')>('react-router')),
  useNavigate: () => navegar,
}));
vi.mock('../enlaces', () => enlaces);

beforeEach(() => {
  navegar.mockClear();
  enlaces.TARIFARIO_DISPONIBLE = false;
  // El calendario abre en el mes de hoy: se fija el día (solo la fecha) para elegir fechas de diciembre de 2026.
  vi.useFakeTimers({ toFake: ['Date'] });
  vi.setSystemTime(new Date(2026, 11, 10, 12, 0, 0));
});
afterEach(() => vi.useRealTimers());

const seccion = (
  seccionId: string,
  etiqueta: string,
  tutor: string | null,
  alumnosOcupanVacante = 0,
): SeccionVacante => ({
  seccionId,
  etiqueta,
  gradoCodigo: 'SEC-1',
  gradoNombre: '1.° de secundaria',
  tutor,
  capacidad: 30,
  alumnosOcupanVacante,
});

const formularioNuevo = (
  extras: Partial<FormularioPeriodoMatricula> = {},
): FormularioPeriodoMatricula => ({
  id: null,
  anioEscolarId: 'anio-2027',
  anio: 2027,
  titulo: 'Matrícula 2027',
  estado: null,
  editable: true,
  eliminable: false,
  fechaInicio: null,
  horaInicio: null,
  fechaCierre: null,
  horaCierre: null,
  diasDelPeriodo: null,
  montoMatricula: 450,
  tarifarioConfirmado: true,
  vacantesTotales: 60,
  matriculasRegistradas: 0,
  cantidadSecciones: 2,
  resumenSecciones: '1.°, A y B',
  bloquearPorMorosidad: true,
  permitirExtemporanea: true,
  secciones: [
    seccion('s1', '1.° A', 'Jorge Ramírez'),
    seccion('s2', '1.° B', null, 2),
  ],
  documentos: [],
  ...extras,
});

const ANIOS = [
  { id: 'anio-2028', anio: 2028, estado: 'PLANIFICADO' },
  { id: 'anio-2027', anio: 2027, estado: 'PLANIFICADO' },
];

type Servidor = {
  anios?: typeof ANIOS;
  nuevo?: FormularioPeriodoMatricula;
  /** Años por los que se pidió `/nuevo`. */
  consultas?: string[];
};

const servir = ({ anios = ANIOS, nuevo, consultas }: Servidor = {}) =>
  server.use(
    http.get(`${API}/anios-disponibles`, () => HttpResponse.json(anios)),
    http.get(`${API}/nuevo`, ({ request }) => {
      const anio = new URL(request.url).searchParams.get('anioEscolarId');
      consultas?.push(String(anio));
      return HttpResponse.json(
        nuevo ?? formularioNuevo({ anioEscolarId: String(anio) }),
      );
    }),
  );

const mostrar = async () =>
  renderApp(<FormularioPeriodo />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

/** Espera a que lleguen las secciones del año elegido. */
const cargado = () => screen.findByText('Tutor: Jorge Ramírez');

const elegirFecha = async (campo: RegExp, dia: string) => {
  await userEvent.click(screen.getByRole('button', { name: campo }));
  await userEvent.click(screen.getByRole('button', { name: dia }));
};

/** El botón del Select de la interfaz (el <select> nativo oculto lleva el mismo nombre). */
const selector = (nombre: RegExp) => {
  const campo = screen.getByLabelText(nombre);
  return campo.tagName === 'BUTTON'
    ? campo
    : campo.parentElement!.querySelector<HTMLElement>('button')!;
};

const elegirHora = async (campo: RegExp, etiqueta: string) => {
  await userEvent.click(selector(campo));
  const lista = await screen.findByRole('listbox');
  await userEvent.click(within(lista).getByRole('option', { name: etiqueta }));
};

/** Inicio el 15/12/2026 a las 10:00 a.m. y cierre el 20/12/2026 a las 11:59 p.m. */
const llenarFechas = async () => {
  await elegirFecha(/Fecha de inicio/, '15/12/2026');
  await elegirHora(/Hora de inicio/, '10:00 a.m.');
  await elegirFecha(/Fecha de cierre/, '20/12/2026');
  await elegirHora(/Hora de cierre/, '11:59 p.m.');
};

const guardar = () =>
  userEvent.click(
    screen.getByRole('button', { name: 'Guardar configuración' }),
  );

const agregarDocumento = async (
  nombre: string,
  descripcion = '',
  archivo?: File,
) => {
  await userEvent.type(screen.getByLabelText('Nombre del documento'), nombre);
  if (descripcion) {
    await userEvent.type(
      screen.getByLabelText('Descripción del documento (opcional)'),
      descripcion,
    );
  }
  if (archivo) {
    await userEvent.upload(
      screen.getByLabelText('Seleccionar archivo'),
      archivo,
    );
  }
  await userEvent.click(
    screen.getByRole('button', { name: 'Agregar documento' }),
  );
};

const csv = (nombre = 'ficha.csv') =>
  new File(['nombre,dni\nAna,1\n'], nombre, { type: 'text/csv' });

const cuerpoEsperado = (extras: Record<string, unknown> = {}) => ({
  anioEscolarId: 'anio-2027',
  fechaInicio: '2026-12-15',
  horaInicio: '10:00',
  fechaCierre: '2026-12-20',
  horaCierre: '23:59',
  vacantes: [
    { seccionId: 's1', capacidad: 30 },
    { seccionId: 's2', capacidad: 30 },
  ],
  documentos: [],
  ...extras,
});

describe('al abrir', () => {
  test('el primer año disponible ya está elegido y se cargan sus secciones', async () => {
    const consultas: string[] = [];
    servir({ consultas });
    await mostrar();
    await cargado();

    expect(selector(/Año académico/)).toHaveTextContent('2027');
    expect(consultas).toEqual(['anio-2027']);
    expect(screen.getByText('1.° A')).toBeInTheDocument();
    expect(screen.getByText('Tutor: Sin tutor')).toBeInTheDocument();
    expect(screen.getAllByText('1.° de secundaria')).toHaveLength(2);
    expect(screen.getByText('Total: 60 vacantes')).toBeInTheDocument();
    // Los indicadores de arriba: las vacantes y el monto ya se conocen; las fechas y los documentos, no.
    expect(screen.getByText('60')).toBeInTheDocument();
    expect(screen.getByText('S/ 450.00')).toBeInTheDocument();
    expect(screen.getByText('No definido')).toBeInTheDocument();
    expect(
      screen.getByText('Se toma del tarifario vigente 2027. No se edita aquí.'),
    ).toBeInTheDocument();
  });

  test('con otro año en la lista se puede elegir y se cargan sus secciones', async () => {
    const consultas: string[] = [];
    servir({ consultas });
    await mostrar();
    await cargado();

    await userEvent.click(selector(/Año académico/));
    await userEvent.click(
      within(await screen.findByRole('listbox')).getByRole('option', {
        name: '2028',
      }),
    );

    await waitFor(() => expect(consultas).toEqual(['anio-2027', 'anio-2028']));
  });

  test('sin años disponibles avisa y deja el formulario deshabilitado', async () => {
    servir({ anios: [] });
    await mostrar();

    expect(
      await screen.findByText(
        'No hay años escolares disponibles para un nuevo periodo de matrícula.',
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Guardar configuración' }),
    ).toBeDisabled();
    expect(
      screen.getByRole('button', { name: /Fecha de inicio/ }),
    ).toBeDisabled();
    expect(
      screen.getByRole('button', { name: 'Agregar documento' }),
    ).toBeDisabled();
    expect(screen.getByLabelText('Nombre del documento')).toBeDisabled();
  });

  test('si las secciones del año no cargan, avisa en la pantalla', async () => {
    server.use(
      http.get(`${API}/anios-disponibles`, () => HttpResponse.json(ANIOS)),
      http.get(`${API}/nuevo`, () =>
        HttpResponse.json({ message: 'Error inesperado.' }, { status: 500 }),
      ),
    );
    await mostrar();

    expect(
      await screen.findByText('No pudimos cargar las secciones del año.'),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Guardar configuración' }),
    ).toBeDisabled();
  });
});

describe('fechas, horas y duración', () => {
  test('al elegirlas, el indicador y la barra muestran el rango y los días reales', async () => {
    servir();
    await mostrar();
    await cargado();

    await llenarFechas();

    expect(screen.getByText('15 dic – 20 dic')).toBeInTheDocument();
    expect(screen.getByText('15-dic')).toBeInTheDocument();
    expect(screen.getByText('20-dic')).toBeInTheDocument();
    expect(screen.getByText('5 días')).toBeInTheDocument();
    expect(screen.queryByText('No definido')).not.toBeInTheDocument();
  });

  test('el selector de hora ofrece cada 30 minutos y las 11:59 p. m.', async () => {
    servir();
    await mostrar();
    await cargado();

    await userEvent.click(selector(/Hora de cierre/));

    const opciones = within(await screen.findByRole('listbox')).getAllByRole(
      'option',
    );
    expect(opciones).toHaveLength(49);
    expect(opciones[0]).toHaveTextContent('12:00 a.m.');
    expect(opciones.at(-1)).toHaveTextContent('11:59 p.m.');
  });
});

describe('vacantes', () => {
  test('al cambiarlas, el total se actualiza', async () => {
    servir();
    await mostrar();
    await cargado();

    const campo = screen.getByLabelText('Vacantes de 1.° A');
    await userEvent.clear(campo);
    await userEvent.type(campo, '40');

    expect(screen.getByText('Total: 70 vacantes')).toBeInTheDocument();
  });

  test('una vacante inválida se rechaza bajo su campo y no se envía nada', async () => {
    let enviado = false;
    servir();
    server.use(
      http.post(API, () => {
        enviado = true;
        return HttpResponse.json(formularioNuevo());
      }),
    );
    await mostrar();
    await cargado();
    await llenarFechas();

    const primera = screen.getByLabelText('Vacantes de 1.° A');
    await userEvent.clear(primera);
    await userEvent.type(primera, '46');
    const segunda = screen.getByLabelText('Vacantes de 1.° B');
    await userEvent.clear(segunda);
    await userEvent.type(segunda, '1');
    await guardar();

    expect(
      await screen.findByText(
        'Las vacantes de la sección 1.° A deben estar entre 1 y 45.',
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        'Las vacantes de la sección 1.° B no pueden ser menos que los alumnos que ocupan vacante (2).',
      ),
    ).toBeInTheDocument();
    expect(enviado).toBe(false);
  });

  test('una vacante vacía pide el dato', async () => {
    servir();
    await mostrar();
    await cargado();
    await llenarFechas();

    await userEvent.clear(screen.getByLabelText('Vacantes de 1.° A'));
    await guardar();

    expect(
      await screen.findByText('Ingresa las vacantes de la sección 1.° A.'),
    ).toBeInTheDocument();
  });
});

describe('validación al guardar', () => {
  test('sin fechas ni horas avisa bajo cada campo y no envía nada', async () => {
    let enviado = false;
    servir();
    server.use(
      http.post(API, () => {
        enviado = true;
        return HttpResponse.json(formularioNuevo());
      }),
    );
    await mostrar();
    await cargado();

    await guardar();

    expect(
      await screen.findByText('Ingresa la fecha y la hora de inicio.'),
    ).toBeInTheDocument();
    expect(
      screen.getByText('Ingresa la fecha y la hora de cierre.'),
    ).toBeInTheDocument();
    expect(enviado).toBe(false);
  });

  test('con la fecha pero sin la hora, el aviso va en la hora', async () => {
    servir();
    await mostrar();
    await cargado();

    await elegirFecha(/Fecha de inicio/, '15/12/2026');
    await guardar();

    expect(
      await screen.findByText('Ingresa la fecha y la hora de inicio.'),
    ).toBeInTheDocument();
  });

  test('el cierre debe ser posterior al inicio', async () => {
    servir();
    await mostrar();
    await cargado();

    await elegirFecha(/Fecha de inicio/, '15/12/2026');
    await elegirHora(/Hora de inicio/, '10:00 a.m.');
    await elegirFecha(/Fecha de cierre/, '15/12/2026');
    await elegirHora(/Hora de cierre/, '9:00 a.m.');
    await guardar();

    expect(
      await screen.findByText(
        'La fecha de cierre debe ser posterior a la de inicio.',
      ),
    ).toBeInTheDocument();
  });
});

describe('documentos requeridos', () => {
  test('se agrega con su descripción (con contador) y se cuenta en el indicador', async () => {
    servir();
    await mostrar();
    await cargado();
    expect(screen.getByText('0/100')).toBeInTheDocument();

    await userEvent.type(
      screen.getByLabelText('Descripción del documento (opcional)'),
      'Con firma',
    );
    expect(screen.getByText('9/100')).toBeInTheDocument();
    await userEvent.type(
      screen.getByLabelText('Nombre del documento'),
      'Ficha de matrícula',
    );
    await userEvent.click(
      screen.getByRole('button', { name: 'Agregar documento' }),
    );

    const lista = screen.getByRole('list', { name: 'Documentos agregados' });
    expect(within(lista).getByText('Ficha de matrícula')).toBeInTheDocument();
    expect(within(lista).getByText('Con firma')).toBeInTheDocument();
    // El formulario de alta queda limpio para el siguiente.
    expect(screen.getByLabelText('Nombre del documento')).toHaveValue('');
    expect(screen.getByText('0/100')).toBeInTheDocument();
    expect(
      screen.getByText('Documentos requeridos', { selector: 'p' }),
    ).toBeInTheDocument();
  });

  test('el nombre es obligatorio y no se repite', async () => {
    servir();
    await mostrar();
    await cargado();

    await userEvent.click(
      screen.getByRole('button', { name: 'Agregar documento' }),
    );
    expect(
      screen.getByText('Ingresa el nombre del documento.'),
    ).toBeInTheDocument();

    await agregarDocumento('DNI del estudiante');
    await agregarDocumento('dni del estudiante');
    expect(
      screen.getByText(
        'Ya hay un documento llamado "dni del estudiante" en este periodo.',
      ),
    ).toBeInTheDocument();
    expect(screen.getAllByText(/DNI del estudiante/i)).toHaveLength(2); // el de la lista y el aviso
  });

  test('se quita con la papelera', async () => {
    servir();
    await mostrar();
    await cargado();
    await agregarDocumento('DNI del estudiante');

    await userEvent.click(
      screen.getByRole('button', { name: 'Quitar DNI del estudiante' }),
    );

    expect(
      screen.queryByRole('list', { name: 'Documentos agregados' }),
    ).not.toBeInTheDocument();
  });

  test('una plantilla de otro formato o de más de 10 MB se rechaza en la zona de carga', async () => {
    servir();
    await mostrar();
    await cargado();
    // El selector filtra por formato; aquí se prueba la regla propia, así que no se aplica el filtro.
    const usuario = userEvent.setup({ applyAccept: false });
    const entrada = screen.getByLabelText('Seleccionar archivo');

    await usuario.upload(entrada, new File(['x'], 'ficha.pdf'));
    expect(
      await screen.findByText(
        'La plantilla debe ser un archivo XLSX, XLS o CSV.',
      ),
    ).toBeInTheDocument();

    const grande = new File(['x'], 'grande.csv');
    Object.defineProperty(grande, 'size', { value: 11 * 1024 * 1024 });
    await usuario.upload(entrada, grande);
    expect(
      await screen.findByText('La plantilla no puede pesar más de 10 MB.'),
    ).toBeInTheDocument();
  });

  test('una plantilla válida queda a la vista hasta guardar y se muestra en el documento', async () => {
    servir();
    await mostrar();
    await cargado();

    await userEvent.upload(
      screen.getByLabelText('Seleccionar archivo'),
      csv('ficha.csv'),
    );
    expect(screen.getByText('ficha.csv')).toBeInTheDocument();
    await userEvent.type(
      screen.getByLabelText('Nombre del documento'),
      'Ficha de matrícula',
    );
    await userEvent.click(
      screen.getByRole('button', { name: 'Agregar documento' }),
    );

    expect(screen.getByText('Plantilla: ficha.csv')).toBeInTheDocument();
  });
});

describe('guardar', () => {
  test('envía el periodo con sus fechas, vacantes y documentos, avisa y vuelve al listado', async () => {
    let cuerpo: unknown = null;
    servir();
    server.use(
      http.post(API, async ({ request }) => {
        cuerpo = await request.json();
        return HttpResponse.json(
          formularioNuevo({ id: 'periodo-2027', estado: 'PROGRAMADO' }),
          { status: 201 },
        );
      }),
    );
    await mostrar();
    await cargado();
    await llenarFechas();
    await agregarDocumento('Ficha de matrícula', 'Con firma');

    await guardar();

    await waitFor(() =>
      expect(navegar).toHaveBeenCalledWith('/app/administrativo/matricula'),
    );
    expect(cuerpo).toEqual(
      cuerpoEsperado({
        documentos: [
          { id: null, nombre: 'Ficha de matrícula', descripcion: 'Con firma' },
        ],
      }),
    );
    expect(
      await screen.findByText('Periodo de matrícula guardado'),
    ).toBeInTheDocument();
  });

  test('con las vacantes cambiadas envía las capacidades nuevas', async () => {
    let cuerpo: { vacantes: unknown } | null = null;
    servir();
    server.use(
      http.post(API, async ({ request }) => {
        cuerpo = (await request.json()) as { vacantes: unknown };
        return HttpResponse.json(formularioNuevo({ id: 'periodo-2027' }), {
          status: 201,
        });
      }),
    );
    await mostrar();
    await cargado();
    await llenarFechas();
    const primera = screen.getByLabelText('Vacantes de 1.° A');
    await userEvent.clear(primera);
    await userEvent.type(primera, '25');

    await guardar();

    await waitFor(() => expect(navegar).toHaveBeenCalled());
    expect(cuerpo!.vacantes).toEqual([
      { seccionId: 's1', capacidad: 25 },
      { seccionId: 's2', capacidad: 30 },
    ]);
  });

  test('las plantillas elegidas se suben después de guardar, con los ids que devuelve el backend', async () => {
    const orden: string[] = [];
    servir();
    server.use(
      http.post(API, () => {
        orden.push('periodo');
        return HttpResponse.json(
          formularioNuevo({
            id: 'periodo-2027',
            estado: 'PROGRAMADO',
            documentos: [
              {
                id: 'doc-ficha',
                nombre: 'Ficha de matrícula',
                descripcion: null,
                orden: 1,
                tienePlantilla: false,
                plantillaNombreArchivo: null,
                tieneEntregas: false,
              },
              {
                id: 'doc-dni',
                nombre: 'DNI del estudiante',
                descripcion: null,
                orden: 2,
                tienePlantilla: false,
                plantillaNombreArchivo: null,
                tieneEntregas: false,
              },
            ],
          }),
          { status: 201 },
        );
      }),
      http.post(
        `${API}/periodo-2027/documentos/:documentoId/plantilla`,
        ({ params }) => {
          orden.push(`plantilla ${String(params.documentoId)}`);
          return HttpResponse.json({
            id: params.documentoId,
            tienePlantilla: true,
          });
        },
      ),
    );
    await mostrar();
    await cargado();
    await llenarFechas();
    await agregarDocumento('Ficha de matrícula', '', csv('ficha.csv'));
    await agregarDocumento('DNI del estudiante'); // sin plantilla: no se sube nada

    await guardar();

    await waitFor(() =>
      expect(navegar).toHaveBeenCalledWith('/app/administrativo/matricula'),
    );
    expect(orden).toEqual(['periodo', 'plantilla doc-ficha']);
  });

  test('si una plantilla falla, avisa y pasa a editar el periodo ya guardado para reintentar', async () => {
    servir();
    server.use(
      http.post(API, () =>
        HttpResponse.json(
          formularioNuevo({
            id: 'periodo-2027',
            estado: 'PROGRAMADO',
            documentos: [
              {
                id: 'doc-ficha',
                nombre: 'Ficha de matrícula',
                descripcion: null,
                orden: 1,
                tienePlantilla: false,
                plantillaNombreArchivo: null,
                tieneEntregas: false,
              },
            ],
          }),
          { status: 201 },
        ),
      ),
      http.post(`${API}/periodo-2027/documentos/doc-ficha/plantilla`, () =>
        HttpResponse.json(
          {
            message:
              'El contenido del archivo no corresponde a un XLSX válido.',
          },
          { status: 400 },
        ),
      ),
    );
    await mostrar();
    await cargado();
    await llenarFechas();
    await agregarDocumento('Ficha de matrícula', '', csv('ficha.csv'));

    await guardar();

    await waitFor(() =>
      expect(navegar).toHaveBeenCalledWith(
        '/app/administrativo/matricula/periodos/periodo-2027/editar',
      ),
    );
    expect(
      await screen.findByText('Periodo guardado, pero faltan plantillas'),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        'No se pudo subir la plantilla de: Ficha de matrícula. Vuelve a subirla desde "Editar".',
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        'El contenido del archivo no corresponde a un XLSX válido.',
      ),
    ).toBeInTheDocument();
  });

  test.each([
    [
      'otro periodo sin cerrar',
      'Ya existe un periodo de matrícula 2026 sin cerrar.',
      409,
    ],
    [
      'sin tarifario confirmado',
      'Confirma el tarifario 2027 antes de habilitar la matrícula.',
      409,
    ],
    [
      'sin secciones',
      'Crea las secciones de 2027 antes de habilitar la matrícula.',
      400,
    ],
    [
      'fecha de inicio pasada',
      'La fecha de inicio no puede ser anterior a la fecha y hora actuales.',
      400,
    ],
  ])(
    'el rechazo del backend (%s) se muestra tal cual y el formulario sigue abierto',
    async (_caso, mensaje, estado) => {
      servir();
      server.use(
        http.post(API, () =>
          HttpResponse.json({ message: mensaje }, { status: estado }),
        ),
      );
      await mostrar();
      await cargado();
      await llenarFechas();

      await guardar();

      expect(await screen.findByText(mensaje)).toBeInTheDocument();
      expect(navegar).not.toHaveBeenCalled();
      await waitFor(() =>
        expect(
          screen.getByRole('button', { name: 'Guardar configuración' }),
        ).toBeEnabled(),
      );
    },
  );

  test('"Cancelar" vuelve al listado sin guardar', async () => {
    servir();
    await mostrar();
    await cargado();

    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(navegar).toHaveBeenCalledWith('/app/administrativo/matricula');
  });
});

describe('año sin tarifario confirmado', () => {
  const sinTarifario = () =>
    servir({
      nuevo: formularioNuevo({
        tarifarioConfirmado: false,
        montoMatricula: null,
      }),
    });

  test('avisa en la tarjeta del monto, no muestra monto y no deja guardar', async () => {
    sinTarifario();
    await mostrar();
    await cargado();

    expect(
      screen.getByText(
        'Confirma el tarifario 2027 antes de habilitar la matrícula.',
      ),
    ).toBeInTheDocument();
    expect(screen.queryByText('S/ 450.00')).not.toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Guardar configuración' }),
    ).toBeDisabled();
  });

  test('el enlace al tarifario está deshabilitado con "Disponible pronto" hasta que exista esa pantalla', async () => {
    sinTarifario();
    await mostrar();
    await cargado();

    const enlace = screen.getByRole('button', { name: 'Ir al tarifario' });

    expect(enlace).toHaveAttribute('aria-disabled', 'true');
    expect(enlace).toHaveAccessibleDescription('Disponible pronto');
    expect(
      screen.queryByRole('link', { name: 'Ir al tarifario' }),
    ).not.toBeInTheDocument();
  });

  test('con la pantalla disponible, el aviso enlaza al tarifario', async () => {
    enlaces.TARIFARIO_DISPONIBLE = true;
    sinTarifario();
    await mostrar();
    await cargado();

    expect(
      screen.getByRole('link', { name: 'Ir al tarifario' }),
    ).toHaveAttribute('href', '/app/administrativo/pensiones/configuracion');
  });
});

describe('"Ver tarifario completo"', () => {
  test('mientras P19 no exista queda deshabilitado, con "Disponible pronto"', async () => {
    servir();
    await mostrar();
    await cargado();

    const boton = screen.getByRole('button', {
      name: 'Ver tarifario completo',
    });

    expect(boton).toHaveAttribute('aria-disabled', 'true');
    expect(boton).toHaveAccessibleDescription('Disponible pronto');
  });

  test('con P19 disponible es un enlace', async () => {
    enlaces.TARIFARIO_DISPONIBLE = true;
    servir();
    await mostrar();
    await cargado();

    expect(
      screen.getByRole('link', { name: 'Ver tarifario completo' }),
    ).toHaveAttribute('href', '/app/administrativo/pensiones/configuracion');
  });
});
