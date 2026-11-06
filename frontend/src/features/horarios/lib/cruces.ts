import { TipoCruce } from '../types';

// Figma > HU-46 > modal con cruce (296:17801): la alerta lleva un título por tipo de cruce y el mensaje del backend
// debajo. El backend manda el mensaje sin título: este es el único lugar donde se escriben los títulos.
export const TITULO_CRUCE: Record<TipoCruce, string> = {
  DOCENTE: 'Docente no disponible',
  AMBIENTE: 'Ambiente no disponible',
};

/** El orden en que se muestran las alertas cuando un bloque tiene cruces de los dos tipos. */
export const ORDEN_DE_CRUCES: TipoCruce[] = ['DOCENTE', 'AMBIENTE'];
