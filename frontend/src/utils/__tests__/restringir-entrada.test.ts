import {
  completarDecimales,
  soloAlfanumerico,
  soloDecimal,
  soloDigitos,
  soloLetras,
} from '../restringir-entrada';

const PESO = { enteros: 3, decimales: 1 };
const ESTATURA = { enteros: 1, decimales: 2 };

describe('soloDecimal', () => {
  test('solo deja números y un punto (la coma no es separador)', () => {
    expect(soloDecimal(PESO)('2a2,5')).toBe('225');
    expect(soloDecimal(PESO)('22.5.1')).toBe('22.5');
  });

  test('respeta el formato: peso ###.#, estatura #.##', () => {
    expect(soloDecimal(PESO)('22.55')).toBe('22.5');
    expect(soloDecimal(PESO)('1234')).toBe('123.4');
    expect(soloDecimal(ESTATURA)('1.185')).toBe('1.18');
  });

  test('pone el punto solo cuando sobran enteros', () => {
    expect(soloDecimal(ESTATURA)('1')).toBe('1');
    expect(soloDecimal(ESTATURA)('11')).toBe('1.1');
    expect(soloDecimal(ESTATURA)('118')).toBe('1.18');
  });
});

describe('completarDecimales', () => {
  test('completa con ceros los decimales que faltan', () => {
    expect(completarDecimales('22', PESO)).toBe('22.0');
    expect(completarDecimales('1.', ESTATURA)).toBe('1.00');
    expect(completarDecimales('1.2', ESTATURA)).toBe('1.20');
    expect(completarDecimales('1.18', ESTATURA)).toBe('1.18');
  });

  test('sin parte entera antepone el 0', () => {
    expect(completarDecimales('.23', ESTATURA)).toBe('0.23');
    expect(completarDecimales('.5', ESTATURA)).toBe('0.50');
    expect(completarDecimales('.', ESTATURA)).toBe('.');
  });

  test('un valor vacío queda vacío', () => {
    expect(completarDecimales('', PESO)).toBe('');
  });
});

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
