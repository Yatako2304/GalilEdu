// Opciones de los selects que se repiten en varios formularios (Figma > Nuevo usuario / Inscripción).
// Si el backend llega a tener tablas de catálogo, reemplaza estas constantes por una consulta.

import { SelectOption } from '@/components/ui/form';

const opciones = (valores: string[]): SelectOption[] =>
  valores.map((v) => ({ label: v, value: v }));

export const TIPOS_DOCUMENTO: SelectOption[] = [
  { label: 'DNI', value: 'DNI' },
  { label: 'Carné de extranjería', value: 'CARNET_EXTRANJERIA' },
];

export const SEXOS: SelectOption[] = [
  { label: 'Masculino', value: 'MASCULINO' },
  { label: 'Femenino', value: 'FEMENINO' },
];

export const ESTADOS_CIVILES = opciones([
  'Soltero(a)',
  'Casado(a)',
  'Conviviente',
  'Divorciado(a)',
  'Viudo(a)',
]);

export const GRADOS_INSTRUCCION = opciones([
  'Primaria',
  'Secundaria',
  'Superior técnica',
  'Superior universitaria',
  'Posgrado',
]);

export const NACIONALIDADES = opciones([
  'Peruana',
  'Venezolana',
  'Colombiana',
  'Argentina',
  'Chilena',
  'Ecuatoriana',
  'Boliviana',
  'Otra',
]);

export const DISTRITOS_LIMA = opciones([
  'Ate',
  'Barranco',
  'Breña',
  'Callao',
  'Carabayllo',
  'Chorrillos',
  'Comas',
  'El Agustino',
  'Independencia',
  'Jesús María',
  'La Molina',
  'La Victoria',
  'Lima',
  'Lince',
  'Los Olivos',
  'Magdalena del Mar',
  'Miraflores',
  'Pueblo Libre',
  'Puente Piedra',
  'Rímac',
  'San Borja',
  'San Isidro',
  'San Juan de Lurigancho',
  'San Juan de Miraflores',
  'San Luis',
  'San Martín de Porres',
  'San Miguel',
  'Santa Anita',
  'Santiago de Surco',
  'Surquillo',
  'Villa El Salvador',
  'Villa María del Triunfo',
  'Otro',
]);

// El colegio tiene primaria y secundaria (catálogo configuracion.grado, V4).
export const NIVELES: SelectOption[] = [
  { label: 'Primaria', value: 'PRIMARIA' },
  { label: 'Secundaria', value: 'SECUNDARIA' },
];

const grados = (prefijo: string, cantidad: number): SelectOption[] =>
  Array.from({ length: cantidad }, (_, i) => ({
    label: `${i + 1}.° grado`,
    value: `${prefijo}-${i + 1}`,
  }));

/** El valor es el código del catálogo de grados (PRI-1…PRI-6, SEC-1…SEC-5). */
export const GRADOS_POR_NIVEL: Record<string, SelectOption[]> = {
  PRIMARIA: grados('PRI', 6),
  SECUNDARIA: grados('SEC', 5),
};

/** "SEC-1" → "1.° de secundaria" (mismo texto que configuracion.grado.nombre). */
export const nombreGrado = (codigo: string) => {
  const [prefijo, numero] = codigo.split('-');
  return `${numero}.° de ${prefijo === 'PRI' ? 'primaria' : 'secundaria'}`;
};

export const TIPOS_SANGRE = opciones([
  'O+',
  'O-',
  'A+',
  'A-',
  'B+',
  'B-',
  'AB+',
  'AB-',
  'No sabe',
]);

export const TIPOS_SEGURO = opciones([
  'EsSalud',
  'SIS',
  'Seguro privado',
  'Ninguno',
]);
