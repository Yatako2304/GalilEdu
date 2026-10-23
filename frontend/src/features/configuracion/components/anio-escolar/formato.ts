import {
  AnioEscolarActual,
  AnioEscolarDetalle,
  EstadoAnioEscolar,
  EstadoPeriodo,
  Regimen,
} from '@/types/api';
import { formatDay } from '@/utils/format';

type ColorTag = 'success' | 'info' | 'neutral' | 'warning' | 'error';

// Etiquetas de estado de Figma (CI-01, CI-03): año Planificado, Activo, Cerrado; periodo Planificado, En curso, Cerrado.
export const ESTADO_ANIO: Record<
  EstadoAnioEscolar,
  { texto: string; color: ColorTag }
> = {
  PLANIFICADO: { texto: 'Planificado', color: 'info' },
  ACTIVO: { texto: 'Activo', color: 'success' },
  CERRADO: { texto: 'Cerrado', color: 'neutral' },
};

export const ESTADO_PERIODO: Record<
  EstadoPeriodo,
  { texto: string; color: ColorTag }
> = {
  PLANIFICADO: { texto: 'Planificado', color: 'info' },
  EN_CURSO: { texto: 'En curso', color: 'success' },
  CERRADO: { texto: 'Cerrado', color: 'neutral' },
};

const REGIMEN: Record<
  Regimen,
  {
    nombre: string;
    unidadSingular: string;
    unidadPlural: string;
    cantidad: number;
  }
> = {
  BIMESTRAL: {
    nombre: 'Bimestral',
    unidadSingular: 'bimestre',
    unidadPlural: 'bimestres',
    cantidad: 4,
  },
  TRIMESTRAL: {
    nombre: 'Trimestral',
    unidadSingular: 'trimestre',
    unidadPlural: 'trimestres',
    cantidad: 3,
  },
};

/** "Bimestral" o "Trimestral". */
export const nombreRegimen = (regimen: Regimen) => REGIMEN[regimen].nombre;

/** "4 bimestres al año" (texto de ayuda de las tarjetas del régimen). */
export const periodosAlAnio = (regimen: Regimen) =>
  `${REGIMEN[regimen].cantidad} ${REGIMEN[regimen].unidadPlural} al año`;

/** "4 bimestres" o "1 bimestre" (columna Periodos de CI-01). */
export const cantidadDePeriodos = (regimen: Regimen, cantidad: number) =>
  `${cantidad} ${cantidad === 1 ? REGIMEN[regimen].unidadSingular : REGIMEN[regimen].unidadPlural}`;

/** "02/03/2026 – 18/12/2026". */
export const rangoFechas = (inicio: string, fin: string) =>
  `${formatDay(inicio)} – ${formatDay(fin)}`;

type DatosSubtitulo = Pick<
  AnioEscolarDetalle,
  'fechaInicio' | 'fechaFin' | 'regimen'
> & { periodos: unknown[] };

/** "Del 02/03/2026 al 18/12/2026 · Régimen bimestral (4 periodos)" (cabecera de CI-03, CI-06, CI-09 y CI-12). */
export const subtituloAnio = (anio: DatosSubtitulo) =>
  `Del ${formatDay(anio.fechaInicio)} al ${formatDay(anio.fechaFin)} · Régimen ${REGIMEN[anio.regimen].nombre.toLowerCase()} (${anio.periodos.length} ${anio.periodos.length === 1 ? 'periodo' : 'periodos'})`;

export type DatoResumen = {
  etiqueta: string;
  valor: string;
  detalle?: string;
};

type PeriodosActuales = Pick<
  AnioEscolarActual,
  'periodoEnCurso' | 'periodoNotasAbiertas' | 'proximoPeriodo'
>;

/**
 * Los dos primeros datos de la tarjeta "Año escolar en curso" (CI-01).
 * - Periodo: el que está en curso; entre periodos (vacaciones), el próximo.
 * - Límite de notas: el de un periodo ya terminado cuyo plazo sigue abierto, que es el que vence primero; si no
 *   hay, el del periodo en curso. Siempre con el nombre del periodo al que pertenece.
 */
export const datosDePeriodos = ({
  periodoEnCurso: enCurso,
  periodoNotasAbiertas: abierto,
  proximoPeriodo: proximo,
}: PeriodosActuales): { periodo: DatoResumen; limite: DatoResumen } => {
  const periodo: DatoResumen = enCurso
    ? {
        etiqueta: 'Periodo en curso',
        valor: enCurso.nombre,
        detalle: rangoFechas(enCurso.fechaInicio, enCurso.fechaFin),
      }
    : proximo
      ? {
          etiqueta: 'Próximo periodo',
          valor: proximo.nombre,
          detalle: `Empieza el ${formatDay(proximo.fechaInicio)}`,
        }
      : { etiqueta: 'Periodo en curso', valor: 'Sin periodo en curso' };

  const etiquetaLimite = 'Límite para registrar notas';
  const limite: DatoResumen = abierto
    ? {
        etiqueta: etiquetaLimite,
        valor: formatDay(abierto.fechaLimiteNotas),
        detalle: `${abierto.nombre} · ${diasRestantes(abierto.diasParaLimiteNotas)}`,
      }
    : enCurso
      ? {
          etiqueta: etiquetaLimite,
          valor: formatDay(enCurso.fechaLimiteNotas),
          detalle: `${enCurso.nombre} · ${diasRestantes(enCurso.diasParaLimiteNotas)}`,
        }
      : { etiqueta: etiquetaLimite, valor: '—', detalle: 'Sin plazo abierto' };

  return { periodo, limite };
};

/** "672 alumnos matriculados" (dato "Secciones activas"). */
export const alumnosMatriculados = (cantidad: number) =>
  `${cantidad} ${cantidad === 1 ? 'alumno matriculado' : 'alumnos matriculados'}`;

/** "EH-PRI-26 · EH-SEC-26" (dato "Estructuras horarias"). */
export const codigosDeEstructuras = (codigos: string[]) =>
  codigos.length > 0 ? codigos.join(' · ') : 'Sin estructuras registradas';

/** "3 años registrados" (cabecera de la tabla de CI-01). */
export const aniosRegistrados = (cantidad: number) =>
  `${cantidad} ${cantidad === 1 ? 'año registrado' : 'años registrados'}`;

/**
 * "Faltan 14 días", "Falta 1 día" o "Vence hoy". Solo da formato a `diasParaLimiteNotas`, que calcula el backend con
 * su fecha: no se compara con la fecha del navegador.
 */
export const diasRestantes = (dias: number) => {
  if (dias <= 0) return 'Vence hoy';
  return dias === 1 ? 'Falta 1 día' : `Faltan ${dias} días`;
};
