import { useNavigate } from 'react-router';

import { ContentLayout } from '@/components/layouts';
import { paths } from '@/config/paths';
import { avisoCuentasNuevas } from '@/features/estudiantes/components/inscripcion/esquema';
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
          // Cada familiar presente tiene usuario, no solo el responsable (validación de la iteración 1 con el PO).
          const aviso = [
            `${r.estudiante} quedó inscrito(a). Responsable de matrícula: ${r.responsable}.`,
            avisoCuentasNuevas(r.cuentas),
          ]
            .filter(Boolean)
            .join(' ');
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
