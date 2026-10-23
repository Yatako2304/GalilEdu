import { CircleCheck, X } from 'lucide-react';
import * as React from 'react';

import { Card } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';
import { Table } from '@/components/ui/table';
import { Tabs } from '@/components/ui/tabs';
import { Tag } from '@/components/ui/tag';
import { FichaEstudiante as Ficha, ModoSalida } from '@/types/api';
import { formatDay } from '@/utils/format';

import { useFichaEstudiante } from '../api/estudiantes';

import { DocumentoApoderado } from './documento-apoderado';
import { EstadoEstudianteTag } from './estudiantes-lista';

const PESTANAS = [
  'Datos personales',
  'Ficha médica',
  'Familia',
  'Contactos y salida',
] as const;

const SALIDA: Record<ModoSalida, string> = {
  PERSONA_AUTORIZADA: 'Recogido por persona autorizada',
  SOLO: 'Puede salir solo(a)',
  MOVILIDAD: 'Se va en movilidad escolar',
};

const siNo = (v?: boolean | null) => (v == null ? '—' : v ? 'Sí' : 'No');

const Datos = ({ filas }: { filas: [string, React.ReactNode][] }) => (
  <dl className="grid grid-cols-1 gap-x-8 gap-y-3 md:grid-cols-2">
    {filas.map(([k, v]) => (
      <div key={k}>
        <dt className="text-body-4 text-content-quartiary">{k}</dt>
        <dd className="text-body-3 text-content-primary">{v || '—'}</dd>
      </div>
    ))}
  </dl>
);

export const encabezadoFicha = (f: Ficha) => {
  const p = f.persona;
  return {
    nombre:
      `${p.nombres} ${p.apellidoPaterno} ${p.apellidoMaterno ?? ''}`.trim(),
    detalle: `${p.tipoDocumento === 'DNI' ? 'DNI' : 'CE'} ${p.numeroDocumento} · ${f.grado} (${f.anioAcademico})`,
  };
};

