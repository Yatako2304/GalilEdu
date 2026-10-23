import { CuotaTarifarioVista } from '@/types/api';

import { aNumero, multiplicarMonto, valoresDeTarifario } from '../esquemas';
import {
  ESTADO_TARIFARIO,
  anioPorDefecto,
  fechaDeDia,
  nombreDelMes,
  resumenDeCuotas,
  vencimientoDePension,
} from '../formato';

import { anio, cuotas, propuestaVacia, tarifario2027 } from './datos';

// Textos y reglas de P19 "Tarifario y calendario" (HU-34)

const cuota = (fecha: string): CuotaTarifarioVista => ({
  numero: 1,
  nombre: 'Pensión',
  fechaVencimiento: fecha,
  monto: 620,
});

test('los estados usan los colores acordados: ámbar y verde', () => {
  expect(ESTADO_TARIFARIO).toEqual({
    EN_CONFIGURACION: { texto: 'En configuración', color: 'warning' },
    CONFIRMADO: { texto: 'Confirmado', color: 'success' },
  });
});

test('las fechas y los meses salen de la fecha, sin pasar por la zona horaria', () => {
  expect(fechaDeDia('2027-02-28')).toBe('28/02/2027');
  expect(nombreDelMes('2027-03-31')).toBe('marzo');
  expect(nombreDelMes('2027-12-15')).toBe('diciembre');
});

describe('resumenDeCuotas', () => {
  test('cuenta las cuotas y dice entre qué meses, como en Figma', () => {
    expect(resumenDeCuotas(cuotas(2027, 620))).toBe(
      '10 cuotas (marzo–diciembre)',
    );
  });

  test('con otra cantidad de cuotas sigue al calendario, no al texto de Figma', () => {
    expect(resumenDeCuotas(cuotas(2027, 620).slice(0, 4))).toBe(
      '4 cuotas (marzo–junio)',
    );
    expect(resumenDeCuotas([cuota('2027-03-31')])).toBe('1 cuota (marzo)');
    expect(resumenDeCuotas([])).toBe('Sin cuotas');
  });
});

describe('vencimientoDePension', () => {
  test('con la última cuota el 15, la dice como en Figma', () => {
    expect(vencimientoDePension(cuotas(2027, 620))).toBe(
      'Último día de cada mes · diciembre: 15/12',
    );
  });

  test('si todas vencen el último día del mes, no hay excepción', () => {
    expect(vencimientoDePension(cuotas(2027, 620).slice(0, 4))).toBe(
      'Último día de cada mes',
    );
  });

  test('una última cuota en otro mes y día sigue al calendario', () => {
    expect(
      vencimientoDePension([cuota('2027-03-31'), cuota('2027-11-15')]),
    ).toBe('Último día de cada mes · noviembre: 15/11');
  });

  test('sin cuotas no hay vencimiento que mostrar', () => {
    expect(vencimientoDePension([])).toBe('—');
  });
});

describe('anioPorDefecto', () => {
  const anios = [
    anio(2025, 'CERRADO'),
    anio(2026, 'ACTIVO'),
    anio(2027, 'PLANIFICADO'),
    anio(2028, 'PLANIFICADO'),
  ];

  test('abre con el año que pide la dirección, si existe', () => {
    expect(anioPorDefecto(anios, 'anio-2026')?.anio).toBe(2026);
  });

  test('si no pide uno (o no existe), el PLANIFICADO más reciente', () => {
    expect(anioPorDefecto(anios, null)?.anio).toBe(2028);
    expect(anioPorDefecto(anios, 'anio-9999')?.anio).toBe(2028);
  });

  test('sin PLANIFICADO, el ACTIVO', () => {
    expect(
      anioPorDefecto(
        anios.filter((a) => a.estado !== 'PLANIFICADO'),
        null,
      )?.anio,
    ).toBe(2026);
  });

  test('con todos cerrados, el más reciente; sin años, ninguno', () => {
    expect(
      anioPorDefecto([anio(2024, 'CERRADO'), anio(2025, 'CERRADO')], null)
        ?.anio,
    ).toBe(2025);
    expect(anioPorDefecto([], null)).toBeUndefined();
  });
});

describe('valores del formulario', () => {
  test('un tarifario guardado se muestra tal cual, como texto', () => {
    expect(valoresDeTarifario(tarifario2027())).toEqual({
      montoCuotaIngreso: '500',
      montoMatricula: '450',
      montoPension: '620',
      diasGracia: '5',
      recargoPorcentaje: '2',
      topeRecargoPorcentaje: '10',
    });
  });

  test('una propuesta sin datos del año anterior deja los campos vacíos', () => {
    expect(valoresDeTarifario(propuestaVacia())).toEqual({
      montoCuotaIngreso: '',
      montoMatricula: '',
      montoPension: '',
      diasGracia: '',
      recargoPorcentaje: '',
      topeRecargoPorcentaje: '',
    });
  });

  test('aNumero devuelve null mientras lo escrito no sea un número', () => {
    expect(aNumero('620')).toBe(620);
    expect(aNumero(' 620.5 ')).toBe(620.5);
    expect(aNumero('')).toBeNull();
    expect(aNumero('  ')).toBeNull();
    expect(aNumero('abc')).toBeNull();
  });

  test('el total no arrastra errores de coma flotante', () => {
    expect(multiplicarMonto(620, 10)).toBe(6200);
    expect(multiplicarMonto(620.1, 3)).toBe(1860.3);
  });
});
