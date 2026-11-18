import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import Cookies from 'js-cookie';
import * as React from 'react';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { api } from '@/lib/api-client';
import { SECCION_5B } from '@/testing/mocks/fixtures/horarios-coordinador';
import {
  AMBIENTE_LAB01,
  AMBIENTE_LAB02,
  RESPONSABLE_MARIA,
  SECCION_1A,
} from '@/testing/mocks/fixtures/reservas-ambiente';
import {
  fijarAhora,
  reiniciarReservas,
  reservasEnMemoria,
} from '@/testing/mocks/handlers/reservas-ambiente';
import { AUTH_COOKIE } from '@/testing/mocks/utils';
import {
  createUser,
  loginAsUser,
  renderHook,
  waitFor,
} from '@/testing/test-utils';
import { Pagina } from '@/types/api';

import {
  ReservaDeAmbiente,
  SolicitudEdicionReserva,
  SolicitudReserva,
} from '../../types-reservas';
import { useCrearReserva, useReservas } from '../reservas';

// HU-48: los hooks de reservas y los simuladores de MSW (que las pruebas de pantalla usan): sesión y rol, filtros,
// paginación, crear, editar y eliminar sobre el listado en memoria. «Hoy» es el miércoles 18/11/2026, 10:00.

const API = env.API_URL;

afterEach(() => reiniciarReservas());

const entrarComo = async (rol: ROLES) =>
  loginAsUser(await createUser({ roles: [rol] }));

const comoCoordinador = () => entrarComo(ROLES.COORDINADOR);

/** La respuesta de error del backend simulado (el cliente rechaza con el error de Axios). */
const fallo = async (llamada: Promise<unknown>) => {
  try {
    await llamada;
  } catch (e) {
    return (e as { response: { status: number; data: any } }).response;
  }
  throw new Error('La llamada debía fallar y no falló.');
};

const lista = (query = ''): Promise<Pagina<ReservaDeAmbiente>> =>
  api.get(`/horarios/reservas${query}`);

const fechas = async (query?: string) =>
  (await lista(query)).data.map((r) => r.fecha);

const NUEVA: SolicitudReserva = {
  ambienteId: AMBIENTE_LAB02,
  seccionId: SECCION_5B,
  responsableId: RESPONSABLE_MARIA.id,
  actividad: 'Práctica de redes',
  fecha: '2026-11-23',
  horaInicio: '08:15',
  horaFin: '09:00',
};

describe('sesión y rol', () => {
  test('sin sesión responde 401', async () => {
    Cookies.remove(AUTH_COOKIE);

    const respuesta = await fetch(`${API}/horarios/reservas`);

    expect(respuesta.status).toBe(401);
  });

  test('cada rol que no es Coordinador recibe 403 en los seis endpoints', async () => {
    for (const rol of [
      ROLES.DOCENTE,
      ROLES.TUTOR,
      ROLES.APODERADO,
      ROLES.ADMINISTRADOR,
      ROLES.PERSONAL_ADMINISTRATIVO,
    ]) {
      await entrarComo(rol);
      const llamadas = [
        api.get('/horarios/reservas'),
        api.get('/horarios/reservas/opciones'),
        api.get('/horarios/reservas/reserva-1'),
        api.post('/horarios/reservas', NUEVA),
        api.put('/horarios/reservas/reserva-1', { ...NUEVA, version: 0 }),
        api.delete('/horarios/reservas/reserva-1'),
      ];
      for (const llamada of llamadas) {
        expect((await fallo(llamada)).status).toBe(403);
      }
    }
  });
});

