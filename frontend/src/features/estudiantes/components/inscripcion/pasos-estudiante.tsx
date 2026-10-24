import { CircleCheck } from 'lucide-react';
import * as React from 'react';
import { UseFormReturn } from 'react-hook-form';

import { LIMITES_PERSONA } from '@/components/personas/limites-persona';
import {
  filtroDocumento,
  largoDocumento,
} from '@/components/personas/persona-fields';
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
  decimal,
  restringir,
  soloDecimal,
  soloDigitos,
  soloLetras,
} from '@/utils/restringir-entrada';

import { Grilla, SeccionPaso, SiNo, Subtitulo } from './campos';
import { InscripcionForm, LIMITES_FICHA, gradoSugerido } from './esquema';

type PasoProps = { form: UseFormReturn<InscripcionForm> };

// Largos de las columnas de estudiantes.estudiante (V2). Los de la persona están en LIMITES_PERSONA.
const LIMITES_ESTUDIANTE = {
  referencia: 200,
  discapacidad: 200,
  colegio: 150,
} as const;

const opciones = (valores: string[]) =>
  valores.map((v) => ({ label: v, value: v }));

// Se inscribe para el año en curso o el siguiente.
const anioActual = new Date().getFullYear();
const aniosAcademicos = opciones([String(anioActual), String(anioActual + 1)]);

// Paso 1 · Figma > "Admin / Estudiantes | Inscripción | Paso 1 - Estudiante"
export const PasoEstudiante = ({
  form,
  bloquearGrado = false,
}: PasoProps & {
  /** "Editar" de un estudiante matriculado (ya pagó): no cambia de año, nivel ni grado. */
  bloquearGrado?: boolean;
}) => {
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

  const tipoDocumento = watch('estudiante.persona.tipoDocumento');

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
            maxLength={largoDocumento(tipoDocumento)}
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
            maxLength={LIMITES_PERSONA.apellido}
            registration={restringir(
              register('estudiante.persona.apellidoPaterno'),
              soloLetras(LIMITES_PERSONA.apellido),
            )}
            error={errorPersona?.apellidoPaterno}
          />
          <Input
            label="Apellido materno (opcional)"
            maxLength={LIMITES_PERSONA.apellido}
            registration={restringir(
              register('estudiante.persona.apellidoMaterno'),
              soloLetras(LIMITES_PERSONA.apellido),
            )}
            error={errorPersona?.apellidoMaterno}
          />
          <Input
            label="Nombres"
            maxLength={LIMITES_PERSONA.nombres}
            registration={restringir(
              register('estudiante.persona.nombres'),
              soloLetras(LIMITES_PERSONA.nombres),
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
            maxLength={LIMITES_PERSONA.direccion}
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
            maxLength={LIMITES_ESTUDIANTE.referencia}
            registration={register('estudiante.referenciaDomicilio')}
          />
          <Input
            label="Teléfono de casa (opcional)"
            inputMode="numeric"
            maxLength={LIMITES_PERSONA.telefonoCasa}
            registration={restringir(
              register('estudiante.persona.telefonoCasa'),
              soloDigitos(LIMITES_PERSONA.telefonoCasa),
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
            maxLength={LIMITES_ESTUDIANTE.discapacidad}
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
            maxLength={LIMITES_ESTUDIANTE.colegio}
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
            disabled={bloquearGrado}
            // Al editar, el año guardado se ofrece aunque ya no sea el en curso o el siguiente.
            options={
              anio && !aniosAcademicos.some((o) => o.value === anio)
                ? [...opciones([anio]), ...aniosAcademicos]
                : aniosAcademicos
            }
            placeholder="Selecciona"
            registration={register('estudiante.anioAcademico')}
            error={errores?.anioAcademico}
          />
          <Select
            label="Nivel"
            disabled={bloquearGrado}
            options={NIVELES}
            placeholder="Selecciona"
            registration={register('estudiante.nivel', {
              onChange: () => setValue('estudiante.grado', ''),
            })}
            error={errores?.nivel}
          />
          <Select
            label="Grado"
            disabled={!nivel || bloquearGrado}
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
  const { register, watch, formState } = form;
  const errores = formState.errors.fichaMedica;
  const [alergias, enfermedad, medicamento] = watch([
    'fichaMedica.tieneAlergias',
    'fichaMedica.tieneEnfermedad',
    'fichaMedica.tomaMedicamento',
  ]);
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
            error={errores?.tipoSangre}
          />
          <Input
            label="Peso (kg)"
            inputMode="decimal"
            registration={decimal(
              register('fichaMedica.pesoKg'),
              LIMITES_FICHA.peso,
            )}
            error={errores?.pesoKg}
          />
          <Input
            label="Estatura (m)"
            inputMode="decimal"
            registration={decimal(
              register('fichaMedica.estaturaM'),
              LIMITES_FICHA.estatura,
            )}
            error={errores?.estaturaM}
          />
          <Select
            label="Tipo de seguro"
            options={TIPOS_SEGURO}
            placeholder="Selecciona"
            registration={register('fichaMedica.tipoSeguro')}
            error={errores?.tipoSeguro}
          />
        </Grilla>
      </section>

      <section className="flex flex-col gap-4 border-t border-stroke-tertiary pt-5">
        <Subtitulo>Alergias</Subtitulo>
        <SiNo
          pregunta="¿Tiene alergias?"
          registration={register('fichaMedica.tieneAlergias')}
        />
        {alergias === 'SI' && (
          <Grilla columnas={2}>
            <Input
              label="Alergias alimentarias"
              maxLength={LIMITES_FICHA.texto}
              registration={register('fichaMedica.alergiasAlimentarias')}
              error={errores?.alergiasAlimentarias}
            />
            <Input
              label="Alergias medicamentosas"
              maxLength={LIMITES_FICHA.texto}
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
            inputMode="decimal"
            registration={restringir(
              register('fichaMedica.fiebreMaxima'),
              soloDecimal(LIMITES_FICHA.fiebre),
            )}
            error={errores?.fiebreMaxima}
          />
          <SiNo
            pregunta="¿Ha convulsionado?"
            registration={register('fichaMedica.haConvulsionado')}
          />
          <SiNo
            pregunta="¿Padece alguna enfermedad que el colegio deba conocer?"
            registration={register('fichaMedica.tieneEnfermedad')}
          />
          {/* "¿Cuál?" aparece recién con Sí (decisión del equipo; Figma lo muestra deshabilitado). */}
          {enfermedad === 'SI' ? (
            <Input
              label="¿Cuál?"
              maxLength={LIMITES_FICHA.texto}
              registration={register('fichaMedica.enfermedadDetalle')}
              error={errores?.enfermedadDetalle}
            />
          ) : (
            <span />
          )}
          <SiNo
            pregunta="¿Toma algún medicamento a diario?"
            registration={register('fichaMedica.tomaMedicamento')}
          />
          {medicamento === 'SI' ? (
            <Input
              label="¿Cuál?"
              maxLength={LIMITES_FICHA.texto}
              registration={register('fichaMedica.medicamentoDetalle')}
              error={errores?.medicamentoDetalle}
            />
          ) : (
            <span />
          )}
        </Grilla>
        <Textarea
          label="Observaciones médicas (opcional)"
          maxLength={LIMITES_FICHA.observaciones}
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
