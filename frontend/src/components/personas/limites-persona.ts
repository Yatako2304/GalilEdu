/**
 * Largos de los datos personales. Mismos valores que las columnas de personas.persona (V1) y que
 * backend/personas/aplicacion/DatosPersona.java; los usan los campos que limitan lo que se escribe.
 */
export const LIMITES_PERSONA = {
  /** DNI: exactamente 8 dígitos. */
  dni: 8,
  /** Carné de extranjería: hasta 12 letras y números. */
  carnet: 12,
  apellido: 80,
  nombres: 120,
  celular: 9,
  telefonoCasa: 15,
  direccion: 200,
} as const;