describe('listado', () => {
  test('sin filtro lista la semana de hoy (lunes a domingo), de la más reciente a la más antigua', async () => {
    await comoCoordinador();

    const pagina = await lista();

    expect(pagina.data.map((r) => r.fecha)).toEqual([
      '2026-11-20',
      '2026-11-19',
    ]);
    expect(pagina.meta).toEqual({ page: 1, size: 10, total: 2, totalPages: 1 });
    expect(pagina.data[0]).toMatchObject({
      id: 'reserva-2',
      ambiente: {
        id: AMBIENTE_LAB02,
        codigo: 'LAB-02',
        nombre: 'Laboratorio de cómputo',
      },
      actividad: 'Taller de programación',
      gradoCodigo: 'SEC-4',
      seccion: { etiqueta: '4.° A' },
      responsable: { nombre: 'María Fernández Ríos' },
      horaInicio: '10:05',
      horaFin: '11:35',
      estado: 'PENDIENTE',
      editable: true,
      version: 0,
    });
  });

  test('el filtro MES suma las dos finalizadas, que salen como FINALIZADO y no editables', async () => {
    await comoCoordinador();

    const pagina = await lista('?filtro=MES');

    expect(pagina.data.map((r) => [r.fecha, r.estado, r.editable])).toEqual([
      ['2026-11-20', 'PENDIENTE', true],
      ['2026-11-19', 'PENDIENTE', true],
      ['2026-11-12', 'FINALIZADO', false],
      ['2026-11-03', 'FINALIZADO', false],
    ]);
  });

  test('el filtro ANIO trae las cinco activas y nunca la cancelada', async () => {
    await comoCoordinador();

    expect(await fechas('?filtro=ANIO')).toEqual([
      '2026-12-09',
      '2026-11-20',
      '2026-11-19',
      '2026-11-12',
      '2026-11-03',
    ]);
  });

  test('la paginación divide el listado y calcula los totales', async () => {
    await comoCoordinador();

    const segunda = await lista('?filtro=ANIO&page=2&size=2');
    const tercera = await lista('?filtro=ANIO&page=3&size=2');
    const fuera = await lista('?filtro=ANIO&page=9&size=2');

    expect(segunda.meta).toEqual({ page: 2, size: 2, total: 5, totalPages: 3 });
    expect(segunda.data.map((r) => r.fecha)).toEqual([
      '2026-11-19',
      '2026-11-12',
    ]);
    expect(tercera.data.map((r) => r.fecha)).toEqual(['2026-11-03']);
    expect(fuera.data).toEqual([]);
  });

  test('un filtro desconocido responde 400', async () => {
    await comoCoordinador();

    expect((await fallo(lista('?filtro=SIEMPRE'))).status).toBe(400);
  });

  test('«hoy» se puede mover: el 25 de nov. la semana ya no trae las del 19 y 20', async () => {
    await comoCoordinador();
    fijarAhora('2026-11-25T10:00');

    expect(await fechas()).toEqual([]);
    expect(await fechas('?filtro=MES')).toEqual([
      '2026-11-20',
      '2026-11-19',
      '2026-11-12',
      '2026-11-03',
    ]);
  });

  test('una reserva cuya hora de fin ya llegó pasa a FINALIZADO', async () => {
    await comoCoordinador();
    fijarAhora('2026-11-19T09:45');

    const [primera] = (await lista('?filtro=SEMANA')).data.filter(
      (r) => r.id === 'reserva-1',
    );

    expect(primera).toMatchObject({ estado: 'FINALIZADO', editable: false });
  });
});

describe('opciones y detalle', () => {
  test('las opciones traen los espacios especiales por código y a María', async () => {
    await comoCoordinador();

    const opciones = await api.get('/horarios/reservas/opciones');

    expect(
      (opciones as any).ambientes.map((a: { codigo: string }) => a.codigo),
    ).toEqual(['BIB-01', 'LAB-01', 'LAB-02']);
    expect((opciones as any).ambientes[0].tipo).toBe('ESPACIO_ESPECIAL');
    expect((opciones as any).responsables).toEqual([RESPONSABLE_MARIA]);
  });

  test('«opciones» no se confunde con un id', async () => {
    await comoCoordinador();

    expect(await api.get('/horarios/reservas/opciones')).toHaveProperty(
      'ambientes',
    );
  });

  test('una reserva se lee por id; una inexistente o cancelada responde 404', async () => {
    await comoCoordinador();

    const reserva = (await api.get(
      '/horarios/reservas/reserva-1',
    )) as ReservaDeAmbiente;
    const inexistente = await fallo(api.get('/horarios/reservas/no-existe'));
    const cancelada = await fallo(api.get('/horarios/reservas/reserva-6'));

    expect(reserva.actividad).toBe('Práctica de laboratorio de química');
    expect(inexistente.status).toBe(404);
    expect(inexistente.data.message).toBe('La reserva no existe.');
    expect(cancelada.status).toBe(404);
  });
});

