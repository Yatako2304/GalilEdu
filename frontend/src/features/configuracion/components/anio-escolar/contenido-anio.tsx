import * as React from 'react';

import { AnioEscolarDetalle } from '@/types/api';

import { EstructuraHorariaPestana } from '../estructura-horaria/estructura-horaria-pestana';
import { SeccionesPestana } from '../secciones/secciones-pestana';

import { PeriodosPestana } from './periodos-pestana';
import { PestanaAnio, PestanasAnio } from './pestanas-anio';
import { RequisitosAnio } from './requisitos-anio';

type ContenidoAnioProps = { anio: AnioEscolarDetalle };

// Figma > CI-03, CI-06, CI-09 y CI-12: los requisitos (solo con el año PLANIFICADO), las pestañas del año y el
// contenido de la elegida. Abre en "Periodos".
export const ContenidoAnio = ({ anio }: ContenidoAnioProps) => {
  const [pestana, setPestana] = React.useState<PestanaAnio>('periodos');

  return (
    <>
      {anio.estado === 'PLANIFICADO' && (
        <RequisitosAnio anio={anio} onRevisar={setPestana} />
      )}

      <div className="flex flex-col gap-5">
        <PestanasAnio anio={anio} value={pestana} onValueChange={setPestana} />
        <div className="h-px w-full bg-stroke-tertiary" aria-hidden="true" />
      </div>

      {pestana === 'periodos' && <PeriodosPestana anio={anio} />}
      {pestana === 'secciones' && (
        <SeccionesPestana anioId={anio.id} estadoAnio={anio.estado} />
      )}
      {pestana === 'estructura-horaria' && (
        <EstructuraHorariaPestana anioId={anio.id} estadoAnio={anio.estado} />
      )}
    </>
  );
};
