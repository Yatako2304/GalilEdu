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
} from '@/testing/test-utils';
import {
  FilaPeriodoMatricula,
  FormularioPeriodoMatricula,
  Pagina,
  SolicitudPeriodoMatricula,
} from '@/types/api';

import {
  useCrearPeriodoMatricula,
  useEditarPeriodoMatricula,
  useEliminarPeriodoMatricula,
  useQuitarPlantilla,
  useSubirPlantilla,
} from '../mutaciones-periodo-matricula';
import {
  usePeriodoMatriculaVigente,
  usePeriodosMatriculaTodos,
} from '../periodos-matricula';

// Figma > Personal Administrativo > pantalla 13 "Periodos de matrícula" (HU-23)

const API = `${env.API_URL}/configuracion/periodos-matricula`;

const solicitud: SolicitudPeriodoMatricula = {
  anioEscolarId: 'anio-2028',
  fechaInicio: '2026-12-01',
  horaInicio: '10:00',
  fechaCierre: '2027-02-15',
  horaCierre: '23:59',
  vacantes: [{ seccionId: 'seccion-1', capacidad: 25 }],
  documentos: [
    { nombre: 'Ficha de matrícula', descripcion: 'Con firma' },
    { id: 'doc-1', nombre: 'DNI del estudiante' },
  ],
};

const formulario = (): FormularioPeriodoMatricula => ({
  id: 'periodo-2028',
  anioEscolarId: 'anio-2028',
  anio: 2028,
  titulo: 'Matrícula 2028',
  estado: 'PROGRAMADO',
  editable: true,
  eliminable: true,
  fechaInicio: '2026-12-01',
  horaInicio: '10:00:00',
  fechaCierre: '2027-02-15',
  horaCierre: '23:59:00',
  diasDelPeriodo: 76,
  montoMatricula: 450,
  tarifarioConfirmado: true,
  vacantesTotales: 145,
  matriculasRegistradas: 0,
  cantidadSecciones: 5,
  resumenSecciones: '1.° a 5.°, A',
  bloquearPorMorosidad: true,
  permitirExtemporanea: true,
  secciones: [],
  documentos: [],
});

const fila = (vigente: boolean): FilaPeriodoMatricula => ({
  id: 'periodo-2027',
  nombre: 'Matrícula 2027',
  anioEscolarId: 'anio-2027',
  anio: 2027,
  inicio: '2026-09-15T13:00:00Z',
  cierre: '2026-11-01T04:59:00Z',
  matriculas: 1,
  estado: 'PROGRAMADO',
  vigente,
});

const administrativo = async () =>
  createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] });

const Acciones = () => {
  const crear = useCrearPeriodoMatricula();
  const editar = useEditarPeriodoMatricula();
  const eliminar = useEliminarPeriodoMatricula();
  const subir = useSubirPlantilla();
  const quitar = useQuitarPlantilla();
  return (
    <div>
      <button onClick={() => crear.mutate(solicitud)}>crear</button>
      <button
        onClick={() => editar.mutate({ id: 'periodo-2028', data: solicitud })}
      >
        editar
      </button>
      <button onClick={() => eliminar.mutate('periodo-2028')}>eliminar</button>
      <button
        onClick={() =>
          subir.mutate({
            periodoId: 'periodo-2028',
            documentoId: 'doc-1',
            archivo: new File(['a,b\n'], 'ficha.csv', { type: 'text/csv' }),
          })
        }
      >
        subir
      </button>
      <button
        onClick={() =>
          quitar.mutate({ periodoId: 'periodo-2028', documentoId: 'doc-1' })
        }
      >
        quitar
      </button>
    </div>
  );
};

