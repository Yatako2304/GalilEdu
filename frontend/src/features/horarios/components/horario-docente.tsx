import * as React from 'react';

import { ContentLayout } from '@/components/layouts';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Select } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';

import { useAniosDocente, useHorarioDocente } from '../api/horario-docente';
import { aMatrizDelDocente } from '../lib/horario-docente';

import { MatrizHoraria } from './matriz-horaria';

const Cargando = () => (
  <div className="flex h-48 w-full items-center justify-center">
    <Spinner size="lg" />
  </div>
);

const Vacio = ({ texto }: { texto: string }) => (
  <Card>
    <CardContent className="py-10 text-center text-body-3 text-content-quartiary">
      {texto}
    </CardContent>
  </Card>
);

/**
 * HU-49 · Figma > Docente / Tutor > Horario (393:18119): el horario de dictado del usuario que inició sesión, solo con
 * las clases donde es titular. Lo usan las rutas del Docente y del Tutor. Un horario (una matriz) por estructura
 * horaria, porque cada una tiene su propia jornada; con más de uno, cada tarjeta lleva su título («Primaria»,
 * «Secundaria»).
 */
export const HorarioDocente = () => {
  const aniosQuery = useAniosDocente();
  const [anioId, setAnioId] = React.useState<string>();
  // Sin año elegido, el backend decide: el ACTIVO si tiene clases y, si no, el más reciente con clases.
  const horarioQuery = useHorarioDocente(anioId);
  const horario = horarioQuery.data;

  const anios = aniosQuery.data ?? [];
  const anioMostrado = anioId ?? horario?.anio?.id;

  let contenido: React.ReactNode;
  if (horarioQuery.isLoading) {
    contenido = <Cargando />;
  } else if (!horario?.anio) {
    contenido = <Vacio texto="Aún no tienes horarios asignados." />;
  } else if (horario.horarios.length === 0) {
    contenido = <Vacio texto="Aún no tienes bloques asignados en este año." />;
  } else {
    const variosHorarios = horario.horarios.length > 1;
    contenido = (
      <>
        {horario.horarios.map((h) => (
          <Card key={`${h.nivel}-${h.titulo}`}>
            {variosHorarios && (
              <CardHeader>
                <CardTitle>{h.titulo}</CardTitle>
              </CardHeader>
            )}
            <CardContent className="p-6">
              <MatrizHoraria horario={aMatrizDelDocente(h)} />
            </CardContent>
          </Card>
        ))}
      </>
    );
  }

  return (
    <ContentLayout
      title="Horario"
      subtitle="Visualiza tu horario de un año académico seleccionado"
      actions={
        anios.length > 0 && anioMostrado ? (
          <Select
            label="Año académico"
            wrapperClassName="w-40"
            value={anioMostrado}
            onChange={(e) => setAnioId(e.target.value)}
            options={anios.map((a) => ({ label: a.anio, value: a.id }))}
          />
        ) : undefined
      }
    >
      <div className="space-y-6">{contenido}</div>
    </ContentLayout>
  );
};
