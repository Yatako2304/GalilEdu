import { destinoSeguro } from '../auth';

test('después del login solo se vuelve a pantallas internas de la app', () => {
  expect(destinoSeguro('/app/apoderado/matricula')).toBe(
    '/app/apoderado/matricula',
  );
  expect(destinoSeguro(null)).toBeNull();
  expect(destinoSeguro('https://sitio-malicioso.com')).toBeNull();
  expect(destinoSeguro('//sitio-malicioso.com')).toBeNull();
  expect(destinoSeguro('/app//sitio-malicioso.com')).toBeNull();
  expect(destinoSeguro('javascript:alert(1)')).toBeNull();
  expect(destinoSeguro('/auth/login')).toBeNull();
});