test('cada mutación llama a su endpoint con su cuerpo', async () => {
  const llamadas: string[] = [];
  let cuerpoCrear: unknown = null;
  let cuerpoEditar: unknown = null;
  server.use(
    http.post(API, async ({ request }) => {
      llamadas.push('crear');
      cuerpoCrear = await request.json();
      return HttpResponse.json(formulario(), { status: 201 });
    }),
    http.put(`${API}/periodo-2028`, async ({ request }) => {
      llamadas.push('editar');
      cuerpoEditar = await request.json();
      return HttpResponse.json(formulario());
    }),
    http.delete(`${API}/periodo-2028`, () => {
      llamadas.push('eliminar');
      return new HttpResponse(null, { status: 204 });
    }),
    http.post(`${API}/periodo-2028/documentos/doc-1/plantilla`, () => {
      llamadas.push('subir');
      return HttpResponse.json({ id: 'doc-1', tienePlantilla: true });
    }),
    http.delete(`${API}/periodo-2028/documentos/doc-1/plantilla`, () => {
      llamadas.push('quitar');
      return HttpResponse.json({ id: 'doc-1', tienePlantilla: false });
    }),
  );
  await renderApp(<Acciones />, { user: await administrativo() });

  for (const nombre of ['crear', 'editar', 'eliminar', 'subir', 'quitar']) {
    await userEvent.click(screen.getByRole('button', { name: nombre }));
    await waitFor(() => expect(llamadas).toContain(nombre));
  }

  expect(cuerpoCrear).toEqual(solicitud);
  expect(cuerpoEditar).toEqual(solicitud);
  // El cuerpo multipart de la plantilla se comprueba en plantillas.test.ts (MSW no lo puede leer bajo jsdom).
});

test('al crear avisa con el periodo guardado', async () => {
  server.use(
    http.post(API, () => HttpResponse.json(formulario(), { status: 201 })),
  );
  const alCrear = vi.fn();
  const Crear = () => {
    const crear = useCrearPeriodoMatricula(alCrear);
    return <button onClick={() => crear.mutate(solicitud)}>crear</button>;
  };
  await renderApp(<Crear />, { user: await administrativo() });

  await userEvent.click(screen.getByRole('button', { name: 'crear' }));

  await waitFor(() => expect(alCrear).toHaveBeenCalled());
  expect(alCrear.mock.calls[0][0].id).toBe('periodo-2028');
});

test('si el backend rechaza, no avisa de éxito y el mensaje sale como notificación', async () => {
  server.use(
    http.post(API, () =>
      HttpResponse.json(
        { message: 'Ya existe un periodo de matrícula 2027 sin cerrar.' },
        { status: 409 },
      ),
    ),
  );
  const alCrear = vi.fn();
  const Crear = () => {
    const crear = useCrearPeriodoMatricula(alCrear);
    return <button onClick={() => crear.mutate(solicitud)}>crear</button>;
  };
  await renderApp(<Crear />, { user: await administrativo() });

  await userEvent.click(screen.getByRole('button', { name: 'crear' }));

  expect(
    await screen.findByText(
      'Ya existe un periodo de matrícula 2027 sin cerrar.',
    ),
  ).toBeInTheDocument();
  expect(alCrear).not.toHaveBeenCalled();
});

describe('al eliminar el periodo vigente', () => {
  const Panel = () => {
    const todos = usePeriodosMatriculaTodos();
    const hay = !!todos.data?.data.some((f) => f.vigente);
    const vigente = usePeriodoMatriculaVigente(hay);
    const eliminar = useEliminarPeriodoMatricula();
    return (
      <div>
        <p>
          Filas: {todos.data?.data.length ?? '…'} · Vigente:{' '}
          {vigente.data ? vigente.data.nombre : 'ninguno'}
        </p>
        <button onClick={() => eliminar.mutate('periodo-2027')}>
          eliminar
        </button>
      </div>
    );
  };

  test('recarga la lista y no vuelve a pedir /vigente (evita el 404)', async () => {
    let eliminado = false;
    let consultasVigente = 0;
    server.use(
      http.get(API, () => {
        const filas = eliminado ? [] : [fila(true)];
        const respuesta: Pagina<FilaPeriodoMatricula> = {
          data: filas,
          meta: { page: 1, size: 100, total: filas.length, totalPages: 1 },
        };
        return HttpResponse.json(respuesta);
      }),
      http.get(`${API}/vigente`, () => {
        consultasVigente++;
        return eliminado
          ? HttpResponse.json(
              { message: 'No hay un periodo de matrícula vigente.' },
              { status: 404 },
            )
          : HttpResponse.json({ ...fila(true), vacantesTotales: 300 });
      }),
      http.delete(`${API}/periodo-2027`, () => {
        eliminado = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    await renderApp(<Panel />, { user: await administrativo() });
    await screen.findByText('Filas: 1 · Vigente: Matrícula 2027');
    expect(consultasVigente).toBe(1);

    await userEvent.click(screen.getByRole('button', { name: 'eliminar' }));

    expect(
      await screen.findByText('Filas: 0 · Vigente: ninguno'),
    ).toBeInTheDocument();
    expect(consultasVigente).toBe(1);
    expect(
      screen.queryByText('No hay un periodo de matrícula vigente.'),
    ).not.toBeInTheDocument();
  });
});
