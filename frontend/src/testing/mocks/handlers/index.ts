import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';

import { networkDelay } from '../utils';

import { authHandlers } from './auth';
import { horariosBrowserHandlers, horariosHandlers } from './horarios';
import {
  cruceHandler,
  horariosCoordinadorHandlers,
} from './horarios-coordinador';

// Handlers de módulos que AÚN NO tienen backend. Se usan en el navegador (npm run dev con
// VITE_APP_ENABLE_API_MOCKING=true). Cuando un módulo tenga endpoints reales, quítalo de aquí.
// HU-45: solo el POST de cruces (lo implementa HU-46); el resto de /horarios/secciones ya lo responde el backend.
export const browserHandlers = [
  ...horariosBrowserHandlers,
  cruceHandler,
  http.get(`${env.API_URL}/healthcheck`, async () => {
    await networkDelay();
    return HttpResponse.json({ ok: true });
  }),
];

// En los tests (vitest) se simula todo, incluida la autenticación.
export const handlers = [
  ...authHandlers,
  ...horariosHandlers,
  ...horariosCoordinadorHandlers,
  ...browserHandlers.filter(
    (h) => !horariosBrowserHandlers.includes(h) && h !== cruceHandler,
  ),
];
