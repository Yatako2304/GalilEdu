import { Paperclip, Plus, Search, Trash2 } from 'lucide-react';
import * as React from 'react';
import { useFieldArray, UseFormReturn } from 'react-hook-form';

import { PersonaFields } from '@/components/personas/persona-fields';
import { personaAFormulario } from '@/components/personas/persona-schema';
import { Alert } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { FileItem } from '@/components/ui/files';
import { Input, Radio, Select } from '@/components/ui/form';
import { Tag } from '@/components/ui/tag';
import { TIPOS_DOCUMENTO } from '@/config/catalogos';

import { buscarPersonaPorDocumento } from '../../api/estudiantes';

import { Casilla, Grilla, SeccionPaso, Subtitulo } from './campos';
import {
  InscripcionForm,
  TIPOS_DOCUMENTO_APODERADO,
  registra,
  validarDocumento,
} from './esquema';

type PasoProps = { form: UseFormReturn<InscripcionForm> };
type Clave = 'padre' | 'madre' | 'apoderado';

const MOTIVOS = [
  'Fallecido(a)',
  'No tiene la tutela',
  'No registra',
  'Otro',
].map((v) => ({
  label: v,
  value: v,
}));

const RELACIONES = [
  'Abuelo(a)',
  'Tío(a)',
  'Hermano(a)',
  'Amigo(a) de la familia',
  'Tutor legal',
  'Otro',
].map((v) => ({ label: v, value: v }));

const TarjetaFamiliar = ({
  form,
  clave,
  titulo,
  descripcion,
}: PasoProps & { clave: Clave; titulo: string; descripcion: string }) => {
  const { register, watch, setValue, getValues, formState } = form;
  const errores = formState.errors[clave];
  const f = watch(clave);
  const [buscando, setBuscando] = React.useState(false);
  const [buscado, setBuscado] = React.useState(false);

  const buscar = async () => {
    const numero = getValues(`${clave}.persona.numeroDocumento`).trim();
    if (!numero) return;
    setBuscando(true);
    try {
      const encontrada = await buscarPersonaPorDocumento(numero);
      if (encontrada) {
        setValue(`${clave}.personaId`, encontrada.personaId);
        setValue(`${clave}.tieneCuenta`, encontrada.tieneCuenta);
        setValue(`${clave}.aCargo`, encontrada.estudiantesACargo);
        setValue(`${clave}.persona`, personaAFormulario(encontrada.persona));
      } else {
        setValue(`${clave}.personaId`, null);
        setValue(`${clave}.tieneCuenta`, false);
        setValue(`${clave}.aCargo`, []);
      }
      setBuscado(true);
    } finally {
      setBuscando(false);
    }
  };

  /** Si cambia el documento, se olvida la persona encontrada. */
  const alCambiarDocumento = () => {
    if (getValues(`${clave}.personaId`)) {
      setValue(`${clave}.personaId`, null);
      setValue(`${clave}.tieneCuenta`, false);
      setValue(`${clave}.aCargo`, []);
    }
    setBuscado(false);
  };

  return (
    <Card className="flex flex-col gap-4 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 className="text-label-1 font-semibold text-content-primary">
            {titulo}
          </h3>
          <p className="text-body-4 text-content-tertiary">{descripcion}</p>
        </div>
        {clave !== 'apoderado' && (
          <Casilla
            etiqueta={
              clave === 'padre'
                ? 'No registra / fallecido'
                : 'No registra / fallecida'
            }
            registration={register(`${clave}.noRegistra`)}
          />
        )}
      </div>

      {f.noRegistra ? (
        <Grilla columnas={2}>
          <Select
            label="Motivo"
            options={MOTIVOS}
            placeholder="Selecciona"
            registration={register(`${clave}.motivo`)}
            error={errores?.motivo}
          />
          <Input
            label="Observación (opcional)"
            registration={register(`${clave}.observacion`)}
          />
        </Grilla>
      ) : (
        <>
          <div className="grid grid-cols-1 items-end gap-4 md:grid-cols-[1fr_1fr_auto]">
            <Select
              label="Tipo de documento"
              options={TIPOS_DOCUMENTO}
              registration={register(`${clave}.persona.tipoDocumento`)}
            />
            <Input
              label="N.° de documento"
              inputMode="numeric"
              registration={register(`${clave}.persona.numeroDocumento`, {
                onChange: alCambiarDocumento,
              })}
              error={errores?.persona?.numeroDocumento}
            />
            <Button
              type="button"
              variant="primary-light"
              icon={<Search className="size-4" />}
              onClick={buscar}
              isLoading={buscando}
            >
              Buscar
            </Button>
          </div>

          {f.personaId ? (
            <div className="rounded-[12px] bg-surface-success-light px-4 py-3 text-body-4 text-content-secondary">
              <Tag color="success" size="sm" className="mr-2">
                Ya registrado
              </Tag>
              Se vinculará sin duplicar sus datos.
              {f.aCargo.length > 0 &&
                ` Ya está vinculado a: ${f.aCargo.join(', ')}.`}
              {f.tieneCuenta && ' Tiene usuario en GalilEdu.'}
            </div>
          ) : (
            buscado && (
              <div className="rounded-[12px] bg-surface-accent-light px-4 py-3 text-body-4 text-content-secondary">
                <Tag color="primary" size="sm" className="mr-2">
                  Persona nueva
                </Tag>
                No está registrada en GalilEdu. Completa sus datos.
              </div>
            )
          )}

          <PersonaFields
            register={register}
            errors={formState.errors}
            prefix={`${clave}.persona`}
            disabled={!!f.personaId}
            sinDocumento
          />

          {clave === 'apoderado' && <RelacionYDocumento form={form} />}

          <div className="flex flex-wrap gap-6">
            <Casilla
              etiqueta="Vive con el estudiante"
              registration={register(`${clave}.viveConEstudiante`)}
            />
            <Casilla
              etiqueta="Tiene la patria potestad / tutela"
              registration={register(`${clave}.tienePatriaPotestad`)}
            />
          </div>
        </>
      )}
    </Card>
  );
};

