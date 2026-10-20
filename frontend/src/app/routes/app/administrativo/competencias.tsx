import * as React from 'react';

import { ContentLayout } from '@/components/layouts';
import { ModulePlaceholder } from '@/components/module-placeholder';
import { PlanCurricular } from '@/features/catalogo-academico/components/plan-curricular';
import { cn } from '@/utils/cn';

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
      'Tabla de conversión de la nota vigesimal a los niveles de logro AD, A, B y C.',
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
      <div
        role="tablist"
        aria-label="Secciones de competencias"
        className="flex gap-2"
      >
        {PESTANAS.map((p) => (
          <button
            key={p.id}
            type="button"
            role="tab"
            aria-selected={p.id === pestana}
            onClick={() => setPestana(p.id)}
            className={cn(
              'rounded-lg px-4 py-2 text-body-3 transition-colors',
              p.id === pestana
                ? 'bg-deep-blue-800 text-white'
                : 'text-content-secondary hover:bg-surface-tertiary',
            )}
          >
            {p.titulo}
          </button>
        ))}
      </div>

      {pestana === 'plan-curricular' && <PlanCurricular />}
      {pestana === 'plan-evaluacion' && (
        <ModulePlaceholder
          feature="catalogo-academico"
          historias={['HU-56']}
          figmaNodeId="853:8120"
        />
      )}
      {pestana === 'escala' && (
        <ModulePlaceholder feature="catalogo-academico" historias={['HU-55']} />
      )}
    </ContentLayout>
  );
};

export default AdministrativoCompetenciasRoute;
