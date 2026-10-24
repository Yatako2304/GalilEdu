import { HttpResponse, delay, http } from 'msw';
import * as React from 'react';

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
  DetalleEstructuraHoraria,
  EstructuraGuardada,
  EstructuraHorariaResumen,
  EstructurasHorariasDelAnio,
  SolicitudEstructuraHoraria,
  SolicitudVistaPrevia,
  VistaPreviaJornada,
} from '@/types/api';

import { useAnioEscolar } from '../anios-escolares';
import {
  useDetalleEstructura,
  useEstructurasHorarias,
  useVistaPreviaJornada,
} from '../estructuras-horarias';
import {
  useCrearEstructura,
  useEditarEstructura,
  useEliminarEstructura,
} from '../mutaciones-estructuras';

// Figma > Personal Administrativo > CI-12 · Estructura horaria (HU-44) y "Modal - Nueva estructura horaria"

const API = env.API_URL;

const resumen = (
  codigo: string,
  nivel: 'PRIMARIA' | 'SECUNDARIA',
  esPrincipal: boolean,
): EstructuraHorariaResumen => ({
  id: `estructura-${codigo}`,
  codigo,
  descripcion: `${nivel === 'PRIMARIA' ? 'Primaria' : 'Secundaria'} · jornada de mañana`,
  nivel,
  esPrincipal,
  diasLectivos: ['LUN', 'MAR', 'MIE', 'JUE', 'VIE'],
  horaEntrada: '07:45',
  horaSalida: '14:20',
  bloquesPorDia: 8,
  duracionBloqueMin: 45,
  recesos: [
    { despuesDeBloque: 3, duracionMin: 20 },
    { despuesDeBloque: 6, duracionMin: 15 },
  ],
  bloquesPorSemana: 40,
});

const lista = (anioModificable = true): EstructurasHorariasDelAnio => ({
  anioModificable,
  estructuras: [
    resumen('EH-PRI-26', 'PRIMARIA', true),
    resumen('EH-SEC-26', 'SECUNDARIA', true),
  ],
});

const detalle = (codigo: string): DetalleEstructuraHoraria => ({
  ...resumen(codigo, 'PRIMARIA', true),
  anioModificable: true,
  jornada: [
    {
      tipo: 'BLOQUE',
      numero: 1,
      inicio: '07:45',
      fin: '08:30',
      duracionMin: 45,
    },
    {
      tipo: 'RECESO',
      numero: null,
      inicio: '10:00',
      fin: '10:20',
      duracionMin: 20,
    },
  ],
});

const solicitud: SolicitudEstructuraHoraria = {
  codigo: 'EH-TAR-26',
  descripcion: 'Secundaria · talleres de tarde',
  nivel: 'SECUNDARIA',
  esPrincipal: false,
  diasLectivos: ['LUN', 'MIE', 'VIE'],
  horaEntrada: '14:45',
  bloquesPorDia: 4,
  duracionBloqueMin: 45,
  recesos: [{ despuesDeBloque: 2, duracionMin: 15 }],
};

const administrativo = async () =>
  createUser({ roles: [ROLES.PERSONAL_ADMINISTRATIVO] });

// --- Consultas ---

const Lista = ({ anioId }: { anioId: string | undefined }) => {
  const consulta = useEstructurasHorarias(anioId);
  return (
    <p>
      {consulta.data
        ? `Modificable: ${consulta.data.anioModificable ? 'sí' : 'no'}. ${consulta.data.estructuras
            .map((e) => e.codigo)
            .join(', ')}`
        : '…'}
    </p>
  );
};

test('la lista es un objeto con `anioModificable` y las estructuras del año', async () => {
  let anioPedido = '';
  server.use(
    http.get(
      `${API}/configuracion/anios-escolares/:anioId/estructuras-horarias`,
      ({ params }) => {
        anioPedido = String(params.anioId);
        return HttpResponse.json(lista());
      },
    ),
  );

  await renderApp(<Lista anioId="anio-2026" />, {
    user: await administrativo(),
  });

  expect(
    await screen.findByText('Modificable: sí. EH-PRI-26, EH-SEC-26'),
  ).toBeInTheDocument();
  expect(anioPedido).toBe('anio-2026');
});

