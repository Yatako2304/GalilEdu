import { CeldaDelHorario, CursoOpcion, DiaCoordinador } from '../../types';
import {
  aSolicitudHorario,
  buscarCelda,
  conCelda,
  hayCambios,
  horasAsignadasEnBorrador,
  sinCelda,
} from '../borrador';
import { estadoDeError, esConflictoDeVersion, formatRango } from '../formato';

const celda = (
  dia: DiaCoordinador,
  numeroBloque: number,
  planCursoId = 'plan-mat',
  ambienteId = 'amb-1',
): CeldaDelHorario => ({
  dia,
  numeroBloque,
  planCursoId,
  curso: 'Curso',
  docente: null,
  ambiente: { id: ambienteId, nombre: 'Aula' },
  cruces: [],
});

const matematica: CursoOpcion = {
  planCursoId: 'plan-mat',
  nombre: 'Matemática',
  horasSemanales: 3,
  horasAsignadas: 2,
  docente: null,
};

test('conCelda reemplaza la del mismo día y bloque y sinCelda la quita', () => {
  const base = [celda('LUN', 1), celda('MAR', 2)];

  const cambiada = conCelda(base, celda('LUN', 1, 'plan-ing'));
  expect(cambiada).toHaveLength(2);
  expect(buscarCelda(cambiada, 'LUN', 1)?.planCursoId).toBe('plan-ing');

  expect(sinCelda(base, 'LUN', 1)).toEqual([celda('MAR', 2)]);
  expect(sinCelda(base, 'VIE', 4)).toEqual(base);
});

test('hayCambios detecta una celda nueva, quitada o con otro curso o ambiente', () => {
  const guardadas = [celda('LUN', 1), celda('MAR', 2)];

  expect(hayCambios(guardadas, [celda('MAR', 2), celda('LUN', 1)])).toBe(false);
  expect(hayCambios(guardadas, [...guardadas, celda('MIE', 3)])).toBe(true);
  expect(hayCambios(guardadas, [celda('LUN', 1)])).toBe(true);
  expect(
    hayCambios(guardadas, [celda('LUN', 1, 'plan-ing'), celda('MAR', 2)]),
  ).toBe(true);
  expect(
    hayCambios(guardadas, [
      celda('LUN', 1, 'plan-mat', 'amb-2'),
      celda('MAR', 2),
    ]),
  ).toBe(true);
});

test('el cuerpo del PUT lleva la versión y todas las celdas ordenadas, sin nombres ni docente', () => {
  const solicitud = aSolicitudHorario(
    [celda('MAR', 2), celda('LUN', 3), celda('LUN', 1)],
    3,
  );

  expect(solicitud).toEqual({
    version: 3,
    celdas: [
      {
        dia: 'LUN',
        numeroBloque: 1,
        planCursoId: 'plan-mat',
        ambienteId: 'amb-1',
      },
      {
        dia: 'LUN',
        numeroBloque: 3,
        planCursoId: 'plan-mat',
        ambienteId: 'amb-1',
      },
      {
        dia: 'MAR',
        numeroBloque: 2,
        planCursoId: 'plan-mat',
        ambienteId: 'amb-1',
      },
    ],
  });
  expect(aSolicitudHorario([], null)).toEqual({ version: null, celdas: [] });
});

test('las horas asignadas parten de las del backend, suman lo que el borrador cambió y no cuentan el bloque editado', () => {
  const guardadas = [celda('LUN', 1), celda('MAR', 1)];

  // Sin tocar nada: las del backend.
  expect(
    horasAsignadasEnBorrador(matematica, guardadas, guardadas, undefined),
  ).toBe(2);
  // El borrador agregó un bloque del curso: 3.
  expect(
    horasAsignadasEnBorrador(
      matematica,
      guardadas,
      [...guardadas, celda('MIE', 1)],
      undefined,
    ),
  ).toBe(3);
  // Se quitó uno: 1.
  expect(
    horasAsignadasEnBorrador(
      matematica,
      guardadas,
      [celda('LUN', 1)],
      undefined,
    ),
  ).toBe(1);
  // Al editar un bloque de ese curso no se cuenta a sí mismo.
  expect(
    horasAsignadasEnBorrador(matematica, guardadas, guardadas, celda('LUN', 1)),
  ).toBe(1);
  // Editar un bloque de otro curso no resta.
  expect(
    horasAsignadasEnBorrador(
      matematica,
      guardadas,
      guardadas,
      celda('LUN', 2, 'plan-ing'),
    ),
  ).toBe(2);
});

test('formatRango escribe el sufijo una vez cuando es el mismo', () => {
  expect(formatRango('08:00', '08:45')).toBe('8:00 - 8:45am');
  expect(formatRango('12:00', '12:45')).toBe('12:00 - 12:45pm');
  expect(formatRango('11:15', '12:00')).toBe('11:15am - 12:00pm');
});

test('un 409 de versión se reconoce por su estado y su mensaje', () => {
  const error = (status: number, message: string) => ({
    response: { status, data: { message } },
  });

  expect(
    esConflictoDeVersion(
      error(
        409,
        'Otra persona modificó el horario de 5.° de secundaria A. Recarga para ver los cambios.',
      ),
    ),
  ).toBe(true);
  expect(
    esConflictoDeVersion(error(409, 'Asigna un docente a Geometría.')),
  ).toBe(false);
  expect(
    esConflictoDeVersion(error(400, 'Otra persona modificó el horario')),
  ).toBe(false);
  expect(estadoDeError(error(404, 'x'))).toBe(404);
  expect(estadoDeError(new Error('red'))).toBeUndefined();
});
