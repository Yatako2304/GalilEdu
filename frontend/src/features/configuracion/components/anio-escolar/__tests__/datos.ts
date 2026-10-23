import { AnioEscolarDetalle, EstadoPeriodo, PeriodoDetalle } from '@/types/api';

// Datos de ejemplo de las pruebas de CI-03 y CI-06 (con los valores de Figma).

export const periodo = (
  numero: number,
  nombre: string,
  fechaInicio: string,
  fechaFin: string,
  fechaLimiteNotas: string,
  estado: EstadoPeriodo,
  duracionSemanas = 10,
): PeriodoDetalle => ({
  id: `p${numero}`,
  numero,
  nombre,
  fechaInicio,
  fechaFin,
  fechaLimiteNotas,
  duracionSemanas,
  estado,
  registroNotasAbierto: estado === 'EN_CURSO',
});

/** CI-03: año 2026 ACTIVO, bimestral, con el III Bimestre en curso. */
export const anio2026: AnioEscolarDetalle = {
  id: 'anio-2026',
  anio: 2026,
  fechaInicio: '2026-03-02',
  fechaFin: '2026-12-18',
  regimen: 'BIMESTRAL',
  estado: 'ACTIVO',
  cantidadSecciones: 23,
  cantidadEstructuras: 2,
  periodos: [
    periodo(
      1,
      'I Bimestre',
      '2026-03-02',
      '2026-05-08',
      '2026-05-15',
      'CERRADO',
    ),
    periodo(
      2,
      'II Bimestre',
      '2026-05-18',
      '2026-07-24',
      '2026-07-31',
      'CERRADO',
    ),
    periodo(
      3,
      'III Bimestre',
      '2026-08-03',
      '2026-10-09',
      '2026-10-16',
      'EN_CURSO',
    ),
    periodo(
      4,
      'IV Bimestre',
      '2026-10-19',
      '2026-12-18',
      '2026-12-23',
      'PLANIFICADO',
      9,
    ),
  ],
};

/** CI-06: año 2027 PLANIFICADO, trimestral. */
export const anio2027: AnioEscolarDetalle = {
  id: 'anio-2027',
  anio: 2027,
  fechaInicio: '2027-03-01',
  fechaFin: '2027-12-17',
  regimen: 'TRIMESTRAL',
  estado: 'PLANIFICADO',
  cantidadSecciones: 23,
  cantidadEstructuras: 2,
  periodos: [
    periodo(
      1,
      'I Trimestre',
      '2027-03-01',
      '2027-05-31',
      '2027-06-05',
      'PLANIFICADO',
      13,
    ),
    periodo(
      2,
      'II Trimestre',
      '2027-06-08',
      '2027-09-07',
      '2027-09-12',
      'PLANIFICADO',
      13,
    ),
    periodo(
      3,
      'III Trimestre',
      '2027-09-15',
      '2027-12-17',
      '2027-12-22',
      'PLANIFICADO',
      13,
    ),
  ],
};

/**
 * Año con dos periodos cortos en el mismo mes: así los cruces y los límites se prueban eligiendo días del mismo
 * calendario, sin cambiar de mes.
 */
export const anioCorto: AnioEscolarDetalle = {
  id: 'anio-2027',
  anio: 2027,
  fechaInicio: '2027-03-05',
  fechaFin: '2027-12-17',
  regimen: 'BIMESTRAL',
  estado: 'PLANIFICADO',
  cantidadSecciones: 0,
  cantidadEstructuras: 0,
  periodos: [
    periodo(
      1,
      'I Bimestre',
      '2027-03-10',
      '2027-03-18',
      '2027-03-25',
      'PLANIFICADO',
      1,
    ),
    periodo(
      2,
      'II Bimestre',
      '2027-03-20',
      '2027-03-31',
      '2027-04-05',
      'PLANIFICADO',
      2,
    ),
  ],
};