// Figma > Card "Apoderado" > RELACIÓN CON EL ESTUDIANTE: relación, "Especificar" y documento que lo acredita.
const RelacionYDocumento = ({ form }: PasoProps) => {
  const { register, watch, setValue } = form;
  const [relacion, documento] = watch([
    'apoderado.relacion',
    'documentoApoderado',
  ]);
  const [error, setError] = React.useState<string | null>(null);
  const entrada = React.useRef<HTMLInputElement>(null);

  const elegir = (archivo: File | undefined) => {
    if (!archivo) return;
    const problema = validarDocumento(archivo);
    setError(problema);
    if (!problema) setValue('documentoApoderado.archivo', archivo);
  };

  return (
    <section className="flex flex-col gap-4 border-t border-stroke-tertiary pt-5">
      <Subtitulo>Relación con el estudiante</Subtitulo>
      <Grilla>
        <Select
          label="Relación"
          options={RELACIONES}
          placeholder="Selecciona"
          registration={register('apoderado.relacion')}
        />
        <Input
          label="Especificar (si elige “Otro”)"
          disabled={relacion !== 'Otro'}
          registration={register('apoderado.relacionDetalle')}
        />
        <Select
          label="Documento que lo acredita"
          options={TIPOS_DOCUMENTO_APODERADO.map((v) => ({
            label: v,
            value: v,
          }))}
          placeholder="Selecciona"
          registration={register('documentoApoderado.tipo')}
        />
      </Grilla>

      <input
        ref={entrada}
        type="file"
        accept="application/pdf,image/jpeg,image/png"
        className="sr-only"
        aria-label="Archivo del documento"
        onChange={(e) => {
          elegir(e.target.files?.[0]);
          e.target.value = '';
        }}
      />
      {documento.archivo ? (
        <FileItem
          name={documento.archivo.name}
          mimeType={documento.archivo.type}
          meta={`${(documento.archivo.size / 1024).toFixed(0)} KB · se subirá al registrar la inscripción`}
          actions={
            <Button
              type="button"
              variant="ghost"
              size="sm"
              icon={<Trash2 className="size-4" />}
              onClick={() => setValue('documentoApoderado.archivo', null)}
            >
              Quitar
            </Button>
          }
        />
      ) : (
        <Button
          type="button"
          variant="primary-light"
          className="self-start"
          icon={<Paperclip className="size-4" />}
          onClick={() => entrada.current?.click()}
        >
          Adjuntar documento (PDF, JPG o PNG · máx. 5 MB)
        </Button>
      )}
      {error && <p className="text-body-4 text-content-error">{error}</p>}
    </section>
  );
};

