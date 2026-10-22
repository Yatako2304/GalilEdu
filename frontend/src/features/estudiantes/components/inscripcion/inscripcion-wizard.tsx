import { zodResolver } from '@hookform/resolvers/zod';
import * as React from 'react';
import { useForm, UseFormReturn } from 'react-hook-form';

import { Button } from '@/components/ui/button';
import { Input, Radio } from '@/components/ui/form';
import { Stepper } from '@/components/ui/stepper';
import { nombreGrado } from '@/config/catalogos';

import { ResultadoInscripcion, useInscribir } from '../../api/estudiantes';

import { Casilla, SeccionPaso } from './campos';
import {
  CAMPOS_POR_PASO,
  FamiliarForm,
  InscripcionForm,
  aSolicitud,
  inscripcionSchema,
  inscripcionVacia,
  registra,
} from './esquema';
import { PasoEstudiante, PasoFichaMedica } from './pasos-estudiante';
import { PasoContactos, PasoFamilia } from './pasos-familia';

const PASOS = [
  { titulo: 'Estudiante', detalle: 'Datos e identificación' },
  { titulo: 'Ficha médica', detalle: 'Salud del estudiante' },
  { titulo: 'Padres y apoderado', detalle: 'Personas vinculadas' },
  { titulo: 'Contactos y salida', detalle: 'Emergencia y recojo' },
  { titulo: 'Responsable', detalle: 'Revisión y registro' },
];

const SIGUIENTE = [
  'Siguiente: Ficha médica',
  'Siguiente: Padres y apoderado',
  'Siguiente: Contactos y salida',
  'Siguiente: Responsable',
];

const nombre = (f: FamiliarForm) =>
  `${f.persona.nombres} ${f.persona.apellidoPaterno} ${f.persona.apellidoMaterno ?? ''}`.trim();

