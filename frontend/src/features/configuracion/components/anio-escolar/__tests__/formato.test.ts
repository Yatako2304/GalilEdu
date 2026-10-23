import { AnioEscolarResumen } from '@/types/api';

import { anioActivo } from '../../../api/anios-escolares';
import {
  ESTADO_ANIO,
  ESTADO_PERIODO,
  alumnosMatriculados,
  aniosRegistrados,
  cantidadDePeriodos,
  codigosDeEstructuras,
  datosDePeriodos,
  diasRestantes,
  nombreRegimen,
  periodosAlAnio,
  rangoFechas,
  subtituloAnio,
} from '../formato';

const anio = (
  anioNumero: number,
  estado: AnioEscolarResumen['estado'],
): AnioEscolarResumen => ({
  id: String(anioNumero),
  anio: anioNumero,
  fechaInicio: `${anioNumero}-03-02`,
  fechaFin: `${anioNumero}-12-18`,
  regimen: 'BIMESTRAL',
  cantidadPeriodos: 4,
  seccionesActivas: 10,
  estructurasHorarias: 2,
  estado,
});

describe('textos del régimen (Figma: CI-01 y modal Nuevo año escolar)', () => {
  test('nombre del régimen y periodos al año', () => {
    expect(nombreRegimen('BIMESTRAL')).toBe('Bimestral');
    expect(nombreRegimen('TRIMESTRAL')).toBe('Trimestral');
    expect(periodosAlAnio('BIMESTRAL')).toBe('4 bimestres al año');
    expect(periodosAlAnio('TRIMESTRAL')).toBe('3 trimestres al año');
  });

  test('la columna Periodos usa la unidad del régimen', () => {
    expect(cantidadDePeriodos('BIMESTRAL', 4)).toBe('4 bimestres');
    expect(cantidadDePeriodos('TRIMESTRAL', 3)).toBe('3 trimestres');
    expect(cantidadDePeriodos('TRIMESTRAL', 1)).toBe('1 trimestre');
  });
});

describe('fechas y subtítulo de la cabecera', () => {
  test('rango de fechas con día/mes/año', () => {
    expect(rangoFechas('2026-03-02', '2026-12-18')).toBe(
      '02/03/2026 – 18/12/2026',
    );
  });

  test('subtítulo como en CI-03 y CI-06', () => {
    expect(
      subtituloAnio({
        fechaInicio: '2026-03-02',
        fechaFin: '2026-12-18',
        regimen: 'BIMESTRAL',
        periodos: [1, 2, 3, 4],
      }),
    ).toBe('Del 02/03/2026 al 18/12/2026 · Régimen bimestral (4 periodos)');
    expect(
      subtituloAnio({
        fechaInicio: '2027-03-01',
        fechaFin: '2027-12-17',
        regimen: 'TRIMESTRAL',
        periodos: [1, 2, 3],
      }),
    ).toBe('Del 01/03/2027 al 17/12/2027 · Régimen trimestral (3 periodos)');
  });
});

describe('etiquetas de estado', () => {
  test('año: Planificado, Activo y Cerrado', () => {
    expect(ESTADO_ANIO.PLANIFICADO).toEqual({
      texto: 'Planificado',
      color: 'info',
    });
    expect(ESTADO_ANIO.ACTIVO).toEqual({ texto: 'Activo', color: 'success' });
    expect(ESTADO_ANIO.CERRADO).toEqual({ texto: 'Cerrado', color: 'neutral' });
  });

  test('periodo: Planificado, En curso y Cerrado', () => {
    expect(ESTADO_PERIODO.EN_CURSO.texto).toBe('En curso');
    expect(ESTADO_PERIODO.PLANIFICADO.texto).toBe('Planificado');
    expect(ESTADO_PERIODO.CERRADO.texto).toBe('Cerrado');
  });
});

test('diasRestantes: plural, singular y el mismo día', () => {
  expect(diasRestantes(14)).toBe('Faltan 14 días');
  expect(diasRestantes(1)).toBe('Falta 1 día');
  expect(diasRestantes(0)).toBe('Vence hoy');
});