test('con el año no modificable la lista lo indica', async () => {
  server.use(
    http.get(
      `${API}/configuracion/anios-escolares/:anioId/estructuras-horarias`,
      () => HttpResponse.json(lista(false)),
    ),
  );

  await renderApp(<Lista anioId="anio-2026" />, {
    user: await administrativo(),
  });

  expect(await screen.findByText(/Modificable: no/)).toBeInTheDocument();
});

test('sin id de año no consulta la lista', async () => {
  let consultas = 0;
  server.use(
    http.get(
      `${API}/configuracion/anios-escolares/:anioId/estructuras-horarias`,
      () => {
        consultas++;
        return HttpResponse.json(lista());
      },
    ),
  );

  await renderApp(<Lista anioId={undefined} />, {
    user: await administrativo(),
  });

  expect(screen.getByText('…')).toBeInTheDocument();
  expect(consultas).toBe(0);
});

const Detalle = ({ id }: { id: string | undefined }) => {
  const consulta = useDetalleEstructura(id);
  return (
    <p>
      {consulta.data
        ? `${consulta.data.codigo}: ${consulta.data.jornada
            .map((f) => `${f.tipo} ${f.inicio}-${f.fin}`)
            .join(' | ')}`
        : '…'}
    </p>
  );
};

test('el detalle trae la jornada calculada, con bloques y recesos', async () => {
  let idPedido = '';
  server.use(
    http.get(`${API}/configuracion/estructuras-horarias/:id`, ({ params }) => {
      idPedido = String(params.id);
      return HttpResponse.json(detalle('EH-PRI-26'));
    }),
  );

  await renderApp(<Detalle id="estructura-EH-PRI-26" />, {
    user: await administrativo(),
  });

  expect(
    await screen.findByText(
      'EH-PRI-26: BLOQUE 07:45-08:30 | RECESO 10:00-10:20',
    ),
  ).toBeInTheDocument();
  expect(idPedido).toBe('estructura-EH-PRI-26');
});

test('sin estructura elegida no pide el detalle', async () => {
  let consultas = 0;
  server.use(
    http.get(`${API}/configuracion/estructuras-horarias/:id`, () => {
      consultas++;
      return HttpResponse.json(detalle('EH-PRI-26'));
    }),
  );

  await renderApp(<Detalle id={undefined} />, { user: await administrativo() });

  expect(screen.getByText('…')).toBeInTheDocument();
  expect(consultas).toBe(0);
});

// --- Vista previa ---

const vistaPrevia = (salida: string): VistaPreviaJornada => ({
  valida: salida <= '18:00',
  errores:
    salida <= '18:00'
      ? []
      : ['La jornada debe terminar como máximo a las 18:00.'],
  jornada: {
    filas: [
      {
        tipo: 'BLOQUE',
        numero: 1,
        inicio: '14:45',
        fin: '15:30',
        duracionMin: 45,
      },
    ],
    horaSalida: salida,
    bloquesPorSemana: 12,
  },
});

const formulario = (bloquesPorDia: number | null): SolicitudVistaPrevia => ({
  codigo: 'EH-TAR-26',
  descripcion: null,
  nivel: 'SECUNDARIA',
  esPrincipal: false,
  diasLectivos: ['LUN'],
  horaEntrada: '14:45',
  bloquesPorDia,
  duracionBloqueMin: 45,
  recesos: [],
});

const VistaPrevia = ({
  solicitud: cuerpo,
}: {
  solicitud: SolicitudVistaPrevia | null;
}) => {
  const consulta = useVistaPreviaJornada('anio-2026', cuerpo);
  return (
    <p>
      {consulta.data
        ? `Salida ${consulta.data.jornada?.horaSalida}${consulta.data.valida ? '' : ' (pasa de las 18:00)'}`
        : '…'}
    </p>
  );
};

