import { useNavigate } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { paths } from '@/config/paths';
import { InscripcionWizard } from '@/features/estudiantes/components/inscripcion/inscripcion-wizard';

// Figma > "Admin / Estudiantes | Inscripción | Paso 1…5" (HU-7)
const NuevaInscripcionRoute = () => {
  const navigate = useNavigate();
  const listado = paths.app.administrativo.estudiantes.getHref();

  return (
    <ContentLayout
      title="Nueva inscripción"
      breadcrumbs={['Estudiantes', 'Nueva inscripción']}
    >
      <InscripcionWizard
        onCancelar={() => navigate(listado)}
        onRegistrada={(r) => {
          const aviso = r.cuentaNueva
            ? `${r.estudiante} se vinculó a ${r.responsable} (responsable). Se creó su usuario ${r.usuarioResponsable} y se enviaron las credenciales a su correo.`
            : `${r.estudiante} se vinculó a la cuenta de ${r.responsable} (responsable, usuario ${r.usuarioResponsable}).`;
          const avisoCompleto = r.documentoFallido
            ? `${aviso} No se pudo subir el documento del apoderado: vuelve a intentarlo desde esta ficha.`
            : aviso;
          navigate(paths.app.administrativo.fichaEstudiante.getHref(r.id), {
            state: { aviso: avisoCompleto },
          });
        }}
      />
    </ContentLayout>
  );
};

export default NuevaInscripcionRoute;
