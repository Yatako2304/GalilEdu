import { Card } from '@/components/ui/card';
import { Table } from '@/components/ui/table';
import { Tag } from '@/components/ui/tag';
import { PlanGrado } from '@/types/api';

import { usePesosCompetencias } from '../api/plan-evaluacion';

/** Cuánto aporta cada curso del grado a cada competencia, con las etiquetas de pesos de Calificaciones. */
export const PesosCompetencias = ({ plan }: { plan: PlanGrado }) => {
  const pesos = usePesosCompetencias({
    anioEscolarId: plan.anioEscolarId,
    grado: plan.gradoCodigo,
  });
  if (!pesos.data?.length) return null;

  return (
    <Card className="space-y-3 p-5">
      <div>
        <h3 className="text-label-1 font-semibold text-content-primary">
          Peso de cada curso por competencia
        </h3>
        <p className="text-body-4 text-content-tertiary">
          Se configura en cada curso, con Configurar competencias. Se aplica
          cuando la regla de nota del año es promedio ponderado.
        </p>
      </div>
      <Table
        data={pesos.data.map((c) => ({ ...c, id: c.competenciaId }))}
        columns={[
          {
            title: 'Competencia',
            field: 'competencia',
            Cell: ({ entry }) => (
              <div>
                <p className="text-body-3 text-content-primary">
                  {entry.competencia}
                </p>
                <p className="text-body-4 text-content-tertiary">
                  {entry.area}
                </p>
              </div>
            ),
          },
          {
            title: 'Cursos',
            field: 'cursos',
            Cell: ({ entry }) => (
              <ul className="space-y-1">
                {entry.cursos.map((c) => (
                  <li key={c.planCursoId} className="text-body-4">
                    {c.curso}:{' '}
                    {c.pesoCurso == null ? 'sin peso' : `${c.pesoCurso} %`}
                  </li>
                ))}
              </ul>
            ),
          },
          {
            title: 'Total',
            field: 'total',
            Cell: ({ entry }) => <span>{entry.total} %</span>,
          },
          {
            title: 'Estado',
            field: 'completo',
            Cell: ({ entry }) => (
              <Tag
                color={entry.completo ? 'success' : 'error'}
                appearance={entry.completo ? 'soft' : 'outline'}
                size="sm"
              >
                {entry.completo ? 'Pesos completos' : 'Pesos incompletos'}
              </Tag>
            ),
          },
        ]}
      />
    </Card>
  );
};