test('la vista previa manda el formulario tal cual, también a medio llenar', async () => {
  let cuerpo: SolicitudVistaPrevia | null = null;
  server.use(
    http.post(
      `${API}/configuracion/anios-escolares/:anioId/estructuras-horarias/vista-previa`,
      async ({ request }) => {
        cuerpo = (await request.json()) as SolicitudVistaPrevia;
        return HttpResponse.json(vistaPrevia('18:00'));
      },
    ),
  );

  await renderApp(<VistaPrevia solicitud={formulario(null)} />, {
    user: await administrativo(),
  });

  expect(await screen.findByText('Salida 18:00')).toBeInTheDocument();
  expect(cuerpo).toEqual(formulario(null));
});

test('la jornada llega aunque pase de las 18:00', async () => {
  server.use(
    http.post(
      `${API}/configuracion/anios-escolares/:anioId/estructuras-horarias/vista-previa`,
      () => HttpResponse.json(vistaPrevia('19:30')),
    ),
  );

  await renderApp(<VistaPrevia solicitud={formulario(6)} />, {
    user: await administrativo(),
  });

  expect(
    await screen.findByText('Salida 19:30 (pasa de las 18:00)'),
  ).toBeInTheDocument();
});

test('sin formulario no pide la vista previa', async () => {
  let consultas = 0;
  server.use(
    http.post(
      `${API}/configuracion/anios-escolares/:anioId/estructuras-horarias/vista-previa`,
      () => {
        consultas++;
        return HttpResponse.json(vistaPrevia('18:00'));
      },
    ),
  );

  await renderApp(<VistaPrevia solicitud={null} />, {
    user: await administrativo(),
  });

  expect(screen.getByText('…')).toBeInTheDocument();
  expect(consultas).toBe(0);
});

test('mientras llega la vista previa nueva se sigue mostrando la anterior, y una respuesta vieja no pisa a la nueva', async () => {
  server.use(
    http.post(
      `${API}/configuracion/anios-escolares/:anioId/estructuras-horarias/vista-previa`,
      async ({ request }) => {
        const cuerpo = (await request.json()) as SolicitudVistaPrevia;
        // El formulario viejo (4 bloques) responde lento; el nuevo (5 bloques), rápido.
        if (cuerpo.bloquesPorDia === 4) {
          await delay(150);
          return HttpResponse.json(vistaPrevia('18:00'));
        }
        return HttpResponse.json(vistaPrevia('18:45'));
      },
    ),
  );

  const Cambiante = () => {
    const [bloques, setBloques] = React.useState(3);
    return (
      <div>
        <button onClick={() => setBloques(4)}>cuatro</button>
        <button onClick={() => setBloques(5)}>cinco</button>
        <VistaPrevia solicitud={formulario(bloques)} />
      </div>
    );
  };
  await renderApp(<Cambiante />, { user: await administrativo() });
  await screen.findByText(/Salida/);

  await userEvent.click(screen.getByRole('button', { name: 'cuatro' }));
  await userEvent.click(screen.getByRole('button', { name: 'cinco' }));

  expect(
    await screen.findByText('Salida 18:45 (pasa de las 18:00)'),
  ).toBeInTheDocument();
  // Esperar a que termine la respuesta lenta: no debe pisar a la actual.
  await new Promise((resolver) => setTimeout(resolver, 250));
  expect(
    screen.getByText('Salida 18:45 (pasa de las 18:00)'),
  ).toBeInTheDocument();
});

// --- Mutaciones ---

const guardada = (advertencias: string[] = []): EstructuraGuardada => ({
  estructura: detalle('EH-TAR-26'),
  advertencias,
});

