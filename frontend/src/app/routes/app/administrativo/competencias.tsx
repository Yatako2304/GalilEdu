import * as React from 'react';

import { ContentLayout } from '@/components/layouts';
import { Tabs } from '@/components/ui/tabs';
import { EscalaCalificacion } from '@/features/catalogo-academico/components/escala-calificacion';
import { PlanCurricular } from '@/features/catalogo-academico/components/plan-curricular';
import { PlanEvaluacion } from '@/features/catalogo-academico/components/plan-evaluacion';

// Figma > Personal Administrativo > Competencias: pestañas CI-14, CI-17 y CI-23.
const PESTANAS = [
  {
    id: 'plan-curricular',
    titulo: 'Plan curricular',
    subtitulo:
      'Organiza el catálogo curricular: cada área agrupa sus competencias y sus cursos.',
  },
  {
    id: 'plan-evaluacion',
    titulo: 'Plan de evaluación',
    subtitulo:
      'Define qué cursos se dictan en cada grado, cuántas horas tienen, qué competencias evalúan y qué docente los dicta en cada sección.',
  },
  {
    id: 'escala',
    titulo: 'Escala de calificación',
    subtitulo:
      'Define cómo se convierte una nota vigesimal (0–20) a un nivel de logro (AD, A, B, C).',
  },
] as const;

type Pestana = (typeof PESTANAS)[number]['id'];

const AdministrativoCompetenciasRoute = () => {
  const [pestana, setPestana] = React.useState<Pestana>('plan-curricular');
  const actual = PESTANAS.find((p) => p.id === pestana)!;

  return (
    <ContentLayout
      title="Competencias"
      breadcrumbs={['Configuración', 'Competencias', actual.titulo]}
      subtitle={actual.subtitulo}
    >
      <Tabs
        variant="pill"
        size="md"
        aria-label="Secciones de competencias"
        items={PESTANAS.map((p) => ({ value: p.id, label: p.titulo }))}
        value={pestana}
        onValueChange={setPestana}
      />

      {pestana === 'plan-curricular' && <PlanCurricular />}
      {pestana === 'plan-evaluacion' && <PlanEvaluacion />}
      {pestana === 'escala' && <EscalaCalificacion />}
    </ContentLayout>
  );
};

export default AdministrativoCompetenciasRoute;
