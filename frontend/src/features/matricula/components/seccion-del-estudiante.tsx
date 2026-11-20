import { Card, CardContent, CardHeader } from '@/components/ui/card';
import { Spinner } from '@/components/ui/spinner';

import {
  useCambiosDeSeccion,
  useDetalleMatricula,
  useMatriculaDeEstudiante,
} from '../api/matricula-detalle';

import { BotonCambiarSeccion } from './boton-cambiar-seccion';
import { ListaCambiosSeccion } from './historial-cambios-seccion';

// Subtítulo y lista de cambios dentro de la tarjeta; no se dibuja si la matrícula nunca cambió de sección.
const CambiosDeSeccion = ({ matriculaId }: { matriculaId: string }) => {
  const cambios = useCambiosDeSeccion(matriculaId);
  if (!cambios.data || cambios.data.length === 0) return null;

  return (
    <div className="w-full border-t border-stroke-tertiary pt-4">
      <h3 className="mb-3 text-label-1 text-content-primary">
        Historial de cambios de sección
      </h3>
      <ListaCambiosSeccion cambios={cambios.data} />
    </div>
  );
};

// Tarjeta "Sección" de la ficha del estudiante (pestaña Datos personales): la sección de su matrícula
// vigente, el mismo botón "Cambiar sección" del detalle de matrícula y el historial de cambios.
export const SeccionDelEstudiante = ({
  estudianteId,
}: {
  estudianteId: string;
}) => {
  const vigente = useMatriculaDeEstudiante(estudianteId);
  const matriculaId = vigente.data?.matriculaId ?? undefined;
  const detalle = useDetalleMatricula(matriculaId);

  if (vigente.isLoading || detalle.isLoading) {
    return (
      <Card className="flex h-24 items-center justify-center">
        <Spinner />
      </Card>
    );
  }

  const matricula = detalle.data;
  const seccion = matricula?.seccion;

  return (
    <Card>
      <CardHeader>
        <h2 className="text-label-1 text-content-primary">Sección actual</h2>
      </CardHeader>
      <CardContent className="flex flex-wrap items-center justify-between gap-4">
        {matricula ? (
          <>
            <div>
              <p className="text-body-3 font-medium text-content-primary">
                {seccion ? seccion.etiqueta : 'Sin sección asignada'}
              </p>
              <p className="text-body-4 text-content-tertiary">
                {[
                  `${matricula.grado} · ${matricula.anio}`,
                  seccion?.tutor && `Tutor: ${seccion.tutor}`,
                ]
                  .filter(Boolean)
                  .join(' · ')}
              </p>
            </div>
            <BotonCambiarSeccion matricula={matricula} />
            <CambiosDeSeccion matriculaId={matricula.id} />
          </>
        ) : (
          <p className="text-body-3 text-content-tertiary">
            Aún no tiene matrícula, por eso no tiene sección.
          </p>
        )}
      </CardContent>
    </Card>
  );
};
