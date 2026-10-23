import { HttpResponse, http } from 'msw';

import { env } from '@/config/env';
import { server } from '@/testing/mocks/server';
import {
  AnioEscolarResumen,
  CuotaTarifarioVista,
  EstadoAnioEscolar,
  TarifarioVista,
} from '@/types/api';

// Datos de ejemplo de P19 "Tarifario y calendario" (HU-34), con los valores de Figma (año 2027).

export const API_TARIFARIOS = `${env.API_URL}/configuracion/tarifarios`;
export const API_ANIOS = `${env.API_URL}/configuracion/anios-escolares`;

export const anio = (
  numero: number,
  estado: EstadoAnioEscolar,
): AnioEscolarResumen => ({
  id: `anio-${numero}`,
  anio: numero,
  fechaInicio: `${numero}-03-01`,
  fechaFin: `${numero}-12-15`,
  regimen: 'BIMESTRAL',
  cantidadPeriodos: 4,
  seccionesActivas: 10,
  estructurasHorarias: 2,
  estado,
});

/** Marzo a diciembre: vencen el último día de cada mes y la última (diciembre) el 15. */
export const cuotas = (numero: number, monto: number | null) => {
  const ultimoDia = [31, 30, 31, 30, 31, 31, 30, 31, 30];
  const nombres = [
    'marzo',
    'abril',
    'mayo',
    'junio',
    'julio',
    'agosto',
    'septiembre',
    'octubre',
    'noviembre',
  ];
  const lista: CuotaTarifarioVista[] = ultimoDia.map((dia, i) => ({
    numero: i + 1,
    nombre: `Pensión de ${nombres[i]}`,
    fechaVencimiento: `${numero}-${String(i + 3).padStart(2, '0')}-${dia}`,
    monto,
  }));
  lista.push({
    numero: 10,
    nombre: 'Pensión de diciembre',
    fechaVencimiento: `${numero}-12-15`,
    monto,
  });
  return lista;
};

export const tarifario2027 = (
  extras: Partial<TarifarioVista> = {},
): TarifarioVista => ({
  anioEscolarId: 'anio-2027',
  anio: 2027,
  existe: true,
  estado: 'EN_CONFIGURACION',
  soloLectura: false,
  motivoSoloLectura: null,
  calendarioDesactualizado: false,
  conceptos: {
    montoCuotaIngreso: 500,
    montoMatricula: 450,
    vencimientoMatricula: '2027-02-28',
    montoPension: 620,
    cantidadCuotas: 10,
  },
  morosidad: { diasGracia: 5, recargoPorcentaje: 2, topeRecargoPorcentaje: 10 },
  calendario: { cuotas: cuotas(2027, 620), totalPensiones: 6200 },
  ...extras,
});

/** Un año sin tarifario ni año anterior de donde copiar: todo vacío. */
export const propuestaVacia = (): TarifarioVista =>
  tarifario2027({
    existe: false,
    conceptos: {
      montoCuotaIngreso: null,
      montoMatricula: null,
      vencimientoMatricula: '2027-02-28',
      montoPension: null,
      cantidadCuotas: 10,
    },
    morosidad: {
      diasGracia: null,
      recargoPorcentaje: null,
      topeRecargoPorcentaje: null,
    },
    calendario: { cuotas: cuotas(2027, null), totalPensiones: null },
  });

type Opciones = {
  /** Cada consulta del tarifario que llega, con su `anioEscolarId`. */
  consultas?: string[];
};

/** Responde la lista de años y el tarifario de cada uno (`porAnio`, por id de año). */
export const servirTarifarios = (
  anios: AnioEscolarResumen[],
  porAnio: Record<string, TarifarioVista>,
  { consultas }: Opciones = {},
) =>
  server.use(
    http.get(API_ANIOS, () => HttpResponse.json(anios)),
    http.get(API_TARIFARIOS, ({ request }) => {
      const id = new URL(request.url).searchParams.get('anioEscolarId') ?? '';
      consultas?.push(id);
      return porAnio[id]
        ? HttpResponse.json(porAnio[id])
        : HttpResponse.json(
            { message: 'El año escolar no existe.' },
            { status: 404 },
          );
    }),
  );
