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

import { PeriodoExistente } from '../periodo-existente';

import { API, detalle2027 } from './datos';

import '../../../__tests__/polyfill-blob';

// Figma > Personal Administrativo > pantalla 13 > "Ver" un periodo y edición de un periodo PROGRAMADO (HU-23)

// Navegar de verdad en jsdom rompe el router de la prueba: se sustituyen `useNavigate` y `Navigate` y se comprueba el
// destino. Tampoco hay `URL.createObjectURL`, así que la descarga de la plantilla se espía.
const { navegar, descargar } = vi.hoisted(() => ({
  navegar: vi.fn(),
  descargar: vi.fn(() => Promise.resolve()),
}));
vi.mock('react-router', async () => ({
  ...(await vi.importActual<typeof import('react-router')>('react-router')),
  useNavigate: () => navegar,
  Navigate: ({ to }: { to: string }) => {
    navegar(to);
    return null;
  },
}));
vi.mock('../../../api/plantillas', async () => ({
  ...(await vi.importActual<typeof import('../../../api/plantillas')>(
    '../../../api/plantillas',
  )),
  descargarPlantilla: descargar,
}));

beforeEach(() => {
  navegar.mockClear();
  descargar.mockClear();
});

const seccion = (
  seccionId: string,
  etiqueta: string,
  tutor: string | null,
  capacidad: number,
  alumnosOcupanVacante = 0,
): SeccionVacante => ({
  seccionId,
  etiqueta,
  gradoCodigo: 'SEC-1',
  gradoNombre: '1.° de secundaria',
  tutor,
  capacidad,
  alumnosOcupanVacante,
});

const documentos = [
  {
    id: 'doc-ficha',
    nombre: 'Ficha de matrícula',
    descripcion: 'Con firma',
    orden: 1,
    tienePlantilla: true,
    plantillaNombreArchivo: 'ficha.xlsx',
    tieneEntregas: false,
  },
  {
    id: 'doc-dni',
    nombre: 'DNI del estudiante',
    descripcion: null,
    orden: 2,
    tienePlantilla: false,
    plantillaNombreArchivo: null,
    tieneEntregas: true,
  },
];

/** Un periodo PROGRAMADO, el único que se edita. */
const programado = (
  extras: Partial<FormularioPeriodoMatricula> = {},
): FormularioPeriodoMatricula =>
  detalle2027({
    estado: 'PROGRAMADO',
    editable: true,
    eliminable: true,
    fechaInicio: '2026-12-15',
    horaInicio: '10:00:00',
    fechaCierre: '2027-02-15',
    horaCierre: '23:59:00',
    matriculasRegistradas: 0,
    vacantesTotales: 55,
    secciones: [
      seccion('s1', '1.° A', 'Jorge Ramírez', 30),
      seccion('s2', '1.° B', null, 25, 2),
    ],
    documentos,
    ...extras,
  });

const servir = (periodo: FormularioPeriodoMatricula) =>
  server.use(
    http.get(`${API}/${periodo.id}`, () => HttpResponse.json(periodo)),
  );

const mostrar = async (modo: 'ver' | 'editar') =>
  renderApp(<PeriodoExistente periodoId="periodo-2027" modo={modo} />, {
    user: await createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] }),
  });

const guardar = () =>
  userEvent.click(
    screen.getByRole('button', { name: 'Guardar configuración' }),
  );

