import { CircleCheck } from 'lucide-react';
import * as React from 'react';
import { UseFormReturn } from 'react-hook-form';

import { PersonaFields } from '@/components/personas/persona-fields';
import { Input, Select, Textarea } from '@/components/ui/form';
import {
  DISTRITOS_LIMA,
  GRADOS_POR_NIVEL,
  NIVELES,
  TIPOS_SANGRE,
  TIPOS_SEGURO,
} from '@/config/catalogos';

import { Grilla, SeccionPaso, SiNo, Subtitulo } from './campos';
import { InscripcionForm, gradoSugerido } from './esquema';

type PasoProps = { form: UseFormReturn<InscripcionForm> };

const opciones = (valores: string[]) =>
  valores.map((v) => ({ label: v, value: v }));

// Paso 1 · Figma > "Admin / Estudiantes | Inscripción | Paso 1 - Estudiante"
export const PasoEstudiante = ({ form }: PasoProps) => {
  const { register, formState, watch, setValue, getValues } = form;
  const errores = formState.errors.estudiante;
  const [fecha, anio, nivel] = watch([
    'estudiante.persona.fechaNacimiento',
    'estudiante.anioAcademico',
    'estudiante.nivel',
  ]);
  const sugerido = gradoSugerido(fecha, Number(anio));

  // Si aún no se eligió nivel/grado, se completa con el sugerido.
  React.useEffect(() => {
    if (sugerido && !getValues('estudiante.nivel')) {
      setValue('estudiante.nivel', sugerido.nivel);
      setValue('estudiante.grado', sugerido.grado);
    }
  }, [sugerido?.nivel, sugerido?.grado]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <SeccionPaso
      titulo="Datos del estudiante"
      descripcion="Campos según la Ficha Única de Matrícula (Minedu) y la ficha del colegio."
    >
      <section className="flex flex-col gap-4">
        <Subtitulo>Identificación</Subtitulo>
        <PersonaFields
          register={register}
          errors={formState.errors}
          prefix="estudiante.persona"
          sinDatosLaborales
        />
      </section>

      <section className="flex flex-col gap-4 border-t border-stroke-tertiary pt-5">
        <Subtitulo>Lugar de nacimiento</Subtitulo>
        <Grilla columnas={4}>
          <Input
            label="País"
            registration={register('estudiante.paisNacimiento')}
          />
          <Input
            label="Departamento"
            registration={register('estudiante.departamentoNacimiento')}
          />
          <Input
            label="Provincia"
            registration={register('estudiante.provinciaNacimiento')}
          />
          <Select
            label="Distrito"
            options={DISTRITOS_LIMA}
            placeholder="Selecciona"
            registration={register('estudiante.distritoNacimiento')}
          />
        </Grilla>
      </section>

      <section className="flex flex-col gap-4 border-t border-stroke-tertiary pt-5">
        <Subtitulo>Domicilio y datos socioculturales</Subtitulo>
        <Grilla>
          <Input
            label="Referencia del domicilio"
            registration={register('estudiante.referenciaDomicilio')}
          />
          <Select
            label="¿Tiene acceso a internet / dispositivo?"
            options={[
              { label: 'Sí', value: 'SI' },
              { label: 'No', value: 'NO' },
            ]}
            placeholder="Selecciona"
            registration={register('estudiante.tieneInternet')}
          />
          <Select
            label="Lengua materna"
            options={opciones(['Castellano', 'Quechua', 'Aimara', 'Otra'])}
            registration={register('estudiante.lenguaMaterna')}
          />
          <Input
            label="Segunda lengua (opcional)"
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
            registration={register('estudiante.autoidentificacionEtnica')}
          />
          <SiNo
            pregunta="¿Tiene alguna discapacidad?"
            registration={register('estudiante.tieneDiscapacidad')}
          />
        </Grilla>
        {watch('estudiante.tieneDiscapacidad') === 'SI' && (
          <Input
            label="¿Cuál?"
            registration={register('estudiante.discapacidadDetalle')}
          />
        )}
      </section>

      <section className="flex flex-col gap-4 border-t border-stroke-tertiary pt-5">
        <Subtitulo>Procedencia y grado</Subtitulo>
        <Grilla columnas={2}>
          <Input
            label="Colegio de procedencia"
            registration={register('estudiante.colegioProcedencia')}
          />
          <Select
            label="Gestión"
            options={opciones(['Pública', 'Privada'])}
            placeholder="Selecciona"
            registration={register('estudiante.gestionProcedencia')}
          />
        </Grilla>
        <Grilla columnas={4}>
          <Input
            label="Año académico"
            type="number"
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
            registration={register('estudiante.formaIngreso')}
          />
        </Grilla>
        {sugerido && (
          <div className="flex gap-3 rounded-[12px] border border-stroke-success bg-surface-success-light px-4 py-3">
            <CircleCheck
              className="mt-0.5 size-5 shrink-0 text-content-success"
              aria-hidden="true"
            />
            <div>
              <p className="text-body-2 text-content-primary">
                Grado sugerido: {sugerido.grado} de{' '}
                {sugerido.nivel.toLowerCase()}
              </p>
              <p className="text-body-4 text-content-secondary">
                Calculado con la fecha de nacimiento ({sugerido.edad} años
                cumplidos al 31/03/{anio}). Otro grado requiere prueba de
                ubicación o convalidación.
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
            label="¿Hasta cuántos grados de fiebre ha llegado?"
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
        <div className="rounded-[12px] bg-surface-warning-light px-4 py-3 text-body-4 text-content-secondary">
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
