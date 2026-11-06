import {
  clasesSecundaria,
  primaria,
  secundaria,
} from '@/testing/mocks/fixtures/horario-docente';

import { HorarioPorEstructura } from '../../api/horario-docente';
import { aMatrizDelDocente } from '../horario-docente';

// HU-49: el horario de una estructura del backend en la forma que dibuja la matriz.

const conClases = (
  clases: HorarioPorEstructura['clases'],
  base: HorarioPorEstructura = secundaria,
): HorarioPorEstructura => ({ ...base, clases });

test('pone cada recreo justo después de su bloque', () => {
  const { bloques } = aMatrizDelDocente(secundaria);

  expect(bloques.map((b) => b.nombre)).toEqual([
    'Bloque 1',
    'Bloque 2',
    'Bloque 3',
    'Recreo 1',
    'Bloque 4',
  ]);
  expect(bloques.map((b) => b.tipo)).toEqual([
    'CLASE',
    'CLASE',
    'CLASE',
    'RECREO',
    'CLASE',
  ]);
  expect(bloques[0]).toMatchObject({ horaInicio: '08:00', horaFin: '08:45' });
  expect(bloques.map((b) => b.orden)).toEqual([0, 1, 2, 3, 4]);
});

test('cada estructura usa su propia jornada', () => {
  const { bloques } = aMatrizDelDocente(primaria);

  // El recreo de primaria va después del bloque 2, no del 3.
  expect(bloques.map((b) => b.nombre)).toEqual([
    'Bloque 1',
    'Bloque 2',
    'Recreo 1',
    'Bloque 3',
  ]);
  expect(bloques[0].horaInicio).toBe('07:45');
});

test('convierte los días del backend a los de la matriz (LUN → LUNES)', () => {
  const { sesiones } = aMatrizDelDocente(
    conClases(
      clasesSecundaria.filter((c) => c.dia === 'LUN' || c.dia === 'VIE'),
    ),
  );

  expect(sesiones.map((s) => s.dia)).toEqual(['LUNES', 'LUNES', 'VIERNES']);
  expect(sesiones.map((s) => s.bloqueId)).toEqual([
    'bloque-1',
    'bloque-2',
    'bloque-4',
  ]);
  expect(sesiones.every((s) => s.tipo === 'ASIGNADO')).toBe(true);
});

test('la sección y el aula van en el curso, y el docente va vacío', () => {
  const { sesiones } = aMatrizDelDocente(
    conClases([
      {
        dia: 'MAR',
        numeroBloque: 3,
        curso: 'Geometría',
        seccion: '2.° A',
        ambiente: 'A-502',
      },
    ]),
  );

  expect(sesiones).toEqual([
    {
      dia: 'MARTES',
      bloqueId: 'bloque-3',
      tipo: 'ASIGNADO',
      cursos: [
        { curso: 'Geometría', docente: '', aula: 'A-502', seccion: '2.° A' },
      ],
    },
  ]);
});

test('dos clases en la misma celda quedan juntas en una sola sesión', () => {
  const { sesiones } = aMatrizDelDocente(
    conClases([
      {
        dia: 'LUN',
        numeroBloque: 1,
        curso: 'Álgebra',
        seccion: '1.° A',
        ambiente: 'A-401',
      },
      {
        dia: 'LUN',
        numeroBloque: 1,
        curso: 'Álgebra',
        seccion: '1.° B',
        ambiente: 'A-402',
      },
      {
        dia: 'LUN',
        numeroBloque: 2,
        curso: 'Geometría',
        seccion: '2.° A',
        ambiente: 'A-502',
      },
    ]),
  );

  expect(sesiones).toHaveLength(2);
  expect(sesiones[0].cursos.map((c) => c.seccion)).toEqual(['1.° A', '1.° B']);
  expect(sesiones[0].cursos.map((c) => c.aula)).toEqual(['A-401', 'A-402']);
  expect(sesiones[1].cursos).toHaveLength(1);
});

test('las clases del sábado se descartan porque la matriz solo dibuja de lunes a viernes', () => {
  const { sesiones } = aMatrizDelDocente(
    conClases([
      {
        dia: 'SAB',
        numeroBloque: 1,
        curso: 'Álgebra',
        seccion: '1.° A',
        ambiente: 'A-401',
      },
      {
        dia: 'LUN',
        numeroBloque: 1,
        curso: 'Álgebra',
        seccion: '1.° A',
        ambiente: 'A-401',
      },
    ]),
  );

  expect(sesiones.map((s) => s.dia)).toEqual(['LUNES']);
});

test('sin clases deja las filas y ninguna sesión', () => {
  const { bloques, sesiones } = aMatrizDelDocente(conClases([]));

  expect(bloques).toHaveLength(5);
  expect(sesiones).toEqual([]);
});