describe('ver (solo lectura)', () => {
  test('un periodo programado muestra todo sin poder cambiarlo, con "Editar" y "Volver"', async () => {
    servir(programado());
    await mostrar('ver');

    expect(await screen.findByText('Tutor: Jorge Ramírez')).toBeInTheDocument();
    expect(screen.getByLabelText('Vacantes de 1.° A')).toHaveValue(30);
    expect(screen.getByLabelText('Vacantes de 1.° A')).toBeDisabled();
    expect(screen.getByLabelText('Vacantes de 1.° B')).toBeDisabled();
    expect(screen.getByText('Total: 55 vacantes')).toBeInTheDocument();
    expect(screen.getByText('15 dic – 15 feb')).toBeInTheDocument();
    expect(screen.getByText('S/ 450.00')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /Fecha de inicio/ }),
    ).toBeDisabled();
    // Los documentos se ven; no se agregan ni se quitan.
    expect(screen.getByText('Ficha de matrícula')).toBeInTheDocument();
    expect(screen.getByText('Plantilla: ficha.xlsx')).toBeInTheDocument();
    expect(
      screen.queryByLabelText('Nombre del documento'),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Agregar documento' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /^Quitar / }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Guardar configuración' }),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: 'Cancelar' }),
    ).not.toBeInTheDocument();

    expect(screen.getByRole('link', { name: 'Editar' })).toHaveAttribute(
      'href',
      '/app/administrativo/matricula/periodos/periodo-2027/editar',
    );
    expect(
      screen.getByRole('link', { name: 'Volver a los periodos de matrícula' }),
    ).toHaveAttribute('href', '/app/administrativo/matricula');
    expect(
      screen.queryByText(
        'Una vez habilitado, el periodo no se puede editar; solo deshabilitar.',
      ),
    ).not.toBeInTheDocument();
  });

  test.each(['ABIERTO', 'CERRADO'] as const)(
    'un periodo %s avisa que no se edita y solo ofrece volver',
    async (estado) => {
      servir(programado({ estado, editable: false, eliminable: false }));
      await mostrar('ver');

      expect(
        await screen.findByText(
          'Una vez habilitado, el periodo no se puede editar; solo deshabilitar.',
        ),
      ).toBeInTheDocument();
      expect(
        screen.getByRole('link', {
          name: 'Volver a los periodos de matrícula',
        }),
      ).toBeInTheDocument();
      expect(
        screen.queryByRole('link', { name: 'Editar' }),
      ).not.toBeInTheDocument();
    },
  );

  test('la plantilla guardada se descarga con su nombre de archivo', async () => {
    servir(programado());
    await mostrar('ver');

    await userEvent.click(
      await screen.findByRole('button', {
        name: 'Descargar plantilla de Ficha de matrícula',
      }),
    );

    expect(descargar).toHaveBeenCalledWith(
      'periodo-2027',
      'doc-ficha',
      'ficha.xlsx',
    );
    // El documento sin plantilla no ofrece descarga.
    expect(
      screen.queryByRole('button', {
        name: 'Descargar plantilla de DNI del estudiante',
      }),
    ).not.toBeInTheDocument();
  });

  test('si el periodo no carga, lo avisa', async () => {
    server.use(
      http.get(`${API}/periodo-2027`, () =>
        HttpResponse.json({ message: 'No existe.' }, { status: 404 }),
      ),
    );
    await mostrar('ver');

    expect(
      await screen.findByText('No pudimos cargar el periodo de matrícula.'),
    ).toBeInTheDocument();
  });
});

