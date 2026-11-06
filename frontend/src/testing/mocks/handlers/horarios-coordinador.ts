import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import type {
  CeldaSolicitada,
  CruceDetectado,
  HorarioDeSeccion,
  SolicitudHorario,
} from '@/features/horarios/types';

import {
  AMBIENTE_LABORATORIO,
  CRUCE_DE_AMBIENTE_NUEVO,
  MENSAJE_SIN_PRINCIPAL,
  SECCION_4A,
  aniosCoordinador,
  ambientesCoordinador,
  gradosCoordinador,
  horarioDe,
  mensajeDeVersion,
  opcionesCoordinador,
} from '../fixtures/horarios-coordinador';
import { networkDelay } from '../utils';

// Contrato del backend de HU-45 (coordinador). Los cuatro de /horarios/secciones y los selectores de año, grado y sección de Horarios:
//   GET  /horarios/anios-escolares                 -> los años (id, anio, estado), del más reciente al más antiguo
//   GET  /horarios/anios-escolares/{id}/secciones  -> los grados con sus secciones del año
//   GET  /horarios/secciones/{id}           -> la matriz (con y sin horario guardado, con cruces, sin docente, año cerrado)
//   GET  /horarios/secciones/{id}/opciones  -> cursos del plan y ambientes activos
//   PUT  /horarios/secciones/{id}           -> guarda la matriz completa: 200 (misma forma que el GET), 409 de versión, 400
//   POST /horarios/secciones/{id}/cruces    -> AÚN NO EXISTE en el backend (lo hace HU-46): solo este simulador
//
// No guardan nada entre peticiones: el PUT responde según lo que recibe y la versión de la sección en los fixtures.

const API = env.API_URL;

const noEncontrada = () =>
  HttpResponse.json({ message: 'La sección no existe.' }, { status: 404 });

/** El horario de la sección o la respuesta de error que daría el backend. */
const horarioOError = (seccionId: string) => {
  if (seccionId === SECCION_4A) {
    return HttpResponse.json(
      { message: MENSAJE_SIN_PRINCIPAL },
      { status: 409 },
    );
  }
  const horario = horarioDe(seccionId);
  return horario ?? noEncontrada();
};

/** Las horas que ya tiene cada curso según el horario guardado (1 bloque = 1 hora semanal). */
const asignadasDe = (horario: HorarioDeSeccion) => {
  const asignadas: Record<string, number> = {};
  horario.celdas.forEach((c) => {
    asignadas[c.planCursoId] = (asignadas[c.planCursoId] ?? 0) + 1;
  });
  return asignadas;
};

/** Cruces que simula el POST: el laboratorio ya está ocupado el lunes en el bloque 1. */
export const crucesSimulados = (celdas: CeldaSolicitada[]): CruceDetectado[] =>
  celdas
    .filter(
      (c) =>
        c.ambienteId === AMBIENTE_LABORATORIO &&
        c.dia === 'LUN' &&
        c.numeroBloque === 1,
    )
    .map((c) => ({
      tipo: 'AMBIENTE' as const,
      dia: c.dia,
      numeroBloque: c.numeroBloque,
      mensaje: CRUCE_DE_AMBIENTE_NUEVO,
      seccion: '5.° B',
      curso: 'Física',
    }));

export const cruceHandler = http.post(
  `${API}/horarios/secciones/:seccionId/cruces`,
  async ({ request }) => {
    await networkDelay();
    const { celdas } = (await request.json()) as { celdas: CeldaSolicitada[] };
    return HttpResponse.json(crucesSimulados(celdas));
  },
);

export const horariosCoordinadorHandlers = [
  http.get(`${API}/horarios/anios-escolares`, async () => {
    await networkDelay();
    return HttpResponse.json(aniosCoordinador);
  }),

  http.get(`${API}/horarios/anios-escolares/:anioId/secciones`, async () => {
    await networkDelay();
    return HttpResponse.json(gradosCoordinador);
  }),

  http.get(`${API}/horarios/secciones/:seccionId`, async ({ params }) => {
    await networkDelay();
    const resultado = horarioOError(String(params.seccionId));
    return resultado instanceof HttpResponse
      ? resultado
      : HttpResponse.json(resultado);
  }),

  http.get(
    `${API}/horarios/secciones/:seccionId/opciones`,
    async ({ params }) => {
      await networkDelay();
      const horario = horarioDe(String(params.seccionId));
      if (!horario) return noEncontrada();
      return HttpResponse.json(
        opcionesCoordinador(horario.seccion.id, asignadasDe(horario)),
      );
    },
  ),

  http.put(
    `${API}/horarios/secciones/:seccionId`,
    async ({ params, request }) => {
      await networkDelay();
      const guardado = horarioDe(String(params.seccionId));
      if (!guardado) return noEncontrada();
      const solicitud = (await request.json()) as SolicitudHorario;

      if (solicitud.version !== guardado.version) {
        return HttpResponse.json(
          {
            message: mensajeDeVersion(
              guardado.seccion.gradoNombre,
              guardado.seccion.nombre,
            ),
          },
          { status: 409 },
        );
      }

      const opciones = opcionesCoordinador(guardado.seccion.id);
      const celdas = [];
      for (const solicitada of solicitud.celdas) {
        const curso = opciones.cursos.find(
          (c) => c.planCursoId === solicitada.planCursoId,
        );
        const ambiente = ambientesCoordinador.find(
          (a) => a.id === solicitada.ambienteId,
        );
        if (!curso) {
          return HttpResponse.json(
            {
              message:
                'El curso elegido no pertenece al plan de 5.° de secundaria de este año.',
            },
            { status: 400 },
          );
        }
        if (!ambiente) {
          return HttpResponse.json(
            { message: 'El ambiente elegido no existe o no está activo.' },
            { status: 400 },
          );
        }
        celdas.push({
          dia: solicitada.dia,
          numeroBloque: solicitada.numeroBloque,
          planCursoId: curso.planCursoId,
          curso: curso.nombre,
          docente: curso.docente,
          ambiente: { id: ambiente.id, nombre: ambiente.nombre },
          cruces: [],
        });
      }

      // Una versión nueva solo si algo cambió: el simulador la sube siempre que hay horario previo.
      const respuesta: HorarioDeSeccion = {
        ...guardado,
        version: guardado.version === null ? 0 : guardado.version + 1,
        celdas,
      };
      return HttpResponse.json(respuesta);
    },
  ),

  cruceHandler,
];
