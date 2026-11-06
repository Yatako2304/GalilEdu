import { EstadoMatriculaApoderado } from '@/types/api';

const escapar = (texto: string) =>
  texto
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');

const fecha = (iso: string) =>
  new Date(iso).toLocaleString('es-PE', {
    dateStyle: 'long',
    timeStyle: 'short',
  });

/** HTML imprimible de la constancia de matrícula (Figma Apoderado › matrícula confirmada). */
export const htmlDeLaConstancia = (estado: EstadoMatriculaApoderado) => {
  const m = estado.matricula;
  const filas: [string, string][] = [
    ['Estudiante', estado.estudiante.nombre],
    ['Grado', m?.grado ?? estado.estudiante.grado],
    ['Sección', m?.seccion ?? 'Por definir'],
    ['Año escolar', String(estado.periodo.anio)],
    ['Código de matrícula', m?.codigo ?? '—'],
  ];
  if (m?.confirmadoEn) filas.push(['Matriculado el', fecha(m.confirmadoEn)]);
  const pago = estado.pagoMatricula;
  if (pago) {
    filas.push(['Cuota de matrícula', `S/ ${pago.monto.toFixed(2)}`]);
    filas.push(['Responsable', pago.responsable]);
  }
  const titulo = `Constancia de matrícula ${estado.periodo.anio}`;
  return `<!doctype html><html lang="es"><head><meta charset="utf-8"><title>${escapar(titulo)}</title>
<style>
body{font-family:Inter,Arial,sans-serif;color:#1f2937;margin:40px}
h1{font-size:22px;margin:0 0 4px}p{font-size:13px;color:#6b7280}
table{width:100%;border-collapse:collapse;margin-top:24px;font-size:14px}
th,td{border-bottom:1px solid #e5e7eb;padding:10px;text-align:left}th{width:35%;color:#6b7280;font-weight:500}
</style></head><body>
<h1>${escapar(titulo)}</h1><p>GalilEdu</p>
<table><tbody>${filas
    .map(([k, v]) => `<tr><th>${escapar(k)}</th><td>${escapar(v)}</td></tr>`)
    .join('')}</tbody></table></body></html>`;
};

/** Abre la constancia en una pestaña nueva y el diálogo de impresión (guardar como PDF). */
export const descargarConstancia = (estado: EstadoMatriculaApoderado) => {
  const ventana = window.open('', '_blank');
  if (!ventana) return false;
  ventana.document.write(htmlDeLaConstancia(estado));
  ventana.document.close();
  ventana.focus();
  ventana.print();
  return true;
};
