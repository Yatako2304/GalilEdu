import {
  soloAlfanumerico,
  soloDigitos,
  soloLetras,
} from '../restringir-entrada';

describe('soloDigitos', () => {
  test('quita letras, espacios y símbolos', () => {
    expect(soloDigitos()('01 452-3344 abc')).toBe('014523344');
  });

  test('respeta el máximo de dígitos', () => {
    expect(soloDigitos(8)('123456789')).toBe('12345678');
  });
});

describe('soloAlfanumerico', () => {
  test('acepta letras y números, sin espacios ni símbolos', () => {
    expect(soloAlfanumerico(12)('A b-1_2/3')).toBe('Ab123');
  });

  test('respeta el máximo de caracteres', () => {
    expect(soloAlfanumerico(12)('ABCDEFGHIJKLMNO')).toBe('ABCDEFGHIJKL');
  });
});

describe('soloLetras', () => {
  test('quita números y símbolos', () => {
    expect(soloLetras()('Mateo 2 Alonso!')).toBe('Mateo  Alonso');
  });

  test('acepta tildes, ñ, espacios, apóstrofo y guion', () => {
    expect(soloLetras()("Ñandú O'Brien-Pérez")).toBe("Ñandú O'Brien-Pérez");
  });

  test('respeta el máximo de caracteres', () => {
    expect(soloLetras(5)('Abcdefgh')).toBe('Abcde');
  });
});
