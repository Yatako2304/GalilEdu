import { AmbienteResumen, EstadoAmbiente, TipoAmbiente } from '@/types/api';

// Figma > Personal Administrativo > CI-26 · Ambientes físicos (HU-43): todas las columnas se ordenan en el cliente.

export const ETIQUETA_TIPO: Record<TipoAmbiente, string> = {
  AULA_REGULAR: 'Aula regular',
  ESPACIO_ESPECIAL: 'Espacio especial',
};

export const ETIQUETA_ESTADO: Record<EstadoAmbiente, string> = {
  ACTIVO: 'Activo',
  INACTIVO: 'Inactivo',
};

export type CampoOrden =
  'nombre' | 'tipo' | 'aforoMaximo' | 'recursos' | 'estado';

export type Orden = { campo: CampoOrden; direccion: 'asc' | 'desc' };

const CAMPOS: CampoOrden[] = [
  'nombre',
  'tipo',
  'aforoMaximo',
  'recursos',
  'estado',
];

export const esCampoOrden = (
  campo: keyof AmbienteResumen,
): campo is CampoOrden => (CAMPOS as string[]).includes(campo);

/** Texto "natural": sin distinguir mayúsculas ni tildes, y "Aula 2" antes que "Aula 10". */
const comparaTexto = (a: string, b: string) =>
  a.localeCompare(b, 'es', { numeric: true, sensitivity: 'base' });

const COMPARADORES: Record<
  CampoOrden,
  (a: AmbienteResumen, b: AmbienteResumen) => number
> = {
  nombre: (a, b) => comparaTexto(a.nombre, b.nombre),
  tipo: (a, b) => comparaTexto(ETIQUETA_TIPO[a.tipo], ETIQUETA_TIPO[b.tipo]),
  aforoMaximo: (a, b) => a.aforoMaximo - b.aforoMaximo,
  // Por el texto de los recursos, tal como se ven en la tabla.
  recursos: (a, b) =>
    comparaTexto(a.recursos.join(', '), b.recursos.join(', ')),
  estado: (a, b) =>
    comparaTexto(ETIQUETA_ESTADO[a.estado], ETIQUETA_ESTADO[b.estado]),
};

/** Copia ordenada. En un empate manda el nombre (siempre ascendente), así el orden es estable al invertir. */
export const ordenarAmbientes = (
  ambientes: AmbienteResumen[],
  { campo, direccion }: Orden,
): AmbienteResumen[] =>
  [...ambientes].sort((a, b) => {
    const resultado = COMPARADORES[campo](a, b);
    if (resultado !== 0) return direccion === 'asc' ? resultado : -resultado;
    return comparaTexto(a.nombre, b.nombre);
  });
