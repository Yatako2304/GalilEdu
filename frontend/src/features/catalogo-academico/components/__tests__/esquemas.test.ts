import { areaSchema, cursoSchema, textoGrados } from '../esquemas';

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
