import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';

import {
  ANIO_2025,
  aniosDocente,
  horarioDocente2025,
  horarioSoloSecundaria,
} from '../fixtures/horario-docente';
import { networkDelay, requireAuth, requireRole } from '../utils';

// HU-49 (Docente y Tutor):
//   GET /horarios/docente/anios               -> AnioDelDocente[] (los años con clases)
//   GET /horarios/docente/horario?anioId=     -> HorarioDocente (sin anioId, el año ACTIVO; 404 si el año no existe)
// Por defecto responden el horario de un docente solo con secundaria; las pruebas usan `server.use` para otros casos.
//
// `validarSesion`: en los tests la sesión también es simulada y se valida aquí (DOCENTE o TUTOR). En el navegador la
// sesión es la real del backend: si se validara aquí respondería 401 y la app mandaría al login en bucle.
const sesionDelDocente = (
  validarSesion: boolean,
  cookies: Record<string, string>,
) => {
  if (!validarSesion) return null;
  const { user, error } = requireAuth(cookies);
  if (error) return HttpResponse.json({ message: error }, { status: 401 });
  try {
    requireRole(user, [ROLES.DOCENTE, ROLES.TUTOR]);
  } catch (e: any) {
    return HttpResponse.json({ message: e.message }, { status: 403 });
  }
  return null;
};

const crearHorarioDocenteHandlers = (validarSesion: boolean) => [
  http.get(`${env.API_URL}/horarios/docente/anios`, async ({ cookies }) => {
    await networkDelay();
    return (
      sesionDelDocente(validarSesion, cookies) ??
      HttpResponse.json(aniosDocente)
    );
  }),
  http.get(
    `${env.API_URL}/horarios/docente/horario`,
    async ({ cookies, request }) => {
      await networkDelay();
      const rechazo = sesionDelDocente(validarSesion, cookies);
      if (rechazo) return rechazo;

      const anioId = new URL(request.url).searchParams.get('anioId');
      if (!anioId) return HttpResponse.json(horarioSoloSecundaria);
      if (anioId === ANIO_2025.id) return HttpResponse.json(horarioDocente2025);
      return anioId === horarioSoloSecundaria.anio?.id
        ? HttpResponse.json(horarioSoloSecundaria)
        : HttpResponse.json(
            { message: 'El año escolar no existe.' },
            { status: 404 },
          );
    },
  ),
];

export const horarioDocenteHandlers = crearHorarioDocenteHandlers(true);
export const horarioDocenteBrowserHandlers = crearHorarioDocenteHandlers(false);
