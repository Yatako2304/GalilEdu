import {
  AnioParaHorarios,
  GradoParaHorarios,
  JornadaDelDia,
} from '../../types';
import {
  aMinutos,
  anioParaReservar,
  calcularHoraFin,
  deMinutos,
  diaDeLaSemana,
  duracionDelBloque,
  duracionEnMinutos,
  erroresDeCampos,
  esConflictoDeVersionDeReserva,
  esDiaLectivo,
  finDeJornada,
  formatHora12,
  gradosConSeccionesActivas,
  mensajeDeDiaNoLectivo,
  opcionesDeDuracion,
  opcionesDeHora,
  partirMensaje,
  seccionesActivas,
} from '../reservas';

// HU-48: la lógica pura de la pantalla de reservas.

/** Lunes a viernes, bloques de 45 min desde las 7:30 con un receso de 20 min después del 3.° (como EH-SEC-26). */
const JORNADA: JornadaDelDia = {
  dias: ['LUN', 'MAR', 'MIE', 'JUE', 'VIE'],
  bloques: [
    { numero: 1, inicio: '07:30', fin: '08:15' },
    { numero: 2, inicio: '08:15', fin: '09:00' },
    { numero: 3, inicio: '09:00', fin: '09:45' },
    { numero: 4, inicio: '10:05', fin: '10:50' },
    { numero: 5, inicio: '10:50', fin: '11:35' },
  ],
  recesos: [{ despuesDeBloque: 3, inicio: '09:45', fin: '10:05' }],
};

const errorDe = (status: number, data: unknown) => ({
  response: { status, data },
});

describe('formato de horas', () => {
  test('formatHora12: dos dígitos, espacio y am/pm', () => {
    expect(formatHora12('08:00')).toBe('08:00 am');
    expect(formatHora12('09:45')).toBe('09:45 am');
    expect(formatHora12('12:00')).toBe('12:00 pm');
    expect(formatHora12('13:30')).toBe('01:30 pm');
    expect(formatHora12('00:15')).toBe('12:15 am');
  });

  test('minutos y horas se convierten de ida y vuelta', () => {
    expect(aMinutos('08:15')).toBe(495);
    expect(deMinutos(495)).toBe('08:15');
    expect(deMinutos(aMinutos('13:05'))).toBe('13:05');
  });

  test('calcularHoraFin suma la duración a la hora de inicio', () => {
    expect(calcularHoraFin('08:15', 45)).toBe('09:00');
    expect(calcularHoraFin('08:15', 90)).toBe('09:45');
    expect(calcularHoraFin('10:05', 135)).toBe('12:20');
  });

  test('duracionEnMinutos es la resta de dos horas', () => {
    expect(duracionEnMinutos('08:15', '09:45')).toBe(90);
  });
});

describe('opciones de la jornada', () => {
  test('finDeJornada y duracionDelBloque salen de los bloques', () => {
    expect(finDeJornada(JORNADA)).toBe('11:35');
    expect(duracionDelBloque(JORNADA)).toBe(45);
    expect(finDeJornada({ ...JORNADA, bloques: [] })).toBeNull();
    expect(duracionDelBloque({ ...JORNADA, bloques: [] })).toBe(0);
  });

  test('las horas son los inicios de bloque, en formato de 12 horas', () => {
    expect(opcionesDeHora(JORNADA)).toEqual([
      { value: '07:30', label: '07:30 am' },
      { value: '08:15', label: '08:15 am' },
      { value: '09:00', label: '09:00 am' },
      { value: '10:05', label: '10:05 am' },
      { value: '10:50', label: '10:50 am' },
    ]);
  });

  test('al editar, una hora guardada que no es inicio de bloque se agrega en su lugar', () => {
    const opciones = opcionesDeHora(JORNADA, '08:30');

    expect(opciones.map((o) => o.value)).toEqual([
      '07:30',
      '08:15',
      '08:30',
      '09:00',
      '10:05',
      '10:50',
    ]);
  });

  test('una hora guardada que ya es inicio de bloque no se repite', () => {
    expect(opcionesDeHora(JORNADA, '08:15')).toHaveLength(5);
  });

  test('las duraciones son 1×, 2× y 3× la del bloque', () => {
    expect(opcionesDeDuracion(JORNADA)).toEqual([
      { value: 45, label: '45 min', disabled: false },
      { value: 90, label: '90 min', disabled: false },
      { value: 135, label: '135 min', disabled: false },
    ]);
  });

  test('las duraciones que pasan del fin de la jornada salen deshabilitadas', () => {
    // La jornada termina a las 11:35: desde las 10:50 solo cabe 1× (45 min); desde las 10:05, hasta 2× (90 min).
    expect(opcionesDeDuracion(JORNADA, '10:50').map((o) => o.disabled)).toEqual(
      [false, true, true],
    );
    expect(opcionesDeDuracion(JORNADA, '10:05').map((o) => o.disabled)).toEqual(
      [false, false, true],
    );
    expect(opcionesDeDuracion(JORNADA, '07:30').map((o) => o.disabled)).toEqual(
      [false, false, false],
    );
  });

  test('terminar justo al final de la jornada se permite', () => {
    // 10:50 + 45 = 11:35.
    expect(opcionesDeDuracion(JORNADA, '10:50')[0].disabled).toBe(false);
  });

  test('al editar, una duración guardada que no es múltiplo se agrega y queda habilitada', () => {
    const opciones = opcionesDeDuracion(JORNADA, '10:50', 60);

    expect(opciones.map((o) => o.value)).toEqual([45, 60, 90, 135]);
    expect(opciones.find((o) => o.value === 60)?.disabled).toBe(false);
  });

  test('una jornada sin bloques no ofrece duraciones', () => {
    expect(opcionesDeDuracion({ ...JORNADA, bloques: [] })).toEqual([]);
  });
});