const Acciones = ({
  onCreada,
  onEditada,
  onEliminada,
}: {
  onCreada?: (g: EstructuraGuardada) => void;
  onEditada?: (g: EstructuraGuardada) => void;
  onEliminada?: () => void;
}) => {
  const consulta = useEstructurasHorarias('anio-2026');
  const detalleElegido = useDetalleEstructura('estructura-EH-PRI-26');
  const anio = useAnioEscolar('anio-2026');
  const crear = useCrearEstructura('anio-2026', onCreada);
  const editar = useEditarEstructura(onEditada);
  const eliminar = useEliminarEstructura(onEliminada);
  return (
    <div>
      <p>
        Estructuras: {consulta.data ? consulta.data.estructuras.length : '…'}
      </p>
      <p>Detalle: {detalleElegido.data ? detalleElegido.data.codigo : '…'}</p>
      <p>Año: {anio.data ? anio.data.cantidadEstructuras : '…'} estructuras</p>
      <button onClick={() => crear.mutate(solicitud)}>crear</button>
      <button
        onClick={() =>
          editar.mutate({
            id: 'estructura-EH-PRI-26',
            data: {
              codigo: 'EH-PRI-26',
              descripcion: null,
              diasLectivos: ['LUN'],
              horaEntrada: '07:45',
              bloquesPorDia: 8,
              duracionBloqueMin: 45,
              recesos: [],
            },
          })
        }
      >
        editar
      </button>
      <button onClick={() => eliminar.mutate('estructura-EH-PRI-26')}>
        eliminar
      </button>
    </div>
  );
};

/** Registra cuántas veces se pidió cada cosa y responde como el backend. */
const servidor = () => {
  const pedidos = { lista: 0, detalle: 0, anio: 0 };
  let estructuras = 2;
  server.use(
    http.get(
      `${API}/configuracion/anios-escolares/:anioId/estructuras-horarias`,
      () => {
        pedidos.lista++;
        return HttpResponse.json({
          ...lista(),
          estructuras: [
            ...lista().estructuras,
            resumen('EH-TAR-26', 'SECUNDARIA', false),
          ].slice(0, estructuras),
        });
      },
    ),
    http.get(`${API}/configuracion/estructuras-horarias/:id`, ({ params }) => {
      pedidos.detalle++;
      return HttpResponse.json(
        detalle(String(params.id).replace('estructura-', '')),
      );
    }),
    http.get(`${API}/configuracion/anios-escolares/anio-2026`, () => {
      pedidos.anio++;
      return HttpResponse.json({
        id: 'anio-2026',
        anio: 2026,
        fechaInicio: '2026-03-02',
        fechaFin: '2026-12-18',
        regimen: 'BIMESTRAL',
        estado: 'PLANIFICADO',
        cantidadSecciones: 0,
        cantidadEstructuras: estructuras,
        periodos: [],
      });
    }),
  );
  return {
    pedidos,
    cambiarCantidad: (n: number) => {
      estructuras = n;
    },
  };
};

test('crear manda el cuerpo, devuelve las advertencias y recarga la lista y los datos del año', async () => {
  const { pedidos, cambiarCantidad } = servidor();
  let cuerpo: SolicitudEstructuraHoraria | null = null;
  server.use(
    http.post(
      `${API}/configuracion/anios-escolares/anio-2026/estructuras-horarias`,
      async ({ request }) => {
        cuerpo = (await request.json()) as SolicitudEstructuraHoraria;
        cambiarCantidad(3);
        return HttpResponse.json(
          guardada([
            'La jornada de secundaria pasa de 30 a 35 bloques por semana.',
          ]),
          { status: 201 },
        );
      },
    ),
  );
  const onCreada = vi.fn();
  await renderApp(<Acciones onCreada={onCreada} />, {
    user: await administrativo(),
  });
  await screen.findByText('Estructuras: 2');
  await screen.findByText('Año: 2 estructuras');

  await userEvent.click(screen.getByRole('button', { name: 'crear' }));

  await waitFor(() => expect(onCreada).toHaveBeenCalledTimes(1));
  expect(cuerpo).toEqual(solicitud);
  expect(onCreada.mock.calls[0][0].advertencias).toEqual([
    'La jornada de secundaria pasa de 30 a 35 bloques por semana.',
  ]);
  expect(await screen.findByText('Estructuras: 3')).toBeInTheDocument();
  expect(await screen.findByText('Año: 3 estructuras')).toBeInTheDocument();
  expect(pedidos.lista).toBeGreaterThanOrEqual(2);
  expect(pedidos.anio).toBeGreaterThanOrEqual(2);
});

