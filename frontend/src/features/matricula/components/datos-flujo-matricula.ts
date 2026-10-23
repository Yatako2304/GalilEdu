export type EstadoFlujoMatricula =
  'SECCION_RESERVADA' | 'PENDIENTE_PAGO' | 'MATRICULADO';

export type MatriculaFlujo = {
  id: string;
  estudianteId?: string;
  nombre: string;
  dni: string;
  tipo: 'Nuevo' | 'Continuador';
  grado: string;
  seccion: string;
  tutor: string;
  estado: EstadoFlujoMatricula;
  codigo?: string;
  apoderado: string;
  contacto: string;
  emergencia: string;
  informacionMedica: string;
};

// Datos de navegación para las maquetas A3/A5/B6/B7. Los dos UUID corresponden
// a las matrículas de prueba usadas por las APIs reales de HU-36 y HU-37.
export const MATRICULAS_FLUJO: MatriculaFlujo[] = [
  {
    id: 'maqueta-seccion-reservada',
    nombre: 'Camila Torres Huamán',
    dni: '77894512',
    tipo: 'Continuador',
    grado: '3.° de secundaria',
    seccion: '3.° B',
    tutor: 'Raúl Vargas',
    estado: 'SECCION_RESERVADA',
    apoderado: 'Lucía Huamán Paz',
    contacto: '945 210 778 · lhuaman@correo.com',
    emergencia: '999999999',
    informacionMedica: 'Alergias',
  },
  {
    id: '50000000-0000-0000-0000-000000000002',
    nombre: 'Mateo Paredes',
    dni: '74125836',
    tipo: 'Continuador',
    grado: '1.° de primaria',
    seccion: '1.° A',
    tutor: 'Andrea Salazar',
    estado: 'PENDIENTE_PAGO',
    apoderado: 'Valeria Paredes',
    contacto: '987 654 321 · vp@example.com',
    emergencia: '987654320',
    informacionMedica: 'Sin observaciones',
  },
  {
    id: '50000000-0000-0000-0000-000000000001',
    estudianteId: '40000000-0000-0000-0000-000000000001',
    nombre: 'Sofía Paredes',
    dni: '72845126',
    tipo: 'Continuador',
    grado: '4.° de primaria',
    seccion: '4.° A',
    tutor: 'María Rojas',
    estado: 'MATRICULADO',
    codigo: 'MAT-2026-0001',
    apoderado: 'Valeria Paredes',
    contacto: '987 654 321 · vp@example.com',
    emergencia: '987654320',
    informacionMedica: 'Sin observaciones',
  },
  {
    id: 'maqueta-valentina',
    nombre: 'Valentina Quispe Ramos',
    dni: '78451236',
    tipo: 'Nuevo',
    grado: '2.° de secundaria',
    seccion: '2.° A',
    tutor: 'Patricia León',
    estado: 'SECCION_RESERVADA',
    apoderado: 'Rosa Ramos Díaz',
    contacto: '956 110 203 · rramos@correo.com',
    emergencia: '956110204',
    informacionMedica: 'Sin observaciones',
  },
  {
    id: 'maqueta-sebastian',
    nombre: 'Sebastián Núñez Acosta',
    dni: '76325814',
    tipo: 'Continuador',
    grado: '5.° de secundaria',
    seccion: '5.° B',
    tutor: 'Luis Prado',
    estado: 'MATRICULADO',
    codigo: 'MAT-2027-0041',
    apoderado: 'Paola Acosta Ruiz',
    contacto: '966 220 104 · pacosta@correo.com',
    emergencia: '966220105',
    informacionMedica: 'Asma',
  },
  {
    id: 'maqueta-isabella',
    nombre: 'Isabella Castillo Núñez',
    dni: '77125896',
    tipo: 'Continuador',
    grado: '4.° de secundaria',
    seccion: '4.° B',
    tutor: 'Carmen Flores',
    estado: 'PENDIENTE_PAGO',
    apoderado: 'Marcos Castillo Vega',
    contacto: '944 180 250 · mcastillo@correo.com',
    emergencia: '944180251',
    informacionMedica: 'Sin observaciones',
  },
];

export const getMatriculaFlujo = (id?: string) =>
  MATRICULAS_FLUJO.find((matricula) => matricula.id === id);
