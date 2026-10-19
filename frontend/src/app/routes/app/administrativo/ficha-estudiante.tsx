import * as React from 'react';
import { useLocation, useParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
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