describe('crear', () => {
  test('201 con la reserva, que aparece en el listado y se guarda', async () => {
    await comoCoordinador();

    const creada = (await api.post(
      '/horarios/reservas',
      NUEVA,
    )) as ReservaDeAmbiente;

    expect(creada).toMatchObject({
      ambiente: { codigo: 'LAB-02' },
      seccion: { id: SECCION_5B, etiqueta: '5.° B' },
      gradoCodigo: 'SEC-5',
      fecha: '2026-11-23',
      estado: 'PENDIENTE',
      editable: true,
      version: 0,
    });
    expect(await fechas('?filtro=MES')).toContain('2026-11-23');
    expect(reservasEnMemoria()).toHaveLength(7);
  });

  test('400 con un error por campo cuando falta algo o la actividad es muy larga', async () => {
    await comoCoordinador();

    const vacio = await fallo(api.post('/horarios/reservas', {}));
    const larga = await fallo(
      api.post('/horarios/reservas', { ...NUEVA, actividad: 'x'.repeat(121) }),
    );

    expect(vacio.status).toBe(400);
    expect(vacio.data.errores).toMatchObject({
      ambienteId: 'Elige el ambiente.',
      seccionId: 'Elige la sección.',
      responsableId: 'Elige el responsable.',
      actividad: 'Escribe la actividad.',
      fecha: 'Elige la fecha.',
      horaInicio: 'Elige la hora de inicio.',
      horaFin: 'Elige la hora de fin.',
    });
    expect(larga.data.errores.actividad).toBe(
      'La actividad admite hasta 120 caracteres.',
    );
  });

  test('400 con una fecha u hora pasada, un ambiente que no es espacio especial o horas invertidas', async () => {
    await comoCoordinador();

    const pasada = await fallo(
      api.post('/horarios/reservas', {
        ...NUEVA,
        fecha: '2026-11-18',
        horaInicio: '08:15',
        horaFin: '09:00',
      }),
    );
    const aula = await fallo(
      api.post('/horarios/reservas', { ...NUEVA, ambienteId: 'amb-a502' }),
    );
    const invertidas = await fallo(
      api.post('/horarios/reservas', {
        ...NUEVA,
        horaInicio: '09:00',
        horaFin: '08:15',
      }),
    );

    expect(pasada.data.message).toBe(
      'No se puede reservar en una fecha u hora que ya pasó.',
    );
    expect(aula.data.message).toBe('Elige un espacio especial activo.');
    expect(invertidas.data.message).toBe(
      'La hora de fin debe ser posterior a la de inicio.',
    );
    expect([pasada.status, aula.status, invertidas.status]).toEqual([
      400, 400, 400,
    ]);
  });

  test('404 con una sección que no existe', async () => {
    await comoCoordinador();

    const respuesta = await fallo(
      api.post('/horarios/reservas', { ...NUEVA, seccionId: 'sec-no-existe' }),
    );

    expect(respuesta.status).toBe(404);
    expect(respuesta.data.message).toBe('La sección no existe.');
  });

  test('409 con «Título: texto» si el ambiente ya está reservado a esa hora', async () => {
    await comoCoordinador();

    const respuesta = await fallo(
      api.post('/horarios/reservas', {
        ...NUEVA,
        ambienteId: AMBIENTE_LAB01,
        fecha: '2026-11-19',
        horaInicio: '09:00',
        horaFin: '10:00',
      }),
    );

    expect(respuesta.status).toBe(409);
    expect(respuesta.data.message).toBe(
      'Ambiente no disponible: Laboratorio de ciencias ya está reservado por 1.° A para Práctica de laboratorio de química el jueves 19/11/2026 de 8:15 a 9:45.',
    );
  });

  test('409 de responsable cuando María ya tiene otra reserva a esa hora', async () => {
    await comoCoordinador();

    const respuesta = await fallo(
      api.post('/horarios/reservas', {
        ...NUEVA,
        fecha: '2026-11-19',
        horaInicio: '09:00',
        horaFin: '10:00',
      }),
    );

    expect(respuesta.data.message).toBe(
      'Responsable no disponible: María Fernández Ríos ya es responsable de Práctica de laboratorio de química con 1.° A el jueves 19/11/2026 de 8:15 a 9:45.',
    );
  });

  test('una reserva contigua a otra no se cruza', async () => {
    await comoCoordinador();

    const creada = (await api.post('/horarios/reservas', {
      ...NUEVA,
      ambienteId: AMBIENTE_LAB01,
      fecha: '2026-11-19',
      horaInicio: '09:45',
      horaFin: '10:30',
    })) as ReservaDeAmbiente;

    expect(creada.horaInicio).toBe('09:45');
  });
});