// Figma > "Admin / Estudiantes | Ficha del estudiante - Familia (inscripción registrada)"
export const FichaEstudiante = ({
  id,
  aviso,
  onCerrarAviso,
}: {
  id: string;
  aviso?: string | null;
  onCerrarAviso?: () => void;
}) => {
  const ficha = useFichaEstudiante(id);
  const [pestana, setPestana] =
    React.useState<(typeof PESTANAS)[number]>('Familia');

  if (ficha.isLoading) {
    return (
      <div className="flex h-60 items-center justify-center">
        <Spinner />
      </div>
    );
  }
  const f = ficha.data;
  if (!f) return null;
  const p = f.persona;
  const m = f.fichaMedica;

  return (
    <>
      <div className="flex items-center gap-2">
        <EstadoEstudianteTag estado={f.estado} />
        <span className="text-body-4 text-content-tertiary">
          {f.estado === 'INSCRITO' && 'Pendiente de matrícula'}
        </span>
      </div>

      {aviso && (
        <div className="flex items-start gap-3 rounded-xl border border-stroke-success bg-surface-success-light px-4 py-3">
          <CircleCheck
            className="mt-0.5 size-5 shrink-0 text-content-success"
            aria-hidden="true"
          />
          <div className="flex-1">
            <p className="text-body-2 text-content-primary">
              Inscripción registrada
            </p>
            <p className="text-body-4 text-content-secondary">{aviso}</p>
          </div>
          <button
            type="button"
            onClick={onCerrarAviso}
            className="text-content-tertiary"
          >
            <X className="size-4" aria-hidden="true" />
            <span className="sr-only">Cerrar aviso</span>
          </button>
        </div>
      )}

      <Tabs
        aria-label="Secciones de la ficha"
        fullWidth
        items={PESTANAS.map((t) => ({ value: t, label: t }))}
        value={pestana}
        onValueChange={setPestana}
      />

      {pestana === 'Datos personales' && (
        <Card className="p-6">
          <Datos
            filas={[
              [
                'Apellidos y nombres',
                `${p.apellidoPaterno} ${p.apellidoMaterno ?? ''}, ${p.nombres}`,
              ],
              [
                'Documento',
                `${p.tipoDocumento === 'DNI' ? 'DNI' : 'Carné de extranjería'} ${p.numeroDocumento}`,
              ],
              [
                'Fecha de nacimiento',
                p.fechaNacimiento ? formatDay(p.fechaNacimiento) : null,
              ],
              [
                'Sexo',
                p.sexo === 'MASCULINO'
                  ? 'Masculino'
                  : p.sexo === 'FEMENINO'
                    ? 'Femenino'
                    : null,
              ],
              ['Nacionalidad', p.nacionalidad],
              [
                'Lugar de nacimiento',
                [
                  f.datos.distritoNacimiento,
                  f.datos.provinciaNacimiento,
                  f.datos.departamentoNacimiento,
                  f.datos.paisNacimiento,
                ]
                  .filter(Boolean)
                  .join(', '),
              ],
              [
                'Dirección',
                [p.direccion, p.distrito].filter(Boolean).join(', '),
              ],
              ['Referencia', f.datos.referenciaDomicilio],
              ['Lengua materna', f.datos.lenguaMaterna],
              ['Colegio de procedencia', f.datos.colegioProcedencia],
              ['Grado', `${f.grado} · ${f.anioAcademico}`],
              ['Forma de ingreso', f.datos.formaIngreso],
            ]}
          />
        </Card>
      )}

      {pestana === 'Ficha médica' && (
        <Card className="p-6">
          {m ? (
            <Datos
              filas={[
                ['Tipo de sangre', m.tipoSangre],
                ['Seguro', m.tipoSeguro],
                [
                  'Peso / estatura',
                  [
                    m.pesoKg && `${m.pesoKg} kg`,
                    m.estaturaM && `${m.estaturaM} m`,
                  ]
                    .filter(Boolean)
                    .join(' · '),
                ],
                [
                  'Alergias',
                  m.tieneAlergias
                    ? [m.alergiasAlimentarias, m.alergiasMedicamentosas]
                        .filter(Boolean)
                        .join(', ')
                    : 'No',
                ],
                ['¿Ha convulsionado?', siNo(m.haConvulsionado)],
                ['Enfermedad', m.tieneEnfermedad ? m.enfermedadDetalle : 'No'],
                [
                  'Medicamento diario',
                  m.tomaMedicamento ? m.medicamentoDetalle : 'No',
                ],
                ['Observaciones', m.observaciones],
              ]}
            />
          ) : (
            <p className="text-body-3 text-content-tertiary">
              Sin ficha médica registrada.
            </p>
          )}
        </Card>
      )}

      {pestana === 'Familia' && (
        <>
          <Card className="p-6">
            <h2 className="text-h6 font-semibold text-content-primary">
              Padres y apoderado
            </h2>
            <p className="mb-4 text-body-4 text-content-tertiary">
              Personas vinculadas al estudiante. Una misma persona puede estar
              vinculada a varios hermanos.
            </p>
            <Table
              data={f.familia.map((v, i) => ({
                ...v,
                id: v.personaId ?? `nr-${i}`,
              }))}
              columns={[
                {
                  title: 'Persona',
                  field: 'nombreCompleto',
                  Cell: ({ entry }) =>
                    entry.nombreCompleto ? (
                      <div>
                        <p className="text-body-3 text-content-primary">
                          {entry.nombreCompleto}
                        </p>
                        <p className="text-body-4 text-content-tertiary">
                          {entry.documento}
                          {entry.celular && ` · ${entry.celular}`}
                        </p>
                      </div>
                    ) : (
                      <span className="text-content-tertiary">
                        No registra · {entry.motivoNoRegistra}
                      </span>
                    ),
                },
                {
                  title: 'Rol',
                  field: 'parentesco',
                  Cell: ({ entry }) => (
                    <Tag color="primary" size="md" className="rounded-full">
                      {entry.parentesco === 'PADRE'
                        ? 'Padre'
                        : entry.parentesco === 'MADRE'
                          ? 'Madre'
                          : 'Apoderado'}
                    </Tag>
                  ),
                },
                {
                  title: 'Vive con el estudiante',
                  field: 'viveConEstudiante',
                  Cell: ({ entry }) => (
                    <span>
                      {entry.nombreCompleto
                        ? siNo(entry.viveConEstudiante)
                        : '—'}
                    </span>
                  ),
                },
                {
                  title: 'Responsable de matrícula',
                  field: 'esResponsable',
                  Cell: ({ entry }) =>
                    entry.esResponsable ? (
                      <Tag color="success" size="md" className="rounded-full">
                        Responsable
                      </Tag>
                    ) : (
                      <span>—</span>
                    ),
                },
                {
                  title: 'Documento que lo acredita',
                  field: 'acreditacion',
                  Cell: ({ entry }) =>
                    entry.parentesco === 'APODERADO' && entry.nombreCompleto ? (
                      <DocumentoApoderado
                        estudianteId={f.id}
                        documento={entry.acreditacion}
                      />
                    ) : (
                      <span>—</span>
                    ),
                },
              ]}
            />
          </Card>
          <Card className="p-6">
            <h2 className="text-h6 font-semibold text-content-primary">
              Hermanos en el colegio
            </h2>
            <p className="mb-4 text-body-4 text-content-tertiary">
              Se detectan por los padres en común.
            </p>
            <Table
              data={f.hermanos}
              emptyMessage="No tiene hermanos registrados en el colegio"
              columns={[
                { title: 'Estudiante', field: 'nombreCompleto' },
                { title: 'Grado', field: 'grado' },
                {
                  title: 'Estado',
                  field: 'estado',
                  Cell: ({ entry }) => (
                    <EstadoEstudianteTag estado={entry.estado} />
                  ),
                },
                { title: 'Responsable', field: 'responsable' },
              ]}
            />
          </Card>
        </>
      )}

      {pestana === 'Contactos y salida' && (
        <Card className="flex flex-col gap-6 p-6">
          <div>
            <h2 className="mb-3 text-h6 font-semibold text-content-primary">
              Contactos de emergencia
            </h2>
            <ul className="divide-y divide-stroke-tertiary">
              {f.contactos.map((c) => (
                <li
                  key={c.nombreCompleto}
                  className="py-2 text-body-3 text-content-primary"
                >
                  {c.nombreCompleto} ·{' '}
                  <span className="text-content-tertiary">{c.parentesco}</span>
                  <span className="text-content-tertiary">
                    {[c.celular, c.telefonoFijo]
                      .filter(Boolean)
                      .map((t) => ` · ${t}`)}
                  </span>
                </li>
              ))}
            </ul>
          </div>
          <div>
            <h2 className="mb-1 text-h6 font-semibold text-content-primary">
              Autorización de salida
            </h2>
            <p className="text-body-3 text-content-secondary">
              {f.modoSalida ? SALIDA[f.modoSalida] : '—'}
            </p>
            {f.autorizados.length > 0 && (
              <ul className="mt-2 text-body-3 text-content-secondary">
                {f.autorizados.map((a) => (
                  <li key={a.nombreCompleto}>
                    ✓ {a.nombreCompleto} · {a.relacion}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </Card>
      )}
    </>
  );
};
