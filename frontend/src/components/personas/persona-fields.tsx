import { FieldErrors, UseFormRegister } from 'react-hook-form';

import { Checkbox, Input, Select } from '@/components/ui/form';
import {
  DISTRITOS_LIMA,
  ESTADOS_CIVILES,
  GRADOS_INSTRUCCION,
  NACIONALIDADES,
  PAISES,
  SEXOS,
  TIPOS_DOCUMENTO,
} from '@/config/catalogos';
import {
  restringir,
  soloAlfanumerico,
  soloDigitos,
  soloLetras,
} from '@/utils/restringir-entrada';

import { LIMITES_PERSONA } from './limites-persona';

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
  /** Tipo de documento elegido: con DNI solo se escriben 8 dígitos; si no, hasta 12 letras y números. */
  tipoDocumento?: string;
  /**
   * Se llama al cambiar el tipo de documento, con el número ya filtrado para el tipo nuevo
   * (igual que la inscripción). Si se pasa, el número queda deshabilitado hasta elegir el tipo.
   */
  onTipoDocumentoChange?: (numeroFiltrado: (numero: string) => string) => void;
  /** Formulario de usuario: "Lugar de nacimiento" se pide como "País de nacimiento" con la lista de países. */
  conPaisDeNacimiento?: boolean;
};

/** DNI: solo 8 dígitos; carné de extranjería: letras y números, hasta 12 (CAHU6-6). */
export const filtroDocumento = (tipo?: string) =>
  tipo === 'DNI'
    ? soloDigitos(LIMITES_PERSONA.dni)
    : soloAlfanumerico(LIMITES_PERSONA.carnet);

export const largoDocumento = (tipo?: string) =>
  tipo === 'DNI' ? LIMITES_PERSONA.dni : LIMITES_PERSONA.carnet;

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
  tipoDocumento,
  onTipoDocumentoChange,
  conPaisDeNacimiento,
}: PersonaFieldsProps) => {
  const campo = (nombre: string) => ({
    registration: register(`${prefix}.${nombre}`),
    error: obtenerError(errors, `${prefix}.${nombre}`),
    disabled,
  });

  // Los campos no dejan escribir caracteres no permitidos (CAHU6-6, 10, 12, 21 y 24).
  const filtrado = (nombre: string, filtro: (valor: string) => string) => {
    const base = campo(nombre);
    return { ...base, registration: restringir(base.registration, filtro) };
  };
  const tipoCampo = campo('tipoDocumento');
  const esperaTipo = !!onTipoDocumentoChange && !tipoDocumento;

  return (
    <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
      {!sinDocumento && (
        <>
          <Select
            label="Tipo de documento"
            options={TIPOS_DOCUMENTO}
            placeholder="Selecciona"
            {...tipoCampo}
            registration={{
              ...tipoCampo.registration,
              onChange: (evento) => {
                onTipoDocumentoChange?.(filtroDocumento(evento.target.value));
                return tipoCampo.registration.onChange(evento);
              },
            }}
          />
          <Input
            label="N.° de documento"
            inputMode={tipoDocumento === 'DNI' ? 'numeric' : 'text'}
            maxLength={largoDocumento(tipoDocumento)}
            {...filtrado('numeroDocumento', filtroDocumento(tipoDocumento))}
            disabled={disabled || esperaTipo}
          />
        </>
      )}
      <Input
        label="Apellido paterno"
        {...filtrado('apellidoPaterno', soloLetras(LIMITES_PERSONA.apellido))}
      />
      <Input
        label="Apellido materno"
        {...filtrado('apellidoMaterno', soloLetras(LIMITES_PERSONA.apellido))}
      />
      <Input
        label="Nombres"
        wrapperClassName="md:col-span-2"
        {...filtrado('nombres', soloLetras(LIMITES_PERSONA.nombres))}
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
      {conPaisDeNacimiento ? (
        <Select
          label="País de nacimiento"
          options={PAISES}
          placeholder="Selecciona"
          {...campo('lugarNacimiento')}
        />
      ) : (
        <Input label="Lugar de nacimiento" {...campo('lugarNacimiento')} />
      )}
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
        inputMode="numeric"
        placeholder="987111222"
        {...filtrado('celular', soloDigitos(LIMITES_PERSONA.celular))}
      />
      <Input label="Correo electrónico" type="email" {...campo('correo')} />
      <Input
        label="Teléfono de casa"
        inputMode="numeric"
        {...filtrado('telefonoCasa', soloDigitos(LIMITES_PERSONA.telefonoCasa))}
      />
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