describe('editar', () => {
  const edicion = (cambios: Partial<SolicitudEdicionReserva> = {}) => ({
    ambienteId: AMBIENTE_LAB01,
    seccionId: SECCION_1A,
    responsableId: RESPONSABLE_MARIA.id,
    actividad: 'Práctica corregida',
    fecha: '2026-11-19',
    horaInicio: '08:15',
    horaFin: '09:45',
    version: 0,
    ...cambios,
  });

  test('200 con los datos nuevos y la versión subida; no choca consigo misma', async () => {
    await comoCoordinador();

    const editada = (await api.put(
      '/horarios/reservas/reserva-1',
      edicion(),
    )) as ReservaDeAmbiente;

    expect(editada).toMatchObject({
      id: 'reserva-1',
      actividad: 'Práctica corregida',
      version: 1,
    });
    expect(reservasEnMemoria().find((r) => r.id === 'reserva-1')?.version).toBe(
      1,
    );
  });

  test('409 con una versión vieja', async () => {
    await comoCoordinador();
    await api.put('/horarios/reservas/reserva-1', edicion());

    const respuesta = await fallo(
      api.put('/horarios/reservas/reserva-1', edicion({ version: 0 })),
    );

    expect(respuesta.status).toBe(409);
    expect(respuesta.data.message).toBe(
      'La reserva cambió mientras la editabas. Vuelve a cargarla.',
    );
  });

  test('409 si la reserva ya finalizó', async () => {
    await comoCoordinador();

    const respuesta = await fallo(
      api.put('/horarios/reservas/reserva-4', edicion({ fecha: '2026-11-24' })),
    );

    expect(respuesta.status).toBe(409);
    expect(respuesta.data.message).toBe(
      'La reserva ya finalizó y no se puede modificar.',
    );
  });

  test('404 si no existe y 400 si falta la versión', async () => {
    await comoCoordinador();

    const inexistente = await fallo(
      api.put('/horarios/reservas/no-existe', edicion()),
    );
    const sinVersion = await fallo(
      api.put('/horarios/reservas/reserva-1', {
        ...edicion(),
        version: undefined,
      }),
    );

    expect(inexistente.status).toBe(404);
    expect(sinVersion.status).toBe(400);
    expect(sinVersion.data.errores.version).toBe(
      'Falta la versión de la reserva.',
    );
  });

  test('409 de ambiente al mover la reserva a una franja ocupada', async () => {
    await comoCoordinador();
    await api.post('/horarios/reservas', {
      ...NUEVA,
      ambienteId: AMBIENTE_LAB01,
      fecha: '2026-11-24',
      horaInicio: '08:15',
      horaFin: '09:00',
    });

    const respuesta = await fallo(
      api.put(
        '/horarios/reservas/reserva-1',
        edicion({ fecha: '2026-11-24', horaInicio: '08:30', horaFin: '09:30' }),
      ),
    );

    expect(respuesta.data.message).toMatch(/^Ambiente no disponible: /);
  });
});

