import * as React from 'react';
import { createRoot } from 'react-dom/client';

import './index.css';
import { App } from './app';
import { enableMocking } from './testing/mocks';

const root = document.getElementById('root');
if (!root) throw new Error('No root element found');

// Si el navegador no permite el Service Worker de MSW, la app arranca igual:
// solo dejan de estar simulados los módulos que aún no tienen backend.
enableMocking()
  .catch((error) => console.warn('No se pudo iniciar MSW', error))
  .finally(() => {
    createRoot(root).render(
      <React.StrictMode>
        <App />
      </React.StrictMode>,
    );
  });
