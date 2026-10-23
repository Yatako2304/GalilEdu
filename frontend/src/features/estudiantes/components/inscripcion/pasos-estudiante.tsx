import { CircleCheck } from 'lucide-react';
import * as React from 'react';
import { UseFormReturn } from 'react-hook-form';

import { Input, Select, Textarea } from '@/components/ui/form';
import {
  DISTRITOS_LIMA,
  GRADOS_POR_NIVEL,
  LENGUAS_MATERNAS,
  NACIONALIDADES_ESTUDIANTE,
  NIVELES,
  nombreGrado,
  PAISES,
  SEGUNDAS_LENGUAS,
  SEXOS,
  TIPOS_DOCUMENTO,
  TIPOS_SANGRE,
  TIPOS_SEGURO,
} from '@/config/catalogos';
import {
  PAIS_CON_UBIGEO,
  Ubigeo,
  cargarUbigeo,
  departamentosDe,
  distritosDe,
  provinciasDe,
} from '@/config/ubigeo';
import {
  restringir,
  soloAlfanumerico,
  soloDigitos,
  soloLetras,
} from '@/utils/restringir-entrada';

import { Grilla, SeccionPaso, SiNo, Subtitulo } from './campos';
import { InscripcionForm, gradoSugerido } from './esquema';

type PasoProps = { form: UseFormReturn<InscripcionForm> };

const opciones = (valores: string[]) =>
  valores.map((v) => ({ label: v, value: v }));

// Se inscribe para el año en curso o el siguiente.
const anioActual = new Date().getFullYear();
const aniosAcademicos = opciones([String(anioActual), String(anioActual + 1)]);

