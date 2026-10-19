import { setupWorker } from 'msw/browser';

import { browserHandlers } from './handlers';

// En el navegador solo se simulan los módulos que aún no tienen backend.
// Lo demás (auth, usuarios, estudiantes) pasa al backend real (onUnhandledRequest: 'bypass').
export const worker = setupWorker(...browserHandlers);