describe('día lectivo', () => {
  test('diaDeLaSemana da el código de LUN a SAB y null el domingo', () => {
    expect(diaDeLaSemana('2026-11-23')).toBe('LUN');
    expect(diaDeLaSemana('2026-11-25')).toBe('MIE');
    expect(diaDeLaSemana('2026-11-28')).toBe('SAB');
    expect(diaDeLaSemana('2026-11-22')).toBeNull();
  });

  test('esDiaLectivo compara con los días de la jornada', () => {
    expect(esDiaLectivo('2026-11-20', JORNADA)).toBe(true);
    expect(esDiaLectivo('2026-11-28', JORNADA)).toBe(false);
    expect(esDiaLectivo('2026-11-22', JORNADA)).toBe(false);
    expect(
      esDiaLectivo('2026-11-28', {
        ...JORNADA,
        dias: [...JORNADA.dias, 'SAB'],
      }),
    ).toBe(true);
  });
});

describe('mensaje de día no lectivo', () => {
  test('nombra el día en minúscula, como el backend', () => {
    expect(mensajeDeDiaNoLectivo('2026-11-28')).toBe(
      'El sábado no es un día de clases de esta sección.',
    );
    expect(mensajeDeDiaNoLectivo('2026-11-22')).toBe(
      'El domingo no es un día de clases de esta sección.',
    );
  });
});

describe('año, grado y sección', () => {
  const anio = (
    id: string,
    numero: number,
    estado: AnioParaHorarios['estado'],
  ): AnioParaHorarios => ({ id, anio: numero, estado });

  test('se reserva en el año ACTIVO', () => {
    const anios = [
      anio('a27', 2027, 'PLANIFICADO'),
      anio('a26', 2026, 'ACTIVO'),
      anio('a25', 2025, 'CERRADO'),
    ];

    expect(anioParaReservar(anios)?.id).toBe('a26');
  });

  test('sin año ACTIVO, el más reciente que no esté CERRADO', () => {
    const anios = [
      anio('a25', 2025, 'CERRADO'),
      anio('a27', 2027, 'PLANIFICADO'),
      anio('a28', 2028, 'PLANIFICADO'),
    ];

    expect(anioParaReservar(anios)?.id).toBe('a28');
  });

  test('si todos están CERRADOS o no hay años, no hay año para reservar', () => {
    expect(anioParaReservar([anio('a25', 2025, 'CERRADO')])).toBeUndefined();
    expect(anioParaReservar([])).toBeUndefined();
  });

  const grado = (
    codigo: string,
    secciones: { id: string; estado: 'ACTIVA' | 'INACTIVA' }[],
  ): GradoParaHorarios => ({
    codigo,
    nombre: codigo,
    nivel: 'SECUNDARIA',
    numero: 1,
    cantidadSeccionesActivas: secciones.filter((s) => s.estado === 'ACTIVA')
      .length,
    secciones: secciones.map((s) => ({ ...s, nombre: s.id })),
  });

  test('seccionesActivas descarta las INACTIVAS', () => {
    const g = grado('SEC-1', [
      { id: 'A', estado: 'ACTIVA' },
      { id: 'B', estado: 'INACTIVA' },
    ]);

    expect(seccionesActivas(g).map((s) => s.id)).toEqual(['A']);
    expect(seccionesActivas(undefined)).toEqual([]);
  });

  test('gradosConSeccionesActivas deja solo los grados con al menos una activa', () => {
    const grados = [
      grado('SEC-1', [{ id: 'A', estado: 'ACTIVA' }]),
      grado('SEC-2', [{ id: 'A', estado: 'INACTIVA' }]),
      grado('PRI-1', []),
    ];

    expect(gradosConSeccionesActivas(grados).map((g) => g.codigo)).toEqual([
      'SEC-1',
    ]);
  });
});

