import { z } from 'zod';

import {
  DiaLectivo,
  EstructuraHorariaResumen,
  Nivel,
  SolicitudEdicionEstructuraHoraria,
  SolicitudEstructuraHoraria,
  SolicitudVistaPrevia,
} from '@/types/api';

// Figma > "Modal - Nueva estructura horaria" (HU-44). Las reglas y los mensajes son los del backend
// (EstructuraHoraria y Receso), para que el formulario y el servidor digan lo mismo. La regla de que la jornada termine
// como máximo a las 18:00 NO está aquí: depende de la suma de bloques y recesos y la confirma la vista previa del
// backend (`errores`), sin repetir el cálculo en el frontend.

export const LONGITUD_MAXIMA_CODIGO = 20;
export const LONGITUD_MAXIMA_DESCRIPCION = 100;
export const BLOQUES_MINIMOS_POR_DIA = 1;
export const BLOQUES_MAXIMOS_POR_DIA = 12;
export const DURACION_MINIMA_BLOQUE_MIN = 20;
export const DURACION_MAXIMA_BLOQUE_MIN = 120;
export const DURACION_MINIMA_RECESO_MIN = 5;
export const DURACION_MAXIMA_RECESO_MIN = 60;

/** Mayúsculas sin tilde, números y guion: es la misma regla del backend (el código ya viene en mayúsculas). */
const CODIGO_VALIDO = /^[A-Z0-9-]+$/;
const HORA_VALIDA = /^([01]\d|2[0-3]):[0-5]\d$/;

const DIAS: [DiaLectivo, ...DiaLectivo[]] = [
  'LUN',
  'MAR',
  'MIE',
  'JUE',
  'VIE',
  'SAB',
];

export const MENSAJE_CODIGO_INVALIDO =
  'El código solo admite letras, números y guion.';
export const MENSAJE_BLOQUES = `Los bloques por día van de ${BLOQUES_MINIMOS_POR_DIA} a ${BLOQUES_MAXIMOS_POR_DIA}.`;
export const MENSAJE_DURACION_BLOQUE = `La duración del bloque va de ${DURACION_MINIMA_BLOQUE_MIN} a ${DURACION_MAXIMA_BLOQUE_MIN} minutos.`;
export const MENSAJE_DURACION_RECESO = `El receso dura de ${DURACION_MINIMA_RECESO_MIN} a ${DURACION_MAXIMA_RECESO_MIN} minutos.`;
export const MENSAJE_RECESO_REPETIDO =
  'No puede haber dos recesos después del mismo bloque.';
export const MENSAJE_SIN_LUGAR_PARA_RECESO =
  'Con un solo bloque por día no hay dónde poner un receso.';
export const mensajePosicionDeReceso = (bloquesPorDia: number) =>
  `Un receso va después de un bloque entre 1 y ${bloquesPorDia - 1}.`;

/** La única regla que el formulario no repite: la confirma la vista previa del backend. */
export const MENSAJE_JORNADA_MUY_TARDE =
  'La jornada debe terminar como máximo a las 18:00.';

/**
 * Cómo empieza el error de la vista previa cuando el código ya existe en el año. La vista previa no sabe si se está
 * editando, así que al editar también lo da para el código de la propia estructura (que no es un error).
 */
export const PREFIJO_CODIGO_REPETIDO = 'Ya existe una estructura con el código';

/**
 * Los mensajes que el formulario ya muestra junto a cada campo. La vista previa del backend devuelve también estos
 * mismos errores; con esta lista el modal solo enseña los que el formulario no cubre.
 */
const MENSAJES_DEL_ESQUEMA = new Set<string>([
  'Ingresa el código de la estructura.',
  `El código admite hasta ${LONGITUD_MAXIMA_CODIGO} caracteres.`,
  MENSAJE_CODIGO_INVALIDO,
  `La descripción admite hasta ${LONGITUD_MAXIMA_DESCRIPCION} caracteres.`,
  'Elige el nivel: primaria o secundaria.',
  'Elige al menos un día lectivo.',
  'Ingresa la hora de entrada.',
  MENSAJE_BLOQUES,
  MENSAJE_DURACION_BLOQUE,
  MENSAJE_DURACION_RECESO,
  MENSAJE_RECESO_REPETIDO,
  MENSAJE_SIN_LUGAR_PARA_RECESO,
  ...Array.from({ length: BLOQUES_MAXIMOS_POR_DIA - 1 }, (_, i) =>
    mensajePosicionDeReceso(i + 2),
  ),
]);

