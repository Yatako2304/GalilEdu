import { anios, etiquetaCuota, filtrarAlumnos, soles } from '../formato';

test('formatea montos en soles como en Figma', () => {
  expect(soles(5820)).toBe('S/ 5,820.00');
  expect(soles(0)).toBe('S/ 0.00');
});

test('la cuota vencida se muestra como Vencida aunque su estado sea PENDIENTE', () => {
  expect(etiquetaCuota({ estado: 'PENDIENTE', vencida: true })).toEqual({
    texto: 'Vencida',
    color: 'error',
  });
  expect(etiquetaCuota({ estado: 'PENDIENTE', vencida: false }).texto).toBe(
    'Pendiente',
  );
  expect(etiquetaCuota({ estado: 'PAGADA', vencida: false }).texto).toBe(
    'Pagada',
  );
});

test('el buscador encuentra por nombre sin tildes y por DNI', () => {
  const alumnos = [
    { nombre: 'Sofía Paredes Rojas', numeroDocumento: '74125896' },
    { nombre: 'Mateo Díaz Herrera', numeroDocumento: '78563214' },
  ];
  expect(filtrarAlumnos(alumnos, 'sofia').map((a) => a.nombre)).toEqual([
    'Sofía Paredes Rojas',
  ]);
  expect(filtrarAlumnos(alumnos, '785').map((a) => a.nombre)).toEqual([
    'Mateo Díaz Herrera',
  ]);
  expect(filtrarAlumnos(alumnos, '  ')).toHaveLength(2);
});

test('la antigüedad se muestra en años', () => {
  expect(anios(1)).toBe('1 año');
  expect(anios(3)).toBe('3 años');
});