test('anioActivo devuelve el año ACTIVO o undefined', () => {
  const lista = [
    anio(2027, 'PLANIFICADO'),
    anio(2026, 'ACTIVO'),
    anio(2025, 'CERRADO'),
  ];
  expect(anioActivo(lista)?.anio).toBe(2026);
  expect(anioActivo([anio(2027, 'PLANIFICADO')])).toBeUndefined();
  expect(anioActivo([])).toBeUndefined();
});

describe('datosDePeriodos (tarjeta "Año escolar en curso" de CI-01)', () => {
  const enCurso = {
    id: 'p3',
    numero: 3,
    nombre: 'III Bimestre',
    fechaInicio: '2026-08-03',
    fechaFin: '2026-10-09',
    fechaLimiteNotas: '2026-10-16',
    diasParaLimiteNotas: 14,
  };
  const notasAbiertas = {
    id: 'p2',
    numero: 2,
    nombre: 'II Bimestre',
    fechaFin: '2026-07-24',
    fechaLimiteNotas: '2026-08-07',
    diasParaLimiteNotas: 4,
  };
  const proximo = {
    id: 'p4',
    numero: 4,
    nombre: 'IV Bimestre',
    fechaInicio: '2026-10-19',
  };

  test('con un periodo en curso muestra ese periodo y su límite, como en Figma', () => {
    const { periodo, limite } = datosDePeriodos({
      periodoEnCurso: enCurso,
      periodoNotasAbiertas: null,
      proximoPeriodo: proximo,
    });

    expect(periodo).toEqual({
      etiqueta: 'Periodo en curso',
      valor: 'III Bimestre',
      detalle: '03/08/2026 – 09/10/2026',
    });
    expect(limite).toEqual({
      etiqueta: 'Límite para registrar notas',
      valor: '16/10/2026',
      detalle: 'III Bimestre · Faltan 14 días',
    });
  });

  test('entre periodos muestra el próximo y el periodo con notas abiertas', () => {
    const { periodo, limite } = datosDePeriodos({
      periodoEnCurso: null,
      periodoNotasAbiertas: { ...notasAbiertas, diasParaLimiteNotas: 1 },
      proximoPeriodo: proximo,
    });

    expect(periodo).toEqual({
      etiqueta: 'Próximo periodo',
      valor: 'IV Bimestre',
      detalle: 'Empieza el 19/10/2026',
    });
    expect(limite).toEqual({
      etiqueta: 'Límite para registrar notas',
      valor: '07/08/2026',
      detalle: 'II Bimestre · Falta 1 día',
    });
  });

  test('con un plazo ampliado y otro periodo en curso, el límite es el del periodo que vence primero', () => {
    const { periodo, limite } = datosDePeriodos({
      periodoEnCurso: enCurso,
      periodoNotasAbiertas: notasAbiertas,
      proximoPeriodo: proximo,
    });

    expect(periodo.valor).toBe('III Bimestre');
    expect(limite.detalle).toBe('II Bimestre · Faltan 4 días');
  });

  test('sin periodos ni plazos, avisa sin inventar fechas', () => {
    const { periodo, limite } = datosDePeriodos({
      periodoEnCurso: null,
      periodoNotasAbiertas: null,
      proximoPeriodo: null,
    });

    expect(periodo.valor).toBe('Sin periodo en curso');
    expect(periodo.detalle).toBeUndefined();
    expect(limite).toEqual({
      etiqueta: 'Límite para registrar notas',
      valor: '—',
      detalle: 'Sin plazo abierto',
    });
  });

  test('el último día del plazo dice "Vence hoy"', () => {
    const { limite } = datosDePeriodos({
      periodoEnCurso: { ...enCurso, diasParaLimiteNotas: 0 },
      periodoNotasAbiertas: null,
      proximoPeriodo: null,
    });

    expect(limite.detalle).toBe('III Bimestre · Vence hoy');
  });
});

test('textos de los datos de la tarjeta y de la tabla', () => {
  expect(alumnosMatriculados(672)).toBe('672 alumnos matriculados');
  expect(alumnosMatriculados(1)).toBe('1 alumno matriculado');
  expect(codigosDeEstructuras(['EH-PRI-26', 'EH-SEC-26'])).toBe(
    'EH-PRI-26 · EH-SEC-26',
  );
  expect(codigosDeEstructuras([])).toBe('Sin estructuras registradas');
  expect(aniosRegistrados(3)).toBe('3 años registrados');
  expect(aniosRegistrados(1)).toBe('1 año registrado');
});
