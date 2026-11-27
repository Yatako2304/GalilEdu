import { CirclePlus } from 'lucide-react';
import * as React from 'react';

import { ContentLayout } from '@/components/layouts';
import { Button } from '@/components/ui/button';
import { Link } from '@/components/ui/link';
import { paths } from '@/config/paths';
import { EstudiantesLista } from '@/features/estudiantes/components/estudiantes-lista';
import { DialogoRetiroAlumno } from '@/features/matricula/components/dialogo-retiro-alumno';
import { EstudianteResumen } from '@/types/api';

// Figma > "Admin / Estudiantes | Listado de estudiantes" (HU-7)
const EstudiantesRoute = () => {
  const [retiro, setRetiro] = React.useState<EstudianteResumen | null>(null);
  return (
    <ContentLayout
      title="Estudiantes"
      subtitle="Estudiantes inscritos. No tienen usuario: se vinculan a sus padres o apoderado, y el responsable gestiona su cuenta."
      actions={
        <Button asChild>
          <Link
            to={paths.app.administrativo.nuevaInscripcion.getHref()}
            className="text-white hover:no-underline"
          >
            <CirclePlus className="size-4" aria-hidden="true" />
            Nueva inscripción
          </Link>
        </Button>
      }
    >
      <EstudiantesLista
        hrefFicha={paths.app.administrativo.fichaEstudiante.getHref}
        hrefEditar={paths.app.administrativo.editarEstudiante.getHref}
        onRetirarMatriculado={setRetiro}
      />
      {retiro && (
        <DialogoRetiroAlumno
          abierto
          onOpenChange={(abierto) => !abierto && setRetiro(null)}
          estudianteId={retiro.id}
          estudiante={retiro.nombreCompleto}
        />
      )}
    </ContentLayout>
  );
};

export default EstudiantesRoute;