export const esMensajeDelEsquema = (mensaje: string) =>
  MENSAJES_DEL_ESQUEMA.has(mensaje);

/**
 * El código se escribe en mayúsculas ("eh-pri-26" y "EH-PRI-26" son el mismo código). Se aplica al recibir lo que se
 * escribe; no quita espacios: uno en medio se avisa en vivo con el mensaje del esquema.
 */
export const normalizarCodigo = (texto: string) => texto.toUpperCase();

/** Entero escrito con dígitos y dentro del rango; todo lo demás (vacío, decimales, letras, signos) da el mismo mensaje. */
const enteroEnRango = (minimo: number, maximo: number, mensaje: string) =>
  z
    .string()
    .trim()
    .refine(
      (texto) =>
        /^\d+$/.test(texto) &&
        Number(texto) >= minimo &&
        Number(texto) <= maximo,
      mensaje,
    );

const esEnteroValido = (texto: string) => /^\d+$/.test(texto.trim());

/**
 * Modal "Nueva estructura horaria". Todo se escribe como texto y se convierte al enviar (como `nuevaSeccionSchema`).
 * Cada regla va una sola vez y en orden, así que el formulario muestra un solo mensaje por campo. Las reglas que cruzan
 * campos (el receso respecto de los bloques por día, y los recesos repetidos) cuelgan del receso afectado.
 */
export const estructuraSchema = z
  .object({
    codigo: z
      .string()
      .trim()
      .min(1, 'Ingresa el código de la estructura.')
      .max(
        LONGITUD_MAXIMA_CODIGO,
        `El código admite hasta ${LONGITUD_MAXIMA_CODIGO} caracteres.`,
      )
      // El código vacío ya tiene su mensaje: no se suma el de los caracteres.
      .refine(
        (valor) => valor === '' || CODIGO_VALIDO.test(valor),
        MENSAJE_CODIGO_INVALIDO,
      ),
    descripcion: z
      .string()
      .trim()
      .max(
        LONGITUD_MAXIMA_DESCRIPCION,
        `La descripción admite hasta ${LONGITUD_MAXIMA_DESCRIPCION} caracteres.`,
      ),
    nivel: z
      .string()
      .refine(
        (valor) => valor === 'PRIMARIA' || valor === 'SECUNDARIA',
        'Elige el nivel: primaria o secundaria.',
      ),
    esPrincipal: z.boolean(),
    diasLectivos: z
      .array(z.enum(DIAS))
      .min(1, 'Elige al menos un día lectivo.'),
    horaEntrada: z
      .string()
      .refine((hora) => HORA_VALIDA.test(hora), 'Ingresa la hora de entrada.'),
    bloquesPorDia: enteroEnRango(
      BLOQUES_MINIMOS_POR_DIA,
      BLOQUES_MAXIMOS_POR_DIA,
      MENSAJE_BLOQUES,
    ),
    duracionBloqueMin: enteroEnRango(
      DURACION_MINIMA_BLOQUE_MIN,
      DURACION_MAXIMA_BLOQUE_MIN,
      MENSAJE_DURACION_BLOQUE,
    ),
    recesos: z.array(
      z.object({
        despuesDeBloque: z.string(),
        duracionMin: enteroEnRango(
          DURACION_MINIMA_RECESO_MIN,
          DURACION_MAXIMA_RECESO_MIN,
          MENSAJE_DURACION_RECESO,
        ),
      }),
    ),
  })
  .superRefine((valores, contexto) => {
    // Sin bloques por día válidos no hay contra qué comparar la posición: el mensaje de los bloques ya avisa.
    const bloquesValidos =
      esEnteroValido(valores.bloquesPorDia) &&
      Number(valores.bloquesPorDia) >= BLOQUES_MINIMOS_POR_DIA &&
      Number(valores.bloquesPorDia) <= BLOQUES_MAXIMOS_POR_DIA;
    const bloques = bloquesValidos ? Number(valores.bloquesPorDia) : 0;
    const vistos = new Set<number>();

    valores.recesos.forEach((receso, indice) => {
      const texto = receso.despuesDeBloque.trim();
      const posicion = esEnteroValido(texto) ? Number(texto) : 0;
      const fueraDeRango = posicion < 1 || posicion > bloques - 1;
      if (fueraDeRango && bloques > 0) {
        contexto.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['recesos', indice, 'despuesDeBloque'],
          message:
            bloques === 1
              ? MENSAJE_SIN_LUGAR_PARA_RECESO
              : mensajePosicionDeReceso(bloques),
        });
      } else if (!fueraDeRango && vistos.has(posicion)) {
        contexto.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['recesos', indice, 'despuesDeBloque'],
          message: MENSAJE_RECESO_REPETIDO,
        });
      }
      if (!fueraDeRango) vistos.add(posicion);
    });
  });

