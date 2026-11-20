import { queryOptions, useQuery } from '@tanstack/react-query';

import { api } from '@/lib/api-client';
import {
  AnioReporte,
  HijoCalificaciones,
  NotasApoderado,
  NotasSeccion,
  ReporteGrado,
  ReporteInstitucional,
} from '@/types/api';

import { descargarBlob } from './calificaciones';

// Reportes de notas (HU-63 Coordinador, HU-64 Coordinador y Administrador). Sin periodoId, la nota final del año.
const reportes = '/calificaciones/reportes';

export type FiltrosReporteGrado = {
  anioEscolarId: string;
  grado: string;
  planCursoId?: string;
  periodoId?: string;
};

export type FiltrosReporteInstitucional = {
  anioEscolarId: string;
  periodoId?: string;
};

/** Quita los filtros vacíos para que el backend no reciba `planCursoId=`. */
const sinVacios = <T extends Record<string, string | undefined>>(filtros: T) =>
  Object.fromEntries(Object.entries(filtros).filter(([, valor]) => valor));

export const useAniosReporte = () =>
  useQuery(
    queryOptions({
      queryKey: ['reportes-notas', 'anios'],
      queryFn: (): Promise<AnioReporte[]> => api.get(`${reportes}/anios`),
    }),
  );

export const useReporteGrado = (filtros: FiltrosReporteGrado | null) =>
  useQuery(
    queryOptions({
      queryKey: ['reportes-notas', 'grado', filtros],
      queryFn: (): Promise<ReporteGrado> =>
        api.get(`${reportes}/grado`, { params: sinVacios(filtros!) }),
      enabled: !!filtros,
    }),
  );

export const useReporteInstitucional = (
  filtros: FiltrosReporteInstitucional | null,
) =>
  useQuery(
    queryOptions({
      queryKey: ['reportes-notas', 'institucional', filtros],
      queryFn: (): Promise<ReporteInstitucional> =>
        api.get(`${reportes}/institucional`, { params: sinVacios(filtros!) }),
      enabled: !!filtros,
    }),
  );

export const descargarReporteGrado = async (filtros: FiltrosReporteGrado) => {
  const contenido = (await api.get(`${reportes}/grado/pdf`, {
    params: sinVacios(filtros),
    responseType: 'blob',
  })) as Blob;
  descargarBlob(contenido, 'reporte-notas-grado.pdf');
};

export const descargarReporteInstitucional = async (
  filtros: FiltrosReporteInstitucional,
) => {
  const contenido = (await api.get(`${reportes}/institucional/pdf`, {
    params: sinVacios(filtros),
    responseType: 'blob',
  })) as Blob;
  descargarBlob(contenido, 'reporte-institucional-notas.pdf');
};

// Detalle de una sección (solo el Coordinador): nota oficial de cada competencia por estudiante y su libreta.
export const useNotasSeccion = (seccionId: string | null) =>
  useQuery(
    queryOptions({
      queryKey: ['reportes-notas', 'seccion', seccionId],
      queryFn: (): Promise<NotasSeccion> =>
        api.get(`${reportes}/secciones/${seccionId}/notas`),
      enabled: !!seccionId,
    }),
  );

export const descargarLibretaMatricula = async (matriculaId: string) => {
  const contenido = (await api.get(
    `${reportes}/matriculas/${matriculaId}/libreta`,
    { responseType: 'blob' },
  )) as Blob;
  descargarBlob(contenido, 'libreta-notas.pdf');
};

// HU-69 · Consulta de notas por el apoderado: solo sus hijos matriculados.
const apoderado = '/calificaciones/apoderado';

export const useHijosCalificaciones = () =>
  useQuery(
    queryOptions({
      queryKey: ['calificaciones-apoderado', 'estudiantes'],
      queryFn: (): Promise<HijoCalificaciones[]> =>
        api.get(`${apoderado}/estudiantes`),
    }),
  );

export const useNotasApoderado = (
  estudianteId: string | undefined,
  anio: number | undefined,
) =>
  useQuery(
    queryOptions({
      queryKey: ['calificaciones-apoderado', 'notas', estudianteId, anio],
      queryFn: (): Promise<NotasApoderado> =>
        api.get(`${apoderado}/estudiantes/${estudianteId}/notas`, {
          params: { anio },
        }),
      enabled: !!estudianteId && !!anio,
    }),
  );

export const descargarLibreta = async (estudianteId: string, anio: number) => {
  const contenido = (await api.get(
    `${apoderado}/estudiantes/${estudianteId}/libreta`,
    { params: { anio }, responseType: 'blob' },
  )) as Blob;
  descargarBlob(contenido, `libreta-notas-${anio}.pdf`);
};
