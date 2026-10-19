import { detalleErroresCsv, validarArchivoCarga } from '../carga-masiva';

const archivo = (nombre: string, bytes = 10) =>
  new File([new Uint8Array(bytes)], nombre);

describe('validarArchivoCarga (HU-8)', () => {
  test('acepta .xlsx, .xls y .csv', () => {
    expect(validarArchivoCarga(archivo('usuarios.xlsx'))).toBeNull();
    expect(validarArchivoCarga(archivo('USUARIOS.XLS'))).toBeNull();
    expect(validarArchivoCarga(archivo('usuarios.csv'))).toBeNull();
  });

  test('rechaza otros formatos y archivos de más de 10 MB', () => {
    expect(validarArchivoCarga(archivo('usuarios.pdf'))).toBe(
      'El archivo debe ser .xlsx, .xls o .csv',
    );
    expect(
      validarArchivoCarga(archivo('usuarios.csv', 10 * 1024 * 1024 + 1)),
    ).toBe('El archivo no puede pesar más de 10 MB');
  });
});

test('detalleErroresCsv escapa comillas y comas', () => {
  expect(
    detalleErroresCsv([
      { fila: 2, mensaje: 'Correo duplicado: a@b.pe ya está registrado' },
      { fila: 5, mensaje: 'Rol no válido: "Jefe", Otro' },
    ]),
  ).toBe(
    'Fila,Error\n"2","Correo duplicado: a@b.pe ya está registrado"\n"5","Rol no válido: ""Jefe"", Otro"',
  );
});
