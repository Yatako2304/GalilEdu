import { z } from 'zod';

import {
  FormularioPeriodoMatricula,
  SolicitudPeriodoMatricula,
} from '@/types/api';

import { normalizarHora } from './formato';

// Reglas del formulario "Nuevo periodo de matrícula" (HU-23). Son las mismas que valida el backend, con sus mismos
// mensajes, y se muestran bajo el campo que corresponde. No se usa el reloj del navegador: que el inicio no sea
// anterior a "ahora" lo decide el backend.

export const CAPACIDAD_MINIMA = 1;
export const CAPACIDAD_MAXIMA = 45;
export const NOMBRE_DOCUMENTO_MAXIMO = 100;
export const DESCRIPCION_DOCUMENTO_MAXIMA = 100;

export type VacanteFormulario = { seccionId: string; capacidad: string };

export type ValoresPeriodoMatricula = {
  anioEscolarId: string;
  fechaInicio: string;
  horaInicio: string;
  fechaCierre: string;
  horaCierre: string;
  vacantes: VacanteFormulario[];
};

/** De cada sección solo hace falta su nombre ("1.° A") y cuántos alumnos ya ocupan vacante. */
export type SeccionParaValidar = {
  seccionId: string;
  etiqueta: string;
  alumnosOcupanVacante: number;
};

/** Un documento del periodo, antes de guardarse: la plantilla elegida se sube después, con el id que devuelve. */
export type DocumentoBorrador = {
  /** Identifica la fila en pantalla; no viaja al backend. */
  clave: string;
  /** Solo un documento que ya existe en el periodo (al editar). */
  id?: string | null;
  nombre: string;
  descripcion: string;
  /** Plantilla elegida y aún sin subir. */
  archivo: File | null;
  /** Plantilla que el documento ya tiene guardada. */
  plantillaNombreArchivo?: string | null;
  /** Al editar: se quitará la plantilla guardada cuando se guarde el periodo. */
  quitarPlantilla?: boolean;
  /** Un documento con entregas no se puede quitar del periodo. */
  tieneEntregas?: boolean;
};

/** Lo que el formulario muestra al abrir un periodo que ya existe (editar o ver). */
export const valoresDePeriodo = (
  periodo: FormularioPeriodoMatricula,
): ValoresPeriodoMatricula => ({
  anioEscolarId: periodo.anioEscolarId,
  fechaInicio: periodo.fechaInicio ?? '',
  horaInicio: periodo.horaInicio ? normalizarHora(periodo.horaInicio) : '',
  fechaCierre: periodo.fechaCierre ?? '',
  horaCierre: periodo.horaCierre ? normalizarHora(periodo.horaCierre) : '',
  vacantes: periodo.secciones.map((s) => ({
    seccionId: s.seccionId,
    capacidad: String(s.capacidad),
  })),
});

export const documentosDePeriodo = (
  periodo: FormularioPeriodoMatricula,
): DocumentoBorrador[] =>
  [...periodo.documentos]
    .sort((a, b) => a.orden - b.orden)
    .map((d) => ({
      clave: d.id,
      id: d.id,
      nombre: d.nombre,
      descripcion: d.descripcion ?? '',
      archivo: null,
      plantillaNombreArchivo: d.tienePlantilla
        ? (d.plantillaNombreArchivo ?? 'Plantilla')
        : null,
      tieneEntregas: d.tieneEntregas,
    }));

const completo = (fecha: string, hora: string) => !!fecha && !!hora;

