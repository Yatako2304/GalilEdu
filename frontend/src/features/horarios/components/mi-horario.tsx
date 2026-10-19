import * as React from 'react';

import { Avatar } from '@/components/ui/avatar';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input, Select } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';

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
                {seccion.grado} ° {seccion.letra}
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Datos del Horario</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-6 pb-6 sm:grid-cols-2 lg:grid-cols-4">
          <Select
            label="Año académico"
            value={anio}
            onChange={(event) => setAnio(Number(event.target.value))}
            options={ANIOS.map((value) => ({ label: value, value }))}
          />
          <Input label="Grado" value={seccion.grado} disabled readOnly />
          <Input label="Sección" value={seccion.letra} disabled readOnly />
          <Input label="Tutor" value={seccion.tutor} disabled readOnly />
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
