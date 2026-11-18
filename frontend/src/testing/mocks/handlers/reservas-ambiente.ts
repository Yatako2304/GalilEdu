import dayjs from 'dayjs';
import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';
import { diaDeLaSemana } from '@/features/horarios/lib/reservas';
import { NOMBRE_DEL_DIA, type AmbienteOpcion } from '@/features/horarios/types';
import type {
  FiltroDeReservas,
  ReservaDeAmbiente,
  SolicitudEdicionReserva,
  SolicitudReserva,
} from '@/features/horarios/types-reservas';
import { formatDay } from '@/utils/format';

import { ANIO_2026 } from '../fixtures/horarios-coordinador';
import {
  AHORA_SIMULADO,
  MENSAJE_DE_VERSION_SIMULADO,
  RESPONSABLE_MARIA,
  ReservaGuardada,
  buscarSeccionDeReserva,
  espaciosDeReserva,
  opcionesDeReserva,
  reservasSembradas,
} from '../fixtures/reservas-ambiente';
import { networkDelay, requireAuth, requireRole } from '../utils';

// HU-48 (Coordinador). Contrato del backend (`ReservaAmbienteController`):
//   GET    /horarios/reservas?filtro=&page=&size=  -> { data, meta } (solo ACTIVAS; fecha y hora de inicio descendentes)
//   GET    /horarios/reservas/opciones             -> { ambientes, responsables }
//   GET    /horarios/reservas/{id}                 -> la reserva (404 si no existe o está cancelada)
//   POST   /horarios/reservas                      -> 201 con la reserva (400, 404, 409)
//   PUT    /horarios/reservas/{id}                 -> 200 con la reserva; `version` obligatoria (400, 404, 409)
//   DELETE /horarios/reservas/{id}?version=        -> 204; cancela (404, 409)
//
// Las reservas viven en memoria y parten de las seis de los datos de prueba: crear, editar y eliminar cambian el
// listado. Las pruebas llaman a `reiniciarReservas()` en `afterEach` (el reinicio global no los conoce). «Hoy» es
// `AHORA_SIMULADO`; `fijarAhora` lo cambia. Solo se registran en `handlers` (pruebas): el backend ya responde.
//
// Solo simulan los cruces 4 (mismo ambiente), 7b (mismo responsable) y 8 (misma sección); el resto lo prueban las
// pruebas del backend. Para otros casos, las pruebas usan `server.use`.

const API = env.API_URL;
const RUTA = `${API}/horarios/reservas`;
const TAMANO_MAXIMO = 100;
const MAXIMO_ACTIVIDAD = 120;

let reservas: ReservaGuardada[] = reservasSembradas();
let ahora = AHORA_SIMULADO;
let contador = 0;

/** Vuelve a las seis reservas sembradas y al «hoy» simulado. */
export const reiniciarReservas = () => {
  reservas = reservasSembradas();
  ahora = AHORA_SIMULADO;
  contador = 0;
};

/** Cambia el «ahora» de los simuladores (ISO local, «2026-11-18T10:00»). */
export const fijarAhora = (iso: string) => {
  ahora = iso;
};

/** Las reservas que hay en memoria, para que las pruebas comprueben lo que se guardó. */
export const reservasEnMemoria = (): readonly ReservaGuardada[] => reservas;

// ---------- Sesión ----------

// En las pruebas la sesión también es simulada y se valida aquí (solo el Coordinador).
const rechazoDeSesion = (cookies: Record<string, string>) => {
  const { user, error } = requireAuth(cookies);
  if (error) return HttpResponse.json({ message: error }, { status: 401 });
  try {
    requireRole(user, [ROLES.COORDINADOR]);
  } catch (e: any) {
    return HttpResponse.json({ message: e.message }, { status: 403 });
  }
  return null;
};

// ---------- Fecha y estado ----------

const finalizada = (r: ReservaGuardada) =>
  !dayjs(`${r.fecha}T${r.horaFin}`).isAfter(dayjs(ahora));

const aDto = (r: ReservaGuardada): ReservaDeAmbiente => {
  const terminada = finalizada(r);
  const { estadoGuardado, ...resto } = r;
  return {
    ...resto,
    estado: terminada ? 'FINALIZADO' : 'PENDIENTE',
    editable: estadoGuardado === 'ACTIVA' && !terminada,
  };
};

