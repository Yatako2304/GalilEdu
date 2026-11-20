import * as React from 'react';

import { DatosPersonaVista } from '@/components/personas/datos-persona-vista';
import { Card, CardHeader, CardTitle } from '@/components/ui/card';
import { Select } from '@/components/ui/form';
import { ListaDatos } from '@/components/ui/lista-datos';
import { Spinner } from '@/components/ui/spinner';
import { Tabs } from '@/components/ui/tabs';
import { useMiFamilia } from '@/lib/auth';
import {
  EstadoEstudiante,
  FamiliaApoderado,
  IntegranteFamiliar,
} from '@/types/api';

type Pestana = 'hijos' | 'padre' | 'madre' | 'apoderado';

const SITUACION: Record<EstadoEstudiante, string> = {
  INSCRITO: 'Inscrito(a)',
  MATRICULADO: 'Matriculado(a)',
  RETIRADO: 'Retirado(a)',
};

const Vacio = ({ texto }: { texto: string }) => (
  <p className="rounded-xl bg-surface-secondary px-4 py-3 text-body-4 text-content-tertiary">
    {texto}
  </p>
);

const Lista = ({
  titulo,
  filas,
}: {
  titulo?: string;
  filas: { clave: string; principal: string; detalle: string }[];
}) => (
  <section className="space-y-3">
    {titulo && (
      <h3 className="text-label-3 uppercase tracking-wide text-content-tertiary">
        {titulo}
      </h3>
    )}
    <ul className="divide-y divide-stroke-tertiary rounded-xl border border-stroke-tertiary">
      {filas.map((f) => (
        <li key={f.clave} className="px-4 py-3">
          <p className="text-body-3 font-medium text-content-primary">
            {f.principal}
          </p>
          <p className="text-body-4 text-content-quartiary">{f.detalle}</p>
        </li>
      ))}
    </ul>
  </section>
);

const PestanaHijos = ({ familia: f }: { familia: FamiliaApoderado }) => (
  <div className="space-y-6">
    <Lista
      filas={f.estudiantes.map((e) => ({
        clave: e.id,
        principal: e.nombreCompleto,
        detalle: `${e.grado} · ${e.anioAcademico} · ${SITUACION[e.estado]}`,
      }))}
    />
    {f.contactosEmergencia.length > 0 && (
      <Lista
        titulo="Contactos de emergencia"
        filas={f.contactosEmergencia.map((c) => ({
          clave: `${c.nombreCompleto}-${c.celular}`,
          principal: c.nombreCompleto,
          detalle: [c.parentesco, c.celular, c.telefonoFijo]
            .filter(Boolean)
            .join(' · '),
        }))}
      />
    )}
  </div>
);

const PestanaIntegrante = ({
  integrante,
  sinRegistro,
}: {
  integrante: IntegranteFamiliar | null;
  sinRegistro: string;
}) => {
  if (!integrante) return <Vacio texto={sinRegistro} />;
  if (!integrante.persona) {
    return (
      <Vacio
        texto={`No registra${integrante.motivoNoRegistra ? `: ${integrante.motivoNoRegistra}` : ''}.`}
      />
    );
  }
  return (
    <div className="space-y-6">
      {(integrante.fallecido || integrante.relacion) && (
        <ListaDatos
          datos={[
            ...(integrante.relacion
              ? [
                  {
                    etiqueta: 'Relación con el estudiante',
                    valor: integrante.relacion,
                  },
                ]
              : []),
            ...(integrante.fallecido
              ? [{ etiqueta: 'Situación', valor: 'Fallecido(a)' }]
              : []),
          ]}
        />
      )}
      <DatosPersonaVista
        persona={integrante.persona}
        conExalumno
        soloIdentidad={integrante.fallecido}
      />
    </div>
  );
};

const etiquetaPestana = (
  texto: string,
  integrante: IntegranteFamiliar | null,
) => (integrante?.esUsted ? `${texto} (tú)` : texto);

/**
 * Apoderado › «Datos de la Familia» (ítem «Familia» del menú lateral), de solo vista. Figma dibuja las pestañas
 * Familia, Padre, Madre y Apoderado; aquí la primera se llama «Hijos(as)» y lista los estudiantes a cargo y sus
 * contactos de emergencia (sin distrito ni dirección: el domicilio es de cada persona, no de la familia). Los hermanos
 * con el mismo padre, madre y apoderado son una familia; si hay más de una, se elige en el selector. Los cambios a la
 * familia los hace el colegio; el apoderado edita solo sus propios datos en «Mis datos».
 */
export const FamiliaDelApoderado = () => {
  const familias = useMiFamilia();
  const [indice, setIndice] = React.useState(0);
  const [pestana, setPestana] = React.useState<Pestana>('hijos');

  if (familias.isPending) {
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    );
  }
  if (!familias.data || familias.data.length === 0) {
    return <Vacio texto="Aún no tienes estudiantes a cargo registrados." />;
  }

  const lista = familias.data;
  const familia = lista[Math.min(indice, lista.length - 1)];

  return (
    <div className="space-y-6">
      {lista.length > 1 && (
        <Select
          label="Familia"
          options={lista.map((f, i) => ({
            value: String(i),
            label: `${f.nombre} (${f.estudiantes.map((e) => e.nombreCompleto.split(' ')[0]).join(', ')})`,
          }))}
          value={String(indice)}
          onChange={(e) => setIndice(Number(e.target.value))}
        />
      )}
      <Card>
        <CardHeader>
          <CardTitle>Familia {familia.nombre}</CardTitle>
        </CardHeader>
        <div className="space-y-5 p-5">
          <Tabs
            aria-label="Secciones de la familia"
            fullWidth
            items={[
              { value: 'hijos', label: 'Hijos(as)' },
              {
                value: 'padre',
                label: etiquetaPestana('Padre', familia.padre),
              },
              {
                value: 'madre',
                label: etiquetaPestana('Madre', familia.madre),
              },
              {
                value: 'apoderado',
                label: etiquetaPestana('Apoderado', familia.apoderado),
              },
            ]}
            value={pestana}
            onValueChange={setPestana}
          />
          <div role="tabpanel">
            {pestana === 'hijos' && <PestanaHijos familia={familia} />}
            {pestana === 'padre' && (
              <PestanaIntegrante
                integrante={familia.padre}
                sinRegistro="No se registró al padre."
              />
            )}
            {pestana === 'madre' && (
              <PestanaIntegrante
                integrante={familia.madre}
                sinRegistro="No se registró a la madre."
              />
            )}
            {pestana === 'apoderado' && (
              <PestanaIntegrante
                integrante={familia.apoderado}
                sinRegistro="No se registró un apoderado aparte: el estudiante tiene padre o madre registrados."
              />
            )}
          </div>
        </div>
      </Card>
    </div>
  );
};