// Paso 1 · Figma > "Admin / Estudiantes | Inscripción | Paso 1 - Estudiante"
export const PasoEstudiante = ({ form }: PasoProps) => {
  const { register, formState, watch, setValue, getValues, clearErrors } = form;
  const errores = formState.errors.estudiante;
  const errorPersona = errores?.persona;
  const [fecha, anio, nivel, pais, departamento, provincia] = watch([
    'estudiante.persona.fechaNacimiento',
    'estudiante.anioAcademico',
    'estudiante.nivel',
    'estudiante.paisNacimiento',
    'estudiante.departamentoNacimiento',
    'estudiante.provinciaNacimiento',
  ]);
  const sugerido = gradoSugerido(fecha, Number(anio));

  // Lugar de nacimiento: departamento → provincia → distrito, solo para nacidos en el Perú.
  const [ubigeo, setUbigeo] = React.useState<Ubigeo>([]);
  React.useEffect(() => {
    let activo = true;
    cargarUbigeo().then((u) => activo && setUbigeo(u));
    return () => {
      activo = false;
    };
  }, []);
  const enPeru = pais === PAIS_CON_UBIGEO;
  const vaciarNacimiento = (
    ...campos: ('departamento' | 'provincia' | 'distrito')[]
  ) =>
    campos.forEach((c) => {
      const nombre = `estudiante.${c}Nacimiento` as const;
      setValue(nombre, '');
      clearErrors(nombre);
    });

  // DNI: solo 8 dígitos; carné de extranjería: letras y números, hasta 12 (CAHU7-6).
  const tipoDocumento = watch('estudiante.persona.tipoDocumento');
  const filtroDocumento = (tipo: string) =>
    tipo === 'DNI' ? soloDigitos(8) : soloAlfanumerico(12);

  return (
    <SeccionPaso
      titulo="Datos del estudiante"
      descripcion="Campos según la Ficha Única de Matrícula (Minedu) y la ficha del colegio."
    >
      <section className="flex flex-col gap-4">
        <Subtitulo>Identificación</Subtitulo>
        <Grilla columnas={2}>
          <Select
            label="Tipo de documento"
            options={TIPOS_DOCUMENTO}
            placeholder="Selecciona"
            registration={register('estudiante.persona.tipoDocumento', {
              onChange: (e) =>
                setValue(
                  'estudiante.persona.numeroDocumento',
                  filtroDocumento(e.target.value)(
                    getValues('estudiante.persona.numeroDocumento'),
                  ),
                ),
            })}
            error={errorPersona?.tipoDocumento}
          />
          <Input
            label="N.° de documento"
            disabled={!tipoDocumento}
            inputMode={tipoDocumento === 'DNI' ? 'numeric' : 'text'}
            maxLength={tipoDocumento === 'DNI' ? 8 : 12}
            registration={restringir(
              register('estudiante.persona.numeroDocumento'),
              filtroDocumento(tipoDocumento),
            )}
            error={errorPersona?.numeroDocumento}
          />
        </Grilla>
        <Grilla>
          <Input
            label="Apellido paterno"
            maxLength={80}
            registration={restringir(
              register('estudiante.persona.apellidoPaterno'),
              soloLetras(80),
            )}
            error={errorPersona?.apellidoPaterno}
          />
          <Input
            label="Apellido materno (opcional)"
            maxLength={80}
            registration={restringir(
              register('estudiante.persona.apellidoMaterno'),
              soloLetras(80),
            )}
            error={errorPersona?.apellidoMaterno}
          />
          <Input
            label="Nombres"
            maxLength={120}
            registration={restringir(
              register('estudiante.persona.nombres'),
              soloLetras(120),
            )}
            error={errorPersona?.nombres}
          />
        </Grilla>
        <Grilla>
          <Select
            label="Sexo"
            options={SEXOS}
            placeholder="Selecciona"
            registration={register('estudiante.persona.sexo')}
            error={errorPersona?.sexo}
          />
          <Input
            label="Fecha de nacimiento"
            type="date"
            registration={register('estudiante.persona.fechaNacimiento')}
            error={errorPersona?.fechaNacimiento}
          />
          <Select
            label="Nacionalidad"
            options={NACIONALIDADES_ESTUDIANTE}
            placeholder="Selecciona"
            registration={register('estudiante.persona.nacionalidad')}
            error={errorPersona?.nacionalidad}
          />
        </Grilla>
      </section>

      <section className="flex flex-col gap-4">
        <Subtitulo>Lugar de nacimiento</Subtitulo>
        <Grilla columnas={4}>
          <Select
            label="País"
            options={PAISES}
            placeholder="Selecciona"
            registration={register('estudiante.paisNacimiento', {
              onChange: (e) => {
                if (e.target.value !== PAIS_CON_UBIGEO) {
                  vaciarNacimiento('departamento', 'provincia', 'distrito');
                }
              },
            })}
            error={errores?.paisNacimiento}
          />
          <Select
            label="Departamento"
            options={departamentosDe(ubigeo)}
            placeholder="Selecciona"
            disabled={!enPeru || ubigeo.length === 0}
            registration={register('estudiante.departamentoNacimiento', {
              onChange: () => vaciarNacimiento('provincia', 'distrito'),
            })}
            error={errores?.departamentoNacimiento}
          />
          <Select
            label="Provincia"
            options={provinciasDe(ubigeo, departamento)}
            placeholder="Selecciona"
            disabled={!enPeru || !departamento}
            registration={register('estudiante.provinciaNacimiento', {
              onChange: () => vaciarNacimiento('distrito'),
            })}
            error={errores?.provinciaNacimiento}
          />
          <Select
            label="Distrito"
            options={distritosDe(ubigeo, departamento, provincia)}
            placeholder="Selecciona"
            disabled={!enPeru || !provincia}
            registration={register('estudiante.distritoNacimiento')}
            error={errores?.distritoNacimiento}
          />
        </Grilla>
      </section>

      <section className="flex flex-col gap-4 border-t border-stroke-tertiary pt-5">
        <Subtitulo>Domicilio y contacto</Subtitulo>
        <Grilla>
          <Input
            label="Dirección"
            maxLength={200}
            registration={register('estudiante.persona.direccion')}
            error={errorPersona?.direccion}
          />
          <Select
            label="Distrito"
            options={DISTRITOS_LIMA}
            placeholder="Selecciona"
            registration={register('estudiante.persona.distrito')}
            error={errorPersona?.distrito}
          />
          <Input
            label="Referencia (opcional)"
            maxLength={200}
            registration={register('estudiante.referenciaDomicilio')}
          />
          <Input
            label="Teléfono de casa (opcional)"
            inputMode="numeric"
            maxLength={9}
            registration={restringir(
              register('estudiante.persona.telefonoCasa'),
              soloDigitos(9),
            )}
            error={errorPersona?.telefonoCasa}
          />
          <Input
            label="Correo electrónico (opcional)"
            type="email"
            registration={register('estudiante.persona.correo')}
            error={errorPersona?.correo}
          />
          <Select
            label="¿Tiene acceso a internet / dispositivo?"
            options={[
              { label: 'Sí', value: 'SI' },
              { label: 'No', value: 'NO' },
            ]}
            placeholder="Selecciona"
            registration={register('estudiante.tieneInternet')}
            error={errores?.tieneInternet}
          />
        </Grilla>
      </section>

      <section className="flex flex-col gap-4 border-t border-stroke-tertiary pt-5">
        <Subtitulo>Datos socioculturales</Subtitulo>
        <Grilla columnas={4}>
          <Select
            label="Lengua materna"
            options={LENGUAS_MATERNAS}
            placeholder="Selecciona"
            registration={register('estudiante.lenguaMaterna')}
            error={errores?.lenguaMaterna}
          />
          <Select
            label="Segunda lengua (opcional)"
            options={SEGUNDAS_LENGUAS}
            placeholder="Selecciona"
            vaciable
            registration={register('estudiante.segundaLengua')}
          />
          <Select
            label="Autoidentificación étnica (opcional)"
            options={opciones([
              'Mestizo',
              'Quechua',
              'Aimara',
              'Afroperuano',
              'Blanco',
              'Otro',
              'Prefiere no responder',
            ])}
            placeholder="Selecciona"
            vaciable
            registration={register('estudiante.autoidentificacionEtnica')}
          />
          <SiNo
            pregunta="¿Tiene alguna discapacidad?"
            orden={['NO', 'SI']}
            registration={register('estudiante.tieneDiscapacidad')}
          />
        </Grilla>
        {watch('estudiante.tieneDiscapacidad') === 'SI' && (
          <Input
            label="¿Cuál?"
            maxLength={200}
            registration={register('estudiante.discapacidadDetalle')}
            error={errores?.discapacidadDetalle}
          />
        )}
      </section>

      <section className="flex flex-col gap-4 border-t border-stroke-tertiary pt-5">
        <Subtitulo>Procedencia y grado</Subtitulo>
        <Grilla columnas={2}>
          <Input
            label="Colegio de procedencia"
            maxLength={150}
            registration={register('estudiante.colegioProcedencia')}
            error={errores?.colegioProcedencia}
          />
          <Select
            label="Gestión"
            options={opciones(['Pública', 'Privada'])}
            placeholder="Selecciona"
            registration={register('estudiante.gestionProcedencia')}
            error={errores?.gestionProcedencia}
          />
        </Grilla>
        <Grilla columnas={4}>
          <Select
            label="Año académico"
            options={aniosAcademicos}
            placeholder="Selecciona"
            registration={register('estudiante.anioAcademico')}
            error={errores?.anioAcademico}
          />
          <Select
            label="Nivel"
            options={NIVELES}
            placeholder="Selecciona"
            registration={register('estudiante.nivel', {
              onChange: () => setValue('estudiante.grado', ''),
            })}
            error={errores?.nivel}
          />
          <Select
            label="Grado"
            disabled={!nivel}
            options={GRADOS_POR_NIVEL[nivel] ?? []}
            placeholder="Selecciona"
            registration={register('estudiante.grado')}
            error={errores?.grado}
          />
          <Select
            label="Forma de ingreso"
            options={opciones([
              'Edad normativa',
              'Traslado de otra IE',
              'Prueba de ubicación',
              'Convalidación',
            ])}
            placeholder="Selecciona"
            registration={register('estudiante.formaIngreso')}
            error={errores?.formaIngreso}
          />
        </Grilla>
        {sugerido && (
          <div className="flex gap-3 rounded-xl border border-stroke-success bg-surface-success-light px-4 py-3">
            <CircleCheck
              className="mt-0.5 size-5 shrink-0 text-content-success"
              aria-hidden="true"
            />
            <div>
              <p className="text-body-2 text-content-primary">
                Grado sugerido: {nombreGrado(sugerido.grado)}
              </p>
              <p className="text-body-4 text-content-secondary">
                Calculado con la fecha de nacimiento ({sugerido.edad} años
                cumplidos al 31/03/{anio}). Otro grado es recomendado una prueba
                de ubicación o convalidación.
              </p>
            </div>
          </div>
        )}
      </section>
    </SeccionPaso>
  );
};

