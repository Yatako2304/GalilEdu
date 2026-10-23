import { GradoConSecciones, SeccionResumen } from '@/types/api';

// Datos de ejemplo de las pruebas de CI-09 (con los valores de Figma).

export const seccion = (
  gradoCodigo: string,
  nombre: string,
  alumnosOcupanVacante: number,
  tutor: string | null,
  cotutor: string | null = null,
): SeccionResumen => ({
  id: `${gradoCodigo}-${nombre}`,
  gradoCodigo,
  nombre,
  capacidadMaxima: 30,
  alumnosOcupanVacante,
  tutorId: tutor ? `tutor-${gradoCodigo}-${nombre}` : null,
  tutor,
  cotutorId: cotutor ? `cotutor-${gradoCodigo}-${nombre}` : null,
  cotutor,
  estado: 'ACTIVA',
});

const grado = (
  numero: number,
  secciones: SeccionResumen[],
): GradoConSecciones => ({
  codigo: `PRI-${numero}`,
  nombre: `${numero}.° de primaria`,
  nivel: 'PRIMARIA',
  numero,
  cantidadSeccionesActivas: secciones.filter((s) => s.estado === 'ACTIVA')
    .length,
  secciones,
});

/** 1.° y 2.° como en Figma; 3.° con una sola sección (singular) y 4.° sin secciones (el backend devuelve los 11 grados). */
export const gradosDeEjemplo: GradoConSecciones[] = [
  grado(1, [
    seccion('PRI-1', 'A', 29, 'Rosa Huamán Quispe', 'Silvia Herrera Luna'),
    seccion('PRI-1', 'B', 24, 'Jorge Salas Medina', 'Julio Castro Rengifo'),
    seccion('PRI-1', 'C', 26, 'Lucía Paredes Ríos', 'Natalia Ríos Garay'),
  ]),
  grado(2, [
    seccion('PRI-2', 'A', 28, 'Martín Céspedes Vela', 'Pedro Villanueva Sosa'),
    seccion('PRI-2', 'B', 23, 'Carmen Rojas Ugarte'),
  ]),
  grado(3, [seccion('PRI-3', 'A', 25, 'Diego Fernández Lazo')]),
  grado(4, []),
];
