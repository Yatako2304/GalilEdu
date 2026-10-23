import { DatePicker } from '@/components/ui/date-picker';
import { rtlRender, screen, userEvent, within } from '@/testing/test-utils';

import { Input } from '../input';
import { Select } from '../select';
import { Textarea } from '../textarea';

// Figma > Componentes > Text Field: la etiqueta flota sobre el borde superior del campo.

const etiqueta = (texto: string) => screen.getByText(texto);

test('la etiqueta va dentro del campo y sigue nombrando al input', () => {
  rtlRender(<Input label="Nombre de la sección *" />);

  const campo = screen.getByLabelText('Nombre de la sección *');
  // Sigue dentro del <label>, así que el lector de pantalla y `getByLabelText` la asocian al campo.
  expect(campo.closest('label')).toContainElement(
    etiqueta('Nombre de la sección *'),
  );
  expect(etiqueta('Nombre de la sección *')).toHaveClass(
    'absolute',
    'text-label-3',
    'pointer-events-none',
  );
});

test('sin placeholder propio el campo usa uno vacío para saber si está vacío', () => {
  rtlRender(<Input label="Capacidad" />);

  expect(screen.getByLabelText('Capacidad')).toHaveAttribute(
    'placeholder',
    ' ',
  );
});

test('un placeholder propio se respeta', () => {
  rtlRender(<Input label="Teléfono" placeholder="987 111 222" />);

  expect(screen.getByPlaceholderText('987 111 222')).toBe(
    screen.getByLabelText('Teléfono'),
  );
});

// Con un placeholder propio `placeholder-shown` también es verdadero: la etiqueta no puede bajar encima de él.
test('con un placeholder propio la etiqueta se queda arriba y sin estilos de reposo', () => {
  rtlRender(<Input label="Usuario" placeholder="U20260001" />);

  expect(etiqueta('Usuario').className).not.toContain('peer-placeholder-shown');
});

test('sin placeholder propio la etiqueta puede descansar dentro del campo', () => {
  rtlRender(<Input label="Usuario" />);

  expect(etiqueta('Usuario').className).toContain(
    'peer-placeholder-shown:top-3',
  );
});

// Un adorno al inicio (el "S/" de un monto) ocupa el lugar donde descansaría la etiqueta.
test('con un adorno al inicio la etiqueta se queda arriba para no taparlo', () => {
  rtlRender(<Input label="Monto (S/)" startAdornment="S/" />);

  expect(etiqueta('Monto (S/)').className).not.toContain(
    'peer-placeholder-shown',
  );
});

// En reposo la etiqueta está dentro del campo: sin fondo propio, para que no se vea un parche blanco en un campo deshabilitado.
test('en reposo la etiqueta no tiene fondo blanco y al enfocar lo recupera', () => {
  rtlRender(<Input label="N.º de documento" disabled />);

  expect(etiqueta('N.º de documento')).toHaveClass(
    'peer-placeholder-shown:bg-transparent',
    'peer-focus:bg-white',
  );
});

test('sin etiqueta no hay nada flotando ni placeholder agregado', () => {
  rtlRender(<Input aria-label="Buscar" />);

  expect(screen.getByLabelText('Buscar')).not.toHaveAttribute('placeholder');
});

test('el texto escrito queda en el campo', async () => {
  rtlRender(<Input label="Nombre" />);

  await userEvent.type(screen.getByLabelText('Nombre'), 'Ana');

  expect(screen.getByLabelText('Nombre')).toHaveValue('Ana');
});

test('con error la etiqueta se pone roja y el campo queda inválido', () => {
  rtlRender(<Input label="Nombre" error={{ type: 'x', message: 'Falta' }} />);

  expect(etiqueta('Nombre')).toHaveClass('text-content-error');
  expect(screen.getByLabelText('Nombre')).toBeInvalid();
});

test('la ayuda va debajo del campo', () => {
  rtlRender(<Input label="Nombre" hint="Ej.: A, B, C" />);

  expect(screen.getByText('Ej.: A, B, C')).toBeInTheDocument();
});

