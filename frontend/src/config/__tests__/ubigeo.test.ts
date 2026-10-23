import {
  PAIS_CON_UBIGEO,
  Ubigeo,
  cargarUbigeo,
  departamentosDe,
  distritosDe,
  provinciasDe,
} from '../ubigeo';

let ubigeo: Ubigeo;

beforeAll(async () => {
  ubigeo = await cargarUbigeo();
});

const valores = (opciones: { value: string | number }[]) =>
  opciones.map((o) => o.value);

describe('catálogo de ubigeo (INEI)', () => {
  test('trae los 25 departamentos, 196 provincias y 1893 distritos', () => {
    const provincias = ubigeo.flatMap((d) => d.p);
    expect(ubigeo).toHaveLength(25);
    expect(provincias).toHaveLength(196);
    expect(provincias.reduce((total, p) => total + p.d.length, 0)).toBe(1893);
  });

  test('no repite distritos dentro de una provincia ni deja listas vacías', () => {
    ubigeo
      .flatMap((d) => d.p)
      .forEach((p) => {
        expect(p.d.length).toBeGreaterThan(0);
        expect(new Set(p.d).size).toBe(p.d.length);
      });
  });

  test('los departamentos llevan tilde y Callao es un departamento', () => {
    const departamentos = valores(departamentosDe(ubigeo));
    expect(departamentos).toEqual(
      expect.arrayContaining(['Áncash', 'Junín', 'Huánuco', 'Callao']),
    );
  });

  test('las provincias dependen del departamento elegido', () => {
    expect(valores(provinciasDe(ubigeo, 'Callao'))).toEqual(['Callao']);
    expect(valores(provinciasDe(ubigeo, 'Lima'))).toContain('Cañete');
    expect(valores(provinciasDe(ubigeo, 'Lima'))).not.toContain('Trujillo');
    expect(provinciasDe(ubigeo, '')).toEqual([]);
  });

  test('los distritos dependen de la provincia elegida', () => {
    const lima = valores(distritosDe(ubigeo, 'Lima', 'Lima'));
    expect(lima).toEqual(expect.arrayContaining(['Miraflores', 'Rímac']));
    expect(lima).not.toContain('Ventanilla');
    expect(valores(distritosDe(ubigeo, 'Callao', 'Callao'))).toContain(
      'Ventanilla',
    );
    expect(distritosDe(ubigeo, 'Lima', '')).toEqual([]);
  });

  test('solo los nacidos en el Perú usan este catálogo', () => {
    expect(PAIS_CON_UBIGEO).toBe('Perú');
  });
});
