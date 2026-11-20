// Regla de Figma para las contraseñas que elige el usuario (HU-4 y HU-5). Es la misma del backend:
// backend/.../usuarios/dominio/PoliticaContrasena.java.
export const REGLA_CONTRASENA =
  'Mínimo 8 caracteres, con al menos una mayúscula, una minúscula, un número y un carácter especial (@, #, $).';

export const MAXIMO_CONTRASENA = 64;

export const cumpleReglaContrasena = (valor: string) =>
  valor.length >= 8 &&
  valor.length <= MAXIMO_CONTRASENA &&
  /\p{Lu}/u.test(valor) &&
  /\p{Ll}/u.test(valor) &&
  /\p{Nd}/u.test(valor) &&
  /[^\p{L}\p{N}\s]/u.test(valor) &&
  !/\s/u.test(valor);
