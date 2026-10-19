import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';

import { networkDelay } from '../utils';

import { authHandlers } from './auth';
import { horariosHandlers } from './horarios';

// Registra aquí los handlers de cada módulo nuevo (uno por archivo).
export const handlers = [
  ...authHandlers,
  ...horariosHandlers,
  http.get(`${env.API_URL}/healthcheck`, async () => {
    await networkDelay();
    return HttpResponse.json({ ok: true });
  }),
];
