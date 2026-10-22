import { ContentLayout } from '@/components/layouts';
import { Spinner } from '@/components/ui/spinner';
import {
  useEstadoMatriculaApoderado,
  useHijosApoderado,
} from '@/features/matricula/api/estado-matricula';
import { ResumenMatricula } from '@/features/matricula/components/resumen-matricula';
import { useUser } from '@/lib/auth';

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
    </ContentLayout>
  );
};

export default ApoderadoInicioRoute;
