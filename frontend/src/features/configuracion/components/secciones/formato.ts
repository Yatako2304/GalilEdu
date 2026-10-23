import { EstadoSeccion, GradoConSecciones, SeccionResumen } from '@/types/api';

/** Motivo del botón deshabilitado cuando el año está CERRADO: un año cerrado es un registro histórico. */
export const MOTIVO_ANIO_CERRADO = 'Un año cerrado ya no admite cambios.';

/** Figma > CI-09 > Tag de la columna Estado: "Activa". */
export const ESTADO_SECCION: Record<
  EstadoSeccion,
  { texto: string; color: 'success' | 'neutral' }
> = {
  ACTIVA: { texto: 'Activa', color: 'success' },
  INACTIVA: { texto: 'Inactiva', color: 'neutral' },
};

/** Figma > CI-09: la sección se nombra con su grado ("1.° A") y debajo va "Sección A". */
export const etiquetaSeccion = (
  grado: Pick<GradoConSecciones, 'numero'>,
  seccion: Pick<SeccionResumen, 'nombre'>,
) => `${grado.numero}.° ${seccion.nombre}`;

/** Figma > CI-09 > fila del grado: "3 secciones activas" (en singular, "1 sección activa"). */
export const textoSeccionesActivas = (cantidad: number) =>
  cantidad === 1 ? '1 sección activa' : `${cantidad} secciones activas`;

/** Porcentaje de la barra "Matriculados": alumnos que ocupan vacante sobre la capacidad máxima. */
export const porcentajeMatriculados = (
  seccion: Pick<SeccionResumen, 'alumnosOcupanVacante' | 'capacidadMaxima'>,
) =>
  seccion.capacidadMaxima > 0
    ? Math.round((seccion.alumnosOcupanVacante / seccion.capacidadMaxima) * 100)
    : 0;