/** Un receso del formulario: se escribe como texto. */
export type ValoresReceso = { despuesDeBloque: string; duracionMin: string };

/** Lo que escribe el usuario en el modal; nada viene precargado. */
export type ValoresEstructura = {
  codigo: string;
  descripcion: string;
  /** '' mientras no se elija. */
  nivel: Nivel | '';
  esPrincipal: boolean;
  diasLectivos: DiaLectivo[];
  horaEntrada: string;
  bloquesPorDia: string;
  duracionBloqueMin: string;
  recesos: ValoresReceso[];
};

export const valoresInicialesEstructura = (): ValoresEstructura => ({
  codigo: '',
  descripcion: '',
  nivel: '',
  esPrincipal: false,
  diasLectivos: [],
  horaEntrada: '',
  bloquesPorDia: '',
  duracionBloqueMin: '',
  recesos: [],
});

/** Para abrir el modal en modo edición: lo que ya tiene la estructura, escrito como lo escribiría el usuario. */
export const valoresDeEstructura = (
  e: Pick<
    EstructuraHorariaResumen,
    | 'codigo'
    | 'descripcion'
    | 'nivel'
    | 'esPrincipal'
    | 'diasLectivos'
    | 'horaEntrada'
    | 'bloquesPorDia'
    | 'duracionBloqueMin'
    | 'recesos'
  >,
): ValoresEstructura => ({
  codigo: e.codigo,
  descripcion: e.descripcion ?? '',
  nivel: e.nivel,
  esPrincipal: e.esPrincipal,
  diasLectivos: enOrdenDeSemana(e.diasLectivos),
  horaEntrada: e.horaEntrada,
  bloquesPorDia: String(e.bloquesPorDia),
  duracionBloqueMin: String(e.duracionBloqueMin),
  recesos: [...e.recesos]
    .sort((a, b) => a.despuesDeBloque - b.despuesDeBloque)
    .map((receso) => ({
      despuesDeBloque: String(receso.despuesDeBloque),
      duracionMin: String(receso.duracionMin),
    })),
});

/** Los días en el orden de la semana, sin importar en qué orden se fueron marcando. */
const enOrdenDeSemana = (dias: DiaLectivo[]) =>
  DIAS.filter((dia) => dias.includes(dia));

const aEntero = (texto: string) =>
  esEnteroValido(texto) ? Number(texto.trim()) : null;

/** El cuerpo de `POST .../estructuras-horarias` a partir de lo escrito (ya validado). */
export const aSolicitudEstructura = (
  valores: ValoresEstructura,
): SolicitudEstructuraHoraria => ({
  codigo: valores.codigo.trim(),
  descripcion: valores.descripcion.trim() || null,
  nivel: valores.nivel as Nivel,
  esPrincipal: valores.esPrincipal,
  diasLectivos: enOrdenDeSemana(valores.diasLectivos),
  horaEntrada: valores.horaEntrada,
  bloquesPorDia: Number(valores.bloquesPorDia),
  duracionBloqueMin: Number(valores.duracionBloqueMin),
  recesos: valores.recesos.map((receso) => ({
    despuesDeBloque: Number(receso.despuesDeBloque),
    duracionMin: Number(receso.duracionMin),
  })),
});

