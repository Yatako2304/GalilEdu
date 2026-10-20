import {
  areaSchema,
  cursoSchema,
  errorDeRangos,
  textoGrados,
} from '../esquemas';

test('el nombre del área es obligatorio y la descripción vacía se envía como null', () => {
  expect(areaSchema.safeParse({ nombre: '  ', descripcion: '' }).success).toBe(
    false,
  );
  expect(areaSchema.parse({ nombre: ' Tutoría ', descripcion: '  ' })).toEqual({
    nombre: 'Tutoría',
    descripcion: null,
  });
});

test('el curso debe dictarse en al menos un grado', () => {
  const r = cursoSchema.safeParse({ nombre: 'Estadística', grados: [] });
  expect(r.success).toBe(false);
  expect(r.error?.issues[0].message).toBe('Elige al menos un grado.');
});

test('los grados se muestran agrupados por nivel', () => {
  expect(textoGrados(['PRI-5', 'PRI-6', 'SEC-1'])).toBe(
    'Primaria 5.°, 6.° · Secundaria 1.°',
  );
  expect(textoGrados(['SEC-1', 'SEC-2'])).toBe('Secundaria 1.°, 2.°');
});

test('los rangos de la escala deben cubrir de 0 a 20 sin vacíos ni cruces', () => {
  const figma = [
    { nivel: 'AD', notaDesde: 18, notaHasta: 20 },
    { nivel: 'A', notaDesde: 14, notaHasta: 17 },
    { nivel: 'B', notaDesde: 11, notaHasta: 13 },
    { nivel: 'C', notaDesde: 0, notaHasta: 10 },
  ];
  expect(errorDeRangos(figma)).toBeNull();
  expect(
    errorDeRangos(
      figma.map((n) => (n.nivel === 'A' ? { ...n, notaDesde: 15 } : n)),
    ),
  ).toBe('Falta cubrir de 14 a 14 antes de A.');
  expect(
    errorDeRangos(
      figma.map((n) => (n.nivel === 'B' ? { ...n, notaHasta: 14 } : n)),
    ),
  ).toBe('El rango de A se cruza con el nivel anterior.');
});
