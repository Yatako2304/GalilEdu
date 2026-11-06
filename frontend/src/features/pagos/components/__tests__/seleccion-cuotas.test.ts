import { CuotaPorPagar } from '@/types/api';

import { alternarCuota, unirNombres } from '../seleccion-cuotas';

// HU-41 · Selección de cuotas para el pago en línea (CAHU41-1 a CAHU41-5).

const cuota = (
  id: string,
  nombre: string,
  extra: Partial<CuotaPorPagar> = {},
): CuotaPorPagar => ({
  cuotaId: id,
  concepto: 'PENSION',
  numero: null,
  nombre,
  anio: 2026,
  fechaVencimiento: '2026-09-30',
  capital: 600,
  recargo: 0,
  total: 600,
  vencida: false,
  estado: 'PENDIENTE',
  seleccionable: true,
  requiere: [],
  ...extra,
});

const septiembre = cuota('sep', 'Pensión de septiembre', {
  recargo: 12,
  total: 612,
  vencida: true,
});
const octubre = cuota('oct', 'Pensión de octubre', {
  fechaVencimiento: '2026-10-31',
  requiere: ['sep'],
});
const noviembre = cuota('nov', 'Pensión de noviembre', {
  fechaVencimiento: '2026-11-30',
  requiere: ['sep', 'oct'],
});

describe('alternarCuota (las pensiones se pagan en orden)', () => {
  const todas = [septiembre, octubre, noviembre];

  test('marcar una pensión marca también las anteriores pendientes', () => {
    expect([...alternarCuota(new Set(), noviembre, todas)].sort()).toEqual([
      'nov',
      'oct',
      'sep',
    ]);
  });

  test('desmarcar una pensión desmarca las que dependen de ella', () => {
    const marcadas = new Set(['sep', 'oct', 'nov']);
    expect([...alternarCuota(marcadas, octubre, todas)]).toEqual(['sep']);
  });

  test('una cuota que no se puede pagar no se marca', () => {
    const enProceso = cuota('x', 'Pensión de agosto', {
      seleccionable: false,
      estado: 'EN_PROCESO',
    });
    expect(alternarCuota(new Set(), enProceso, [enProceso]).size).toBe(0);
  });
});

test('une los nombres como en Figma', () => {
  expect(unirNombres(['Pensión de agosto', 'Pensión de septiembre'])).toBe(
    'Pensión de agosto y Pensión de septiembre',
  );
  expect(unirNombres(['A', 'B', 'C'])).toBe('A, B y C');
});
