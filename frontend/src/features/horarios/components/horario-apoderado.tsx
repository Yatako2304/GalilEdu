import { FileDown, Mail, Phone } from 'lucide-react';
import * as React from 'react';

import { ContentLayout } from '@/components/layouts';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Select } from '@/components/ui/form';
import { useNotifications } from '@/components/ui/notifications';
import { Spinner } from '@/components/ui/spinner';
import { Tag } from '@/components/ui/tag';

import {
  HorarioDelEstudiante,
  useHijosConHorario,
  useHorarioDelEstudiante,
} from '../api/horario-apoderado';
import { aMatrizHoraria, descargarHorario } from '../lib/horario-apoderado';

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

/** Figma HU-51 > "Datos del horario": año académico, grado y sección, y la tarjeta del tutor(a). */
const DatosDelHorario = ({
  anios,
  anio,
  onAnio,
  horario,
}: {
  anios: number[];
  anio: number;
  onAnio: (anio: number) => void;
  horario: HorarioDelEstudiante | undefined;
}) => (
  <Card>
    <CardHeader>
      <CardTitle>Datos del horario</CardTitle>
    </CardHeader>
    <CardContent className="flex flex-wrap items-center gap-6 pb-6">
      <Select
        label="Año académico"
        wrapperClassName="w-40"
        value={anio}
        onChange={(e) => onAnio(Number(e.target.value))}
        options={anios.map((a) => ({ label: a, value: a }))}
      />
      {horario && (
        <div className="border-l border-stroke-tertiary pl-6">
          <p className="text-body-4 text-content-tertiary">Grado y sección</p>
          <p className="mt-1 text-label-2 font-semibold text-content-primary">
            {horario.grado} · Sección {horario.seccion}
          </p>
        </div>
      )}
      {horario?.tutor && (
        <div className="flex items-center gap-3 border-l border-stroke-tertiary pl-6">
          <Avatar name={horario.tutor.nombre} />
          <div>
            <p className="flex items-center gap-2 text-label-2 font-medium text-content-primary">
              {horario.tutor.nombre}
              <Tag size="sm">Tutor(a)</Tag>
            </p>
            {(horario.tutor.celular || horario.tutor.correo) && (
              <p className="mt-1 flex flex-wrap gap-x-4 text-body-4 text-content-tertiary">
                {horario.tutor.celular && (
                  <span className="inline-flex items-center gap-1">
                    <Phone className="size-3.5" aria-hidden="true" />
                    {horario.tutor.celular}
                  </span>
                )}
                {horario.tutor.correo && (
                  <span className="inline-flex items-center gap-1">
                    <Mail className="size-3.5" aria-hidden="true" />
                    {horario.tutor.correo}
                  </span>
                )}
              </p>
            )}
          </div>
        </div>
      )}
    </CardContent>
  </Card>
);

/**
 * HU-51 · Figma > Apoderado (real) > "Horario" (selector del hijo, "Descargar Horario" y matriz) y Apoderado >
 * "HU-51 Consulta de horario…" (datos del horario con año y tutor). Arma su propio encabezado porque el botón
 * "Descargar Horario" del encabezado depende del horario elegido.
 */
export const HorarioApoderado = () => {
  const { addNotification } = useNotifications();
  const hijosQuery = useHijosConHorario();
  const hijos = React.useMemo(() => hijosQuery.data ?? [], [hijosQuery.data]);

  const [estudianteId, setEstudianteId] = React.useState<string>();
  const [anio, setAnio] = React.useState<number>();

  // Empieza elegido el primer hijo y su año más reciente.
  const hijo = hijos.find((h) => h.estudianteId === estudianteId) ?? hijos[0];
  const anios = hijo?.matriculas.map((m) => m.anio) ?? [];
  const anioElegido = anio && anios.includes(anio) ? anio : anios[0];
  const matricula = hijo?.matriculas.find((m) => m.anio === anioElegido);

  const horarioQuery = useHorarioDelEstudiante(hijo?.estudianteId, anioElegido);
  const horario = horarioQuery.data;

  const descargar = () => {
    if (horario && !descargarHorario(horario)) {
      addNotification({
        type: 'error',
        title: 'No se pudo abrir el horario',
        message:
          'Permite las ventanas emergentes de GalilEdu e inténtalo de nuevo.',
      });
    }
  };

  let contenido: React.ReactNode;
  if (hijosQuery.isLoading) {
    contenido = <Cargando />;
  } else if (!hijo) {
    contenido = <Vacio texto="No tienes estudiantes matriculados." />;
  } else {
    contenido = (
      <>
        <Select
          label="Estudiante"
          value={hijo.estudianteId}
          onChange={(e) => {
            setEstudianteId(e.target.value);
            setAnio(undefined);
          }}
          options={hijos.map((h) => ({
            label: h.nombreCompleto,
            value: h.estudianteId,
          }))}
          hint={
            matricula
              ? `${matricula.grado} · Sección ${matricula.seccion}`
              : undefined
          }
        />

        <DatosDelHorario
          anios={anios}
          anio={anioElegido}
          onAnio={setAnio}
          horario={horario}
        />

        {horarioQuery.isLoading ? (
          <Cargando />
        ) : !horario?.disponible || !horario.jornada ? (
          <Vacio texto="El horario de esta sección aún no está disponible." />
        ) : (
          <Card>
            <CardHeader>
              <CardTitle>Matriz horaria</CardTitle>
            </CardHeader>
            <CardContent className="p-6">
              <MatrizHoraria
                horario={aMatrizHoraria(horario.jornada, horario.clases)}
              />
            </CardContent>
          </Card>
        )}
      </>
    );
  }

  return (
    <ContentLayout
      title="Horario"
      subtitle="Pantalla para que el apoderado consulte el horario de clases del alumno."
      actions={
        <Button
          icon={<FileDown className="size-4" />}
          disabled={!horario?.disponible}
          onClick={descargar}
        >
          Descargar Horario
        </Button>
      }
    >
      {contenido}
    </ContentLayout>
  );
};