// Paso 3 · Figma > "Inscripción | Paso 3 - Padres y apoderado" (y su variante sin padres)
export const PasoFamilia = ({ form }: PasoProps) => {
  const { watch, setValue, formState } = form;
  const [padre, madre, incluirApoderado] = watch([
    'padre',
    'madre',
    'incluirApoderado',
  ]);
  const sinPadres =
    !registra(padre) &&
    !registra(madre) &&
    padre.noRegistra &&
    madre.noRegistra;

  React.useEffect(() => {
    if (sinPadres && !incluirApoderado) setValue('incluirApoderado', true);
  }, [sinPadres, incluirApoderado, setValue]);

  return (
    <SeccionPaso
      titulo="Registra al menos al padre o a la madre"
      descripcion="El apoderado se agrega solo si no hay padre ni madre, o si el responsable de la matrícula es otra persona (puede no ser familiar)."
    >
      <TarjetaFamiliar
        form={form}
        clave="padre"
        titulo="Padre"
        descripcion="Busca por documento: si ya está registrado (por ejemplo, por un hermano), solo se vincula."
      />
      <TarjetaFamiliar
        form={form}
        clave="madre"
        titulo="Madre"
        descripcion="Busca por documento: si ya está registrada, solo se vincula."
      />

      {sinPadres && (
        <Alert type="warning" title="No hay padre ni madre registrados">
          Para continuar debes registrar un apoderado. Puede ser un familiar o
          una persona sin parentesco.
        </Alert>
      )}

      {incluirApoderado ? (
        <>
          <TarjetaFamiliar
            form={form}
            clave="apoderado"
            titulo={sinPadres ? 'Apoderado' : 'Apoderado (opcional)'}
            descripcion="Persona que representa al estudiante ante el colegio."
          />
          {!sinPadres && (
            <Button
              type="button"
              variant="ghost"
              className="self-start"
              icon={<Trash2 className="size-4" />}
              onClick={() => setValue('incluirApoderado', false)}
            >
              Quitar apoderado
            </Button>
          )}
        </>
      ) : (
        <Button
          type="button"
          variant="primary-light"
          className="self-start"
          icon={<Plus className="size-4" />}
          onClick={() => setValue('incluirApoderado', true)}
        >
          Agregar apoderado (opcional)
        </Button>
      )}
      {formState.errors.incluirApoderado && (
        <p className="text-body-4 text-content-error">
          {formState.errors.incluirApoderado.message}
        </p>
      )}
    </SeccionPaso>
  );
};

const nombreFamiliar = (f: InscripcionForm['padre']) =>
  `${f.persona.nombres} ${f.persona.apellidoPaterno} ${f.persona.apellidoMaterno ?? ''}`.trim();