describe('eliminar', () => {
  test('204: la reserva queda cancelada y desaparece del listado y de la lectura', async () => {
    await comoCoordinador();

    await api.delete('/horarios/reservas/reserva-1');

    expect(await fechas()).toEqual(['2026-11-20']);
    expect((await fallo(api.get('/horarios/reservas/reserva-1'))).status).toBe(
      404,
    );
    expect(
      reservasEnMemoria().find((r) => r.id === 'reserva-1')?.estadoGuardado,
    ).toBe('CANCELADA');
  });

  test('409 si ya está cancelada, si ya finalizó o si la versión es vieja', async () => {
    await comoCoordinador();

    const cancelada = await fallo(api.delete('/horarios/reservas/reserva-6'));
    const finalizada = await fallo(api.delete('/horarios/reservas/reserva-4'));
    const vieja = await fallo(
      api.delete('/horarios/reservas/reserva-1', { params: { version: 7 } }),
    );

    expect(cancelada.data.message).toBe('La reserva ya fue cancelada.');
    expect(finalizada.data.message).toBe(
      'La reserva ya finalizó y no se puede modificar.',
    );
    expect(vieja.data.message).toBe(
      'La reserva cambió mientras la editabas. Vuelve a cargarla.',
    );
    expect([cancelada.status, finalizada.status, vieja.status]).toEqual([
      409, 409, 409,
    ]);
  });

  test('con la versión actual se cancela; una inexistente responde 404', async () => {
    await comoCoordinador();

    await api.delete('/horarios/reservas/reserva-1', {
      params: { version: 0 },
    });

    expect(
      (await fallo(api.delete('/horarios/reservas/no-existe'))).status,
    ).toBe(404);
  });
});

describe('reinicio entre pruebas', () => {
  test('crear y cancelar cambian el listado en memoria…', async () => {
    await comoCoordinador();
    await api.post('/horarios/reservas', NUEVA);
    await api.delete('/horarios/reservas/reserva-1');

    expect(reservasEnMemoria()).toHaveLength(7);
  });

  test('…y la siguiente prueba vuelve a las seis sembradas', async () => {
    await comoCoordinador();

    expect(reservasEnMemoria()).toHaveLength(6);
    expect(await fechas()).toEqual(['2026-11-20', '2026-11-19']);
  });
});

describe('hooks', () => {
  const envoltorio = () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    const Envoltorio = ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    return { queryClient, Envoltorio };
  };

  test('useReservas pide el filtro y la página, y crear invalida la lista', async () => {
    await comoCoordinador();
    const { Envoltorio } = envoltorio();

    const { result } = renderHook(
      () => ({ lista: useReservas('MES', 1), crear: useCrearReserva() }),
      { wrapper: Envoltorio },
    );
    await waitFor(() => expect(result.current.lista.data?.meta.total).toBe(4));

    result.current.crear.mutate(NUEVA);

    await waitFor(() => expect(result.current.lista.data?.meta.total).toBe(5));
    expect(result.current.lista.data?.data.map((r) => r.fecha)).toContain(
      '2026-11-23',
    );
  });

  test('un 409 al crear no avisa por notificación: lo recibe quien llama', async () => {
    await comoCoordinador();
    const { Envoltorio } = envoltorio();
    const { result } = renderHook(() => useCrearReserva(), {
      wrapper: Envoltorio,
    });

    result.current.mutate({
      ...NUEVA,
      ambienteId: AMBIENTE_LAB01,
      fecha: '2026-11-19',
      horaInicio: '09:00',
      horaFin: '10:00',
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(
      (result.current.error as unknown as { response: { status: number } })
        .response.status,
    ).toBe(409);
  });
});
