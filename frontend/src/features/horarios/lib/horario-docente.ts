import {
  BloqueHorario,
  CursoSesion,
  DiaSemana,
  HorarioSeccion,
  SesionClase,
} from '@/types/api';

import { HorarioPorEstructura } from '../api/horario-docente';
import { DiaCoordinador } from '../types';

import { filasDeLaJornada } from './horario-apoderado';

// `MatrizHoraria` solo dibuja de lunes a viernes: las clases del sábado se descartan (igual que en el horario del
// apoderado).
const DIA_DE_LA_MATRIZ: Partial<Record<DiaCoordinador, DiaSemana>> = {
  LUN: 'LUNES',
  MAR: 'MARTES',
  MIE: 'MIERCOLES',
  JUE: 'JUEVES',
  VIE: 'VIERNES',
};

/**
 * El horario de una estructura en la forma que dibuja `MatrizHoraria`. Las filas (bloques y recreos) salen de la
 * jornada de esa estructura. El docente va vacío: es el propio usuario y `ScheduleBlock` no muestra «Prof.»; la
 * sección y el aula van en cada curso. Dos clases en la misma celda (un cruce guardado) quedan en UNA sola sesión con
 * varios cursos, porque `MatrizHoraria` solo dibuja la primera sesión de cada celda.
 */
export const aMatrizDelDocente = (
  horario: HorarioPorEstructura,
): Pick<HorarioSeccion, 'bloques' | 'sesiones'> => {
  const bloques: BloqueHorario[] = filasDeLaJornada(horario.jornada).map(
    (f, i) => ({
      id: f.tipo === 'CLASE' ? `bloque-${f.numero}` : `recreo-${i}`,
      nombre: f.nombre,
      tipo: f.tipo,
      horaInicio: f.inicio,
      horaFin: f.fin,
      orden: i,
    }),
  );

  const sesiones = new Map<string, SesionClase>();
  horario.clases.forEach((c) => {
    const dia = DIA_DE_LA_MATRIZ[c.dia];
    if (!dia) return;
    const curso: CursoSesion = {
      curso: c.curso,
      docente: '',
      aula: c.ambiente,
      seccion: c.seccion,
    };
    const clave = `${dia}:${c.numeroBloque}`;
    const existente = sesiones.get(clave);
    if (existente) {
      existente.cursos.push(curso);
    } else {
      sesiones.set(clave, {
        dia,
        bloqueId: `bloque-${c.numeroBloque}`,
        tipo: 'ASIGNADO',
        cursos: [curso],
      });
    }
  });

  return { bloques, sesiones: [...sesiones.values()] };
};
