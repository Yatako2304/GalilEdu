import {
  CalendarDays,
  ClipboardSignature,
  GraduationCap,
  Star,
  Wallet,
} from 'lucide-react';

import { ContentLayout } from '@/components/layouts';
import { ShortcutCard } from '@/components/ui/shortcut-card';
import { Spinner } from '@/components/ui/spinner';
import { paths } from '@/config/paths';
import {
  useEstadoMatriculaApoderado,
  useHijosApoderado,
} from '@/features/matricula/api/estado-matricula';
import { ResumenMatricula } from '@/features/matricula/components/resumen-matricula';
import { useUser } from '@/lib/auth';

// Figma > "00 · Home — Apoderado" > Accesos rápidos. "Familia" aún no tiene pantalla.
const ACCESOS = [
  {
    title: 'Matrícula',
    description: 'Estado, documentos y pago de matrícula',
    to: paths.app.apoderado.matricula.getHref(),
    icon: <ClipboardSignature />,
  },
  {
    title: 'Pensiones',
    description: 'Cronograma de cuotas y pagos en línea',
    to: paths.app.apoderado.pensiones.getHref(),
    icon: <Wallet />,
  },
  {
    title: 'Calificaciones',
    description: 'Notas por curso y competencia',
    to: paths.app.apoderado.calificaciones.getHref(),
    icon: <Star />,
  },
  {
    title: 'Horarios',
    description: 'Horario de clases de la sección',
    to: paths.app.apoderado.horarios.getHref(),
    icon: <CalendarDays />,
  },
  {
    title: 'Estudiante',
    description: 'Datos del estudiante',
    to: paths.app.apoderado.datosAlumno.getHref(),
    icon: <GraduationCap />,
  },
];

const ApoderadoInicioRoute = () => {
  const usuario = useUser();
  const hijos = useHijosApoderado();
  const estado = useEstadoMatriculaApoderado(hijos.data?.[0]?.id);
  const cantidad = hijos.data?.length ?? 0;

  return (
    <ContentLayout
      title={`Hola, ${usuario.data?.firstName ?? ''}`.trim()}
      subtitle={`Apoderado · ${cantidad} ${cantidad === 1 ? 'estudiante' : 'estudiantes'} a tu cargo`}
    >
      {hijos.isLoading || estado.isLoading ? (
        <div
          className="flex min-h-48 items-center justify-center"
          data-testid="inicio-loading"
        >
          <Spinner size="lg" />
        </div>
      ) : (
        <div className="grid gap-6 lg:grid-cols-2">
          <ResumenMatricula estado={estado.data} />
        </div>
      )}

      <section className="flex flex-col gap-3">
        <h2 className="text-h6 font-semibold text-content-primary">
          Accesos rápidos
        </h2>
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {ACCESOS.map((acceso) => (
            <ShortcutCard key={acceso.title} {...acceso} />
          ))}
        </div>
      </section>
    </ContentLayout>
  );
};

export default ApoderadoInicioRoute;
