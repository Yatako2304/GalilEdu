import { Eye } from 'lucide-react';
import * as React from 'react';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Tag } from '@/components/ui/tag';
import {
  EstadoEstudiante,
  EstudianteACargo,
  Parentesco,
  SeccionTutoria,
} from '@/types/api';

import { abrirDocumentoAcreditacion } from '../api/get-usuario';

const PARENTESCOS: Record<Parentesco, string> = {
  PADRE: 'Padre',
  MADRE: 'Madre',
  APODERADO: 'Apoderado',
};

const ESTADOS: Record<
  EstadoEstudiante,
  { label: string; color: 'info' | 'success' | 'neutral' }
> = {
  INSCRITO: { label: 'Inscrito', color: 'info' },
  MATRICULADO: { label: 'Matriculado', color: 'success' },
  RETIRADO: { label: 'Retirado', color: 'neutral' },
};

const siNo = (v: boolean) => (v ? 'Sí' : 'No');

const Encabezado = ({ titulo }: { titulo: string }) => (
  <h3 className="text-label-3 uppercase tracking-wide text-content-tertiary">
    {titulo}
  </h3>
);

const Vacio = ({ texto }: { texto: string }) => (
  <p className="rounded-xl bg-surface-secondary px-4 py-3 text-body-4 text-content-tertiary">
    {texto}
  </p>
);

/**
 * Estudiantes a cargo de un apoderado, con los datos de su vínculo tal como se registraron en la
 * inscripción (Paso 3 · Padres y apoderado). Solo de vista: se modifican desde la ficha del estudiante.
 */
export const EstudiantesACargo = ({
  estudiantes = [],
}: {
  estudiantes?: EstudianteACargo[];
}) => (
  <section className="space-y-3 border-t border-stroke-tertiary pt-5">
    <Encabezado titulo="Estudiantes a cargo" />
    {estudiantes.length === 0 ? (
      <Vacio texto="No tiene estudiantes a cargo." />
    ) : (
      <ul className="space-y-3">
        {estudiantes.map((e) => (
          <li key={e.estudianteId}>
            <TarjetaEstudiante estudiante={e} />
          </li>
        ))}
      </ul>
    )}
  </section>
);

const TarjetaEstudiante = ({
  estudiante: e,
}: {
  estudiante: EstudianteACargo;
}) => {
  const esApoderado = e.parentesco === 'APODERADO';
  const datos: [string, React.ReactNode][] = [
    ...(esApoderado
      ? ([['Relación', e.relacion ?? '—']] as [string, React.ReactNode][])
      : []),
    ['Vive con el estudiante', siNo(e.viveConEstudiante)],
    ['Tiene la patria potestad / tutela', siNo(e.tienePatriaPotestad)],
    ['Responsable de matrícula', siNo(e.esResponsable)],
    ...(esApoderado
      ? ([
          [
            'Documento que lo acredita',
            e.acreditacion ? (
              <Button
                type="button"
                variant="link"
                size="sm"
                className="h-auto p-0"
                icon={<Eye className="size-4" />}
                title={e.acreditacion.nombreArchivo}
                onClick={() => abrirDocumentoAcreditacion(e.estudianteId)}
              >
                {e.acreditacion.tipo}
              </Button>
            ) : (
              'No adjuntado'
            ),
          ],
        ] as [string, React.ReactNode][])
      : []),
  ];

  return (
    <Card className="space-y-4 p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-label-2 font-medium text-content-primary">
            {e.nombreCompleto}
          </p>
          <p className="text-body-4 text-content-tertiary">
            {e.documento} · {e.grado} ({e.anioAcademico})
          </p>
        </div>
        <div className="flex gap-2">
          <Tag color="primary" size="md" className="rounded-full">
            {PARENTESCOS[e.parentesco]}
          </Tag>
          <Tag
            color={ESTADOS[e.estado].color}
            size="md"
            className="rounded-full"
          >
            {ESTADOS[e.estado].label}
          </Tag>
        </div>
      </div>
      <dl className="grid grid-cols-1 gap-x-8 gap-y-3 md:grid-cols-2">
        {datos.map(([k, v]) => (
          <div key={k}>
            <dt className="text-body-4 text-content-quartiary">{k}</dt>
            <dd className="text-body-3 text-content-primary">{v}</dd>
          </div>
        ))}
      </dl>
    </Card>
  );
};

/** Secciones del año escolar actual en las que el usuario es tutor o cotutor. Solo de vista. */
export const SeccionesTutor = ({
  secciones = [],
}: {
  secciones?: SeccionTutoria[];
}) => (
  <section className="space-y-3 border-t border-stroke-tertiary pt-5">
    <Encabezado titulo="Tutoría del año actual" />
    {secciones.length === 0 ? (
      <Vacio texto="No es tutor de ninguna sección este año." />
    ) : (
      <ul className="divide-y divide-stroke-tertiary rounded-xl border border-stroke-tertiary">
        {secciones.map((s) => (
          <li
            key={s.seccionId}
            className="flex items-center justify-between gap-3 px-4 py-3"
          >
            <span className="text-body-3 text-content-primary">
              {s.seccion}
            </span>
            <Tag
              color={s.cargo === 'TUTOR' ? 'primary' : 'neutral'}
              size="md"
              className="rounded-full"
            >
              {s.cargo === 'TUTOR' ? 'Tutor' : 'Cotutor'}
            </Tag>
          </li>
        ))}
      </ul>
    )}
  </section>
);
