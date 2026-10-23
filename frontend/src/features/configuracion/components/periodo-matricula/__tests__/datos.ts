import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import {
  EstadoVisiblePeriodoMatricula,
  FilaPeriodoMatricula,
  FormularioPeriodoMatricula,
  Pagina,
  PeriodoMatriculaVigente,
} from '@/types/api';

// Datos de ejemplo de la pantalla 13 "Periodos de matrícula" (HU-23), con los valores de Figma.

export const API = `${env.API_URL}/configuracion/periodos-matricula`;

export const fila = (
  anio: number,
  estado: EstadoVisiblePeriodoMatricula,
  extras: Partial<FilaPeriodoMatricula> = {},
): FilaPeriodoMatricula => ({
  id: `periodo-${anio}`,
  nombre: `Matrícula ${anio}`,
  anioEscolarId: `anio-${anio}`,
  anio,
  // 10:00 a.m. en Lima (UTC-5) del 01/12 del año anterior y del 15/02.
  inicio: `${anio - 1}-12-01T15:00:00Z`,
  cierre: `${anio}-02-15T15:00:00Z`,
  matriculas: 0,
  estado,
  vigente: false,
  ...extras,
});

export const programado2027 = fila(2027, 'PROGRAMADO', { vigente: true });
export const cerrado2026 = fila(2026, 'CERRADO', { matriculas: 331 });
export const cerrado2025 = fila(2025, 'CERRADO', { matriculas: 318 });

export const vigente2027 = (
  extras: Partial<PeriodoMatriculaVigente> = {},
): PeriodoMatriculaVigente => ({
  id: 'periodo-2027',
  anioEscolarId: 'anio-2027',
  anio: 2027,
  nombre: 'Matrícula 2027',
  inicio: '2026-12-01T15:00:00Z',
  cierre: '2027-02-15T15:00:00Z',
  estado: 'ABIERTO',
  bloquearPorMorosidad: true,
  permitirExtemporanea: true,
  vacantesTotales: 300,
  matriculasRegistradas: 8,
  ...extras,
});

export const detalle2027 = (
  extras: Partial<FormularioPeriodoMatricula> = {},
): FormularioPeriodoMatricula => ({
  id: 'periodo-2027',
  anioEscolarId: 'anio-2027',
  anio: 2027,
  titulo: 'Matrícula 2027',
  estado: 'ABIERTO',
  editable: false,
  eliminable: false,
  fechaInicio: '2026-09-15',
  horaInicio: '08:00:00',
  fechaCierre: '2026-10-31',
  horaCierre: '23:59:00',
  diasDelPeriodo: 46,
  montoMatricula: 450,
  tarifarioConfirmado: true,
  vacantesTotales: 300,
  matriculasRegistradas: 8,
  cantidadSecciones: 10,
  resumenSecciones: '1.° a 5.°, A y B',
  bloquearPorMorosidad: true,
  permitirExtemporanea: true,
  secciones: [],
  documentos: [1, 2, 3, 4].map((n) => ({
    id: `doc-${n}`,
    nombre: `Documento ${n}`,
    descripcion: null,
    orden: n,
    tienePlantilla: false,
    plantillaNombreArchivo: null,
    tieneEntregas: false,
  })),
  ...extras,
});

type Opciones = {
  /** Cada consulta que llega al listado, para revisar sus parámetros. */
  consultas?: URLSearchParams[];
};

/**
 * Responde el listado como el backend: la lista completa (`size=100`) devuelve todo, y la paginada aplica los filtros
 * `anio` y `estado` y la página pedida.
 */
export const servirPeriodos = (
  filas: FilaPeriodoMatricula[],
  { consultas }: Opciones = {},
) =>
  server.use(
    http.get(API, ({ request }) => {
      const parametros = new URL(request.url).searchParams;
      consultas?.push(parametros);
      const size = Number(parametros.get('size') ?? 8);
      const page = Number(parametros.get('page') ?? 1);
      const anio = parametros.get('anio');
      const estado = parametros.get('estado');
      const visibles =
        size === 100
          ? filas
          : filas.filter(
              (f) =>
                (!anio || f.anio === Number(anio)) &&
                (!estado || f.estado === estado),
            );
      const pagina: Pagina<FilaPeriodoMatricula> = {
        data: visibles.slice((page - 1) * size, page * size),
        meta: {
          page,
          size,
          total: visibles.length,
          totalPages: Math.max(1, Math.ceil(visibles.length / size)),
        },
      };
      return HttpResponse.json(pagina);
    }),
  );
