import { test, expect } from '@playwright/test';

// Usa los usuarios de prueba del backend simulado (src/testing/mocks/seed.ts)
test('el estudiante inicia sesión y ve su horario', async ({ page }) => {
  await page.goto('/');
  await page.waitForURL('**/auth/login');

  await page.getByLabel('Usuario').fill('estudiante');
  await page.getByLabel('Contraseña', { exact: true }).fill('galiledu123');
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  await page.waitForURL('**/app/estudiante');

  await page.getByRole('link', { name: 'Horarios' }).click();
  await page.waitForURL('**/app/estudiante/horarios');

  await expect(page.getByRole('heading', { name: 'Horario' })).toBeVisible();
  await expect(page.getByText('Valeria Sánchez Ferrara').first()).toBeVisible();
  await expect(
    page.getByRole('table', { name: 'Matriz horaria' }),
  ).toBeVisible();

  await page.getByRole('button', { name: 'Cerrar sesión' }).click();
  await page.waitForURL('**/auth/login');
});

test('un usuario no puede entrar a las rutas de otro rol', async ({ page }) => {
  await page.goto('/auth/login');
  await page.getByLabel('Usuario').fill('estudiante');
  await page.getByLabel('Contraseña', { exact: true }).fill('galiledu123');
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  await page.waitForURL('**/app/estudiante');

  await page.goto('/app/admin/usuarios');
  await page.waitForURL('**/app/estudiante');
});
