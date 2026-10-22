import { FieldErrors, UseFormRegister } from 'react-hook-form';

import { Checkbox, Input, Select } from '@/components/ui/form';
import {
  DISTRITOS_LIMA,
  ESTADOS_CIVILES,
  GRADOS_INSTRUCCION,
  NACIONALIDADES,
  SEXOS,
  TIPOS_DOCUMENTO,
} from '@/config/catalogos';

type PersonaFieldsProps = {
  // El formulario contenedor puede tener cualquier forma; los campos viven en `prefix`.
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  register: UseFormRegister<any>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  errors?: FieldErrors<any>;
  /** Ruta del objeto persona dentro del formulario, ej. "persona" o "padre.persona". */
  prefix: string;
  disabled?: boolean;
  /** Oculta los campos laborales (no aplican al estudiante). */
  sinDatosLaborales?: boolean;
  /** Oculta el documento (cuando se muestra aparte, ej. con botón "Buscar"). */
  sinDocumento?: boolean;
};

const obtenerError = (errors: FieldErrors | undefined, ruta: string) =>
  ruta
    .split('.')
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    .reduce<any>((acc, parte) => (acc ? acc[parte] : undefined), errors);

// Figma > "Nuevo usuario" > DATOS PERSONALES (grilla de 2 columnas).
export const PersonaFields = ({
  register,
  errors,
  prefix,
  disabled,
  sinDatosLaborales,
  sinDocumento,
}: PersonaFieldsProps) => {
  const campo = (nombre: string) => ({
    registration: register(`${prefix}.${nombre}`),
    error: obtenerError(errors, `${prefix}.${nombre}`),
    disabled,
  });

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
      {!sinDocumento && (
        <>
          <Select
            label="Tipo de documento"
            options={TIPOS_DOCUMENTO}
            {...campo('tipoDocumento')}
          />
          <Input
            label="N.° de documento"
            inputMode="numeric"
            {...campo('numeroDocumento')}
          />
        </>
      )}
      <Input label="Apellido paterno" {...campo('apellidoPaterno')} />
      <Input label="Apellido materno" {...campo('apellidoMaterno')} />
      <Input
        label="Nombres"
        wrapperClassName="md:col-span-2"
        {...campo('nombres')}
      />
      <Input
        label="Fecha de nacimiento"
        type="date"
        {...campo('fechaNacimiento')}
      />
      <Select
        label="Nacionalidad"
        options={NACIONALIDADES}
        placeholder="Selecciona"
        {...campo('nacionalidad')}
      />
      <Input label="Lugar de nacimiento" {...campo('lugarNacimiento')} />
      <Select
        label="Sexo"
        options={SEXOS}
        placeholder="Selecciona"
        {...campo('sexo')}
      />
      {!sinDatosLaborales && (
        <>
          <Select
            label="Estado civil"
            options={ESTADOS_CIVILES}
            placeholder="Selecciona"
            {...campo('estadoCivil')}
          />
          <Select
            label="Grado de instrucción"
            options={GRADOS_INSTRUCCION}
            placeholder="Selecciona"
            {...campo('gradoInstruccion')}
          />
        </>
      )}
      <Input
        label="Celular"
        inputMode="tel"
        placeholder="987 111 222"
        {...campo('celular')}
      />
      <Input label="Correo electrónico" type="email" {...campo('correo')} />
      <Input label="Teléfono de casa" {...campo('telefonoCasa')} />
      {!sinDatosLaborales && (
        <>
          <Input label="Profesión u ocupación" {...campo('profesion')} />
          <Input label="Centro de trabajo" {...campo('centroTrabajo')} />
          <Input label="Cargo" {...campo('cargo')} />
        </>
      )}
      <Select
        label="Distrito"
        options={DISTRITOS_LIMA}
        placeholder="Selecciona"
        {...campo('distrito')}
      />
      <Input label="Dirección" {...campo('direccion')} />
      {!sinDatosLaborales && (
        <Checkbox
          className="md:col-span-2"
          label="Es exalumno(a) del colegio"
          disabled={disabled}
          {...register(`${prefix}.esExalumno`)}
        />
      )}
    </div>
  );
};