describe('errores del backend', () => {
  test('partirMensaje separa el título de los tres cruces', () => {
    expect(
      partirMensaje(
        'Ambiente no disponible: Laboratorio de ciencias ya está reservado por 1.° A.',
      ),
    ).toEqual({
      titulo: 'Ambiente no disponible',
      texto: 'Laboratorio de ciencias ya está reservado por 1.° A.',
    });
    expect(
      partirMensaje('Responsable no disponible: María ya dicta Álgebra.'),
    ).toEqual({
      titulo: 'Responsable no disponible',
      texto: 'María ya dicta Álgebra.',
    });
    expect(
      partirMensaje('Sección no disponible: 1.° A ya tiene reservado X.'),
    ).toEqual({
      titulo: 'Sección no disponible',
      texto: '1.° A ya tiene reservado X.',
    });
  });

  test('un mensaje sin esos títulos se deja entero y sin título', () => {
    expect(
      partirMensaje('1.° R1 tiene Álgebra en LAB-02 el lunes de 8:00 a 8:45.'),
    ).toEqual({
      texto: '1.° R1 tiene Álgebra en LAB-02 el lunes de 8:00 a 8:45.',
    });
    expect(
      partirMensaje('No se puede reservar un espacio en un año cerrado.'),
    ).toEqual({ texto: 'No se puede reservar un espacio en un año cerrado.' });
  });

  test('el título solo cuenta al principio y seguido de «: »', () => {
    expect(
      partirMensaje('Hubo un problema. Ambiente no disponible: algo'),
    ).toEqual({ texto: 'Hubo un problema. Ambiente no disponible: algo' });
    expect(partirMensaje('Ambiente no disponible sin dos puntos')).toEqual({
      texto: 'Ambiente no disponible sin dos puntos',
    });
  });

  test('el 409 de versión se reconoce por el estado y el texto', () => {
    const mensaje =
      'La reserva cambió mientras la editabas. Vuelve a cargarla.';

    expect(
      esConflictoDeVersionDeReserva(errorDe(409, { message: mensaje })),
    ).toBe(true);
    expect(
      esConflictoDeVersionDeReserva(errorDe(400, { message: mensaje })),
    ).toBe(false);
    expect(
      esConflictoDeVersionDeReserva(
        errorDe(409, { message: 'La reserva ya fue cancelada.' }),
      ),
    ).toBe(false);
    // El conflicto de versión del horario (HU-45) es otro.
    expect(
      esConflictoDeVersionDeReserva(
        errorDe(409, { message: 'Otra persona modificó el horario de 5.° A.' }),
      ),
    ).toBe(false);
  });

  test('erroresDeCampos devuelve los errores por campo del 400, o undefined', () => {
    expect(
      erroresDeCampos(
        errorDe(400, {
          message: 'Elige el ambiente.',
          errores: { ambienteId: 'Elige el ambiente.' },
        }),
      ),
    ).toEqual({ ambienteId: 'Elige el ambiente.' });
    expect(
      erroresDeCampos(errorDe(400, { message: 'x', errores: {} })),
    ).toBeUndefined();
    expect(erroresDeCampos(errorDe(409, { message: 'x' }))).toBeUndefined();
    expect(erroresDeCampos(new Error('sin red'))).toBeUndefined();
  });
});
