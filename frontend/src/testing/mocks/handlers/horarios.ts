import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { ROLES } from '@/config/roles';

import { horarioSeccion5A } from '../fixtures/horarios';
import { requireAuth, requireRole, networkDelay } from '../utils';

// Contrato esperado del backend:
//   GET /horarios/mi-seccion?anio=2026 -> { data: HorarioSeccion }
//   (Estudiante: su sección. Apoderado: la sección de su hijo/a.)
export const horariosHandlers = [
  http.get(
    `${env.API_URL}/horarios/mi-seccion`,
    async ({ cookies, request }) => {
      await networkDelay();

      const { user, error } = requireAuth(cookies);
      if (error) {
        return HttpResponse.json({ message: error }, { status: 401 });
      }

      try {
        requireRole(user, [ROLES.ESTUDIANTE, ROLES.APODERADO]);
      } catch (e: any) {
        return HttpResponse.json({ message: e.message }, { status: 403 });
      }

      const anio = Number(new URL(request.url).searchParams.get('anio'));

      return HttpResponse.json({
        data: {
          ...horarioSeccion5A,
          anioAcademico: anio || horarioSeccion5A.anioAcademico,
        },
      });
    },
  ),
];
