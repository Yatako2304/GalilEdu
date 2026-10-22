import * as React from 'react';

import { ContentLayout } from '@/components/layouts';
import { Tag } from '@/components/ui/tag';
import {
  useEstadoMatriculaApoderado,
  useHijosApoderado,
} from '@/features/matricula/api/estado-matricula';
import {
  ConsultaMatricula,
  datosResumenMatricula,
  fechaPeriodo,
} from '@/features/matricula/components/consulta-matricula';

const ApoderadoMatriculaRoute = () => {
  const hijos = useHijosApoderado();
  const [elegido, setElegido] = React.useState<string>();
  const estudianteId = elegido ?? hijos.data?.[0]?.id;
  const estado = useEstadoMatriculaApoderado(estudianteId);
  const datosEstado = datosResumenMatricula(
    estado.data?.matricula?.estado ??
      hijos.data?.find((hijo) => hijo.id === estudianteId)?.estadoMatricula,
  );
  const etiquetaTitulo =
    (estado.data?.deuda.total ?? 0) > 0
      ? {
          etiqueta: estado.data?.deuda.cuotas.some(
            (cuota) => cuota.estudianteId === estudianteId,
          )
            ? 'Deuda propia'
            : 'Deuda familiar',
          color: 'error' as const,
        }
      : datosEstado;
  const anio = estado.data?.periodo.anio;

  return (
    <ContentLayout
      title={anio ? `Matrícula ${anio}` : 'Matrícula'}
      breadcrumbs={[
        'Matrícula',
        anio ? `Matrícula ${anio}` : 'Periodo actual',
        estado.data?.estudiante.nombre ?? '…',
      ]}
      subtitle={
        estado.data
          ? `Periodo de matrícula ${estado.data.periodo.abierto ? 'abierto' : 'cerrado'} hasta el ${fechaPeriodo(estado.data.periodo.cierre)}`
          : undefined
      }
      titleTag={
        estado.data ? (
          <Tag color={etiquetaTitulo.color} size="md">
            {etiquetaTitulo.etiqueta}
          </Tag>
        ) : undefined
      }
    >
      <ConsultaMatricula
        hijos={hijos.data ?? []}
        estudianteId={estudianteId}
        estado={estado.data}
        isLoading={hijos.isLoading || estado.isLoading}
        onElegir={setElegido}
      />
    </ContentLayout>
  );
};

export default ApoderadoMatriculaRoute;