/** El rango «AAAA-MM-DD» del filtro respecto de «hoy»: la semana va de lunes a domingo. */
const rangoDe = (filtro: FiltroDeReservas): [string, string] => {
  const hoy = dayjs(ahora);
  const formato = 'YYYY-MM-DD';
  if (filtro === 'MES') {
    return [
      hoy.startOf('month').format(formato),
      hoy.endOf('month').format(formato),
    ];
  }
  if (filtro === 'ANIO') {
    return [
      hoy.startOf('year').format(formato),
      hoy.endOf('year').format(formato),
    ];
  }
  const lunes = hoy.subtract((hoy.day() + 6) % 7, 'day');
  return [lunes.format(formato), lunes.add(6, 'day').format(formato)];
};

const FILTROS: FiltroDeReservas[] = ['SEMANA', 'MES', 'ANIO'];

const enteroPositivo = (texto: string | null, porDefecto: number) => {
  const n = Number(texto);
  return Number.isInteger(n) && n > 0 ? n : porDefecto;
};

// ---------- Validación del cuerpo (los mismos mensajes que el backend) ----------

const CAMPOS_OBLIGATORIOS: [keyof SolicitudReserva, string][] = [
  ['ambienteId', 'Elige el ambiente.'],
  ['seccionId', 'Elige la sección.'],
  ['responsableId', 'Elige el responsable.'],
  ['actividad', 'Escribe la actividad.'],
  ['fecha', 'Elige la fecha.'],
  ['horaInicio', 'Elige la hora de inicio.'],
  ['horaFin', 'Elige la hora de fin.'],
];

const respuestaDe400 = (errores: Record<string, string>) =>
  HttpResponse.json(
    { message: Object.values(errores)[0], errores },
    { status: 400 },
  );

const errorDe = (mensaje: string, status: number) =>
  HttpResponse.json({ message: mensaje }, { status });

/** Los errores de `@Valid`: campos vacíos, actividad en blanco o muy larga (y `version` en el PUT). */
const erroresDelCuerpo = (
  cuerpo: Partial<SolicitudEdicionReserva>,
  conVersion: boolean,
) => {
  const errores: Record<string, string> = {};
  if (conVersion && (cuerpo.version === undefined || cuerpo.version === null)) {
    errores.version = 'Falta la versión de la reserva.';
  }
  CAMPOS_OBLIGATORIOS.forEach(([campo, mensaje]) => {
    const valor = cuerpo[campo];
    if (valor === undefined || valor === null || String(valor).trim() === '') {
      errores[campo] = mensaje;
    }
  });
  if (
    !errores.actividad &&
    String(cuerpo.actividad).length > MAXIMO_ACTIVIDAD
  ) {
    errores.actividad = 'La actividad admite hasta 120 caracteres.';
  }
  return errores;
};

const tipoDe = (id: string): AmbienteOpcion | undefined =>
  espaciosDeReserva.find((a) => a.id === id);

/** Las reglas de forma que simulan los mismos 400 / 404 del backend. Devuelve la respuesta de error o null. */
const rechazoDeForma = (cuerpo: SolicitudReserva) => {
  if (!buscarSeccionDeReserva(cuerpo.seccionId)) {
    return errorDe('La sección no existe.', 404);
  }
  if (!tipoDe(cuerpo.ambienteId)) {
    return errorDe('Elige un espacio especial activo.', 400);
  }
  if (cuerpo.responsableId !== RESPONSABLE_MARIA.id) {
    return errorDe('Elige un docente o tutor activo como responsable.', 400);
  }
  if (cuerpo.horaFin <= cuerpo.horaInicio) {
    return errorDe('La hora de fin debe ser posterior a la de inicio.', 400);
  }
  if (dayjs(`${cuerpo.fecha}T${cuerpo.horaInicio}`).isBefore(dayjs(ahora))) {
    return errorDe(
      'No se puede reservar en una fecha u hora que ya pasó.',
      400,
    );
  }
  return null;
};

// ---------- Cruces ----------

