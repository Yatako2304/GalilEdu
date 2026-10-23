import {
  FORMATOS_PLANTILLA,
  TAMANO_MAXIMO_PLANTILLA,
  validarPlantilla,
} from '../plantilla';

// Plantilla de un documento requerido (HU-23): XLSX, XLS o CSV de hasta 10 MB, con los mensajes del backend.

test('acepta XLSX, XLS y CSV, sin importar las mayúsculas', () => {
  for (const name of ['ficha.xlsx', 'ficha.XLS', 'Ficha de matrícula.Csv']) {
    expect(validarPlantilla({ name, size: 1024 })).toBeNull();
  }
});

test('rechaza otros formatos', () => {
  for (const name of ['ficha.pdf', 'ficha.docx', 'ficha', 'xlsx']) {
    expect(validarPlantilla({ name, size: 1024 })).toBe(
      'La plantilla debe ser un archivo XLSX, XLS o CSV.',
    );
  }
});

test('el límite es de 10 MB: justo 10 MB sirve y un byte más no', () => {
  expect(
    validarPlantilla({ name: 'a.csv', size: TAMANO_MAXIMO_PLANTILLA }),
  ).toBeNull();
  expect(
    validarPlantilla({ name: 'a.csv', size: TAMANO_MAXIMO_PLANTILLA + 1 }),
  ).toBe('La plantilla no puede pesar más de 10 MB.');
});

test('un archivo vacío no sirve', () => {
  expect(validarPlantilla({ name: 'a.csv', size: 0 })).toBe(
    'Selecciona un archivo para la plantilla.',
  );
});

test('los formatos del selector de archivos coinciden con los permitidos', () => {
  expect(FORMATOS_PLANTILLA).toBe('.xlsx,.xls,.csv');
});