// Paso 5 · Figma > "Inscripción | Paso 5 - Responsable y registro"
const PasoResponsable = ({
  form,
}: {
  form: UseFormReturn<InscripcionForm>;
}) => {
  const { register, watch, setValue } = form;
  const v = watch();
  const candidatos = (
    [
      ['PADRE', 'Padre', v.padre],
      ['MADRE', 'Madre', v.madre],
      ['APODERADO', 'Apoderado', v.incluirApoderado ? v.apoderado : null],
    ] as const
  ).filter(([, , f]) => f && registra(f)) as [string, string, FamiliarForm][];

  const elegido = candidatos.find(([clave]) => clave === v.responsable)?.[2];

  React.useEffect(() => {
    if (!v.responsable && candidatos[0]) {
      setValue(
        'responsable',
        candidatos[0][0] as InscripcionForm['responsable'],
      );
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  React.useEffect(() => {
    if (elegido) {
      setValue('correoResponsable', elegido.persona.correo ?? '');
      setValue('celularResponsable', elegido.persona.celular ?? '');
    }
  }, [v.responsable]); // eslint-disable-line react-hooks/exhaustive-deps

  const p = v.estudiante.persona;
  const familiarResumen = (f: FamiliarForm, aplica = true) =>
    !aplica
      ? 'No aplica'
      : registra(f)
        ? `${nombre(f)} · DNI ${f.persona.numeroDocumento} ${f.personaId ? '(ya registrado, vinculado)' : '(nuevo)'}`
        : `No registra${f.motivo ? ` · ${f.motivo}` : ''}`;

  const resumen: [string, string][] = [
    [
      'Estudiante',
      `${p.nombres} ${p.apellidoPaterno} ${p.apellidoMaterno ?? ''} · DNI ${p.numeroDocumento}`,
    ],
    [
      'Grado',
      `${nombreGrado(v.estudiante.grado)} · Año académico ${v.estudiante.anioAcademico}`,
    ],
    [
      'Ficha médica',
      v.fichaMedica.tieneAlergias === 'SI'
        ? `Alergias: ${[v.fichaMedica.alergiasAlimentarias, v.fichaMedica.alergiasMedicamentosas].filter(Boolean).join(', ')}`
        : 'Completa · Sin alergias registradas',
    ],
    ['Padre', familiarResumen(v.padre)],
    ['Madre', familiarResumen(v.madre)],
    ['Apoderado', familiarResumen(v.apoderado, v.incluirApoderado)],
    ...(v.incluirApoderado
      ? ([
          [
            'Documento del apoderado',
            v.documentoApoderado.archivo
              ? `${v.documentoApoderado.tipo || 'Documento'} · ${v.documentoApoderado.archivo.name}`
              : 'Sin documento adjunto',
          ],
        ] as [string, string][])
      : []),
    ['Contactos de emergencia', `${v.contactos.length} registrados`],
    [
      'Autorizados a recoger',
      `${v.autorizados.filter((a) => a.nombreCompleto).length + candidatos.length} personas`,
    ],
    [
      'Responsable de matrícula',
      elegido
        ? `${nombre(elegido)} ${elegido.tieneCuenta ? '(cuenta existente)' : '(se creará su usuario)'}`
        : '—',
    ],
  ];

  return (
    <>
      <SeccionPaso
        titulo="Responsable de matrícula"
        descripcion="Firma la matrícula, recibe las boletas y tiene el usuario en GalilEdu (un usuario por familia). Se elige entre el padre, la madre o el apoderado."
      >
        <div className="flex flex-col gap-3">
          {candidatos.map(([clave, rol, f]) => (
            <Radio
              key={clave}
              value={clave}
              label={`${nombre(f)} — ${rol}`}
              helperText={
                f.tieneCuenta
                  ? `Ya tiene usuario${f.aCargo.length ? `: es responsable de ${f.aCargo.join(', ')}` : ''}.`
                  : 'No tiene usuario: se creará uno nuevo con su correo.'
              }
              {...register('responsable')}
            />
          ))}
        </div>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Input
            label="Correo de acceso"
            type="email"
            registration={register('correoResponsable')}
          />
          <Input
            label="Celular para notificaciones"
            registration={register('celularResponsable')}
          />
        </div>
      </SeccionPaso>

      <SeccionPaso
        titulo="Resumen de la inscripción"
        descripcion="Revisa los datos antes de registrar."
      >
        <dl className="divide-y divide-stroke-tertiary">
          {resumen.map(([k, valor]) => (
            <div
              key={k}
              className="grid grid-cols-1 gap-1 py-2.5 md:grid-cols-[220px_1fr]"
            >
              <dt className="text-body-3 text-content-tertiary">{k}</dt>
              <dd className="text-body-3 text-content-primary">{valor}</dd>
            </div>
          ))}
        </dl>
        <Casilla
          etiqueta="Verifiqué los datos con los documentos originales (DNI del estudiante y de los padres)."
          registration={register('verificado')}
        />
      </SeccionPaso>
    </>
  );
};

// Figma > Personal Administrativo > "Admin / Estudiantes | Inscripción | Paso 1…5" (HU-7)
export const InscripcionWizard = ({
  onCancelar,
  onRegistrada,
}: {
  onCancelar: () => void;
  onRegistrada: (r: ResultadoInscripcion) => void;
}) => {
  const [paso, setPaso] = React.useState(0);
  const form = useForm<InscripcionForm>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(inscripcionSchema as any),
    defaultValues: inscripcionVacia(),
    mode: 'onSubmit',
  });
  const inscribir = useInscribir(onRegistrada);
  const verificado = form.watch('verificado');
  const responsable = form.watch('responsable');

  const subir = () => window.scrollTo({ top: 0, behavior: 'smooth' });

  const siguiente = async () => {
    const campos = CAMPOS_POR_PASO[
      paso
    ] as unknown as (keyof InscripcionForm)[];
    const ok = await form.trigger(campos, { shouldFocus: true });
    if (ok) {
      setPaso((p) => p + 1);
      subir();
    }
  };

  const registrar = () => {
    const valores = form.getValues();
    const { tipo, archivo } = valores.documentoApoderado;
    inscribir.mutate({
      solicitud: aSolicitud(valores),
      documento:
        valores.incluirApoderado && archivo
          ? { tipo: tipo || 'Otro', archivo }
          : null,
    });
  };

  const e = form.watch('estudiante.persona');
  const subtitulo =
    paso === 0
      ? 'Registra los datos del estudiante. El estudiante no tiene usuario: su cuenta la gestiona el responsable de matrícula.'
      : `${e.nombres} ${e.apellidoPaterno} ${e.apellidoMaterno ?? ''} · DNI ${e.numeroDocumento}`;

  return (
    <div className="flex flex-col gap-8">
      <p className="-mt-6 text-body-3 text-content-quartiary">{subtitulo}</p>
      <Stepper
        steps={PASOS.map((p) => ({ label: p.titulo, description: p.detalle }))}
        current={paso + 1}
      />

      <form
        onSubmit={(ev) => ev.preventDefault()}
        className="flex flex-col gap-6"
        noValidate
      >
        {paso === 0 && <PasoEstudiante form={form} />}
        {paso === 1 && <PasoFichaMedica form={form} />}
        {paso === 2 && <PasoFamilia form={form} />}
        {paso === 3 && <PasoContactos form={form} />}
        {paso === 4 && <PasoResponsable form={form} />}
      </form>

      <div className="flex items-center justify-between gap-3">
        <Button variant="primary-light" onClick={onCancelar}>
          Cancelar
        </Button>
        <div className="flex gap-3">
          {paso > 0 && (
            <Button
              variant="primary-light"
              onClick={() => {
                setPaso((p) => p - 1);
                subir();
              }}
            >
              Atrás
            </Button>
          )}
          {paso < PASOS.length - 1 ? (
            <Button variant="secondary" onClick={siguiente}>
              {SIGUIENTE[paso]}
            </Button>
          ) : (
            <Button
              variant="secondary"
              onClick={registrar}
              isLoading={inscribir.isPending}
              disabled={!verificado || !responsable}
            >
              Registrar inscripción
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};
