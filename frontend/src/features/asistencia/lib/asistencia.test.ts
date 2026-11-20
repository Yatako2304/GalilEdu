import { FilaAsistencia } from '@/types/api';

import {
  borradorDe,
  cambios,
  contar,
  fechaLarga,
  hoyEnLima,
  tardanzaSinHora,
} from './asistencia';

const fila = (
  matriculaId: string,
  estado: FilaAsistencia['estado'],
  horaLlegada: string | null = null,
): FilaAsistencia => ({
  matriculaId,
  estudianteId: `e-${matriculaId}`,
  nombres: 'Ana',
  apellidos: matriculaId,
  documento: '74125896',
  estado,
  horaLlegada,
  modificado: false,
  justificacion: null,
});

test('el botón de fecha dice el día como en Figma', () => {
  expect(fechaLarga('2026-10-02')).toBe('Viernes 02 oct 2026');
  expect(fechaLarga('2026-11-19')).toBe('Jueves 19 nov 2026');
});

test('"hoy" es la fecha de Lima aunque en UTC ya sea el día siguiente', () => {
  // 20/11 a las 03:00 UTC = 19/11 a las 22:00 en Lima.
  expect(hoyEnLima(new Date('2026-11-20T03:00:00Z'))).toBe('2026-11-19');
});

test('cuenta presentes, tardanzas, faltas y sin marcar del borrador', () => {
  const filas = [fila('a', null), fila('b', null), fila('c', null)];
  expect(
    contar(filas, {
      a: { estado: 'PRESENTE', hora: '' },
      b: { estado: 'TARDANZA', hora: '08:10' },
      c: { estado: null, hora: '' },
    }),
  ).toEqual({ presentes: 1, tardanzas: 1, faltas: 0, sinMarcar: 1, total: 3 });
});

test('solo cuenta como cambio lo que difiere de lo guardado', () => {
  const filas = [
    fila('a', 'PRESENTE'),
    fila('b', 'FALTA'),
    fila('c', 'TARDANZA', '08:10:00'),
  ];
  const borrador = {
    ...borradorDe(filas),
    b: { estado: 'TARDANZA' as const, hora: '09:10' },
    c: { estado: 'TARDANZA' as const, hora: '08:10' },
  };

  const lista = cambios(filas, borrador);
  expect(lista).toHaveLength(1);
  expect(lista[0].fila.matriculaId).toBe('b');
  expect(lista[0].antes.estado).toBe('FALTA');
});

test('una tardanza sin hora no se puede guardar', () => {
  expect(tardanzaSinHora({ estado: 'TARDANZA', hora: '' })).toBe(true);
  expect(tardanzaSinHora({ estado: 'TARDANZA', hora: '08:10' })).toBe(false);
  expect(tardanzaSinHora({ estado: 'FALTA', hora: '' })).toBe(false);
});
