import { EstadoMatricula } from '@/types/api';

const estadosConSeccionAsignada = new Set<EstadoMatricula>([
  'MATRICULADO',
  'RETIRADO',
]);

export const seccionMatricula = (
  estado?: EstadoMatricula | null,
  seccion?: string | null,
) =>
  estado && estadosConSeccionAsignada.has(estado) && seccion
    ? seccion
    : 'Por asignar';