/**
 * El cuerpo de `PUT /estructuras-horarias/{id}`: sin `nivel`, que no se cambia. `esPrincipal` va siempre; el backend
 * rechaza desmarcar la principal (409), así que el modal no lo ofrece para esa estructura.
 */
export const aSolicitudEdicion = (
  valores: ValoresEstructura,
): SolicitudEdicionEstructuraHoraria => {
  const solicitud = aSolicitudEstructura(valores);
  return {
    codigo: solicitud.codigo,
    descripcion: solicitud.descripcion,
    esPrincipal: solicitud.esPrincipal,
    diasLectivos: solicitud.diasLectivos,
    horaEntrada: solicitud.horaEntrada,
    bloquesPorDia: solicitud.bloquesPorDia,
    duracionBloqueMin: solicitud.duracionBloqueMin,
    recesos: solicitud.recesos,
  };
};

const enRango = (valor: number | null, minimo: number, maximo: number) =>
  valor !== null && valor >= minimo && valor <= maximo;

/**
 * Si con estos datos el backend puede calcular la jornada: hora de entrada, bloques por día y duración válidos. Con
 * otros valores la vista previa respondería sin jornada, y la pantalla preferiría seguir mostrando la anterior.
 */
export const puedeCalcularseLaJornada = (solicitud: SolicitudVistaPrevia) =>
  solicitud.horaEntrada !== null &&
  enRango(
    solicitud.bloquesPorDia,
    BLOQUES_MINIMOS_POR_DIA,
    BLOQUES_MAXIMOS_POR_DIA,
  ) &&
  enRango(
    solicitud.duracionBloqueMin,
    DURACION_MINIMA_BLOQUE_MIN,
    DURACION_MAXIMA_BLOQUE_MIN,
  );

/**
 * El cuerpo de la vista previa con el formulario a medio llenar: lo que no se puede leer va como null y el backend
 * responde con la lista de errores en vez de un 400.
 *
 * Los recesos solo se mandan cuando están completos y son válidos (después de un bloque entre 1 y bloques - 1, de 5 a
 * 60 minutos, sin repetir bloque). Un receso a medio llenar haría que el backend respondiera sin jornada y la vista
 * previa desapareciera; así, agregar una fila vacía no cambia lo que se manda y la vista previa se queda como estaba
 * hasta que el receso esté completo. Los errores de esas filas ya salen junto a sus campos.
 */
export const aSolicitudVistaPrevia = (
  valores: ValoresEstructura,
): SolicitudVistaPrevia => {
  const bloquesPorDia = aEntero(valores.bloquesPorDia);
  const posicionesUsadas = new Set<number>();
  const recesos = valores.recesos.flatMap((receso) => {
    const despuesDeBloque = aEntero(receso.despuesDeBloque);
    const duracionMin = aEntero(receso.duracionMin);
    const completoYValido =
      despuesDeBloque !== null &&
      bloquesPorDia !== null &&
      enRango(despuesDeBloque, 1, bloquesPorDia - 1) &&
      !posicionesUsadas.has(despuesDeBloque) &&
      enRango(
        duracionMin,
        DURACION_MINIMA_RECESO_MIN,
        DURACION_MAXIMA_RECESO_MIN,
      );
    if (!completoYValido) return [];
    posicionesUsadas.add(despuesDeBloque);
    return [{ despuesDeBloque, duracionMin }];
  });

  return {
    codigo: valores.codigo.trim() || null,
    descripcion: valores.descripcion.trim() || null,
    nivel: valores.nivel || null,
    esPrincipal: valores.esPrincipal,
    diasLectivos: enOrdenDeSemana(valores.diasLectivos),
    horaEntrada: HORA_VALIDA.test(valores.horaEntrada)
      ? valores.horaEntrada
      : null,
    bloquesPorDia,
    duracionBloqueMin: aEntero(valores.duracionBloqueMin),
    recesos,
  };
};
