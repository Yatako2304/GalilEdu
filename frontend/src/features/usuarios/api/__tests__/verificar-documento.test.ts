import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';

import { verificarDocumento } from '../verificar-documento';

const MENSAJE = 'Ya existe un usuario registrado con este documento';

const responder = (status: number, cuerpo: object) =>
  server.use(
    http.get(`${env.API_URL}/estudiantes/personas`, () =>
      HttpResponse.json(cuerpo, { status }),
    ),
  );

test('documento libre', async () => {
  responder(404, { message: 'No está registrada' });
  expect(await verificarDocumento('DNI', '11111111', true)).toBeNull();
});

test('documento ya registrado (usuario, persona o estudiante): mismo mensaje', async () => {
  responder(200, { personaId: 'p1', tieneCuenta: true });
  expect(await verificarDocumento('DNI', '11111111', true)).toBe(MENSAJE);
  responder(200, { personaId: 'p1', tieneCuenta: false });
  expect(await verificarDocumento('DNI', '11111111', true)).toBe(MENSAJE);
  responder(409, {
    message: 'El documento corresponde a un estudiante, no a un familiar',
  });
  expect(await verificarDocumento('DNI', '11111111', true)).toBe(MENSAJE);
});

test('documento de un usuario eliminado: se puede volver a registrar, pero no usar al editar', async () => {
  responder(200, { personaId: 'p1', tieneCuenta: true, cuentaEliminada: true });
  expect(await verificarDocumento('DNI', '11111111', true)).toBeNull();
  expect(await verificarDocumento('DNI', '11111111', false)).toBe(MENSAJE);
});

test('con formato inválido no consulta', async () => {
  expect(await verificarDocumento('DNI', '123', true)).toBeNull();
});
