import { Mail, Phone } from 'lucide-react';
import * as React from 'react';

import { Avatar } from '@/components/ui/avatar';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Select } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';
import { Tag } from '@/components/ui/tag';

import { useMiHorario } from '../api/get-mi-horario';

import { MatrizHoraria } from './matriz-horaria';

const ANIO_ACTUAL = new Date().getFullYear();
const ANIOS = [ANIO_ACTUAL, ANIO_ACTUAL - 1, ANIO_ACTUAL - 2];

// Figma: Estudiante/Horarios (HU-50) y Apoderado (HU-51) comparten esta vista.
export const MiHorario = () => {
  const [anio, setAnio] = React.useState(ANIOS[0]);
  const horarioQuery = useMiHorario({ anio });

  if (horarioQuery.isLoading) {
    return (
      <div className="flex h-48 w-full items-center justify-center">
        <Spinner size="lg" />
      </div>
    );
  }

  const horario = horarioQuery.data?.data;

  if (!horario) {
    return (
      <Card>
        <CardContent className="py-10 text-center text-body-3 text-content-quartiary">
          No se encontró un horario para el año seleccionado.
        </CardContent>
      </Card>
    );
  }

  const { seccion, estudiante } = horario;
  const grado = `${seccion.grado}.°${seccion.nivel ? ` de ${seccion.nivel === 'PRIMARIA' ? 'primaria' : 'secundaria'}` : ''}`;

  return (
    <>
      {estudiante && (
        <Card>
          <CardContent className="flex items-center gap-8 py-5">
            <Avatar name={estudiante.nombreCompleto} />
            <div>
              <p className="text-label-1 font-medium text-content-primary">
                {estudiante.nombreCompleto}
              </p>
              <p className="mt-1 text-body-4 text-content-tertiary">
                {grado} · {seccion.letra}
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Figma HU-50/51 > "Datos del horario": año, grado y sección, y la tarjeta del tutor. */}
      <Card>
        <CardHeader>
          <CardTitle>Datos del horario</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap items-center gap-6 pb-6">
          <Select
            label="Año académico"
            wrapperClassName="w-40"
            value={anio}
            onChange={(event) => setAnio(Number(event.target.value))}
            options={ANIOS.map((value) => ({ label: value, value }))}
          />
          <div className="border-l border-stroke-tertiary pl-6">
            <p className="text-body-4 text-content-tertiary">Grado y sección</p>
            <p className="mt-1 text-label-2 font-semibold text-content-primary">
              {grado} · Sección {seccion.letra}
            </p>
          </div>
          <div className="flex items-center gap-3 border-l border-stroke-tertiary pl-6">
            <Avatar name={seccion.tutor} />
            <div>
              <p className="flex items-center gap-2 text-label-2 font-medium text-content-primary">
                {seccion.tutor}
                <Tag size="sm">Tutor(a)</Tag>
              </p>
              {(seccion.tutorCelular || seccion.tutorCorreo) && (
                <p className="mt-1 flex flex-wrap gap-x-4 text-body-4 text-content-tertiary">
                  {seccion.tutorCelular && (
                    <span className="inline-flex items-center gap-1">
                      <Phone className="size-3.5" aria-hidden="true" />
                      {seccion.tutorCelular}
                    </span>
                  )}
                  {seccion.tutorCorreo && (
                    <span className="inline-flex items-center gap-1">
                      <Mail className="size-3.5" aria-hidden="true" />
                      {seccion.tutorCorreo}
                    </span>
                  )}
                </p>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Matriz horaria</CardTitle>
        </CardHeader>
        <CardContent className="p-6">
          <MatrizHoraria horario={horario} />
        </CardContent>
      </Card>
    </>
  );
};
