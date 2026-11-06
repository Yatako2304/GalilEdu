import {
  BloqueHorario,
  DiaSemana,
  HorarioSeccion,
  SesionClase,
} from '@/types/api';

import {
  ClaseDelHorario,
  HorarioDelEstudiante,
} from '../api/horario-apoderado';
import { DiaCoordinador, JornadaDelDia, NOMBRE_DEL_DIA } from '../types';

import { formatRango } from './formato';

/** Los días de la matriz de lectura (`MatrizHoraria`) usan LUNES…VIERNES; el backend, LUN…VIE. */
const DIA_DE_LA_MATRIZ: Partial<Record<DiaCoordinador, DiaSemana>> = {
  LUN: 'LUNES',
  MAR: 'MARTES',
  MIE: 'MIERCOLES',
  JUE: 'JUEVES',
  VIE: 'VIERNES',
};

type Fila =
  | {
      tipo: 'CLASE';
      numero: number;
      nombre: string;
      inicio: string;
      fin: string;
    }
  | { tipo: 'RECREO'; nombre: string; inicio: string; fin: string };

/** Los bloques en orden, con cada recreo justo después del bloque que dice `despuesDeBloque`. */
export const filasDeLaJornada = (jornada: JornadaDelDia): Fila[] => {
  const filas: Fila[] = [];
  let recreos = 0;
  [...jornada.bloques]
    .sort((a, b) => a.numero - b.numero)
    .forEach((b) => {
      filas.push({
        tipo: 'CLASE',
        numero: b.numero,
        nombre: `Bloque ${b.numero}`,
        inicio: b.inicio,
        fin: b.fin,
      });
      jornada.recesos
        .filter((r) => r.despuesDeBloque === b.numero)
        .forEach((r) => {
          recreos += 1;
          filas.push({
            tipo: 'RECREO',
            nombre: `Recreo ${recreos}`,
            inicio: r.inicio,
            fin: r.fin,
          });
        });
    });
  return filas;
};

/** El horario del backend en la forma que dibuja `MatrizHoraria` (filas de bloques y recreos, y sus sesiones). */
export const aMatrizHoraria = (
  jornada: JornadaDelDia,
  clases: ClaseDelHorario[],
): Pick<HorarioSeccion, 'bloques' | 'sesiones'> => {
  const bloques: BloqueHorario[] = filasDeLaJornada(jornada).map((f, i) => ({
    id: f.tipo === 'CLASE' ? `bloque-${f.numero}` : `recreo-${i}`,
    nombre: f.nombre,
    tipo: f.tipo,
    horaInicio: f.inicio,
    horaFin: f.fin,
    orden: i,
  }));
  const sesiones: SesionClase[] = clases.flatMap((c) => {
    const dia = DIA_DE_LA_MATRIZ[c.dia];
    return dia
      ? [
          {
            dia,
            bloqueId: `bloque-${c.numeroBloque}`,
            tipo: 'ASIGNADO' as const,
            cursos: [
              {
                curso: c.curso, // Figma: "Prof. Valeria Montes Ríos".
                docente: c.docente ? `Prof. ${c.docente}` : '',
                aula: c.aula ?? '',
              },
            ],
          },
        ]
      : [];
  });
  return { bloques, sesiones };
};

const escapar = (texto: string) =>
  texto
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');

/**
 * Página HTML imprimible del horario (con la que el navegador lo guarda como PDF): estudiante, año, grado y sección,
 * tutor(a) y la matriz de lunes a viernes con los recreos.
 */
export const htmlDelHorario = (horario: HorarioDelEstudiante): string => {
  const jornada = horario.jornada;
  const dias = (jornada?.dias ?? []).filter((d) => d !== 'SAB');
  const filas = jornada ? filasDeLaJornada(jornada) : [];
  const celda = (dia: DiaCoordinador, numero: number) => {
    const clase = horario.clases.find(
      (c) => c.dia === dia && c.numeroBloque === numero,
    );
    if (!clase) return '<td></td>';
    const detalle = [clase.docente, clase.aula]
      .filter(Boolean)
      .map((t) => escapar(t as string));
    return `<td><strong>${escapar(clase.curso)}</strong>${detalle.map((t) => `<br><small>${t}</small>`).join('')}</td>`;
  };
  const cuerpo = filas
    .map((f) => {
      const hora = `<th>${escapar(f.nombre)}<br><small>${escapar(formatRango(f.inicio, f.fin))}</small></th>`;
      return f.tipo === 'RECREO'
        ? `<tr class="recreo">${hora}<td colspan="${dias.length}">Recreo</td></tr>`
        : `<tr>${hora}${dias.map((d) => celda(d, f.numero)).join('')}</tr>`;
    })
    .join('');
  const tutor = horario.tutor
    ? `<p>Tutor(a): ${escapar(
        [horario.tutor.nombre, horario.tutor.celular, horario.tutor.correo]
          .filter(Boolean)
          .join(' · '),
      )}</p>`
    : '';
  const titulo = `Horario ${horario.anio} · ${horario.nombreCompleto}`;

  return `<!doctype html><html lang="es"><head><meta charset="utf-8"><title>${escapar(titulo)}</title>
<style>
body{font-family:Inter,Arial,sans-serif;color:#1f2937;margin:24px}
h1{font-size:20px;margin:0 0 4px}p{margin:2px 0;font-size:12px}
table{width:100%;border-collapse:collapse;margin-top:16px;font-size:11px}
th,td{border:1px solid #d1d5db;padding:6px;vertical-align:top;text-align:left}
thead th{background:#eef2ff;text-align:center}tr.recreo td{background:#fef9c3;text-align:center}
small{color:#6b7280}
</style></head><body>
<h1>${escapar(titulo)}</h1>
<p>${escapar(`${horario.grado} · Sección ${horario.seccion}`)}</p>${tutor}
<table><thead><tr><th>Bloque</th>${dias.map((d) => `<th>${NOMBRE_DEL_DIA[d]}</th>`).join('')}</tr></thead>
<tbody>${cuerpo}</tbody></table></body></html>`;
};

/** Abre el horario en una pestaña nueva y abre el diálogo de impresión, donde se puede guardar como PDF. */
export const descargarHorario = (horario: HorarioDelEstudiante) => {
  const ventana = window.open('', '_blank');
  if (!ventana) return false;
  ventana.document.write(htmlDelHorario(horario));
  ventana.document.close();
  ventana.focus();
  ventana.print();
  return true;
};
