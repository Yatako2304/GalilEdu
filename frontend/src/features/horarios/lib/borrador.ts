import {
  CeldaDelHorario,
  CeldaSolicitada,
  CursoOpcion,
  DiaCoordinador,
  SolicitudHorario,
} from '../types';

// El borrador es la matriz que el coordinador va armando en el navegador. Es una lista de celdas con la misma forma
// que las del GET (con los nombres, para dibujarlas); solo se envía al backend al guardar.

export const buscarCelda = (
  celdas: CeldaDelHorario[],
  dia: DiaCoordinador,
  numeroBloque: number,
) => celdas.find((c) => c.dia === dia && c.numeroBloque === numeroBloque);

/** Pone (o reemplaza) una celda del borrador. */
export const conCelda = (
  celdas: CeldaDelHorario[],
  celda: CeldaDelHorario,
): CeldaDelHorario[] => [
  ...sinCelda(celdas, celda.dia, celda.numeroBloque),
  celda,
];

export const sinCelda = (
  celdas: CeldaDelHorario[],
  dia: DiaCoordinador,
  numeroBloque: number,
): CeldaDelHorario[] =>
  celdas.filter((c) => !(c.dia === dia && c.numeroBloque === numeroBloque));

export const aCeldasSolicitadas = (
  celdas: CeldaDelHorario[],
): CeldaSolicitada[] =>
  [...celdas]
    .sort(
      (a, b) => a.dia.localeCompare(b.dia) || a.numeroBloque - b.numeroBloque,
    )
    .map((c) => ({
      dia: c.dia,
      numeroBloque: c.numeroBloque,
      planCursoId: c.planCursoId,
      ambienteId: c.ambiente.id,
    }));

/** El PUT lleva la matriz COMPLETA del borrador y la versión que devolvió el GET. */
export const aSolicitudHorario = (
  celdas: CeldaDelHorario[],
  version: number | null,
): SolicitudHorario => ({ version, celdas: aCeldasSolicitadas(celdas) });

const firma = (c: CeldaDelHorario) =>
  `${c.dia}:${c.numeroBloque}=${c.planCursoId}|${c.ambiente.id}`;

/** Si el borrador difiere de lo guardado: otra celda, otro curso u otro ambiente. */
export const hayCambios = (
  guardadas: CeldaDelHorario[],
  borrador: CeldaDelHorario[],
) => {
  if (guardadas.length !== borrador.length) return true;
  const guardado = new Set(guardadas.map(firma));
  return borrador.some((c) => !guardado.has(firma(c)));
};

const contar = (celdas: CeldaDelHorario[], planCursoId: string) =>
  celdas.filter((c) => c.planCursoId === planCursoId).length;

/**
 * Bloques que el curso ya tiene en el borrador, sin contar el que se está editando. Parte de `horasAsignadas` del
 * backend y le suma lo que el borrador cambió respecto de lo guardado; un bloque = una hora semanal.
 */
export const horasAsignadasEnBorrador = (
  curso: CursoOpcion,
  guardadas: CeldaDelHorario[],
  borrador: CeldaDelHorario[],
  enEdicion: CeldaDelHorario | undefined,
) =>
  curso.horasAsignadas +
  (contar(borrador, curso.planCursoId) - contar(guardadas, curso.planCursoId)) -
  (enEdicion?.planCursoId === curso.planCursoId ? 1 : 0);