// Paso 4 · Figma > "Inscripción | Paso 4 - Contactos y autorización de salida"
export const PasoContactos = ({ form }: PasoProps) => {
  const { register, control, watch, formState } = form;
  const contactos = useFieldArray({ control, name: 'contactos' });
  const autorizados = useFieldArray({ control, name: 'autorizados' });
  const [padre, madre, apoderado, incluirApoderado, modoSalida] = watch([
    'padre',
    'madre',
    'apoderado',
    'incluirApoderado',
    'modoSalida',
  ]);
  const errores = formState.errors.contactos;

  const familiares = [
    registra(padre) && `${nombreFamiliar(padre)} · Padre`,
    registra(madre) && `${nombreFamiliar(madre)} · Madre`,
    incluirApoderado &&
      registra(apoderado) &&
      `${nombreFamiliar(apoderado)} · Apoderado`,
  ].filter(Boolean) as string[];

  return (
    <>
      <SeccionPaso
        titulo="Contactos de emergencia"
        descripcion="Mínimo 2 personas distintas al padre, la madre o el apoderado. Se llaman si no se ubica a los padres."
      >
        {contactos.fields.map((c, i) => (
          <div key={c.id} className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <Subtitulo>Contacto {i + 1}</Subtitulo>
              {i >= 2 && (
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => contactos.remove(i)}
                >
                  Quitar
                </Button>
              )}
            </div>
            <Grilla columnas={4}>
              <Input
                label="Nombre completo"
                registration={register(`contactos.${i}.nombreCompleto`)}
                error={errores?.[i]?.nombreCompleto}
              />
              <Input
                label="Parentesco / relación"
                registration={register(`contactos.${i}.parentesco`)}
                error={errores?.[i]?.parentesco}
              />
              <Input
                label="Teléfono fijo"
                registration={register(`contactos.${i}.telefonoFijo`)}
              />
              <Input
                label="Celular"
                registration={register(`contactos.${i}.celular`)}
              />
            </Grilla>
          </div>
        ))}
        <Button
          type="button"
          variant="primary-light"
          className="self-start"
          icon={<Plus className="size-4" />}
          onClick={() =>
            contactos.append({
              nombreCompleto: '',
              parentesco: '',
              telefonoFijo: '',
              celular: '',
            })
          }
        >
          Agregar contacto
        </Button>
      </SeccionPaso>

      <SeccionPaso
        titulo="Autorización de salida"
        descripcion="Marca quiénes pueden recoger al estudiante. Se les pedirá DNI al momento del recojo."
      >
        <div className="flex flex-wrap gap-6">
          {(
            [
              ['PERSONA_AUTORIZADA', 'Recogido por persona autorizada'],
              ['SOLO', 'Puede salir solo(a)'],
              ['MOVILIDAD', 'Se va en movilidad escolar'],
            ] as const
          ).map(([valor, etiqueta]) => (
            <Radio
              key={valor}
              value={valor}
              label={etiqueta}
              {...register('modoSalida')}
            />
          ))}
        </div>
        {modoSalida === 'PERSONA_AUTORIZADA' && (
          <div className="flex flex-col gap-3">
            {familiares.map((nombre) => (
              <p key={nombre} className="text-body-3 text-content-secondary">
                ✓ {nombre}
              </p>
            ))}
            {autorizados.fields.map((a, i) => (
              <div
                key={a.id}
                className="grid grid-cols-1 items-end gap-3 md:grid-cols-[1fr_1fr_auto]"
              >
                <Input
                  label="Nombre completo"
                  registration={register(`autorizados.${i}.nombreCompleto`)}
                />
                <Input
                  label="Relación"
                  registration={register(`autorizados.${i}.relacion`)}
                />
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => autorizados.remove(i)}
                >
                  Quitar
                </Button>
              </div>
            ))}
            <Button
              type="button"
              variant="primary-light"
              className="self-start"
              icon={<Plus className="size-4" />}
              onClick={() =>
                autorizados.append({ nombreCompleto: '', relacion: '' })
              }
            >
              Agregar otra persona autorizada
            </Button>
          </div>
        )}
        <div className="rounded-[12px] bg-surface-secondary px-4 py-3 text-body-4 text-content-secondary">
          <strong className="text-content-primary">
            Si alguien no autorizado recoge al estudiante,
          </strong>{' '}
          el colegio debe comunicarse con el responsable de matrícula antes de
          entregarlo.
        </div>
      </SeccionPaso>
    </>
  );
};
