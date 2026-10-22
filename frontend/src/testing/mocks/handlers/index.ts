import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';

import { networkDelay } from '../utils';

import { authHandlers } from './auth';
import { horariosBrowserHandlers, horariosHandlers } from './horarios';

// Handlers de módulos que AÚN NO tienen backend. Se usan en el navegador (npm run dev con
// VITE_APP_ENABLE_API_MOCKING=true). Cuando un módulo tenga endpoints reales, quítalo de aquí.
export const browserHandlers = [
  ...horariosBrowserHandlers,
  http.get(`${env.API_URL}/healthcheck`, async () => {
    await networkDelay();
    return HttpResponse.json({ ok: true });
  }),
];

// En los tests (vitest) se simula todo, incluida la autenticación.
export const handlers = [
  ...authHandlers,
  ...horariosHandlers,
  ...browserHandlers.filter((h) => !horariosBrowserHandlers.includes(h)),
];
