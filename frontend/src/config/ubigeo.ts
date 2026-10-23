import { SelectOption } from '@/components/ui/form';

// Departamentos, provincias y distritos del Perú (INEI): 25 · 196 · 1893.
// Datos de jmcastagnetto/ubigeo (MIT, https://github.com/jmcastagnetto/ubigeo), convertidos a nombres en
// formato título; solo los departamentos, las provincias y los distritos de Lima y Callao llevan tilde.

export type ProvinciaUbigeo = { n: string; d: string[] };
export type DepartamentoUbigeo = { n: string; p: ProvinciaUbigeo[] };
export type Ubigeo = DepartamentoUbigeo[];

/** Solo los nacidos en este país tienen departamento, provincia y distrito. */
export const PAIS_CON_UBIGEO = 'Perú';

/** Se descarga aparte (~28 KB) para no pesar en el resto de la aplicación. */
export const cargarUbigeo = (): Promise<Ubigeo> =>
  import('./ubigeo.json').then((m) => m.default as Ubigeo);

const opciones = (nombres: string[]): SelectOption[] =>
  nombres.map((n) => ({ label: n, value: n }));

export const departamentosDe = (ubigeo: Ubigeo): SelectOption[] =>
  opciones(ubigeo.map((d) => d.n));

export const provinciasDe = (
  ubigeo: Ubigeo,
  departamento: string,
): SelectOption[] =>
  opciones(ubigeo.find((d) => d.n === departamento)?.p.map((p) => p.n) ?? []);

export const distritosDe = (
  ubigeo: Ubigeo,
  departamento: string,
  provincia: string,
): SelectOption[] =>
  opciones(
    ubigeo.find((d) => d.n === departamento)?.p.find((p) => p.n === provincia)
      ?.d ?? [],
  );