export const crearPeriodoMatriculaSchema = (secciones: SeccionParaValidar[]) =>
  z
    .object({
      anioEscolarId: z.string().min(1, 'Elige el año académico.'),
      fechaInicio: z.string(),
      horaInicio: z.string(),
      fechaCierre: z.string(),
      horaCierre: z.string(),
      vacantes: z.array(
        z.object({ seccionId: z.string(), capacidad: z.string() }),
      ),
    })
    .superRefine((v, ctx) => {
      const error = (path: (string | number)[], message: string) =>
        ctx.addIssue({ code: z.ZodIssueCode.custom, path, message });

      // El aviso va bajo el campo que falta: la fecha o, si la fecha está, la hora.
      if (!completo(v.fechaInicio, v.horaInicio)) {
        error(
          [v.fechaInicio ? 'horaInicio' : 'fechaInicio'],
          'Ingresa la fecha y la hora de inicio.',
        );
      }
      if (!completo(v.fechaCierre, v.horaCierre)) {
        error(
          [v.fechaCierre ? 'horaCierre' : 'fechaCierre'],
          'Ingresa la fecha y la hora de cierre.',
        );
      }
      if (
        completo(v.fechaInicio, v.horaInicio) &&
        completo(v.fechaCierre, v.horaCierre) &&
        `${v.fechaCierre}T${v.horaCierre}` <= `${v.fechaInicio}T${v.horaInicio}`
      ) {
        error(
          ['fechaCierre'],
          'La fecha de cierre debe ser posterior a la de inicio.',
        );
      }

      v.vacantes.forEach((vacante, i) => {
        const seccion = secciones.find(
          (s) => s.seccionId === vacante.seccionId,
        );
        if (!seccion) return;
        const campo = ['vacantes', i, 'capacidad'];
        const texto = vacante.capacidad.trim();
        if (texto === '') {
          error(
            campo,
            `Ingresa las vacantes de la sección ${seccion.etiqueta}.`,
          );
          return;
        }
        const capacidad = Number(texto);
        if (
          !/^\d+$/.test(texto) ||
          capacidad < CAPACIDAD_MINIMA ||
          capacidad > CAPACIDAD_MAXIMA
        ) {
          error(
            campo,
            `Las vacantes de la sección ${seccion.etiqueta} deben estar entre ${CAPACIDAD_MINIMA} y ${CAPACIDAD_MAXIMA}.`,
          );
          return;
        }
        if (capacidad < seccion.alumnosOcupanVacante) {
          error(
            campo,
            `Las vacantes de la sección ${seccion.etiqueta} no pueden ser menos que los alumnos que ocupan vacante (${seccion.alumnosOcupanVacante}).`,
          );
        }
      });
    });

/** Suma de las vacantes escritas; lo que todavía no es un número válido cuenta como 0. */
export const totalDeVacantes = (vacantes: VacanteFormulario[]) =>
  vacantes.reduce((total, { capacidad }) => {
    const numero = Number(capacidad);
    return /^\d+$/.test(capacidad.trim()) && Number.isFinite(numero)
      ? total + numero
      : total;
  }, 0);

const clave = (nombre: string) => nombre.trim().toLowerCase();

/**
 * Reglas de un documento al agregarlo. El nombre es único en el periodo sin distinguir mayúsculas ni espacios de los
 * bordes. Devuelve un mensaje por campo (vacío si todo está bien).
 */
export const validarDocumento = (
  documento: { nombre: string; descripcion: string },
  existentes: string[],
): { nombre?: string; descripcion?: string } => {
  const errores: { nombre?: string; descripcion?: string } = {};
  const nombre = documento.nombre.trim();
  if (!nombre) {
    errores.nombre = 'Ingresa el nombre del documento.';
  } else if (nombre.length > NOMBRE_DOCUMENTO_MAXIMO) {
    errores.nombre = `El nombre del documento no puede pasar de ${NOMBRE_DOCUMENTO_MAXIMO} caracteres.`;
  } else if (existentes.some((e) => clave(e) === clave(nombre))) {
    errores.nombre = `Ya hay un documento llamado "${nombre}" en este periodo.`;
  }
  if (documento.descripcion.trim().length > DESCRIPCION_DOCUMENTO_MAXIMA) {
    errores.descripcion = `La descripción del documento no puede pasar de ${DESCRIPCION_DOCUMENTO_MAXIMA} caracteres.`;
  }
  return errores;
};

/** El cuerpo que recibe `POST` y `PUT /periodos-matricula`. La plantilla no va: se sube aparte. */
export const solicitudDePeriodo = (
  valores: ValoresPeriodoMatricula,
  documentos: DocumentoBorrador[],
): SolicitudPeriodoMatricula => ({
  anioEscolarId: valores.anioEscolarId,
  fechaInicio: valores.fechaInicio,
  horaInicio: valores.horaInicio,
  fechaCierre: valores.fechaCierre,
  horaCierre: valores.horaCierre,
  vacantes: valores.vacantes.map(({ seccionId, capacidad }) => ({
    seccionId,
    capacidad: Number(capacidad),
  })),
  documentos: documentos.map(({ id, nombre, descripcion }) => ({
    id: id ?? null,
    nombre: nombre.trim(),
    descripcion: descripcion.trim() || null,
  })),
});
