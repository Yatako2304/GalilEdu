import { horarioValeria2026 } from '@/testing/mocks/fixtures/horario-apoderado';

import {
  aMatrizHoraria,
  filasDeLaJornada,
  htmlDelHorario,
} from '../horario-apoderado';

// HU-51: el horario del backend (jornada + clases) en la forma que dibuja la matriz y en la página imprimible.

test('pone cada recreo justo después de su bloque', () => {
  const filas = filasDeLaJornada(horarioValeria2026.jornada!);
  expect(filas.map((f) => f.nombre)).toEqual([
    'Bloque 1',
    'Bloque 2',
    'Bloque 3',
    'Recreo 1',
    'Bloque 4',
    'Bloque 5',
    'Recreo 2',
    'Bloque 6',
    'Bloque 7',
  ]);
});

test('el docente va con "Prof." como en Figma', () => {
  const { sesiones } = aMatrizHoraria(horarioValeria2026.jornada!, [
    {
      dia: 'LUN',
      numeroBloque: 1,
      curso: 'Álgebra',
      docente: 'María Fernández Ríos',
      aula: 'Aula 101',
    },
  ]);
  expect(sesiones[0].cursos[0].docente).toBe('Prof. María Fernández Ríos');
});

test('convierte las clases a sesiones de la matriz (LUN → LUNES)', () => {
  const { bloques, sesiones } = aMatrizHoraria(horarioValeria2026.jornada!, [
    {
      dia: 'MIE',
      numeroBloque: 4,
      curso: 'Álgebra',
      docente: null,
      aula: 'Aula 101',
    },
  ]);
  expect(bloques).toHaveLength(9);
  expect(bloques[3]).toMatchObject({ nombre: 'Recreo 1', tipo: 'RECREO' });
  expect(sesiones).toEqual([
    {
      dia: 'MIERCOLES',
      bloqueId: 'bloque-4',
      tipo: 'ASIGNADO',
      cursos: [{ curso: 'Álgebra', docente: '', aula: 'Aula 101' }],
    },
  ]);
});

test('la página para descargar trae los datos del horario y escapa el texto', () => {
  const html = htmlDelHorario({
    ...horarioValeria2026,
    nombreCompleto: 'Ana <Prueba>',
  });
  expect(html).toContain('Horario 2026 · Ana &lt;Prueba&gt;');
  expect(html).toContain('5.° de secundaria · Sección A');
  expect(html).toContain('Lucía Ayala Prado · 987456123');
  expect(html).toContain('<th>Lunes</th>');
  expect(html).toContain('Innovación y descubrimiento');
  expect(html.match(/class="recreo"/g)).toHaveLength(2);
});
