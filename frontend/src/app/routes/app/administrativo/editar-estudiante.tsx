import { useNavigate, useParams } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { Spinner } from '@/components/ui/spinner';
import { paths } from '@/config/paths';
import { useEdicionEstudiante } from '@/features/estudiantes/api/estudiantes';
import { InscripcionWizard } from '@/features/estudiantes/components/inscripcion/inscripcion-wizard';

// "Editar" del listado de estudiantes: el mismo asistente de "Nueva inscripción" con los datos guardados (UX).
const EditarEstudianteRoute = () => {
  const navigate = useNavigate();
  const { estudianteId = '' } = useParams();
  const edicion = useEdicionEstudiante(estudianteId);
  const ficha = paths.app.administrativo.fichaEstudiante.getHref(estudianteId);

  return (
    <ContentLayout
      title="Editar inscripción"
      breadcrumbs={['Estudiantes', 'Editar inscripción']}
    >
      {edicion.data ? (
        <InscripcionWizard
          edicion={edicion.data}
          onCancelar={() => navigate(ficha)}
          onRegistrada={(r) => {
            const aviso = r.cuentaNueva
              ? `Se guardaron los cambios de ${r.estudiante}. ${r.responsable} es ahora el responsable: se creó su usuario ${r.usuarioResponsable} y se enviaron las credenciales a su correo.`
              : `Se guardaron los cambios de ${r.estudiante}.`;
            navigate(ficha, {
              state: {
                aviso: r.documentoFallido
                  ? `${aviso} No se pudo subir el documento del apoderado: vuelve a intentarlo desde esta ficha.`
                  : aviso,
              },
            });
          }}
        />
      ) : (
        <div className="flex h-60 items-center justify-center">
          <Spinner />
        </div>
      )}
    </ContentLayout>
  );
};

export default EditarEstudianteRoute;
