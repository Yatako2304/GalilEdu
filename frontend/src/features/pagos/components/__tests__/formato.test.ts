import { etiquetaCuota, soles } from '../formato';

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