describe('campo de fecha', () => {
  // Va junto a otros campos (por ejemplo, un Select): si su etiqueta fuera arriba, los dos quedarían desalineados.
  test('tiene la misma etiqueta flotante que los demás campos', () => {
    rtlRender(<Input type="date" label="Fecha de nacimiento" />);

    expect(etiqueta('Fecha de nacimiento')).toHaveClass(
      'absolute',
      'pointer-events-none',
    );
  });

  test('el botón del calendario no cambia de nombre accesible al descansar la etiqueta', () => {
    rtlRender(<Input type="date" label="Fecha de nacimiento" />);

    expect(
      screen.getByRole('button', { name: 'DD/MM/AAAA' }),
    ).toBeInTheDocument();
  });

  test('vacío y sin placeholder, la etiqueta descansa dentro y "DD/MM/AAAA" no se ve', () => {
    rtlRender(<Input type="date" label="Fecha de nacimiento" />);

    expect(etiqueta('Fecha de nacimiento')).toHaveClass('top-3');
    expect(screen.getByRole('button', { name: 'DD/MM/AAAA' })).toHaveClass(
      'text-transparent',
    );
  });

  test('con una fecha elegida la etiqueta sube y se ve la fecha', () => {
    rtlRender(
      <DatePicker
        label="Fecha de nacimiento"
        value="2009-11-02"
        onChange={vi.fn()}
      />,
    );

    expect(etiqueta('Fecha de nacimiento')).not.toHaveClass('top-3');
    expect(screen.getByText('02/11/2009')).toBeInTheDocument();
  });

  test('sin etiqueta sigue mostrando "DD/MM/AAAA"', () => {
    rtlRender(<DatePicker value="" onChange={vi.fn()} />);

    expect(screen.getByText('DD/MM/AAAA')).toBeInTheDocument();
  });

  test('con placeholder propio la etiqueta queda arriba y se ve el placeholder', () => {
    rtlRender(
      <DatePicker
        label="Fecha"
        placeholder="Elige una fecha"
        value=""
        onChange={vi.fn()}
      />,
    );

    expect(etiqueta('Fecha')).not.toHaveClass('top-3');
    expect(screen.getByText('Elige una fecha')).toBeVisible();
  });
});

// Select y Textarea comparten la etiqueta flotante: así los campos de una misma fila quedan alineados.
describe('Select y Textarea', () => {
  const opciones = [{ label: 'Ana', value: 'a' }];

  test('Select sin nada elegido ni placeholder: la etiqueta descansa dentro', () => {
    rtlRender(<Select label="Nivel" options={[]} />);

    expect(etiqueta('Nivel')).toHaveClass('top-3', 'absolute');
  });

  test('Select con placeholder propio: la etiqueta se queda arriba', () => {
    rtlRender(
      <Select label="Grado" placeholder="Elige un grado" options={opciones} />,
    );

    expect(etiqueta('Grado')).not.toHaveClass('top-3');
    expect(etiqueta('Grado')).toHaveClass('top-[-7px]');
  });

  test('Select con valor elegido: la etiqueta sube', () => {
    rtlRender(<Select label="Nivel" options={opciones} defaultValue="a" />);

    expect(etiqueta('Nivel')).not.toHaveClass('top-3');
  });

  test('Textarea con placeholder propio: la etiqueta se queda arriba, sin estilos de reposo', () => {
    rtlRender(<Textarea label="Descripción" placeholder="Qué desarrolla" />);

    expect(etiqueta('Descripción').className).not.toContain(
      'peer-placeholder-shown',
    );
  });

  test('Textarea sin placeholder: la etiqueta descansa a la altura del relleno y cambia con foco y hover', () => {
    rtlRender(<Textarea label="Descripción" />);

    expect(etiqueta('Descripción')).toHaveClass(
      'peer-placeholder-shown:top-4',
      'peer-focus:text-content-accent-primary',
      'peer-hover:text-content-tertiary-hover',
    );
  });

  test('Select vaciable con placeholder: la etiqueta queda arriba y "Selecciona" se ve como opción', async () => {
    rtlRender(
      <Select
        label="Sexo"
        placeholder="Selecciona"
        vaciable
        options={[{ label: 'Femenino', value: 'F' }]}
      />,
    );

    expect(etiqueta('Sexo')).not.toHaveClass('top-3');
    await userEvent.click(screen.getByRole('combobox', { expanded: false }));
    expect(
      within(screen.getByRole('listbox'))
        .getAllByRole('option')
        .map((o) => o.textContent?.trim()),
    ).toEqual(['Selecciona', 'Femenino']);
  });

  test('Textarea con contador: sigue mostrando "n/máximo" con la etiqueta flotante', async () => {
    rtlRender(<Textarea label="Descripción" maxLength={50} showCount />);

    await userEvent.type(screen.getByLabelText('Descripción'), 'Hola');

    expect(screen.getByText('4/50')).toBeInTheDocument();
    expect(etiqueta('Descripción')).toHaveClass('absolute');
  });

  test('Textarea: la etiqueta flota y el campo conserva su nombre accesible', () => {
    rtlRender(<Textarea label="Descripción" />);

    expect(screen.getByLabelText('Descripción')).toHaveAttribute(
      'placeholder',
      ' ',
    );
    expect(etiqueta('Descripción')).toHaveClass('absolute');
    // Text Area de Figma (Large): relleno de 12/16 px y texto de 16 px, así el texto no queda pegado a la etiqueta.
    expect(screen.getByLabelText('Descripción')).toHaveClass(
      'px-3',
      'py-4',
      'text-label-1',
    );
  });
});
