import { z } from 'zod';

import { formatDay } from '@/utils/format';

export const NOMBRE_PERIODO_MAXIMO = 30;

export type ValoresPeriodo = {
  nombre: string;
  fechaInicio: string;
  fechaFin: string;
  fechaLimiteNotas: string;
};

export type ContextoPeriodo = {
  /** Fechas del año al que pertenece el periodo. */
  anio: { fechaInicio: string; fechaFin: string };
  /** Los demás periodos del año, para detectar cruces. */
  otros: { nombre: string; fechaInicio: string; fechaFin: string }[];
};

type Campo = keyof ValoresPeriodo;

/**
 * Reglas del modal "Editar periodo" (Figma). Son las mismas que valida el backend, con sus mismos mensajes, y se
 * muestran bajo el campo que corresponde. El backend sigue siendo la segunda barrera. Las fechas son yyyy-mm-dd,
 * así que se comparan como texto. Cada campo muestra a lo sumo un error: el primero de este orden.
 */
export const crearPeriodoSchema = ({ anio, otros }: ContextoPeriodo) =>
  z
    .object({
      nombre: z
        .string()
        .trim()
        .min(1, 'Ingresa el nombre del periodo.')
        .max(
          NOMBRE_PERIODO_MAXIMO,
          `El nombre del periodo no puede pasar de ${NOMBRE_PERIODO_MAXIMO} caracteres.`,
        ),
      fechaInicio: z.string().min(1, 'Ingresa la fecha de inicio.'),
      fechaFin: z.string().min(1, 'Ingresa la fecha de fin.'),
      fechaLimiteNotas: z.string().min(1, 'Ingresa la fecha límite de notas.'),
    })
    .superRefine((v, ctx) => {
      const marcados = new Set<Campo>();
      const marcar = (campo: Campo, message: string) => {
        if (marcados.has(campo)) return;
        marcados.add(campo);
        ctx.addIssue({ code: z.ZodIssueCode.custom, path: [campo], message });
      };

      if (v.fechaInicio && v.fechaFin) {
        if (v.fechaFin <= v.fechaInicio) {
          marcar(
            'fechaFin',
            'La fecha de fin debe ser posterior a la fecha de inicio.',
          );
        }
        const fuera = `Las fechas del periodo deben estar dentro del año escolar (${formatDay(anio.fechaInicio)} al ${formatDay(anio.fechaFin)}).`;
        if (v.fechaInicio < anio.fechaInicio) marcar('fechaInicio', fuera);
        if (v.fechaFin > anio.fechaFin) marcar('fechaFin', fuera);

        const cruzado = otros.find(
          (o) => v.fechaInicio <= o.fechaFin && v.fechaFin >= o.fechaInicio,
        );
        if (cruzado) {
          marcar(
            'fechaFin',
            `Se cruza con el ${cruzado.nombre}, que empieza el ${formatDay(cruzado.fechaInicio)}.`,
          );
        }
      }

      if (v.fechaFin && v.fechaLimiteNotas && v.fechaLimiteNotas < v.fechaFin) {
        marcar(
          'fechaLimiteNotas',
          'La fecha límite de notas no puede ser anterior al fin del periodo.',
        );
      }
    });