/** «8:00» (sin cero a la izquierda), como los mensajes del backend. */
const hora = (texto: string) =>
  `${Number(texto.slice(0, 2))}:${texto.slice(3)}`;

const cuando = (r: ReservaGuardada) => {
  const dia = diaDeLaSemana(r.fecha);
  const nombre = dia ? NOMBRE_DEL_DIA[dia].toLowerCase() : 'domingo';
  return `el ${nombre} ${formatDay(r.fecha)} de ${hora(r.horaInicio)} a ${hora(r.horaFin)}`;
};

/** El primer cruce con otra reserva ACTIVA (4, 7b y 8, en ese orden), con «Título: texto». Excluye `excluirId`. */
const primerCruce = (cuerpo: SolicitudReserva, excluirId?: string) => {
  const otras = reservas.filter(
    (r) =>
      r.estadoGuardado === 'ACTIVA' &&
      r.id !== excluirId &&
      r.fecha === cuerpo.fecha &&
      cuerpo.horaInicio < r.horaFin &&
      r.horaInicio < cuerpo.horaFin,
  );
  const delAmbiente = otras.find((r) => r.ambiente.id === cuerpo.ambienteId);
  if (delAmbiente) {
    return `Ambiente no disponible: ${delAmbiente.ambiente.nombre} ya está reservado por ${delAmbiente.seccion.etiqueta} para ${delAmbiente.actividad} ${cuando(delAmbiente)}.`;
  }
  const delResponsable = otras.find(
    (r) => r.responsable.id === cuerpo.responsableId,
  );
  if (delResponsable) {
    return `Responsable no disponible: ${delResponsable.responsable.nombre} ya es responsable de ${delResponsable.actividad} con ${delResponsable.seccion.etiqueta} ${cuando(delResponsable)}.`;
  }
  const delaSeccion = otras.find((r) => r.seccion.id === cuerpo.seccionId);
  if (delaSeccion) {
    return `Sección no disponible: ${delaSeccion.seccion.etiqueta} ya tiene reservado ${delaSeccion.ambiente.nombre} para ${delaSeccion.actividad} ${cuando(delaSeccion)}.`;
  }
  return null;
};

/** Los datos de una reserva a partir de lo que mandó el cuerpo. */
const datosDe = (cuerpo: SolicitudReserva) => {
  const seccion = buscarSeccionDeReserva(cuerpo.seccionId) as NonNullable<
    ReturnType<typeof buscarSeccionDeReserva>
  >;
  const ambiente = tipoDe(cuerpo.ambienteId) as AmbienteOpcion;
  return {
    ambiente: {
      id: ambiente.id,
      codigo: ambiente.codigo,
      nombre: ambiente.nombre,
    },
    actividad: cuerpo.actividad.trim(),
    gradoCodigo: seccion.gradoCodigo,
    seccion: { id: cuerpo.seccionId, etiqueta: seccion.etiqueta },
    responsable: RESPONSABLE_MARIA,
    fecha: cuerpo.fecha,
    horaInicio: cuerpo.horaInicio,
    horaFin: cuerpo.horaFin,
  };
};

// ---------- Handlers ----------

