import { FieldError, UseFormReturn } from 'react-hook-form';

import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/form';
import { Spinner } from '@/components/ui/spinner';
import { Table } from '@/components/ui/table';
import { SeccionVacante } from '@/types/api';

import {
  CAPACIDAD_MAXIMA,
  CAPACIDAD_MINIMA,
  ValoresPeriodoMatricula,
} from './esquemas';

type TablaVacantesProps = {
  form: UseFormReturn<ValoresPeriodoMatricula>;
  /** null mientras se cargan (o antes de elegir el año). */
  secciones: SeccionVacante[] | null;
  total: number;
  mensajeVacio: string;
  deshabilitado?: boolean;
};

type Fila = {
  id: string;
  seccion: SeccionVacante;
  indice: number;
  form: UseFormReturn<ValoresPeriodoMatricula>;
  error?: FieldError;
  deshabilitado: boolean;
};

// Las celdas van fuera del componente: si se crearan en cada render, la tabla las volvería a montar con cada tecla y
// el campo perdería el foco.
const CeldaSeccion = ({ entry }: { entry: Fila }) => (
  <div>
    <p className="text-label-2 text-content-primary">
      {entry.seccion.etiqueta}
    </p>
    <p className="text-caption-1 text-content-tertiary">
      Tutor: {entry.seccion.tutor ?? 'Sin tutor'}
    </p>
  </div>
);

const CeldaGrado = ({ entry }: { entry: Fila }) => (
  <span>{entry.seccion.gradoNombre}</span>
);

const CeldaVacantes = ({ entry }: { entry: Fila }) => (
  <Input
    type="number"
    inputMode="numeric"
    min={CAPACIDAD_MINIMA}
    max={CAPACIDAD_MAXIMA}
    aria-label={`Vacantes de ${entry.seccion.etiqueta}`}
    className="h-9 w-24"
    disabled={entry.deshabilitado}
    registration={entry.form.register(`vacantes.${entry.indice}.capacidad`)}
    error={entry.error}
  />
);

// Figma > pantalla 13 > formulario > tarjeta "Vacantes por grado y sección". Cada fila es una sección activa del año
// con su capacidad máxima (que es lo que ocupan las vacantes); cambiarla cambia la capacidad de la sección.
export const TablaVacantes = ({
  form,
  secciones,
  total,
  mensajeVacio,
  deshabilitado = false,
}: TablaVacantesProps) => {
  const errores = form.formState.errors.vacantes;
  const filas: Fila[] = (secciones ?? []).map((seccion, indice) => ({
    id: seccion.seccionId,
    seccion,
    indice,
    form,
    error: errores?.[indice]?.capacidad,
    deshabilitado,
  }));

  return (
    <Card className="flex flex-col gap-4 p-6">
      <div className="border-b border-stroke-tertiary pb-4">
        <h2 className="text-label-1 font-semibold text-content-primary">
          Vacantes por grado y sección
        </h2>
        <p className="text-body-4 text-content-tertiary">
          Cantidad máxima de estudiantes que se pueden matricular en cada
          sección.
        </p>
      </div>

      {secciones === null ? (
        <div className="flex h-40 items-center justify-center">
          <Spinner />
        </div>
      ) : (
        <>
          <Table
            data={filas}
            emptyMessage={mensajeVacio}
            columns={[
              { title: 'Sección', field: 'seccion', Cell: CeldaSeccion },
              { title: 'Grado', field: 'seccion', Cell: CeldaGrado },
              { title: 'Vacantes', field: 'indice', Cell: CeldaVacantes },
            ]}
          />
          {filas.length > 0 && (
            <p className="text-right text-label-1 font-semibold text-content-primary">
              Total: {total} {total === 1 ? 'vacante' : 'vacantes'}
            </p>
          )}
        </>
      )}
    </Card>
  );
};
