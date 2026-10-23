import {
  ESTADO_PERIODO_MATRICULA,
  diaYMes,
  diaYMesCorto,
  diasEntre,
  etiquetaHora,
  fechaHoraLima,
  fechaLima,
  horaLima,
  lineaDeApertura,
  matriculasDeLaFila,
  matriculasDeVacantes,
  normalizarHora,
  opcionesDeHora,
  rangoCorto,
  rangoDeMatricula,
  siONo,
  soles,
  textoExtemporanea,
} from '../formato';

// Textos y fechas de la pantalla 13 "Periodos de matrícula" (HU-23)

describe('fechas en hora de Lima', () => {
  test('un instante UTC se ve como la fecha y la hora del colegio, como en Figma', () => {
    // 15:00 UTC son las 10:00 a.m. en Lima.
    expect(fechaLima('2026-12-01T15:00:00Z')).toBe('01/12/2026');
    expect(horaLima('2026-12-01T15:00:00Z')).toBe('10:00 a.m.');
    expect(fechaHoraLima('2027-02-15T15:00:00Z')).toBe('15/02/2027 10:00 a.m.');
  });

  test('cerca de la medianoche cambia de día igual que en Lima, no que en UTC', () => {
    // 04:59 UTC del 01/11 son las 11:59 p.m. del 31/10 en Lima.
    expect(fechaLima('2026-11-01T04:59:00Z')).toBe('31/10/2026');
    expect(horaLima('2026-11-01T04:59:00Z')).toBe('11:59 p.m.');
  });

  test('las horas de la mañana y del mediodía', () => {
    expect(horaLima('2026-09-15T13:00:00Z')).toBe('8:00 a.m.');
    expect(horaLima('2026-09-15T17:00:00Z')).toBe('12:00 p.m.');
    expect(horaLima('2026-09-15T05:00:00Z')).toBe('12:00 a.m.');
  });
});

describe('lineaDeApertura', () => {
  const periodo = {
    inicio: '2026-12-01T15:00:00Z',
    cierre: '2027-02-15T15:00:00Z',
  };

  test('un periodo abierto, como en Figma', () => {
    expect(lineaDeApertura({ ...periodo, estado: 'ABIERTO' })).toBe(
      'Abierto el 01/12/2026 10:00 a.m. · cierra el 15/02/2027 10:00 a.m.',
    );
  });

  test('un periodo programado dice cuándo abre', () => {
    expect(lineaDeApertura({ ...periodo, estado: 'PROGRAMADO' })).toBe(
      'Abre el 01/12/2026 10:00 a.m. · cierra el 15/02/2027 10:00 a.m.',
    );
  });

  test('un periodo cerrado dice cuándo cerró', () => {
    expect(lineaDeApertura({ ...periodo, estado: 'CERRADO' })).toBe(
      'Cerró el 15/02/2027 10:00 a.m.',
    );
  });
});

test('los estados de la tabla usan los colores de Figma', () => {
  expect(ESTADO_PERIODO_MATRICULA).toEqual({
    PROGRAMADO: { texto: 'Programado', color: 'warning' },
    ABIERTO: { texto: 'Abierto', color: 'success' },
    CERRADO: { texto: 'Cerrado', color: 'primary' },
  });
});

test('la columna Matrículas muestra "-" solo mientras el periodo no abre', () => {
  expect(matriculasDeLaFila({ estado: 'PROGRAMADO', matriculas: 0 })).toBe('-');
  expect(matriculasDeLaFila({ estado: 'ABIERTO', matriculas: 0 })).toBe('0');
  expect(matriculasDeLaFila({ estado: 'ABIERTO', matriculas: 8 })).toBe('8');
  expect(matriculasDeLaFila({ estado: 'CERRADO', matriculas: 331 })).toBe(
    '331',
  );
});

test('textos de la tarjeta vigente y del modal de configuración', () => {
  expect(matriculasDeVacantes(8, 300)).toBe('8 de 300 vacantes');
  expect(matriculasDeVacantes(0, 1)).toBe('0 de 1 vacante');
  expect(soles(450)).toBe('S/ 450.00');
  expect(soles(1234.5)).toBe('S/ 1,234.50');
  expect(rangoDeMatricula('2026-09-15', '2026-10-31')).toBe(
    '15/09/2026 – 31/10/2026',
  );
  expect(siONo(true)).toBe('Sí');
  expect(siONo(false)).toBe('No');
  expect(textoExtemporanea(true)).toBe('Permitida tras el cierre');
  expect(textoExtemporanea(false)).toBe('No permitida');
});

describe('formulario: fechas cortas y duración', () => {
  test('día y mes como en la tarjeta "Periodo de matrícula" de Figma', () => {
    expect(diaYMes('2026-12-01')).toBe('01 dic');
    expect(diaYMes('2027-02-15')).toBe('15 feb');
    expect(rangoCorto('2026-12-01', '2027-02-15')).toBe('01 dic – 15 feb');
    expect(diaYMesCorto('2026-12-01')).toBe('01-dic');
  });

  test('los días son los que hay entre las dos fechas (76, no los 71 de Figma)', () => {
    expect(diasEntre('2026-12-01', '2027-02-15')).toBe(76);
    expect(diasEntre('2026-12-01', '2026-12-02')).toBe(1);
  });
});

describe('formulario: horas', () => {
  test('etiquetas de 12 horas, como "10:00 a.m." de Figma', () => {
    expect(etiquetaHora('00:00')).toBe('12:00 a.m.');
    expect(etiquetaHora('10:00')).toBe('10:00 a.m.');
    expect(etiquetaHora('12:00')).toBe('12:00 p.m.');
    expect(etiquetaHora('13:30')).toBe('1:30 p.m.');
    expect(etiquetaHora('23:59')).toBe('11:59 p.m.');
  });

  test('el backend manda segundos y se reciben sin ellos', () => {
    expect(normalizarHora('08:00:00')).toBe('08:00');
    expect(normalizarHora('23:59')).toBe('23:59');
  });

  test('cada 30 minutos del día, y las 11:59 p. m. al final', () => {
    const opciones = opcionesDeHora();

    expect(opciones).toHaveLength(49);
    expect(opciones[0]).toEqual({ label: '12:00 a.m.', value: '00:00' });
    expect(opciones[1].value).toBe('00:30');
    expect(opciones[20]).toEqual({ label: '10:00 a.m.', value: '10:00' });
    expect(opciones.at(-2)?.value).toBe('23:30');
    expect(opciones.at(-1)).toEqual({ label: '11:59 p.m.', value: '23:59' });
  });

  test('si el valor actual no está en la lista, se agrega en su lugar', () => {
    const opciones = opcionesDeHora('08:15:00');

    expect(opciones).toHaveLength(50);
    expect(opciones.map((o) => o.value).slice(16, 19)).toEqual([
      '08:00',
      '08:15',
      '08:30',
    ]);
    expect(opcionesDeHora('10:00:00')).toHaveLength(49);
  });
});
