import { env } from '@/config/env';

const disableMocking = async () => {
  if (!('serviceWorker' in navigator)) return;

  const registrations = await navigator.serviceWorker.getRegistrations();
  const mockRegistrations = registrations.filter((registration) =>
    [registration.active, registration.waiting, registration.installing].some(
      (worker) => worker?.scriptURL.includes('/mockServiceWorker.js'),
    ),
  );

  if (mockRegistrations.length === 0) return;

  await Promise.all(
    mockRegistrations.map((registration) => registration.unregister()),
  );

  // El worker deja de controlar esta pestaña después de una navegación.
  if (
    navigator.serviceWorker.controller?.scriptURL.includes(
      '/mockServiceWorker.js',
    )
  ) {
    window.location.reload();
  }
};

export const enableMocking = async () => {
  if (env.ENABLE_API_MOCKING) {
    const { worker } = await import('./browser');
    const { initializeDb } = await import('./db');
    const { seedDb } = await import('./seed');
    await initializeDb();
    await seedDb();
    return worker.start({ onUnhandledRequest: 'bypass' });
  }

  return disableMocking();
};