describe('editar', () => {
  test('un periodo abierto o cerrado no se edita: redirige a "Ver"', async () => {
    servir(programado({ estado: 'ABIERTO', editable: false }));
    await mostrar('editar');

    await waitFor(() =>
      expect(navegar).toHaveBeenCalledWith(
        '/app/administrativo/matricula/periodos/periodo-2027',
      ),
    );
    expect(
      screen.queryByRole('button', { name: 'Guardar configuración' }),
    ).not.toBeInTheDocument();
  });

  test('abre con lo guardado: año fijo, fechas, horas, vacantes y documentos', async () => {
    servir(programado());
    await mostrar('editar');

    expect(await screen.findByText('Tutor: Jorge Ramírez')).toBeInTheDocument();
    expect(screen.getByLabelText('Año académico')).toBeDisabled();
    expect(screen.getByLabelText('Vacantes de 1.° A')).toHaveValue(30);
    expect(screen.getByLabelText('Vacantes de 1.° B')).toHaveValue(25);
    expect(screen.getByText('Total: 55 vacantes')).toBeInTheDocument();
    expect(screen.getByLabelText('Hora de inicio')).toHaveDisplayValue(
      '10:00 a.m.',
    );
    expect(screen.getByLabelText('Hora de cierre')).toHaveDisplayValue(
      '11:59 p.m.',
    );
    const lista = screen.getByRole('list', { name: 'Documentos agregados' });
    expect(within(lista).getByText('Ficha de matrícula')).toBeInTheDocument();
    expect(within(lista).getByText('DNI del estudiante')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Guardar configuración' }),
    ).toBeEnabled();
  });

  test('un documento con entregas no se puede quitar; los demás sí', async () => {
    servir(programado());
    await mostrar('editar');
    await screen.findByText('Tutor: Jorge Ramírez');

    expect(
      screen.queryByRole('button', { name: 'Quitar DNI del estudiante' }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByText('Ya tiene entregas: no se puede quitar.'),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: 'Quitar Ficha de matrícula' }),
    ).toBeInTheDocument();
  });

  test('guarda los cambios sin cambiar el año, con los ids de los documentos, avisa y vuelve al listado', async () => {
    let cuerpo: unknown = null;
    servir(programado());
    server.use(
      http.put(`${API}/periodo-2027`, async ({ request }) => {
        cuerpo = await request.json();
        return HttpResponse.json(programado());
      }),
    );
    await mostrar('editar');
    await screen.findByText('Tutor: Jorge Ramírez');
    const primera = screen.getByLabelText('Vacantes de 1.° A');
    await userEvent.clear(primera);
    await userEvent.type(primera, '40');
    expect(screen.getByText('Total: 65 vacantes')).toBeInTheDocument();

    await guardar();

    await waitFor(() =>
      expect(navegar).toHaveBeenCalledWith('/app/administrativo/matricula'),
    );
    expect(cuerpo).toEqual({
      anioEscolarId: null,
      fechaInicio: '2026-12-15',
      horaInicio: '10:00',
      fechaCierre: '2027-02-15',
      horaCierre: '23:59',
      vacantes: [
        { seccionId: 's1', capacidad: 40 },
        { seccionId: 's2', capacidad: 25 },
      ],
      documentos: [
        {
          id: 'doc-ficha',
          nombre: 'Ficha de matrícula',
          descripcion: 'Con firma',
        },
        { id: 'doc-dni', nombre: 'DNI del estudiante', descripcion: null },
      ],
    });
    expect(
      await screen.findByText('Periodo de matrícula actualizado'),
    ).toBeInTheDocument();
  });

  test('las reglas de las vacantes se validan antes de enviar', async () => {
    let enviado = false;
    servir(programado());
    server.use(
      http.put(`${API}/periodo-2027`, () => {
        enviado = true;
        return HttpResponse.json(programado());
      }),
    );
    await mostrar('editar');
    await screen.findByText('Tutor: Jorge Ramírez');
    const segunda = screen.getByLabelText('Vacantes de 1.° B');
    await userEvent.clear(segunda);
    await userEvent.type(segunda, '1');

    await guardar();

    expect(
      await screen.findByText(
        'Las vacantes de la sección 1.° B no pueden ser menos que los alumnos que ocupan vacante (2).',
      ),
    ).toBeInTheDocument();
    expect(enviado).toBe(false);
  });

  test('un documento quitado deja de enviarse', async () => {
    let cuerpo: { documentos: unknown } | null = null;
    servir(programado());
    server.use(
      http.put(`${API}/periodo-2027`, async ({ request }) => {
        cuerpo = (await request.json()) as { documentos: unknown };
        return HttpResponse.json(programado());
      }),
    );
    await mostrar('editar');
    await screen.findByText('Tutor: Jorge Ramírez');

    await userEvent.click(
      screen.getByRole('button', { name: 'Quitar Ficha de matrícula' }),
    );
    await guardar();

    await waitFor(() => expect(navegar).toHaveBeenCalled());
    expect(cuerpo!.documentos).toEqual([
      { id: 'doc-dni', nombre: 'DNI del estudiante', descripcion: null },
    ]);
  });

  test('un documento nuevo viaja sin id y su plantilla se sube con el id que devuelve el backend', async () => {
    const orden: string[] = [];
    let cuerpo: { documentos: unknown } | null = null;
    servir(programado());
    server.use(
      http.put(`${API}/periodo-2027`, async ({ request }) => {
        cuerpo = (await request.json()) as { documentos: unknown };
        orden.push('periodo');
        return HttpResponse.json(
          programado({
            documentos: [
              ...documentos,
              {
                id: 'doc-foto',
                nombre: 'Foto',
                descripcion: null,
                orden: 3,
                tienePlantilla: false,
                plantillaNombreArchivo: null,
                tieneEntregas: false,
              },
            ],
          }),
        );
      }),
      http.post(
        `${API}/periodo-2027/documentos/:documentoId/plantilla`,
        ({ params }) => {
          orden.push(`plantilla ${String(params.documentoId)}`);
          return HttpResponse.json({ id: params.documentoId });
        },
      ),
    );
    await mostrar('editar');
    await screen.findByText('Tutor: Jorge Ramírez');

    await userEvent.type(screen.getByLabelText('Nombre del documento'), 'Foto');
    await userEvent.upload(
      screen.getByLabelText('Seleccionar archivo'),
      new File(['a,b'], 'foto.csv', { type: 'text/csv' }),
    );
    await userEvent.click(
      screen.getByRole('button', { name: 'Agregar documento' }),
    );
    await guardar();

    await waitFor(() =>
      expect(navegar).toHaveBeenCalledWith('/app/administrativo/matricula'),
    );
    expect(orden).toEqual(['periodo', 'plantilla doc-foto']);
    expect(cuerpo!.documentos).toEqual([
      {
        id: 'doc-ficha',
        nombre: 'Ficha de matrícula',
        descripcion: 'Con firma',
      },
      { id: 'doc-dni', nombre: 'DNI del estudiante', descripcion: null },
      { id: null, nombre: 'Foto', descripcion: null },
    ]);
  });

  test('"Cambiar plantilla" sube la nueva al guardar; no antes', async () => {
    const subidas: string[] = [];
    servir(programado());
    server.use(
      http.put(`${API}/periodo-2027`, () => HttpResponse.json(programado())),
      http.post(
        `${API}/periodo-2027/documentos/:documentoId/plantilla`,
        ({ params }) => {
          subidas.push(String(params.documentoId));
          return HttpResponse.json({ id: params.documentoId });
        },
      ),
    );
    await mostrar('editar');
    await screen.findByText('Tutor: Jorge Ramírez');

    await userEvent.upload(
      screen.getByLabelText('Archivo de plantilla de Ficha de matrícula'),
      new File(['a,b'], 'nueva.csv', { type: 'text/csv' }),
    );

    expect(screen.getByText('Plantilla: nueva.csv')).toBeInTheDocument();
    expect(subidas).toEqual([]);
    await guardar();
    await waitFor(() => expect(subidas).toEqual(['doc-ficha']));
  });

  test('una plantilla de formato no permitido se rechaza en el documento', async () => {
    servir(programado());
    await mostrar('editar');
    await screen.findByText('Tutor: Jorge Ramírez');
    // El selector filtra por formato; aquí se prueba la regla propia, así que no se aplica el filtro.
    const usuario = userEvent.setup({ applyAccept: false });

    await usuario.upload(
      screen.getByLabelText('Archivo de plantilla de Ficha de matrícula'),
      new File(['x'], 'ficha.pdf'),
    );

    expect(
      await screen.findByText(
        'La plantilla debe ser un archivo XLSX, XLS o CSV.',
      ),
    ).toBeInTheDocument();
    expect(screen.getByText('Plantilla: ficha.xlsx')).toBeInTheDocument();
  });

  test('"Quitar plantilla" la quita al guardar, no antes, y se puede deshacer eligiendo otra', async () => {
    const quitadas: string[] = [];
    servir(programado());
    server.use(
      http.put(`${API}/periodo-2027`, () => HttpResponse.json(programado())),
      http.delete(
        `${API}/periodo-2027/documentos/:documentoId/plantilla`,
        ({ params }) => {
          quitadas.push(String(params.documentoId));
          return HttpResponse.json({ id: params.documentoId });
        },
      ),
    );
    await mostrar('editar');
    await screen.findByText('Tutor: Jorge Ramírez');

    await userEvent.click(
      screen.getByRole('button', {
        name: 'Quitar plantilla de Ficha de matrícula',
      }),
    );

    expect(screen.queryByText('Plantilla: ficha.xlsx')).not.toBeInTheDocument();
    expect(
      screen.getByText('La plantilla se quitará al guardar.'),
    ).toBeInTheDocument();
    expect(quitadas).toEqual([]);
    await guardar();
    await waitFor(() => expect(quitadas).toEqual(['doc-ficha']));
    await waitFor(() => expect(navegar).toHaveBeenCalled());
  });

  test('si una plantilla falla, avisa, no sale de la edición y deja volver a intentar', async () => {
    servir(programado());
    server.use(
      http.put(`${API}/periodo-2027`, () => HttpResponse.json(programado())),
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
    await mostrar('editar');
    await screen.findByText('Tutor: Jorge Ramírez');
    await userEvent.upload(
      screen.getByLabelText('Archivo de plantilla de Ficha de matrícula'),
      new File(['a,b'], 'nueva.csv', { type: 'text/csv' }),
    );

    await guardar();

    expect(
      await screen.findByText('Periodo guardado, pero faltan plantillas'),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        'No se pudo actualizar la plantilla de: Ficha de matrícula. Vuelve a intentarlo.',
      ),
    ).toBeInTheDocument();
    expect(navegar).not.toHaveBeenCalled();
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Guardar configuración' }),
      ).toBeEnabled(),
    );
    // Tras la recarga, el documento vuelve a mostrar la plantilla que sigue guardada.
    expect(
      await screen.findByText('Plantilla: ficha.xlsx'),
    ).toBeInTheDocument();
  });

  test('el rechazo del backend (409) se muestra tal cual y la edición sigue abierta', async () => {
    const mensaje =
      'No se puede quitar el documento "DNI del estudiante": ya tiene entregas.';
    servir(programado());
    server.use(
      http.put(`${API}/periodo-2027`, () =>
        HttpResponse.json({ message: mensaje }, { status: 409 }),
      ),
    );
    await mostrar('editar');
    await screen.findByText('Tutor: Jorge Ramírez');

    await guardar();

    expect(await screen.findByText(mensaje)).toBeInTheDocument();
    expect(navegar).not.toHaveBeenCalled();
  });

  test('"Cancelar" vuelve al listado sin guardar', async () => {
    servir(programado());
    await mostrar('editar');
    await screen.findByText('Tutor: Jorge Ramírez');

    await userEvent.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(navegar).toHaveBeenCalledWith('/app/administrativo/matricula');
  });
});