// Paso 2 · Figma > "Admin / Estudiantes | Inscripción | Paso 2 - Ficha médica"
export const PasoFichaMedica = ({ form }: PasoProps) => {
  const { register, watch } = form;
  return (
    <SeccionPaso
      titulo="Ficha médica del estudiante"
      descripcion="Información que el colegio debe conocer para atender al estudiante ante una emergencia."
    >
      <section className="flex flex-col gap-4">
        <Subtitulo>Datos generales</Subtitulo>
        <Grilla columnas={4}>
          <Select
            label="Tipo de sangre"
            options={TIPOS_SANGRE}
            placeholder="Selecciona"
            registration={register('fichaMedica.tipoSangre')}
          />
          <Input
            label="Peso (kg)"
            type="number"
            step="0.1"
            registration={register('fichaMedica.pesoKg')}
          />
          <Input
            label="Estatura (m)"
            type="number"
            step="0.01"
            registration={register('fichaMedica.estaturaM')}
          />
          <Select
            label="Tipo de seguro"
            options={TIPOS_SEGURO}
            placeholder="Selecciona"
            registration={register('fichaMedica.tipoSeguro')}
          />
        </Grilla>
      </section>

      <section className="flex flex-col gap-4 border-t border-stroke-tertiary pt-5">
        <Subtitulo>Alergias</Subtitulo>
        <SiNo
          pregunta="¿Tiene alergias?"
          registration={register('fichaMedica.tieneAlergias')}
        />
        {watch('fichaMedica.tieneAlergias') === 'SI' && (
          <Grilla columnas={2}>
            <Input
              label="Alergias alimentarias"
              registration={register('fichaMedica.alergiasAlimentarias')}
            />
            <Input
              label="Alergias medicamentosas"
              registration={register('fichaMedica.alergiasMedicamentosas')}
            />
          </Grilla>
        )}
      </section>

      <section className="flex flex-col gap-4 border-t border-stroke-tertiary pt-5">
        <Subtitulo>Antecedentes</Subtitulo>
        <Grilla columnas={2}>
          <Input
            label="¿Hasta cuántos grados de fiebre ha llegado? (°C)"
            registration={register('fichaMedica.fiebreMaxima')}
          />
          <SiNo
            pregunta="¿Ha convulsionado?"
            registration={register('fichaMedica.haConvulsionado')}
          />
          <SiNo
            pregunta="¿Padece alguna enfermedad que el colegio deba conocer?"
            registration={register('fichaMedica.tieneEnfermedad')}
          />
          {watch('fichaMedica.tieneEnfermedad') === 'SI' ? (
            <Input
              label="¿Cuál?"
              registration={register('fichaMedica.enfermedadDetalle')}
            />
          ) : (
            <span />
          )}
          <SiNo
            pregunta="¿Toma algún medicamento a diario?"
            registration={register('fichaMedica.tomaMedicamento')}
          />
          {watch('fichaMedica.tomaMedicamento') === 'SI' ? (
            <Input
              label="¿Cuál?"
              registration={register('fichaMedica.medicamentoDetalle')}
            />
          ) : (
            <span />
          )}
        </Grilla>
        <Textarea
          label="Observaciones médicas (opcional)"
          registration={register('fichaMedica.observaciones')}
        />
        <div className="rounded-xl bg-surface-warning-light px-4 py-3 text-body-4 text-content-secondary">
          <strong className="text-content-primary">
            Información sensible.
          </strong>{' '}
          Solo la ven administración, el tutor del aula y tópico. Se firma como
          declaración jurada.
        </div>
      </section>
    </SeccionPaso>
  );
};
