import { env } from '@/config/env';

export const enableMocking = async () => {
  if (env.ENABLE_API_MOCKING) {
    const { worker } = await import('./browser');
    const { initializeDb } = await import('./db');
    const { seedDb } = await import('./seed');
    await initializeDb();
    await seedDb();
    return worker.start({ onUnhandledRequest: 'bypass' });
  }
};
