import { Pencil } from 'lucide-react';
import * as React from 'react';
import { useLocation, useParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { Button } from '@/components/ui/button';
import { Link } from '@/components/ui/link';
import { paths } from '@/config/paths';
import { useFichaEstudiante } from '@/features/estudiantes/api/estudiantes';
import {
  FichaEstudiante,
  encabezadoFicha,
} from '@/features/estudiantes/components/ficha-estudiante';

// Figma > "Admin / Estudiantes | Ficha del estudiante"
const FichaEstudianteRoute = () => {
  const { estudianteId = '' } = useParams();
  const location = useLocation();
  const [aviso, setAviso] = React.useState<string | null>(
    (location.state as { aviso?: string } | null)?.aviso ?? null,
  );
  const ficha = useFichaEstudiante(estudianteId);
  const cabecera = ficha.data ? encabezadoFicha(ficha.data) : null;

  return (
    <ContentLayout
      title={cabecera?.nombre ?? 'Estudiante'}
      breadcrumbs={['Estudiantes', cabecera?.nombre ?? '…']}
      subtitle={cabecera?.detalle}
      actions={
        // Figma: "Editar datos" abre el mismo asistente de la inscripción con los datos guardados.
        <Button asChild variant="primary-light">
          <Link
            to={paths.app.administrativo.editarEstudiante.getHref(estudianteId)}
            className="hover:no-underline"
          >
            <Pencil className="size-4" aria-hidden="true" />
            Editar datos
          </Link>
        </Button>
      }
    >
      <FichaEstudiante
        id={estudianteId}
        aviso={aviso}
        onCerrarAviso={() => setAviso(null)}
      />
    </ContentLayout>
  );
};

export default FichaEstudianteRoute;