test('un 409 al crear (código repetido) se avisa una sola vez y no se llama a onSuccess ni se recarga', async () => {
  const { pedidos } = servidor();
  const mensaje =
    'Ya existe una estructura con el código EH-TAR-26 en este año.';
  server.use(
    http.post(
      `${API}/configuracion/anios-escolares/anio-2026/estructuras-horarias`,
      () => HttpResponse.json({ message: mensaje }, { status: 409 }),
    ),
  );
  const onCreada = vi.fn();
  await renderApp(<Acciones onCreada={onCreada} />, {
    user: await administrativo(),
  });
  await screen.findByText('Estructuras: 2');
  const listasAntes = pedidos.lista;

  await userEvent.click(screen.getByRole('button', { name: 'crear' }));

  expect(await screen.findAllByText(mensaje)).toHaveLength(1);
  expect(onCreada).not.toHaveBeenCalled();
  expect(pedidos.lista).toBe(listasAntes);
});

test('editar manda el cuerpo sin nivel a la estructura elegida y recarga su detalle', async () => {
  const { pedidos } = servidor();
  let cuerpo: Record<string, unknown> | null = null;
  server.use(
    http.put(
      `${API}/configuracion/estructuras-horarias/estructura-EH-PRI-26`,
      async ({ request }) => {
        cuerpo = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(guardada());
      },
    ),
  );
  const onEditada = vi.fn();
  await renderApp(<Acciones onEditada={onEditada} />, {
    user: await administrativo(),
  });
  await screen.findByText('Detalle: EH-PRI-26');
  const detallesAntes = pedidos.detalle;

  await userEvent.click(screen.getByRole('button', { name: 'editar' }));

  await waitFor(() => expect(onEditada).toHaveBeenCalledTimes(1));
  expect(cuerpo).not.toHaveProperty('nivel');
  expect(cuerpo).toMatchObject({ codigo: 'EH-PRI-26', bloquesPorDia: 8 });
  await waitFor(() => expect(pedidos.detalle).toBeGreaterThan(detallesAntes));
});

test('eliminar responde 204, recarga la lista y no vuelve a pedir el detalle de la estructura eliminada', async () => {
  const { pedidos, cambiarCantidad } = servidor();
  server.use(
    http.delete(
      `${API}/configuracion/estructuras-horarias/estructura-EH-PRI-26`,
      () => {
        cambiarCantidad(1);
        return new HttpResponse(null, { status: 204 });
      },
    ),
  );
  const onEliminada = vi.fn();
  await renderApp(<Acciones onEliminada={onEliminada} />, {
    user: await administrativo(),
  });
  await screen.findByText('Detalle: EH-PRI-26');
  const detallesAntes = pedidos.detalle;

  await userEvent.click(screen.getByRole('button', { name: 'eliminar' }));

  await waitFor(() => expect(onEliminada).toHaveBeenCalledTimes(1));
  expect(await screen.findByText('Estructuras: 1')).toBeInTheDocument();
  // Pedir el detalle de lo eliminado daría un 404 y un aviso de error por una estructura que ya no existe.
  expect(pedidos.detalle).toBe(detallesAntes);
});

test('un 409 al eliminar (la principal con otras del nivel) se avisa una sola vez y no se recarga', async () => {
  const { pedidos } = servidor();
  const mensaje = 'Marca otra como principal primero.';
  server.use(
    http.delete(
      `${API}/configuracion/estructuras-horarias/estructura-EH-PRI-26`,
      () => HttpResponse.json({ message: mensaje }, { status: 409 }),
    ),
  );
  const onEliminada = vi.fn();
  await renderApp(<Acciones onEliminada={onEliminada} />, {
    user: await administrativo(),
  });
  await screen.findByText('Estructuras: 2');
  const listasAntes = pedidos.lista;

  await userEvent.click(screen.getByRole('button', { name: 'eliminar' }));

  expect(await screen.findAllByText(mensaje)).toHaveLength(1);
  expect(onEliminada).not.toHaveBeenCalled();
  expect(pedidos.lista).toBe(listasAntes);
});