export const reservasAmbienteHandlers = [
  http.get(RUTA, async ({ cookies, request }) => {
    await networkDelay();
    const rechazo = rechazoDeSesion(cookies);
    if (rechazo) return rechazo;

    const parametros = new URL(request.url).searchParams;
    const filtro = (parametros.get('filtro') ?? 'SEMANA') as FiltroDeReservas;
    if (!FILTROS.includes(filtro)) {
      return errorDe('La solicitud tiene un formato inválido', 400);
    }
    const page = enteroPositivo(parametros.get('page'), 1);
    const size = Math.min(
      enteroPositivo(parametros.get('size'), 10),
      TAMANO_MAXIMO,
    );

    const [desde, hasta] = rangoDe(filtro);
    const delRango = reservas
      .filter(
        (r) =>
          r.estadoGuardado === 'ACTIVA' && r.fecha >= desde && r.fecha <= hasta,
      )
      .sort(
        (a, b) =>
          b.fecha.localeCompare(a.fecha) ||
          b.horaInicio.localeCompare(a.horaInicio) ||
          a.id.localeCompare(b.id),
      );
    return HttpResponse.json({
      data: delRango.slice((page - 1) * size, page * size).map(aDto),
      meta: {
        page,
        size,
        total: delRango.length,
        totalPages: Math.max(1, Math.ceil(delRango.length / size)),
      },
    });
  }),

  // Antes de `/:id`: «opciones» no es un id.
  http.get(`${RUTA}/opciones`, async ({ cookies }) => {
    await networkDelay();
    return rechazoDeSesion(cookies) ?? HttpResponse.json(opcionesDeReserva);
  }),

  http.get(`${RUTA}/:id`, async ({ cookies, params }) => {
    await networkDelay();
    const rechazo = rechazoDeSesion(cookies);
    if (rechazo) return rechazo;
    const reserva = reservas.find((r) => r.id === params.id);
    return reserva && reserva.estadoGuardado === 'ACTIVA'
      ? HttpResponse.json(aDto(reserva))
      : errorDe('La reserva no existe.', 404);
  }),

  http.post(RUTA, async ({ cookies, request }) => {
    await networkDelay();
    const rechazo = rechazoDeSesion(cookies);
    if (rechazo) return rechazo;

    const cuerpo = (await request.json()) as SolicitudReserva;
    const errores = erroresDelCuerpo(cuerpo, false);
    if (Object.keys(errores).length > 0) return respuestaDe400(errores);
    const deForma = rechazoDeForma(cuerpo);
    if (deForma) return deForma;
    const cruce = primerCruce(cuerpo);
    if (cruce) return errorDe(cruce, 409);

    contador += 1;
    const nueva: ReservaGuardada = {
      id: `reserva-nueva-${contador}`,
      ...datosDe(cuerpo),
      anioId: ANIO_2026,
      version: 0,
      estadoGuardado: 'ACTIVA',
    };
    reservas.push(nueva);
    return HttpResponse.json(aDto(nueva), { status: 201 });
  }),

  http.put(`${RUTA}/:id`, async ({ cookies, params, request }) => {
    await networkDelay();
    const rechazo = rechazoDeSesion(cookies);
    if (rechazo) return rechazo;

    const cuerpo = (await request.json()) as SolicitudEdicionReserva;
    const errores = erroresDelCuerpo(cuerpo, true);
    if (Object.keys(errores).length > 0) return respuestaDe400(errores);
    const reserva = reservas.find((r) => r.id === params.id);
    if (!reserva) return errorDe('La reserva no existe.', 404);
    if (reserva.version !== cuerpo.version) {
      return errorDe(MENSAJE_DE_VERSION_SIMULADO, 409);
    }
    if (reserva.estadoGuardado === 'CANCELADA') {
      return errorDe('La reserva ya fue cancelada.', 409);
    }
    if (finalizada(reserva)) {
      return errorDe('La reserva ya finalizó y no se puede modificar.', 409);
    }
    const deForma = rechazoDeForma(cuerpo);
    if (deForma) return deForma;
    const cruce = primerCruce(cuerpo, reserva.id);
    if (cruce) return errorDe(cruce, 409);

    Object.assign(reserva, datosDe(cuerpo), { version: reserva.version + 1 });
    return HttpResponse.json(aDto(reserva));
  }),

  http.delete(`${RUTA}/:id`, async ({ cookies, params, request }) => {
    await networkDelay();
    const rechazo = rechazoDeSesion(cookies);
    if (rechazo) return rechazo;

    const reserva = reservas.find((r) => r.id === params.id);
    if (!reserva) return errorDe('La reserva no existe.', 404);
    const version = new URL(request.url).searchParams.get('version');
    if (version !== null && Number(version) !== reserva.version) {
      return errorDe(MENSAJE_DE_VERSION_SIMULADO, 409);
    }
    if (reserva.estadoGuardado === 'CANCELADA') {
      return errorDe('La reserva ya fue cancelada.', 409);
    }
    if (finalizada(reserva)) {
      return errorDe('La reserva ya finalizó y no se puede modificar.', 409);
    }
    reserva.estadoGuardado = 'CANCELADA';
    reserva.version += 1;
    return new HttpResponse(null, { status: 204 });
  }),
];
