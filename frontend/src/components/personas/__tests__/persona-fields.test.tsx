import { useForm } from 'react-hook-form';

import { renderApp, screen, userEvent } from '@/testing/test-utils';

import { PersonaFields } from '../persona-fields';
import { personaVacia } from '../persona-schema';

const Formulario = ({ tipoDocumento }: { tipoDocumento: string }) => {
  const form = useForm({
    defaultValues: { persona: { ...personaVacia(), tipoDocumento } },
  });
  return (
    <PersonaFields
      register={form.register}
      prefix="persona"
      tipoDocumento={tipoDocumento}
    />
  );
};

const escribir = async (etiqueta: RegExp, texto: string) => {
  const campo = screen.getByLabelText(etiqueta) as HTMLInputElement;
  await userEvent.type(campo, texto);
  return campo.value;
};

// CAHU6-6, 10, 12, 21 y 24: los campos no dejan escribir caracteres no permitidos.
test('nombres y apellidos no aceptan números', async () => {
  await renderApp(<Formulario tipoDocumento="DNI" />);
  expect(await escribir(/apellido paterno/i, 'Pérez2')).toBe('Pérez');
  expect(await escribir(/apellido materno/i, "O'Brien-3")).toBe("O'Brien-");
  expect(await escribir(/nombres/i, 'María José 1')).toBe('María José ');
});

test('con DNI el documento solo acepta 8 dígitos', async () => {
  await renderApp(<Formulario tipoDocumento="DNI" />);
  expect(await escribir(/n\.° de documento/i, '4123a456789')).toBe('41234567');
});

test('con carné de extranjería acepta hasta 12 letras y números', async () => {
  await renderApp(<Formulario tipoDocumento="CARNET_EXTRANJERIA" />);
  expect(await escribir(/n\.° de documento/i, 'AB-12 34567890X')).toBe(
    'AB1234567890',
  );
});

test('celular y teléfono de casa solo aceptan números', async () => {
  await renderApp(<Formulario tipoDocumento="DNI" />);
  expect(await escribir(/celular/i, '987 111 2229')).toBe('987111222');
  expect(await escribir(/teléfono de casa/i, '01-452 3344')).toBe('014523344');
});

test('el número de documento espera a que se elija el tipo', async () => {
  const Vacio = () => {
    const form = useForm({
      defaultValues: { persona: { ...personaVacia(), tipoDocumento: '' } },
    });
    return (
      <PersonaFields
        register={form.register}
        prefix="persona"
        tipoDocumento={form.watch('persona.tipoDocumento')}
        onTipoDocumentoChange={() => {}}
      />
    );
  };
  await renderApp(<Vacio />);
  expect(screen.getByLabelText(/n\.° de documento/i)).toBeDisabled();
});
